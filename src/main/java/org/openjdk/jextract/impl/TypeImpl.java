/*
 *  Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.  Oracle designates this
 *  particular file as subject to the "Classpath" exception as provided
 *  by Oracle in the LICENSE file that accompanied this code.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

package org.openjdk.jextract.impl;

import java.lang.foreign.GroupLayout;
import java.lang.foreign.PaddingLayout;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import java.lang.foreign.AddressLayout;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.ValueLayout;
import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.Constant;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Scoped.Kind;
import org.openjdk.jextract.Declaration.Variable;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.impl.DeclarationImpl.ClangAlignOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangOffsetOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangSizeOf;
import org.openjdk.jextract.impl.DeclarationImpl.ScopedLayout;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

import static java.lang.foreign.ValueLayout.ADDRESS;

public abstract class TypeImpl extends AttributedImpl implements Type {

    public static final boolean IS_WINDOWS = System.getProperty("os.name").startsWith("Windows");

    @Override
    public boolean isErroneous() {
        return false;
    }

    static boolean equals(Type t1, Type.Delegated t2) {
        assert t1 != null;
        assert t2 != null;

        return (t2.kind() == Delegated.Kind.TYPEDEF) && t1.equals(t2.type());
    }

    public static class ErronrousTypeImpl extends TypeImpl {
        final String erroneousName;

        public ErronrousTypeImpl(String erroneousName) {
            this.erroneousName = erroneousName;
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitType(this, data);
        }

        @Override
        public boolean isErroneous() {
            return true;
        }
    };

    public static final class PrimitiveImpl extends TypeImpl implements Type.Primitive {

        private final Primitive.Kind kind;

        public PrimitiveImpl(Kind kind) {
            this.kind = Objects.requireNonNull(kind);
            if (kind.layout().isPresent() && kind.layout().get() instanceof PaddingLayout) {
                // this type is unsupported, add a Skip attribute
                Skip.with(this);
            }
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitPrimitive(this, data);
        }

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type.Primitive primitive)) {
                return (o instanceof Delegated delegated) && equals(this, delegated);
            }
            return kind == primitive.kind();
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind);
        }
    }

    static abstract class DelegatedBase extends TypeImpl implements Type.Delegated {
        Delegated.Kind kind;
        Optional<String> name;

        DelegatedBase(Kind kind, Optional<String> name) {
            this.kind = Objects.requireNonNull(kind);
            this.name = Objects.requireNonNull(name);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitDelegated(this, data);
        }

        @Override
        public final Delegated.Kind kind() {
            return kind;
        }

        @Override
        public final Optional<String> name() {
            return name;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type.Delegated delegated)) {
                return (o instanceof Type type) && equals(type, this);
            }
            return kind == delegated.kind() &&
                    name.equals(delegated.name());
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, name);
        }
    }

    public static final class QualifiedImpl extends DelegatedBase {
        private final Type type;

        public QualifiedImpl(Kind kind, Type type) {
            this(kind, Optional.empty(), type);
        }

        public QualifiedImpl(Kind kind, String name, Type type) {
            this(kind, Optional.of(name), type);
        }

        private QualifiedImpl(Kind kind, Optional<String> name, Type type) {
            super(kind, name);
            this.type = type;
        }

        @Override
        public Type type() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type.Delegated qualified)) return false;
            if (!super.equals(o)) {
                return (o instanceof Delegated delegated) && equals(this, delegated);
            }
            return Objects.equals(type, qualified.type());
        }

        @Override
        public int hashCode() {
            return (kind() == Kind.TYPEDEF)? type().hashCode() : Objects.hash(super.hashCode(), type);
        }
    }

    public static final class PointerImpl extends DelegatedBase {
        public static final AddressLayout POINTER_LAYOUT = ADDRESS
                .withTargetLayout(MemoryLayout.sequenceLayout(Long.MAX_VALUE, ValueLayout.JAVA_BYTE));

        private final Supplier<Type> pointeeFactory;

        public PointerImpl(Supplier<Type> pointeeFactory) {
            super(Kind.POINTER, Optional.empty());
            this.pointeeFactory = Objects.requireNonNull(pointeeFactory);
        }

        public PointerImpl(Type pointee) {
            this(() -> pointee);
        }

        @Override
        public Type type() {
            return pointeeFactory.get();
        }
    }

    public static final class DeclaredImpl extends TypeImpl implements Type.Declared {

        private final Declaration.Scoped declaration;

        public DeclaredImpl(Declaration.Scoped declaration) {
            super();
            this.declaration = Objects.requireNonNull(declaration);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitDeclared(this, data);
        }

        @Override
        public Declaration.Scoped tree() {
            return declaration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type.Declared declared)) {
                return (o instanceof Delegated delegated) && equals(this, delegated);
            }
            return declaration.equals(declared.tree());
        }

        @Override
        public int hashCode() {
            return Objects.hash(declaration);
        }
    }

    public static final class FunctionImpl extends TypeImpl implements Type.Function {

        private final boolean varargs;
        private final List<Type> argtypes;
        private final Type restype;
        private final Optional<List<String>> paramNames;

        public FunctionImpl(boolean varargs, List<Type> argtypes, Type restype, List<String> paramNames) {
            super();
            this.varargs = varargs;
            this.argtypes = Objects.requireNonNull(argtypes);
            this.restype = Objects.requireNonNull(restype);
            this.paramNames = Optional.ofNullable(paramNames);
        }

        public FunctionImpl(boolean varargs, List<Type> argtypes, Type restype) {
            this(varargs, argtypes, restype, null);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitFunction(this, data);
        }

        @Override
        public boolean varargs() {
            return varargs;
        }

        @Override
        public List<Type> argumentTypes() {
            return argtypes;
        }

        @Override
        public Type returnType() {
            return restype;
        }

        @Override
        public Type.Function withParameterNames(List<String> paramNames) {
            Objects.requireNonNull(paramNames);
            return new FunctionImpl(varargs, argtypes, restype, paramNames);
        }

        @Override
        public Optional<List<String>> parameterNames() {
            return paramNames;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type.Function function)) {
                return (o instanceof Delegated delegated) && equals(this, delegated);
            }
            return varargs == function.varargs() &&
                    argtypes.equals(function.argumentTypes()) &&
                    restype.equals(function.returnType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(varargs, argtypes, restype);
        }
    }

    public static final class ArrayImpl extends TypeImpl implements Type.Array {

        private final Kind kind;
        private final OptionalLong elemCount;
        private final Type elemType;

        public ArrayImpl(Kind kind, long count, Type elemType) {
            this(kind, elemType, OptionalLong.of(count));
        }

        public ArrayImpl(Kind kind, Type elemType) {
            this(kind, elemType, OptionalLong.empty());
        }

        private ArrayImpl(Kind kind, Type elemType, OptionalLong elemCount) {
            super();
            this.kind = Objects.requireNonNull(kind);
            this.elemCount = Objects.requireNonNull(elemCount);
            this.elemType = Objects.requireNonNull(elemType);
        }

        @Override
        public <R, D> R accept(Visitor<R, D> visitor, D data) {
            return visitor.visitArray(this, data);
        }

        @Override
        public OptionalLong elementCount() {
            return elemCount;
        }

        @Override
        public Type elementType() {
            return elemType;
        }

        @Override
        public Kind kind() {
            return kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Type.Array array)) {
                return (o instanceof Delegated delegated) && equals(this, delegated);
            }
            return kind == array.kind() &&
                    elemType.equals(array.elementType());
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind, elemType);
        }
    }

    public boolean isPointer() {
        return this instanceof Type.Delegated delegated &&
                delegated.kind() == Type.Delegated.Kind.POINTER;
    }

    @Override
    public String toString() {
        return PrettyPrinter.type(this);
    }

    // Utilities to fetch layouts/descriptor from types

    public static Optional<MemoryLayout> getLayout(org.openjdk.jextract.Type t) {
        try {
            return Optional.of(getLayoutInternal(t));
        } catch (UnsupportedOperationException ex) {
            return Optional.empty();
        }
    }

    public static Optional<FunctionDescriptor> getDescriptor(Function t) {
        try {
            MemoryLayout[] args = t.argumentTypes().stream()
                    .map(TypeImpl::getLayoutInternal)
                    .toArray(MemoryLayout[]::new);
            Type retType = t.returnType();
            if (isVoidType(retType)) {
                return Optional.of(FunctionDescriptor.ofVoid(args));
            } else {
                return Optional.of(FunctionDescriptor.of(getLayoutInternal(retType), args));
            }
        } catch (UnsupportedOperationException ex) {
            return Optional.empty();
        }
    }

    private static boolean isVoidType(org.openjdk.jextract.Type type) {
        if (type instanceof org.openjdk.jextract.Type.Primitive pt) {
            return pt.kind() == org.openjdk.jextract.Type.Primitive.Kind.Void;
        } else if (type instanceof org.openjdk.jextract.Type.Delegated dt) {
            return dt.kind() == org.openjdk.jextract.Type.Delegated.Kind.TYPEDEF? isVoidType(dt.type()) : false;
        }
        return false;
    }

    public static MemoryLayout getLayoutInternal(org.openjdk.jextract.Type t) {
        return t.accept(layoutMaker, null);
    }

    private static org.openjdk.jextract.Type.Visitor<MemoryLayout, Void> layoutMaker = new org.openjdk.jextract.Type.Visitor<>() {
        @Override
        public MemoryLayout visitPrimitive(org.openjdk.jextract.Type.Primitive t, Void _ignored) {
            return t.kind().layout().orElseThrow(UnsupportedOperationException::new);
        }

        @Override
        public MemoryLayout visitDelegated(org.openjdk.jextract.Type.Delegated t, Void _ignored) {
            if (t.kind() == org.openjdk.jextract.Type.Delegated.Kind.POINTER) {
                return PointerImpl.POINTER_LAYOUT;
            } else {
                return t.type().accept(this, null);
            }
        }

        @Override
        public MemoryLayout visitFunction(org.openjdk.jextract.Type.Function t, Void _ignored) {
            /*
             * // pointer to function declared as function like this
             *
             * typedef void CB(int);
             * void func(CB cb);
             */
            return PointerImpl.POINTER_LAYOUT;
        }

        @Override
        public MemoryLayout visitDeclared(org.openjdk.jextract.Type.Declared t, Void _ignored) {
            return declaredLayout(t.tree());
        }

        @Override
        public MemoryLayout visitArray(org.openjdk.jextract.Type.Array t, Void _ignored) {
            MemoryLayout elem = t.elementType().accept(this, null);
            if (t.elementCount().isPresent()) {
                return MemoryLayout.sequenceLayout(t.elementCount().getAsLong(), elem);
            } else {
                return MemoryLayout.sequenceLayout(0, elem);
            }
        }

        @Override
        public MemoryLayout visitType(org.openjdk.jextract.Type t, Void _ignored) {
            throw new UnsupportedOperationException();
        }

        private MemoryLayout declaredLayout(Scoped scoped) {
            Optional<MemoryLayout> layout = ScopedLayout.get(scoped);
            if (layout.isPresent()) {
                return layout.get();
            } else {
                // compute and cache for later use
                switch (scoped.kind()) {
                    case Kind.STRUCT, Kind.UNION -> {
                        if (ClangSizeOf.get(scoped).isPresent()) {
                            GroupLayout groupLayout = recordLayout(0, new AtomicInteger(), scoped);
                            ScopedLayout.with(scoped, groupLayout);
                            return groupLayout;
                        }
                    }
                    case Kind.ENUM -> {
                        MemoryLayout constLayout = Type.layoutFor(((Constant)scoped.members().get(0)).type())
                                .orElseThrow(UnsupportedOperationException::new);
                        ScopedLayout.with(scoped, constLayout);
                        return constLayout;
                    }
                }
                throw new UnsupportedOperationException();
            }
        }

        private GroupLayout recordLayout(long base, AtomicInteger anonCount, Scoped scoped) {
            boolean isStruct = scoped.kind() == Kind.STRUCT;
            String name = scoped.name().isEmpty() ?
                    "$anon$" + anonCount.getAndIncrement() :
                    scoped.name();

            long offset = base; // bits
            long size = 0L; // bits
            List<MemoryLayout> memberLayouts = new ArrayList<>();
            for (Declaration member : scoped.members()) {
                if (member instanceof Scoped nested && nested.kind() == Kind.BITFIELDS) {
                    // skip
                } else if (nextOffset(member).isPresent()) {
                    long nextOffset = nextOffset(member).getAsLong();
                    long delta = nextOffset - offset;
                    if (delta > 0) {
                        memberLayouts.add(MemoryLayout.paddingLayout(delta / 8));
                        offset += delta;
                        if (isStruct) {
                            size += delta;
                        }
                    }
                    boolean added = false;
                    if (member instanceof Scoped nested) {
                        // nested anonymous struct or union, recurse
                        GroupLayout layout = recordLayout(base + offset, anonCount, nested);
                        ScopedLayout.with(nested, layout);
                        memberLayouts.add(layout);
                        added = true;
                    } else {
                        Variable field = (Variable) member;
                        Optional<MemoryLayout> fieldLayout = Type.layoutFor(field.type());
                        if (fieldLayout.isPresent()) {
                            memberLayouts.add(fieldLayout.get()
                                    .withName(field.name()));
                            added = true;
                        }
                    }
                    if (added) {
                        // update offset and size
                        long fieldSize = ClangSizeOf.getOrThrow(member);
                        if (isStruct) {
                            offset += fieldSize;
                            size += fieldSize;
                        } else {
                            size = Math.max(size, ClangSizeOf.getOrThrow(member));
                        }
                    }
                }
            }
            long expectedSize = ClangSizeOf.getOrThrow(scoped);
            if (size != expectedSize) {
                memberLayouts.add(MemoryLayout.paddingLayout((expectedSize - size) / 8));
            }
            long align = ClangAlignOf.getOrThrow(scoped) / 8;
            GroupLayout layout = isStruct ?
                    MemoryLayout.structLayout(alignFields(memberLayouts, align)) :
                    MemoryLayout.unionLayout(alignFields(memberLayouts, align));
            return layout.withName(name);
        }

        private OptionalLong nextOffset(Declaration member) {
            if (member instanceof Variable) {
                return ClangOffsetOf.get(member);
            } else {
                Optional<Declaration> firstDecl = ((Scoped)member).members().stream().findFirst();
                return firstDecl.isEmpty() ?
                        OptionalLong.empty() :
                        nextOffset(firstDecl.get());
            }
        }

        private MemoryLayout[] alignFields(List<MemoryLayout> members, long align) {
            return members.stream()
                    .map(l -> forceAlign(l, align))
                    .toArray(MemoryLayout[]::new);
        }

        private MemoryLayout forceAlign(MemoryLayout layout, long align) {
            if (align >= layout.byteAlignment()) {
                return layout; // fast-path
            }
            MemoryLayout res = switch (layout) {
                case GroupLayout groupLayout -> {
                    MemoryLayout[] newMembers = groupLayout.memberLayouts()
                            .stream().map(l -> forceAlign(l, align)).toArray(MemoryLayout[]::new);
                    yield groupLayout instanceof StructLayout ?
                            MemoryLayout.structLayout(newMembers) :
                            MemoryLayout.unionLayout(newMembers);
                }
                case SequenceLayout sequenceLayout ->
                        MemoryLayout.sequenceLayout(sequenceLayout.elementCount(),
                                forceAlign(sequenceLayout.elementLayout(), align));
                default -> layout.withByteAlignment(align);
            };
            // copy name and target layout, if present
            if (layout.name().isPresent()) {
                res = res.withName(layout.name().get());
            }
            if (layout instanceof AddressLayout addressLayout && addressLayout.targetLayout().isPresent()) {
                ((AddressLayout)res).withTargetLayout(addressLayout.targetLayout().get());
            }
            return res;
        }
    };
}
