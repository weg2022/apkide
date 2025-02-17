/*
 * Copyright 2014, Google LLC
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

package com.apkide.smali.dexlib2.rewriter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.apkide.smali.dexlib2.base.reference.BaseTypeReference;
import com.apkide.smali.dexlib2.iface.Annotation;
import com.apkide.smali.dexlib2.iface.ClassDef;
import com.apkide.smali.dexlib2.iface.Field;
import com.apkide.smali.dexlib2.iface.Method;
import com.google.common.collect.Iterators;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ClassDefRewriter implements Rewriter<ClassDef> {
    @NonNull
    protected final Rewriters rewriters;

    public ClassDefRewriter(@NonNull Rewriters rewriters) {
        this.rewriters = rewriters;
    }

    @NonNull @Override public ClassDef rewrite(@NonNull ClassDef classDef) {
        return new RewrittenClassDef(classDef);
    }

    protected class RewrittenClassDef extends BaseTypeReference implements ClassDef {
        @NonNull protected ClassDef classDef;

        public RewrittenClassDef(@NonNull ClassDef classdef) {
            this.classDef = classdef;
        }

        @Override @NonNull public String getType() {
            return rewriters.getTypeRewriter().rewrite(classDef.getType());
        }

        @Override public int getAccessFlags() {
            return classDef.getAccessFlags();
        }

        @Override @Nullable public String getSuperclass() {
            return RewriterUtils.rewriteNullable(rewriters.getTypeRewriter(), classDef.getSuperclass());
        }

        @Override @NonNull public List<String> getInterfaces() {
            return RewriterUtils.rewriteList(rewriters.getTypeRewriter(), classDef.getInterfaces());
        }

        @Override @Nullable public String getSourceFile() {
            return classDef.getSourceFile();
        }

        @Override @NonNull public Set<? extends Annotation> getAnnotations() {
            return RewriterUtils.rewriteSet(rewriters.getAnnotationRewriter(), classDef.getAnnotations());
        }

        @Override @NonNull public Iterable<? extends Field> getStaticFields() {
            return RewriterUtils.rewriteIterable(rewriters.getFieldRewriter(), classDef.getStaticFields());
        }

        @Override @NonNull public Iterable<? extends Field> getInstanceFields() {
            return RewriterUtils.rewriteIterable(rewriters.getFieldRewriter(), classDef.getInstanceFields());
        }

        @NonNull
        @Override
        public Iterable<? extends Field> getFields() {
            return new Iterable<Field>() {
                @NonNull
                @Override
                public Iterator<Field> iterator() {
                    return Iterators.concat(getStaticFields().iterator(), getInstanceFields().iterator());
                }
            };
        }

        @Override @NonNull public Iterable<? extends Method> getDirectMethods() {
            return RewriterUtils.rewriteIterable(rewriters.getMethodRewriter(), classDef.getDirectMethods());
        }

        @Override @NonNull public Iterable<? extends Method> getVirtualMethods() {
            return RewriterUtils.rewriteIterable(rewriters.getMethodRewriter(), classDef.getVirtualMethods());
        }

        @NonNull
        @Override
        public Iterable<? extends Method> getMethods() {
            return new Iterable<Method>() {
                @NonNull
                @Override
                public Iterator<Method> iterator() {
                    return Iterators.concat(getDirectMethods().iterator(), getVirtualMethods().iterator());
                }
            };
        }
    }
}
