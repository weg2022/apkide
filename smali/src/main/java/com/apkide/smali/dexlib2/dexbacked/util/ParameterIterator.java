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

package com.apkide.smali.dexlib2.dexbacked.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apkide.smali.dexlib2.base.BaseMethodParameter;
import com.apkide.smali.dexlib2.iface.Annotation;
import com.apkide.smali.dexlib2.iface.MethodParameter;
import com.google.common.collect.ImmutableSet;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ParameterIterator implements Iterator<MethodParameter> {
    private final Iterator<? extends CharSequence> parameterTypes;
    private final Iterator<? extends Set<? extends Annotation>> parameterAnnotations;
    private final Iterator<String> parameterNames;

    public ParameterIterator(@NonNull List<? extends CharSequence> parameterTypes,
                             @NonNull List<? extends Set<? extends Annotation>> parameterAnnotations,
                             @NonNull Iterator<String> parameterNames) {
        this.parameterTypes = parameterTypes.iterator();
        this.parameterAnnotations = parameterAnnotations.iterator();
        this.parameterNames = parameterNames;
    }

    @Override public boolean hasNext() {
        return parameterTypes.hasNext();
    }

    @Override public MethodParameter next() {
        @NonNull final String type = parameterTypes.next().toString();
        @NonNull final Set<? extends Annotation> annotations;
        @Nullable final String name;

        if (parameterAnnotations.hasNext()) {
            annotations = parameterAnnotations.next();
        } else {
            annotations = ImmutableSet.of();
        }

        if (parameterNames.hasNext()) {
            name = parameterNames.next();
        } else {
            name = null;
        }

        return new BaseMethodParameter() {
            @NonNull @Override public Set<? extends Annotation> getAnnotations() { return annotations; }
            @Nullable @Override public String getName() { return name; }
            @NonNull @Override public String getType() { return type; }
        };
    }

    @Override public void remove() {
        throw new UnsupportedOperationException();
    }
}
