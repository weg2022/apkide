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

package com.apkide.smali.dexlib2.writer.pool;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apkide.smali.dexlib2.base.reference.BaseTypeReference;
import com.apkide.smali.dexlib2.iface.Annotation;
import com.apkide.smali.dexlib2.iface.ClassDef;
import com.apkide.smali.dexlib2.iface.Field;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

class PoolClassDef extends BaseTypeReference implements ClassDef {
    @NonNull
    final ClassDef classDef;
    @NonNull final TypeListPool.Key<List<String>> interfaces;
    @NonNull final ImmutableSortedSet<Field> staticFields;
    @NonNull final ImmutableSortedSet<Field> instanceFields;
    @NonNull final ImmutableSortedSet<PoolMethod> directMethods;
    @NonNull final ImmutableSortedSet<PoolMethod> virtualMethods;

    int classDefIndex = DexPool.NO_INDEX;
    int annotationDirectoryOffset = DexPool.NO_OFFSET;

    PoolClassDef(@NonNull ClassDef classDef) {
        this.classDef = classDef;

        interfaces = new TypeListPool.Key<List<String>>(ImmutableList.copyOf(classDef.getInterfaces()));
        staticFields = ImmutableSortedSet.copyOf(classDef.getStaticFields());
        instanceFields = ImmutableSortedSet.copyOf(classDef.getInstanceFields());
        directMethods = ImmutableSortedSet.copyOf(
                Iterables.transform(classDef.getDirectMethods(), PoolMethod.TRANSFORM));
        virtualMethods = ImmutableSortedSet.copyOf(
                Iterables.transform(classDef.getVirtualMethods(), PoolMethod.TRANSFORM));
    }

    @NonNull @Override public String getType() {
        return classDef.getType();
    }

    @Override public int getAccessFlags() {
        return classDef.getAccessFlags();
    }

    @Nullable @Override public String getSuperclass() {
        return classDef.getSuperclass();
    }

    @NonNull @Override public List<String> getInterfaces() {
        return interfaces.types;
    }

    @Nullable @Override public String getSourceFile() {
        return classDef.getSourceFile();
    }

    @NonNull @Override public Set<? extends Annotation> getAnnotations() {
        return classDef.getAnnotations();
    }

    @NonNull @Override public SortedSet<Field> getStaticFields() {
        return staticFields;
    }

    @NonNull @Override public SortedSet<Field> getInstanceFields() {
        return instanceFields;
    }

    @NonNull @Override public Collection<Field> getFields() {
        return new AbstractCollection<Field>() {
            @NonNull @Override public Iterator<Field> iterator() {
                return Iterators.mergeSorted(
                        ImmutableList.of(staticFields.iterator(), instanceFields.iterator()),
                        Ordering.natural());
            }

            @Override public int size() {
                return staticFields.size() + instanceFields.size();
            }
        };
    }

    @NonNull @Override public SortedSet<PoolMethod> getDirectMethods() {
        return directMethods;
    }

    @NonNull @Override public SortedSet<PoolMethod> getVirtualMethods() {
        return virtualMethods;
    }

    @NonNull @Override public Collection<PoolMethod> getMethods() {
        return new AbstractCollection<PoolMethod>() {
            @NonNull @Override public Iterator<PoolMethod> iterator() {
                return Iterators.mergeSorted(
                        ImmutableList.of(directMethods.iterator(), virtualMethods.iterator()),
                        Ordering.natural());
            }

            @Override public int size() {
                return directMethods.size() + virtualMethods.size();
            }
        };
    }
}
