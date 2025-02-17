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

package com.apkide.smali.dexlib2.builder.instruction;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apkide.smali.dexlib2.Format;
import com.apkide.smali.dexlib2.Opcode;
import com.apkide.smali.dexlib2.builder.BuilderInstruction;
import com.apkide.smali.dexlib2.iface.instruction.formats.ArrayPayload;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class BuilderArrayPayload extends BuilderInstruction implements ArrayPayload {
    public static final Opcode OPCODE = Opcode.ARRAY_PAYLOAD;

    protected final int elementWidth;
    @NonNull
    protected final List<Number> arrayElements;

    public BuilderArrayPayload(int elementWidth,
                               @Nullable List<Number> arrayElements) {
        super(OPCODE);
        this.elementWidth = elementWidth;
        this.arrayElements = arrayElements==null?ImmutableList.<Number>of():arrayElements;
    }

    @Override public int getElementWidth() { return elementWidth; }
    @NonNull @Override public List<Number> getArrayElements() { return arrayElements; }

    @Override public int getCodeUnits() { return 4 + (elementWidth * arrayElements.size() + 1) / 2; }
    @Override public Format getFormat() { return OPCODE.format; }
}
