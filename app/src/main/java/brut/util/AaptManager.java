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
package brut.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import brut.common.BrutException;

public class AaptManager {
	
	public static File getAapt2() throws BrutException {
		return getAapt(2);
	}
	
	public static File getAapt1() throws BrutException {
		return getAapt(1);
	}
	
	private static File getAapt(Integer version) throws BrutException {
		File aaptBinary;
		String aaptVersion = getAaptBinaryName(version);
		
		String prefix="bin"+File.separator;
		if (OSDetection.isAarch64())
			prefix += "aarch64";
		else if (OSDetection.isAarch32())
			prefix += "arm";
		else if (OSDetection.isX86())
			prefix += "x86";
		else if (OSDetection.isX86_64())
			prefix += "x86_64";
		else
			throw new BrutException("No binaries available.");
		
		
		String fileName = prefix + File.separator + "aapt" + aaptVersion;
		aaptBinary = SyncAssets.get().foundFile(fileName);
		
		if (!aaptBinary.exists())
			throw new BrutException("Can't found aapt binary");
		
		if (aaptBinary.setExecutable(true))
			return aaptBinary;
		
		throw new BrutException("Can't set aapt binary as executable");
	}
	
	public static String getAaptExecutionCommand(String aaptPath, File aapt) throws BrutException {
		if (!aaptPath.isEmpty()) {
			File aaptFile = new File(aaptPath);
			if (aaptFile.canRead() && aaptFile.exists()) {
				aaptFile.setExecutable(true);
				return aaptFile.getPath();
			} else {
				throw new BrutException("binary could not be read: " + aaptFile.getAbsolutePath());
			}
		} else {
			return aapt.getAbsolutePath();
		}
	}
	
	public static int getAaptVersion(String aaptLocation) throws BrutException {
		return getAaptVersion(new File(aaptLocation));
	}
	
	public static String getAaptBinaryName(Integer version) {
		return "aapt" + (version == 2 ? "2" : "");
	}
	
	public static int getAppVersionFromString(String version) throws BrutException {
		if (version.startsWith("Android Asset Packaging Tool (aapt) 2:")) {
			return 2;
		} else if (version.startsWith("Android Asset Packaging Tool (aapt) 2.")) {
			return 2; // Prior to Android SDK 26.0.2
		} else if (version.startsWith("Android Asset Packaging Tool, v0.")) {
			return 1;
		}
		
		throw new BrutException("aapt version could not be identified: " + version);
	}
	
	public static int getAaptVersion(File aapt) throws BrutException {
		if (!aapt.isFile())
			throw new BrutException("Could not identify aapt binary as executable.");
		
		aapt.setExecutable(true);
		
		List<String> cmd = new ArrayList<>();
		cmd.add(aapt.getAbsolutePath());
		cmd.add("version");
		
		String version = OS.execAndReturn(cmd.toArray(new String[0]));
		
		if (version == null)
			throw new BrutException("Could not execute aapt binary at location: " + aapt.getAbsolutePath());
		return getAppVersionFromString(version);
	}
}