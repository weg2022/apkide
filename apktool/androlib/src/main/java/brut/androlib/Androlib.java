/*
 *  Copyright (C) 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
 *  Copyright (C) 2010 Connor Tumbleson <connor.tumbleson@gmail.com>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package brut.androlib;

import com.android.tools.smali.dexlib2.iface.DexFile;
import com.apkide.common.FileUtils;

import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import brut.androlib.meta.MetaInfo;
import brut.androlib.meta.UsesFramework;
import brut.androlib.options.BuildOptions;
import brut.androlib.res.AndrolibResources;
import brut.androlib.res.data.ResConfigFlags;
import brut.androlib.res.data.ResPackage;
import brut.androlib.res.data.ResTable;
import brut.androlib.res.data.ResUnknownFiles;
import brut.androlib.res.xml.ResXmlPatcher;
import brut.androlib.src.SmaliBuilder;
import brut.androlib.src.SmaliDecoder;
import brut.common.BrutException;
import brut.common.InvalidUnknownFileException;
import brut.common.RootUnknownFileException;
import brut.common.TraversalUnknownFileException;
import brut.directory.Directory;
import brut.directory.DirectoryException;
import brut.directory.ExtFile;
import brut.util.BrutIO;
import brut.util.Logger;
import brut.util.OS;

public class Androlib {
    public static final String VERSION="2.7.0";
    private final AndrolibResources mAndRes = new AndrolibResources();
    protected final ResUnknownFiles mResUnknownFiles = new ResUnknownFiles();
    private int mMinSdkVersion = 0;

    public Androlib() {
    }

    public ResTable getResTable(ExtFile apkFile)
            throws AndrolibException {
        return mAndRes.getResTable(apkFile, true);
    }

    public ResTable getResTable(ExtFile apkFile, boolean loadMainPkg)
            throws AndrolibException {
        return mAndRes.getResTable(apkFile, loadMainPkg);
    }

    public int getMinSdkVersion() {
        return mMinSdkVersion;
    }

    public void decodeSourcesRaw(ExtFile apkFile, File outDir, String filename)
            throws AndrolibException {
        try {
            Logger.get().info("Copying raw " + filename + " file...");
            apkFile.getDirectory().copyToDir(outDir, filename);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeSourcesSmali(File apkFile, File outDir, String filename, boolean bakDeb, int apiLevel)
            throws AndrolibException {
        File smaliDir;
        if (filename.equalsIgnoreCase("classes.dex")) {
            smaliDir = new File(outDir, SMALI_DIRNAME);
        } else {
            smaliDir = new File(outDir, SMALI_DIRNAME + "_" + filename.substring(0, filename.indexOf(".")));
        }
        OS.rmdir(smaliDir);
        smaliDir.mkdirs();
        Logger.get().info("Baksmaling " + filename + "...");
        DexFile dexFile = SmaliDecoder.decode(apkFile, smaliDir, filename, bakDeb, apiLevel);
        int minSdkVersion = dexFile.getOpcodes().api;
        if (mMinSdkVersion == 0 || mMinSdkVersion > minSdkVersion) {
            mMinSdkVersion = minSdkVersion;
        }
    }

    public void decodeManifestRaw(ExtFile apkFile, File outDir)
            throws AndrolibException {
        try {
            Logger.get().info("Copying raw manifest...");
            apkFile.getDirectory().copyToDir(outDir, APK_MANIFEST_FILENAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeManifestFull(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        mAndRes.decodeManifest(resTable, apkFile, outDir);
    }

    public void decodeResourcesRaw(ExtFile apkFile, File outDir)
            throws AndrolibException {
        try {
            Logger.get().info("Copying raw resources...");
            apkFile.getDirectory().copyToDir(outDir, APK_RESOURCES_FILENAMES);
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void decodeResourcesFull(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        mAndRes.decode(resTable, apkFile, outDir);
    }

    public void decodeManifestWithResources(ExtFile apkFile, File outDir, ResTable resTable)
            throws AndrolibException {
        mAndRes.decodeManifestWithResources(resTable, apkFile, outDir);
    }

    public void decodeRawFiles(ExtFile apkFile, File outDir, short decodeAssetMode)
            throws AndrolibException {
        Logger.get().info("Copying assets and libs...");
        try {
            Directory in = apkFile.getDirectory();

            if (decodeAssetMode == ApkDecoder.DECODE_ASSETS_FULL) {
                if (in.containsDir("assets")) {
                    in.copyToDir(outDir, "assets");
                }
            }
            if (in.containsDir("lib")) {
                in.copyToDir(outDir, "lib");
            }
            if (in.containsDir("libs")) {
                in.copyToDir(outDir, "libs");
            }
            if (in.containsDir("kotlin")) {
                in.copyToDir(outDir, "kotlin");
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void recordUncompressedFiles(ExtFile apkFile, Collection<String> uncompressedFilesOrExts) throws AndrolibException {
        try {
            Directory unk = apkFile.getDirectory();
            Set<String> files = unk.getFiles(true);

            for (String file : files) {
                if (isAPKFileNames(file) && unk.getCompressionLevel(file) == 0) {
                    String extOrFile = "";
                    if (unk.getSize(file) != 0) {
                        extOrFile = FileUtils.getExtension(file);
                    }

                    if (extOrFile.isEmpty() || !NO_COMPRESS_PATTERN.matcher(extOrFile).find()) {
                        extOrFile = file;
                        if (mAndRes.mResFileMapping.containsKey(extOrFile)) {
                            extOrFile = mAndRes.mResFileMapping.get(extOrFile);
                        }
                    }
                    if (!uncompressedFilesOrExts.contains(extOrFile)) {
                        uncompressedFilesOrExts.add(extOrFile);
                    }
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    private boolean isAPKFileNames(String file) {
        for (String apkFile : APK_STANDARD_ALL_FILENAMES) {
            if (apkFile.equals(file) || file.startsWith(apkFile + "/")) {
                return true;
            }
        }
        return false;
    }

    public void decodeUnknownFiles(ExtFile apkFile, File outDir)
            throws AndrolibException {
        Logger.get().info("Copying unknown files...");
        File unknownOut = new File(outDir, UNK_DIRNAME);
        try {
            Directory unk = apkFile.getDirectory();

            // loop all items in container recursively, ignoring any that are pre-defined by aapt
            Set<String> files = unk.getFiles(true);
            for (String file : files) {
                if (!isAPKFileNames(file) && !file.endsWith(".dex")) {

                    // copy file out of archive into special "unknown" folder
                    unk.copyToDir(unknownOut, file);
                    // lets record the name of the file, and its compression type
                    // so that we may re-include it the same way
                    mResUnknownFiles.addUnknownFileInfo(file, String.valueOf(unk.getCompressionLevel(file)));
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void writeOriginalFiles(ExtFile apkFile, File outDir)
            throws AndrolibException {
        Logger.get().info("Copying original files...");
        File originalDir = new File(outDir, "original");
        if (!originalDir.exists()) {
            originalDir.mkdirs();
        }

        try {
            Directory in = apkFile.getDirectory();
            if (in.containsFile("AndroidManifest.xml")) {
                in.copyToDir(originalDir, "AndroidManifest.xml");
            }
            if (in.containsFile("stamp-cert-sha256")) {
                in.copyToDir(originalDir, "stamp-cert-sha256");
            }
            if (in.containsDir("META-INF")) {
                in.copyToDir(originalDir, "META-INF");

                if (in.containsDir("META-INF/services")) {
                    // If the original APK contains the folder META-INF/services folder
                    // that is used for service locators (like coroutines on android),
                    // copy it to the destination folder so it does not get dropped.
                    Logger.get().info("Copying META-INF/services directory");
                    in.copyToDir(outDir, "META-INF/services");
                }
            }
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void writeMetaFile(File mOutDir, MetaInfo meta)
            throws AndrolibException {
        try {
            meta.save(new File(mOutDir, "apktool.yml"));
        } catch (IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public MetaInfo readMetaFile(ExtFile appDir)
            throws AndrolibException {
        try(
                InputStream in = appDir.getDirectory().getFileInput("apktool.yml")
        ) {
            return MetaInfo.load(in);
        } catch (DirectoryException | IOException ex) {
            throw new AndrolibException(ex);
        }
    }

    public void build(File appDir, File outFile) throws BrutException {
        build(new ExtFile(appDir), outFile);
    }

    public void build(ExtFile appDir, File outFile) throws BrutException {
        Logger.get().info("Using Apktool " + Androlib.getVersion());

        MetaInfo meta = readMetaFile(appDir);
        BuildOptions.get().setIsFramework(meta.isFrameworkApk);
        BuildOptions.get().setResourceAreCompressed(meta.compressionType);
        BuildOptions.get().setDoNotCompressResources(meta.doNotCompress);

        mAndRes.setSdkInfo(meta.sdkInfo);
        mAndRes.setPackageId(meta.packageInfo);
        mAndRes.setPackageRenamed(meta.packageInfo);
        mAndRes.setVersionInfo(meta.versionInfo);
        mAndRes.setSharedLibrary(meta.sharedLibrary);
        mAndRes.setSparseResources(meta.sparseResources);

        if (meta.sdkInfo != null && meta.sdkInfo.get("minSdkVersion") != null) {
            String minSdkVersion = meta.sdkInfo.get("minSdkVersion");
            mMinSdkVersion = mAndRes.getMinSdkVersionFromAndroidCodename(meta, minSdkVersion);
        }

        if (outFile == null) {
            String outFileName = meta.apkFileName;
            outFile = new File(appDir, "dist" + File.separator + (outFileName == null ? "out.apk" : outFileName));
        }

        new File(appDir, APK_DIRNAME).mkdirs();
        File manifest = new File(appDir, "AndroidManifest.xml");
        File manifestOriginal = new File(appDir, "AndroidManifest.xml.orig");

        buildSources(appDir);
        buildNonDefaultSources(appDir);
        buildManifestFile(appDir, manifest, manifestOriginal);
        buildResources(appDir, meta.usesFramework);
        buildLibs(appDir);
        buildCopyOriginalFiles(appDir);
        buildApk(appDir, outFile);

        // we must go after the Apk is built, and copy the files in via Zip
        // this is because Aapt won't add files it doesn't know (ex unknown files)
        buildUnknownFiles(appDir, outFile, meta);

        // we copied the AndroidManifest.xml to AndroidManifest.xml.orig so we can edit it
        // lets restore the unedited one, to not change the original
        if (manifest.isFile() && manifest.exists() && manifestOriginal.isFile()) {
            try {
                if (new File(appDir, "AndroidManifest.xml").delete()) {
                    FileUtils.copyFile(manifestOriginal,manifest);
                    manifestOriginal.delete();
                }
            } catch (IOException ex) {
                throw new AndrolibException(ex.getMessage());
            }
        }
        Logger.get().info("Built apk into: " + outFile.getPath());
    }

    private void buildManifestFile(File appDir, File manifest, File manifestOriginal)
            throws AndrolibException {

        // If we decoded in "raw", we cannot patch AndroidManifest
        if (new File(appDir, "resources.arsc").exists()) {
            return;
        }
        if (manifest.isFile() && manifest.exists()) {
            try {
                if (manifestOriginal.exists()) {
                    manifestOriginal.delete();
                }
               FileUtils.copyFile(manifest, manifestOriginal);
                ResXmlPatcher.fixingPublicAttrsInProviderAttributes(manifest);
            } catch (IOException ex) {
                throw new AndrolibException(ex.getMessage());
            }
        }
    }

    public void buildSources(File appDir)
            throws AndrolibException {
        if (!buildSourcesRaw(appDir, "classes.dex") && !buildSourcesSmali(appDir, "smali", "classes.dex")) {
            Logger.get().warning("Could not find sources");
        }
    }

    public void buildNonDefaultSources(ExtFile appDir)
            throws AndrolibException {
        try {
            // loop through any smali_ directories for multi-dex apks
            Map<String, Directory> dirs = appDir.getDirectory().getDirs();
            for (Map.Entry<String, Directory> directory : dirs.entrySet()) {
                String name = directory.getKey();
                if (name.startsWith("smali_")) {
                    String filename = name.substring(name.indexOf("_") + 1) + ".dex";

                    if (!buildSourcesRaw(appDir, filename) && !buildSourcesSmali(appDir, name, filename)) {
                        Logger.get().warning("Could not find sources");
                    }
                }
            }

            // loop through any classes#.dex files for multi-dex apks
            File[] dexFiles = appDir.listFiles();
            if (dexFiles != null) {
                for (File dex : dexFiles) {

                    // skip classes.dex because we have handled it in buildSources()
                    if (dex.getName().endsWith(".dex") && ! dex.getName().equalsIgnoreCase("classes.dex")) {
                        buildSourcesRaw(appDir, dex.getName());
                    }
                }
            }
        } catch(DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildSourcesRaw(File appDir, String filename)
            throws AndrolibException {
        File working = new File(appDir, filename);
        if (!working.exists()) {
            return false;
        }
        File stored = new File(appDir, APK_DIRNAME + "/" + filename);
        if (BuildOptions.get().isForceBuildAll() || isModified(working, stored)) {
            Logger.get().info("Copying " + appDir.toString() + " " + filename + " file...");
            try {
                BrutIO.copyAndClose(Files.newInputStream(working.toPath()), Files.newOutputStream(stored.toPath()));
                return true;
            } catch (IOException ex) {
                throw new AndrolibException(ex);
            }
        }
        return true;
    }

    public boolean buildSourcesSmali(File appDir, String folder, String filename) throws AndrolibException {
        ExtFile smaliDir = new ExtFile(appDir, folder);
        if (!smaliDir.exists()) {
            return false;
        }
        File dex = new File(appDir, APK_DIRNAME + "/" + filename);
        if (! BuildOptions.get().isForceBuildAll()) {
            Logger.get().info("Checking whether sources has changed...");
        }
        if (BuildOptions.get().isForceBuildAll() || isModified(smaliDir, dex)) {
            Logger.get().info("Smaling " + folder + " folder into " + filename + "...");
            dex.delete();
            SmaliBuilder.build(smaliDir, dex, BuildOptions.get().getForceApi() > 0 ? BuildOptions.get().getForceApi() : mMinSdkVersion);
        }
        return true;
    }

    public void buildResources(ExtFile appDir, UsesFramework usesFramework)
            throws BrutException {
        if (!buildResourcesRaw(appDir) && !buildResourcesFull(appDir, usesFramework)
                && !buildManifest(appDir, usesFramework)) {
            Logger.get().warning("Could not find resources");
        }
    }

    public boolean buildResourcesRaw(ExtFile appDir)
            throws AndrolibException {
        try {
            if (!new File(appDir, "resources.arsc").exists()) {
                return false;
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            if (!BuildOptions.get().isForceBuildAll()) {
                Logger.get().info("Checking whether resources has changed...");
            }
            if (BuildOptions.get().isForceBuildAll() || isModified(newFiles(APK_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir))) {
                Logger.get().info("Copying raw resources...");
                appDir.getDirectory().copyToDir(apkDir, APK_RESOURCES_FILENAMES);
            }
            return true;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildResourcesFull(File appDir, UsesFramework usesFramework)
            throws AndrolibException {
        try {
            if (!new File(appDir, "res").exists()) {
                return false;
            }
            if (! BuildOptions.get().isForceBuildAll()) {
                Logger.get().info("Checking whether resources has changed...");
            }
            File apkDir = new File(appDir, APK_DIRNAME);
            File resourceFile = new File(apkDir.getParent(), "resources.zip");

            if (BuildOptions.get().isForceBuildAll()|| isModified(newFiles(APP_RESOURCES_FILENAMES, appDir),
                    newFiles(APK_RESOURCES_FILENAMES, apkDir)) || (BuildOptions.get().isUseAapt2() && !isFile(resourceFile))) {
                Logger.get().info("Building resources...");

                if (BuildOptions.get().isDebugMode()) {
                    if (BuildOptions.get().isUseAapt2()) {
                        Logger.get().info("Using aapt2 - setting 'debuggable' attribute to 'true' in AndroidManifest.xml");
                        ResXmlPatcher.setApplicationDebugTagTrue(new File(appDir, "AndroidManifest.xml"));
                    } else {
                        ResXmlPatcher.removeApplicationDebugTag(new File(appDir, "AndroidManifest.xml"));
                    }
                }

                if (BuildOptions.get().isNetSecConf()) {
                    MetaInfo meta = readMetaFile(new ExtFile(appDir));
                    if (meta.sdkInfo != null && meta.sdkInfo.get("targetSdkVersion") != null) {
                        if (Integer.parseInt(Objects.requireNonNull(meta.sdkInfo.get("targetSdkVersion"))) < ResConfigFlags.SDK_NOUGAT) {
                            Logger.get().warning("Target SDK version is lower than 24! Network Security Configuration might be ignored!");
                        }
                    }
                    File netSecConfOrig = new File(appDir, "res/xml/network_security_config.xml");
                    if (netSecConfOrig.exists()) {
                        Logger.get().info("Replacing existing network_security_config.xml!");
                        netSecConfOrig.delete();
                    }
                    ResXmlPatcher.modNetworkSecurityConfig(netSecConfOrig);
                    ResXmlPatcher.setNetworkSecurityConfig(new File(appDir, "AndroidManifest.xml"));
                    Logger.get().info("Added permissive network security config in manifest");
                }

                File apkFile = File.createTempFile("APKTOOL", null);
                apkFile.delete();
                resourceFile.delete();

                File ninePatch = new File(appDir, "9patch");
                if (!ninePatch.exists()) {
                    ninePatch = null;
                }
                mAndRes.aaptPackage(apkFile, new File(appDir,
                                "AndroidManifest.xml"), new File(appDir, "res"),
                        ninePatch, null, parseUsesFramework(usesFramework));

                ExtFile tmpExtFile = new ExtFile(apkFile);
                Directory tmpDir = tmpExtFile.getDirectory();

                // Sometimes an application is built with a resources.arsc file with no resources,
                // Apktool assumes it will have a rebuilt arsc file, when it doesn't. So if we
                // encounter a copy error, move to a warning and continue on. (#1730)
                try {
                    tmpDir.copyToDir(apkDir,
                            tmpDir.containsDir("res") ? APK_RESOURCES_FILENAMES
                                    : APK_RESOURCES_WITHOUT_RES_FILENAMES);
                } catch (DirectoryException ex) {
                    Logger.get().warning(ex.getMessage());
                } finally {
                    tmpExtFile.close();
                }

                // delete tmpDir
                apkFile.delete();
            }
            return true;
        } catch (IOException | ParserConfigurationException | TransformerException | SAXException ex) {
            throw new AndrolibException(ex);
        } catch (DirectoryException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean buildManifestRaw(ExtFile appDir)
            throws AndrolibException {
        try {
            File apkDir = new File(appDir, APK_DIRNAME);
            Logger.get().info("Copying raw AndroidManifest.xml...");
            appDir.getDirectory().copyToDir(apkDir, APK_MANIFEST_FILENAMES);
            return true;
        } catch (DirectoryException ex) {
            throw new AndrolibException(ex);
        }
    }

    public boolean buildManifest(ExtFile appDir, UsesFramework usesFramework)
            throws BrutException {
        try {
            if (!new File(appDir, "AndroidManifest.xml").exists()) {
                return false;
            }
            if (! BuildOptions.get().isForceBuildAll()) {
                Logger.get().info("Checking whether resources has changed...");
            }

            File apkDir = new File(appDir, APK_DIRNAME);

            if (BuildOptions.get().isForceBuildAll() || isModified(newFiles(APK_MANIFEST_FILENAMES, appDir),
                    newFiles(APK_MANIFEST_FILENAMES, apkDir))) {
                Logger.get().info("Building AndroidManifest.xml...");

                File apkFile = File.createTempFile("APKTOOL", null);
                apkFile.delete();

                File ninePatch = new File(appDir, "9patch");
                if (!ninePatch.exists()) {
                    ninePatch = null;
                }

                mAndRes.aaptPackage(apkFile, new File(appDir,
                                "AndroidManifest.xml"), null, ninePatch, null,
                        parseUsesFramework(usesFramework));

                Directory tmpDir = new ExtFile(apkFile).getDirectory();
                tmpDir.copyToDir(apkDir, APK_MANIFEST_FILENAMES);

                apkFile.delete();
            }
            return true;
        } catch (IOException | DirectoryException ex) {
            throw new AndrolibException(ex);
        } catch (AndrolibException ex) {
            Logger.get().warning("Parse AndroidManifest.xml failed, treat it as raw file.");
            return buildManifestRaw(appDir);
        }
    }

    public void buildLibs(File appDir) throws AndrolibException {
        buildLibrary(appDir, "lib");
        buildLibrary(appDir, "libs");
        buildLibrary(appDir, "kotlin");
        buildLibrary(appDir, "META-INF/services");
    }

    public void buildLibrary(File appDir, String folder) throws AndrolibException {
        File working = new File(appDir, folder);

        if (! working.exists()) {
            return;
        }

        File stored = new File(appDir, APK_DIRNAME + "/" + folder);
        if (BuildOptions.get().isForceBuildAll() || isModified(working, stored)) {
            Logger.get().info("Copying libs... (/" + folder + ")");
            try {
                OS.rmdir(stored);
                OS.cpdir(working, stored);
            } catch (BrutException ex) {
                throw new AndrolibException(ex);
            }
        }
    }

    public void buildCopyOriginalFiles(File appDir)
            throws AndrolibException {
        if (BuildOptions.get().isCopyOriginalFiles()) {
            File originalDir = new File(appDir, "original");
            if (originalDir.exists()) {
                try {
                    Logger.get().info("Copy original files...");
                    Directory in = (new ExtFile(originalDir)).getDirectory();
                    if (in.containsFile("AndroidManifest.xml")) {
                        Logger.get().info("Copy AndroidManifest.xml...");
                        in.copyToDir(new File(appDir, APK_DIRNAME), "AndroidManifest.xml");
                    }
                    if (in.containsFile("stamp-cert-sha256")) {
                        Logger.get().info("Copy stamp-cert-sha256...");
                        in.copyToDir(new File(appDir, APK_DIRNAME), "stamp-cert-sha256");
                    }
                    if (in.containsDir("META-INF")) {
                        Logger.get().info("Copy META-INF...");
                        in.copyToDir(new File(appDir, APK_DIRNAME), "META-INF");
                    }
                } catch (DirectoryException ex) {
                    throw new AndrolibException(ex);
                }
            }
        }
    }

    public void buildUnknownFiles(File appDir, File outFile, MetaInfo meta)
            throws AndrolibException {
        if (meta.unknownFiles != null) {
            Logger.get().info("Copying unknown files/dir...");

            Map<String, String> files = meta.unknownFiles;
            File tempFile = new File(outFile.getParent(), outFile.getName() + ".apktool_temp");
            boolean renamed = outFile.renameTo(tempFile);
            if (!renamed) {
                throw new AndrolibException("Unable to rename temporary file");
            }

            try (
                    ZipFile inputFile = new ZipFile(tempFile);
                    ZipOutputStream actualOutput = new ZipOutputStream(Files.newOutputStream(outFile.toPath()))
            ) {
                copyExistingFiles(inputFile, actualOutput);
                copyUnknownFiles(appDir, actualOutput, files);
            } catch (IOException | BrutException ex) {
                throw new AndrolibException(ex);
            }

            // Remove our temporary file.
            tempFile.delete();
        }
    }

    private void copyExistingFiles(ZipFile inputFile, ZipOutputStream outputFile) throws IOException {
        // First, copy the contents from the existing outFile:
        Enumeration<? extends ZipEntry> entries = inputFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = new ZipEntry(entries.nextElement());

            // We can't reuse the compressed size because it depends on compression sizes.
            entry.setCompressedSize(-1);
            outputFile.putNextEntry(entry);

            // No need to create directory entries in the final apk
            if (! entry.isDirectory()) {
                BrutIO.copy(inputFile, outputFile, entry);
            }

            outputFile.closeEntry();
        }
    }

    private void copyUnknownFiles(File appDir, ZipOutputStream outputFile, Map<String, String> files)
            throws BrutException, IOException {
        File unknownFileDir = new File(appDir, UNK_DIRNAME);

        // loop through unknown files
        for (Map.Entry<String,String> unknownFileInfo : files.entrySet()) {
            File inputFile;

            try {
                inputFile = new File(unknownFileDir, BrutIO.sanitizeUnknownFile(unknownFileDir, unknownFileInfo.getKey()));
            } catch (RootUnknownFileException | InvalidUnknownFileException | TraversalUnknownFileException exception) {
                Logger.get().warning(String.format("Skipping file %s (%s)", unknownFileInfo.getKey(), exception.getMessage()));
                continue;
            }

            if (inputFile.isDirectory()) {
                continue;
            }

            ZipEntry newEntry = new ZipEntry(unknownFileInfo.getKey());
            int method = Integer.parseInt(unknownFileInfo.getValue());
            Logger.get().info(String.format("Copying unknown file %s with method %d", unknownFileInfo.getKey(), method));
            if (method == ZipEntry.STORED) {
                newEntry.setMethod(ZipEntry.STORED);
                newEntry.setSize(inputFile.length());
                newEntry.setCompressedSize(-1);
                BufferedInputStream unknownFile = new BufferedInputStream(Files.newInputStream(inputFile.toPath()));
                CRC32 crc = BrutIO.calculateCrc(unknownFile);
                newEntry.setCrc(crc.getValue());
                unknownFile.close();
            } else {
                newEntry.setMethod(ZipEntry.DEFLATED);
            }
            outputFile.putNextEntry(newEntry);

            BrutIO.copy(inputFile, outputFile);
            outputFile.closeEntry();
        }
    }

    public void buildApk(File appDir, File outApk) throws AndrolibException {
        Logger.get().info("Building apk file...");
        if (outApk.exists()) {
            outApk.delete();
        } else {
            File outDir = outApk.getParentFile();
            if (outDir != null && !outDir.exists()) {
                outDir.mkdirs();
            }
        }
        File assetDir = new File(appDir, "assets");
        if (!assetDir.exists()) {
            assetDir = null;
        }
        mAndRes.zipPackage(outApk, new File(appDir, APK_DIRNAME), assetDir);
    }

    public void publicizeResources(File arscFile) throws AndrolibException {
        mAndRes.publicizeResources(arscFile);
    }

    public void installFramework(File frameFile)
            throws AndrolibException {
        mAndRes.installFramework(frameFile);
    }

    public void listFrameworks() throws AndrolibException {
        mAndRes.listFrameworkDirectory();
    }

    public void emptyFrameworkDirectory() throws AndrolibException {
        mAndRes.emptyFrameworkDirectory();
    }

    public boolean isFrameworkApk(ResTable resTable) {
        for (ResPackage pkg : resTable.listMainPackages()) {
            if (pkg.getId() > 0 && pkg.getId() < 64) {
                return true;
            }
        }
        return false;
    }

    public static String getVersion() {
        return VERSION;
    }

    private File[] parseUsesFramework(UsesFramework usesFramework)
            throws AndrolibException {
        if (usesFramework == null) {
            return null;
        }

        List<Integer> ids = usesFramework.ids;
        if (ids == null || ids.isEmpty()) {
            return null;
        }

        String tag = usesFramework.tag;
        File[] files = new File[ids.size()];
        int i = 0;
        for (int id : ids) {
            files[i++] = mAndRes.getFrameworkApk(id, tag);
        }
        return files;
    }

    private boolean isModified(File working, File stored) {
        return ! stored.exists() || BrutIO.recursiveModifiedTime(working) > BrutIO .recursiveModifiedTime(stored);
    }

    private boolean isFile(File working) {
        return working.exists();
    }

    private boolean isModified(File[] working, File[] stored) {
        for (File file : stored) {
            if (!file.exists()) {
                return true;
            }
        }
        return BrutIO.recursiveModifiedTime(working) > BrutIO.recursiveModifiedTime(stored);
    }

    private File[] newFiles(String[] names, File dir) {
        File[] files = new File[names.length];
        for (int i = 0; i < names.length; i++) {
            files[i] = new File(dir, names[i]);
        }
        return files;
    }

    public void close() throws IOException {
        mAndRes.close();
    }

  
    private final static String SMALI_DIRNAME = "smali";
    private final static String APK_DIRNAME = "build/apk";
    private final static String UNK_DIRNAME = "unknown";
    private final static String[] APK_RESOURCES_FILENAMES = new String[] {
            "resources.arsc", "AndroidManifest.xml", "res", "r", "R" };
    private final static String[] APK_RESOURCES_WITHOUT_RES_FILENAMES = new String[] {
            "resources.arsc", "AndroidManifest.xml" };
    private final static String[] APP_RESOURCES_FILENAMES = new String[] {
            "AndroidManifest.xml", "res" };
    private final static String[] APK_MANIFEST_FILENAMES = new String[] {
            "AndroidManifest.xml" };
    private final static String[] APK_STANDARD_ALL_FILENAMES = new String[] {
            "classes.dex", "AndroidManifest.xml", "resources.arsc", "res", "r", "R",
            "lib", "libs", "assets", "META-INF", "kotlin" };
    private final static Pattern NO_COMPRESS_PATTERN = Pattern.compile("(" +
            "jpg|jpeg|png|gif|wav|mp2|mp3|ogg|aac|mpg|mpeg|mid|midi|smf|jet|rtttl|imy|xmf|mp4|" +
            "m4a|m4v|3gp|3gpp|3g2|3gpp2|amr|awb|wma|wmv|webm|webp|mkv)$");
}