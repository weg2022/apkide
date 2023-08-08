package com.apkide.common;

import androidx.annotation.NonNull;

public abstract class FileNameMatcher {

    private static final Object myLock = new Object();

    private static FileNameMatcher sMatcher = new DefaultFileNameMatcher();

    public static void set(FileNameMatcher matcher) {
        synchronized (myLock) {
            sMatcher = matcher;
        }
    }

    public static FileNameMatcher get() {
        synchronized (myLock) {
            return sMatcher;
        }
    }

    public abstract boolean match(@NonNull String fileName,@NonNull String pattern);

    public static class DefaultFileNameMatcher extends FileNameMatcher {
        @Override
        public boolean match(@NonNull String fileName, @NonNull String pattern) {
            if (pattern.startsWith("*")) {
                return fileName.toLowerCase().endsWith(pattern.substring(1).toLowerCase());
            }
            return fileName.equalsIgnoreCase(pattern)||fileName.toLowerCase().endsWith(pattern.toLowerCase());
        }
    }
}