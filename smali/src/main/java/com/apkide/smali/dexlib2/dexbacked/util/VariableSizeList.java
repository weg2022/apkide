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

import com.apkide.smali.dexlib2.dexbacked.DexBuffer;
import com.apkide.smali.dexlib2.dexbacked.DexReader;

import java.util.AbstractSequentialList;

public abstract class VariableSizeList<T> extends AbstractSequentialList<T> {
    @NonNull
    private final DexBuffer buffer;
    private final int offset;
    private final int size;

    public VariableSizeList(@NonNull DexBuffer buffer, int offset, int size) {
        this.buffer = buffer;
        this.offset = offset;
        this.size = size;
    }

    protected abstract T readNextItem(@NonNull DexReader reader, int index);

    @Override
    @NonNull
    public VariableSizeListIterator<T> listIterator() {
        return listIterator(0);
    }

    @Override public int size() { return size; }

    @NonNull
    @Override
    public VariableSizeListIterator<T> listIterator(int index) {
        VariableSizeListIterator<T> iterator = new VariableSizeListIterator<T>(buffer, offset, size) {
            @Override
            protected T readNextItem(@NonNull DexReader reader, int index) {
                return VariableSizeList.this.readNextItem(reader, index);
            }
        };
        for (int i=0; i<index; i++) {
            iterator.next();
        }
        return iterator;
    }
}
