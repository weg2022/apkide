/*
 * Copyright 2013, Google LLC
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

package com.apkide.smali.dexlib2.writer;

import androidx.annotation.NonNull;

import com.apkide.smali.dexlib2.Opcode;
import com.apkide.smali.dexlib2.Opcodes;
import com.apkide.smali.dexlib2.ReferenceType;
import com.apkide.smali.dexlib2.iface.instruction.DualReferenceInstruction;
import com.apkide.smali.dexlib2.iface.instruction.ReferenceInstruction;
import com.apkide.smali.dexlib2.iface.instruction.SwitchElement;
import com.apkide.smali.dexlib2.iface.instruction.formats.ArrayPayload;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction10t;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction10x;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction11n;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction11x;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction12x;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction20bc;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction20t;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction21c;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction21ih;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction21lh;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction21s;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction21t;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction22b;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction22c;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction22cs;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction22s;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction22t;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction22x;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction23x;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction30t;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction31c;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction31i;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction31t;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction32x;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction35c;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction35mi;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction35ms;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction3rc;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction3rmi;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction3rms;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction45cc;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction4rcc;
import com.apkide.smali.dexlib2.iface.instruction.formats.Instruction51l;
import com.apkide.smali.dexlib2.iface.instruction.formats.PackedSwitchPayload;
import com.apkide.smali.dexlib2.iface.instruction.formats.SparseSwitchPayload;
import com.apkide.smali.dexlib2.iface.reference.CallSiteReference;
import com.apkide.smali.dexlib2.iface.reference.FieldReference;
import com.apkide.smali.dexlib2.iface.reference.MethodHandleReference;
import com.apkide.smali.dexlib2.iface.reference.MethodProtoReference;
import com.apkide.smali.dexlib2.iface.reference.MethodReference;
import com.apkide.smali.dexlib2.iface.reference.Reference;
import com.apkide.smali.dexlib2.iface.reference.StringReference;
import com.apkide.smali.dexlib2.iface.reference.TypeReference;
import com.apkide.smali.util.ExceptionWithContext;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;

public class InstructionWriter<StringRef extends StringReference, TypeRef extends TypeReference,
        FieldRefKey extends FieldReference, MethodRefKey extends MethodReference,
        ProtoRefKey extends MethodProtoReference, MethodHandleKey extends MethodHandleReference,
        CallSiteKey extends CallSiteReference> {
    @NonNull
    private final Opcodes opcodes;
    @NonNull private final DexDataWriter writer;
    @NonNull private final StringSection<?, StringRef> stringSection;
    @NonNull private final TypeSection<?, ?, TypeRef> typeSection;
    @NonNull private final FieldSection<?, ?, FieldRefKey, ?> fieldSection;
    @NonNull private final MethodSection<?, ?, ?, MethodRefKey, ?> methodSection;
    @NonNull private final ProtoSection<?, ?, ProtoRefKey, ?> protoSection;
    @NonNull private final MethodHandleSection<MethodHandleKey, ?, ?> methodHandleSection;
    @NonNull private final CallSiteSection<CallSiteKey, ?> callSiteSection;

    @NonNull static <StringRef extends StringReference, TypeRef extends TypeReference,
            FieldRefKey extends FieldReference, MethodRefKey extends MethodReference,
            ProtoRefKey extends MethodProtoReference, MethodHandleKey extends MethodHandleReference,
            CallSiteKey extends CallSiteReference>
            InstructionWriter<StringRef, TypeRef, FieldRefKey, MethodRefKey, ProtoRefKey, MethodHandleKey, CallSiteKey>
            makeInstructionWriter(
                @NonNull Opcodes opcodes,
                @NonNull DexDataWriter writer,
                @NonNull StringSection<?, StringRef> stringSection,
                @NonNull TypeSection<?, ?, TypeRef> typeSection,
                @NonNull FieldSection<?, ?, FieldRefKey, ?> fieldSection,
                @NonNull MethodSection<?, ?, ?, MethodRefKey, ?> methodSection,
                @NonNull ProtoSection<?, ?, ProtoRefKey, ?> protoSection,
                @NonNull MethodHandleSection<MethodHandleKey, ?, ?> methodHandleSection,
                @NonNull CallSiteSection<CallSiteKey, ?> callSiteSection) {
        return new InstructionWriter<
                StringRef, TypeRef, FieldRefKey, MethodRefKey, ProtoRefKey, MethodHandleKey,CallSiteKey>(
                        opcodes, writer, stringSection, typeSection, fieldSection, methodSection, protoSection,
                        methodHandleSection, callSiteSection);
    }

    InstructionWriter(@NonNull Opcodes opcodes,
                      @NonNull DexDataWriter writer,
                      @NonNull StringSection<?, StringRef> stringSection,
                      @NonNull TypeSection<?, ?, TypeRef> typeSection,
                      @NonNull FieldSection<?, ?, FieldRefKey, ?> fieldSection,
                      @NonNull MethodSection<?, ?, ?, MethodRefKey, ?> methodSection,
                      @NonNull ProtoSection<?, ?, ProtoRefKey, ?> protoSection,
                      @NonNull MethodHandleSection<MethodHandleKey, ?, ?> methodHandleSection,
                      @NonNull CallSiteSection<CallSiteKey, ?> callSiteSection) {
        this.opcodes = opcodes;
        this.writer = writer;
        this.stringSection = stringSection;
        this.typeSection = typeSection;
        this.fieldSection = fieldSection;
        this.methodSection = methodSection;
        this.protoSection = protoSection;
        this.methodHandleSection = methodHandleSection;
        this.callSiteSection = callSiteSection;
    }

    private short getOpcodeValue(Opcode opcode) {
        Short value = opcodes.getOpcodeValue(opcode);
        if (value == null) {
            throw new ExceptionWithContext("Instruction %s is invalid for api %d", opcode.name, opcodes.api);
        }
        return value;
    }

    public void write(@NonNull Instruction10t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction10x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(0);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction11n instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getNarrowLiteral()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction11x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction12x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction20bc instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getVerificationError());
            writer.writeUshort(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction20t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(0);
            writer.writeShort(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction21c instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeUshort(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction21ih instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getHatLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction21lh instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getHatLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction21s instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction21t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeShort(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction22b instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.write(instruction.getRegisterB());
            writer.write(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction22c instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeUshort(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction22cs instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeUshort(instruction.getFieldOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction22s instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeShort(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction22t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterA(), instruction.getRegisterB()));
            writer.writeShort(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction22x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeUshort(instruction.getRegisterB());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction23x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.write(instruction.getRegisterB());
            writer.write(instruction.getRegisterC());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction30t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(0);
            writer.writeInt(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction31c instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeInt(getReferenceIndex(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction31i instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeInt(instruction.getNarrowLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction31t instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeInt(instruction.getCodeOffset());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction32x instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(0);
            writer.writeUshort(instruction.getRegisterA());
            writer.writeUshort(instruction.getRegisterB());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction35c instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterG(), instruction.getRegisterCount()));
            writer.writeUshort(getReferenceIndex(instruction));
            writer.write(packNibbles(instruction.getRegisterC(), instruction.getRegisterD()));
            writer.write(packNibbles(instruction.getRegisterE(), instruction.getRegisterF()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction35mi instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterG(), instruction.getRegisterCount()));
            writer.writeUshort(instruction.getInlineIndex());
            writer.write(packNibbles(instruction.getRegisterC(), instruction.getRegisterD()));
            writer.write(packNibbles(instruction.getRegisterE(), instruction.getRegisterF()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction35ms instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterG(), instruction.getRegisterCount()));
            writer.writeUshort(instruction.getVtableIndex());
            writer.write(packNibbles(instruction.getRegisterC(), instruction.getRegisterD()));
            writer.write(packNibbles(instruction.getRegisterE(), instruction.getRegisterF()));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction3rc instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterCount());
            writer.writeUshort(getReferenceIndex(instruction));
            writer.writeUshort(instruction.getStartRegister());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction3rmi instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterCount());
            writer.writeUshort(instruction.getInlineIndex());
            writer.writeUshort(instruction.getStartRegister());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }


    public void write(@NonNull Instruction3rms instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterCount());
            writer.writeUshort(instruction.getVtableIndex());
            writer.writeUshort(instruction.getStartRegister());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction45cc instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(packNibbles(instruction.getRegisterG(), instruction.getRegisterCount()));
            writer.writeUshort(getReferenceIndex(instruction));
            writer.write(packNibbles(instruction.getRegisterC(), instruction.getRegisterD()));
            writer.write(packNibbles(instruction.getRegisterE(), instruction.getRegisterF()));
            writer.writeUshort(getReference2Index(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction4rcc instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterCount());
            writer.writeUshort(getReferenceIndex(instruction));
            writer.writeUshort(instruction.getStartRegister());
            writer.writeUshort(getReference2Index(instruction));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull Instruction51l instruction) {
        try {
            writer.write(getOpcodeValue(instruction.getOpcode()));
            writer.write(instruction.getRegisterA());
            writer.writeLong(instruction.getWideLiteral());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull ArrayPayload instruction) {
        try {
            writer.writeUshort(getOpcodeValue(instruction.getOpcode()));
            writer.writeUshort(instruction.getElementWidth());
            List<Number> elements = instruction.getArrayElements();
            writer.writeInt(elements.size());
            switch (instruction.getElementWidth()) {
                case 1:
                    for (Number element: elements) {
                        writer.write(element.byteValue());
                    }
                    break;
                case 2:
                    for (Number element: elements) {
                        writer.writeShort(element.shortValue());
                    }
                    break;
                case 4:
                    for (Number element: elements) {
                        writer.writeInt(element.intValue());
                    }
                    break;
                case 8:
                    for (Number element: elements) {
                        writer.writeLong(element.longValue());
                    }
                    break;
            }
            if ((writer.getPosition() & 1) != 0) {
                writer.write(0);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void write(@NonNull SparseSwitchPayload instruction) {
        try {
            writer.writeUbyte(0);
            writer.writeUbyte(getOpcodeValue(instruction.getOpcode()) >> 8);
            List<? extends SwitchElement> elements = Ordering.from(switchElementComparator).immutableSortedCopy(
                    instruction.getSwitchElements());
            writer.writeUshort(elements.size());
            for (SwitchElement element: elements) {
                writer.writeInt(element.getKey());
            }
            for (SwitchElement element: elements) {
                writer.writeInt(element.getOffset());
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final Comparator<SwitchElement> switchElementComparator = new Comparator<SwitchElement>() {
        @Override public int compare(SwitchElement element1, SwitchElement element2) {
            return Ints.compare(element1.getKey(), element2.getKey());
        }
    };

    public void write(@NonNull PackedSwitchPayload instruction) {
        try {
            writer.writeUbyte(0);
            writer.writeUbyte(getOpcodeValue(instruction.getOpcode()) >> 8);
            List<? extends SwitchElement> elements = instruction.getSwitchElements();
            writer.writeUshort(elements.size());
            if (elements.size() == 0) {
                writer.writeInt(0);
            } else {
                writer.writeInt(elements.get(0).getKey());
                for (SwitchElement element: elements) {
                    writer.writeInt(element.getOffset());
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static int packNibbles(int a, int b) {
        return (b << 4) | a;
    }

    private int getReferenceIndex(ReferenceInstruction referenceInstruction) {
        return getReferenceIndex(referenceInstruction.getReferenceType(),
                referenceInstruction.getReference());
    }

    private int getReference2Index(DualReferenceInstruction referenceInstruction) {
        return getReferenceIndex(referenceInstruction.getReferenceType2(),
                referenceInstruction.getReference2());
    }

    private int getReferenceIndex(int referenceType, Reference reference) {
        switch (referenceType) {
            case ReferenceType.FIELD:
                return fieldSection.getItemIndex((FieldRefKey) reference);
            case ReferenceType.METHOD:
                return methodSection.getItemIndex((MethodRefKey) reference);
            case ReferenceType.STRING:
                return stringSection.getItemIndex((StringRef) reference);
            case ReferenceType.TYPE:
                return typeSection.getItemIndex((TypeRef) reference);
            case ReferenceType.METHOD_PROTO:
                return protoSection.getItemIndex((ProtoRefKey) reference);
            case ReferenceType.METHOD_HANDLE:
                return methodHandleSection.getItemIndex((MethodHandleKey) reference);
            case ReferenceType.CALL_SITE:
                return callSiteSection.getItemIndex((CallSiteKey) reference);
            default:
                throw new ExceptionWithContext("Unknown reference type: %d",  referenceType);
        }
    }
}
