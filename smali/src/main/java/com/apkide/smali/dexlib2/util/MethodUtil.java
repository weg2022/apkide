/*
 * Copyright 2012, Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.apkide.smali.dexlib2.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apkide.smali.dexlib2.AccessFlags;
import com.apkide.smali.dexlib2.iface.Method;
import com.apkide.smali.dexlib2.iface.reference.MethodReference;
import com.apkide.smali.util.CharSequenceUtils;
import com.google.common.base.Predicate;

import java.util.Collection;

public final class MethodUtil {
    private static int directMask = AccessFlags.STATIC.getValue() | AccessFlags.PRIVATE.getValue() |
            AccessFlags.CONSTRUCTOR.getValue();

    public static Predicate<Method> METHOD_IS_DIRECT = new Predicate<Method>() {
        @Override public boolean apply(@Nullable Method input) {
            return input != null && isDirect(input);
        }
    };

    public static Predicate<Method> METHOD_IS_VIRTUAL = new Predicate<Method>() {
        @Override public boolean apply(@Nullable Method input) {
            return input != null && !isDirect(input);
        }
    };

    public static boolean isDirect(@NonNull Method method) {
        return (method.getAccessFlags() & directMask) != 0;
    }

    public static boolean isStatic(@NonNull Method method) {
        return AccessFlags.STATIC.isSet(method.getAccessFlags());
    }

    public static boolean isConstructor(@NonNull MethodReference methodReference) {
        return methodReference.getName().equals("<init>");
    }

    public static boolean isPackagePrivate(@NonNull Method method) {
        return (method.getAccessFlags() & (AccessFlags.PRIVATE.getValue() |
                AccessFlags.PROTECTED.getValue() |
                AccessFlags.PUBLIC.getValue())) == 0;
    }

    public static int getParameterRegisterCount(@NonNull Method method) {
        return getParameterRegisterCount(method, MethodUtil.isStatic(method));
    }

    public static int getParameterRegisterCount(@NonNull MethodReference methodRef, boolean isStatic) {
        return getParameterRegisterCount(methodRef.getParameterTypes(), isStatic);
    }

    public static int getParameterRegisterCount(@NonNull Collection<? extends CharSequence> parameterTypes,
                                                boolean isStatic) {
        int regCount = 0;
        for (CharSequence paramType: parameterTypes) {
            int firstChar = paramType.charAt(0);
            if (firstChar == 'J' || firstChar == 'D') {
                regCount += 2;
            } else {
                regCount++;
            }
        }
        if (!isStatic) {
            regCount++;
        }
        return regCount;
    }

    private static char getShortyType(CharSequence type) {
        if (type.length() > 1) {
            return 'L';
        }
        return type.charAt(0);
    }

    public static String getShorty(Collection<? extends CharSequence> params, String returnType) {
        StringBuilder sb = new StringBuilder(params.size() + 1);
        sb.append(getShortyType(returnType));
        for (CharSequence typeRef: params) {
            sb.append(getShortyType(typeRef));
        }
        return sb.toString();
    }

    public static boolean methodSignaturesMatch(@NonNull MethodReference a, @NonNull MethodReference b) {
        return (a.getName().equals(b.getName()) &&
                a.getReturnType().equals(b.getReturnType()) &&
                CharSequenceUtils.listEquals(a.getParameterTypes(), b.getParameterTypes()));
    }

    private MethodUtil() {}
}
