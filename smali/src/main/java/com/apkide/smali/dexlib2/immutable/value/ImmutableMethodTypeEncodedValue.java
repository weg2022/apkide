/*
 * Copyright 2018, Google LLC
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

package com.apkide.smali.dexlib2.immutable.value;

import androidx.annotation.NonNull;

import com.apkide.smali.dexlib2.base.value.BaseMethodTypeEncodedValue;
import com.apkide.smali.dexlib2.iface.value.MethodTypeEncodedValue;
import com.apkide.smali.dexlib2.immutable.reference.ImmutableMethodProtoReference;

public class ImmutableMethodTypeEncodedValue extends BaseMethodTypeEncodedValue implements ImmutableEncodedValue {
    @NonNull
    protected final ImmutableMethodProtoReference methodProtoReference;

    public ImmutableMethodTypeEncodedValue(@NonNull ImmutableMethodProtoReference methodProtoReference) {
        this.methodProtoReference = methodProtoReference;
    }

    @NonNull
    public static ImmutableMethodTypeEncodedValue of(@NonNull MethodTypeEncodedValue methodTypeEncodedValue) {
        if (methodTypeEncodedValue instanceof ImmutableMethodTypeEncodedValue) {
            return (ImmutableMethodTypeEncodedValue) methodTypeEncodedValue;
        }
        return new ImmutableMethodTypeEncodedValue(
                ImmutableMethodProtoReference.of(methodTypeEncodedValue.getValue()));
    }

    @NonNull @Override public ImmutableMethodProtoReference getValue() { return methodProtoReference; }
}
