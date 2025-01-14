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

package com.apkide.smali.dexlib2.dexbacked;

import androidx.annotation.NonNull;

import com.apkide.smali.dexlib2.base.BaseAnnotation;
import com.apkide.smali.dexlib2.dexbacked.util.VariableSizeSet;

import java.util.Set;

public class DexBackedAnnotation extends BaseAnnotation {
    @NonNull
    public final DexBackedDexFile dexFile;

    public final int visibility;
    public final int typeIndex;
    private final int elementsOffset;

    public DexBackedAnnotation(@NonNull DexBackedDexFile dexFile,
                               int annotationOffset) {
        this.dexFile = dexFile;

        DexReader reader = dexFile.getDataBuffer().readerAt(annotationOffset);
        this.visibility = reader.readUbyte();
        this.typeIndex = reader.readSmallUleb128();
        this.elementsOffset = reader.getOffset();
    }

    @Override public int getVisibility() { return visibility; }
    @NonNull @Override public String getType() { return dexFile.getTypeSection().get(typeIndex); }

    @NonNull
    @Override
    public Set<? extends DexBackedAnnotationElement> getElements() {
        DexReader reader = dexFile.getDataBuffer().readerAt(elementsOffset);
        final int size = reader.readSmallUleb128();

        return new VariableSizeSet<DexBackedAnnotationElement>(dexFile.getDataBuffer(), reader.getOffset(), size) {
            @NonNull
            @Override
            protected DexBackedAnnotationElement readNextItem(@NonNull DexReader reader, int index) {
                return new DexBackedAnnotationElement(dexFile, reader);
            }
        };
    }
}
