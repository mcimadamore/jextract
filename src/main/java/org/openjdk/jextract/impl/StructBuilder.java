/*
 * Copyright (c) 2020, 2024, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.jextract.impl;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Variable;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.impl.DeclarationImpl.AnonymousStruct;
import org.openjdk.jextract.impl.DeclarationImpl.ClangAlignOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangOffsetOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangSizeOf;
import org.openjdk.jextract.impl.DeclarationImpl.JavaFunctionalInterfaceName;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;
import org.openjdk.jextract.impl.DeclarationImpl.Skip;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class generates static utilities class for C structs, unions.
 */
final class StructBuilder extends ClassSourceBuilder implements OutputFactory.Builder {

    private final Declaration.Scoped structTree;
    private final Type structType;
    private final Deque<Declaration> nestedAnonDeclarations;

    StructBuilder(SourceFileBuilder builder, String modifiers, String className,
                  ClassSourceBuilder enclosing, String runtimeHelperName, Declaration.Scoped structTree) {
        super(builder, modifiers, Kind.CLASS, className, null, enclosing, runtimeHelperName);
        this.structTree = structTree;
        this.structType = Type.declared(structTree);
        this.nestedAnonDeclarations = new ArrayDeque<>();
    }

    private String safeParameterName(String paramName) {
        return isEnclosedBySameName(paramName)? paramName + "$" : paramName;
    }

    private void pushNestedAnonDecl(Declaration anonDecl) {
        nestedAnonDeclarations.push(anonDecl);
    }

    private void popNestedAnonDecl() {
        nestedAnonDeclarations.pop();
    }

    void begin() {
        if (!inAnonymousNested()) {
            if (isNested()) {
                sourceFileBuilder().incrAlign();
            }
            appendBlankLine();
            emitDocComment(structTree);
            classBegin();
            emitDefaultConstructor();
            emitLayoutDecl();
        }
    }

    void end() {
        if (!inAnonymousNested()) {
            emitAsSlice();
            appendBlankLine();
            emitSizeof();
            emitAllocatorAllocate();
            emitAllocatorAllocateArray();
            emitReinterpret();
            classEnd();
            if (isNested()) {
                // we are nested. Decrease align
                sourceFileBuilder().decrAlign();
            }
        } else {
            // we're in an anonymous struct which got merged into this one, return this very builder and keep it open
            popNestedAnonDecl();
        }
    }

    private boolean inAnonymousNested() {
        return !nestedAnonDeclarations.isEmpty();
    }

    @Override
    public StructBuilder addStruct(Declaration.Scoped tree) {
        if (AnonymousStruct.isPresent(tree)) {
            //nested anon struct - merge into this builder!
            pushNestedAnonDecl(tree);
            return this;
        } else {
            StructBuilder builder = new StructBuilder(sourceFileBuilder(), "public static",
                    JavaName.getOrThrow(tree), this, runtimeHelperName(), tree);
            builder.begin();
            return builder;
        }
    }

    @Override
    public void addFunctionalInterface(Declaration parentDecl, Type.Function funcType) {
        incrAlign();
        FunctionalInterfaceBuilder.generate(sourceFileBuilder(), JavaFunctionalInterfaceName.getOrThrow(parentDecl),
                this, runtimeHelperName(), parentDecl, funcType);
        decrAlign();
    }

    @Override
    public void addVar(Declaration.Variable varTree) {
        String javaName = JavaName.getOrThrow(varTree);
        appendBlankLine();
        String offsetField = emitOffsetFieldDecl(varTree);
        if (Utils.isArray(varTree.type()) || Utils.isStructOrUnion(varTree.type())) {
            String sizeField = emitSizeFieldDecl(varTree);
            emitSegmentGetter(javaName, varTree, offsetField, sizeField);
            emitSegmentSetter(javaName, varTree, offsetField, sizeField);
        } else if (Utils.isPointer(varTree.type()) || Utils.isPrimitive(varTree.type())) {
            String layoutField = emitLayoutFieldDecl(varTree);
            emitFieldGetter(javaName, varTree, layoutField, offsetField);
            emitFieldSetter(javaName, varTree, layoutField, offsetField);
        } else {
            throw new IllegalArgumentException(STR."Type not supported: \{varTree.type()}");
        }
    }

    private List<String> prefixNamesList() {
        return nestedAnonDeclarations.stream()
                .map(d -> AnonymousStruct.anonName((Declaration.Scoped)d))
                .toList().reversed();
    }

    private String fieldElementPaths(String nativeName) {
        StringBuilder builder = new StringBuilder();
        String prefix = "";
        for (String prefixElementName : prefixNamesList()) {
            builder.append(prefix + "groupElement(\"" + prefixElementName + "\")");
            prefix = ", ";
        }
        builder.append(prefix + "groupElement(\"" + nativeName + "\")");
        return builder.toString();
    }

    private void emitFieldDocComment(Declaration.Variable varTree, String header) {
        incrAlign();
        emitDocComment(varTree, header);
        decrAlign();
    }

    private String kindName() {
        return structTree.kind() == Scoped.Kind.STRUCT ? "struct" : "union";
    }

    private void emitFieldGetter(String javaName, Declaration.Variable varTree, String layoutField, String offsetField) {
        String segmentParam = safeParameterName(kindName());
        Class<?> type = Utils.carrierFor(varTree.type());
        appendBlankLine();
        emitFieldDocComment(varTree, "Getter for field:");
        appendIndentedLines(STR."""
            public static \{type.getSimpleName()} \{javaName}(MemorySegment \{segmentParam}) {
                return \{segmentParam}.get(\{layoutField}, \{offsetField});
            }
            """);
    }

    private void emitFieldSetter(String javaName, Declaration.Variable varTree, String layoutField, String offsetField) {
        String segmentParam = safeParameterName(kindName());
        String valueParam = safeParameterName("fieldValue");
        Class<?> type = Utils.carrierFor(varTree.type());
        appendBlankLine();
        emitFieldDocComment(varTree, "Setter for field:");
        appendIndentedLines(STR."""
            public static void \{javaName}(MemorySegment \{segmentParam}, \{type.getSimpleName()} \{valueParam}) {
                \{segmentParam}.set(\{layoutField}, \{offsetField}, \{valueParam});
            }
            """);
    }

    private void emitSegmentGetter(String javaName, Declaration.Variable varTree, String offsetField, String sizeField) {
        appendBlankLine();
        emitFieldDocComment(varTree, "Getter for field:");
        String segmentParam = safeParameterName(kindName());
        appendIndentedLines(STR."""
            public static MemorySegment \{javaName}(MemorySegment \{segmentParam}) {
                return \{segmentParam}.asSlice(\{offsetField}, \{sizeField});
            }
            """);
    }

    private void emitSegmentSetter(String javaName, Declaration.Variable varTree, String offsetField, String sizeField) {
        appendBlankLine();
        emitFieldDocComment(varTree, "Setter for field:");
        String segmentParam = safeParameterName(kindName());
        String valueParam = safeParameterName("fieldValue");
        appendIndentedLines(STR."""
            public static void \{javaName}(MemorySegment \{segmentParam}, MemorySegment \{valueParam}) {
                MemorySegment.copy(\{valueParam}, 0L, \{segmentParam}, \{offsetField}, \{sizeField});
            }
            """);
    }

    private void emitAsSlice() {
        String arrayParam = safeParameterName("array");
        appendIndentedLines(STR."""

            public static MemorySegment asSlice(MemorySegment \{arrayParam}, long index) {
                return \{arrayParam}.asSlice($LAYOUT().byteSize() * index);
            }
            """);
    }

    private void emitSizeof() {
        appendIndentedLines("""
            public static long sizeof() { return $LAYOUT().byteSize(); }
            """);
    }

    private void emitAllocatorAllocate() {
        String allocatorParam = safeParameterName("allocator");
        appendIndentedLines(STR."""

            public static MemorySegment allocate(SegmentAllocator \{allocatorParam}) {
                return \{allocatorParam}.allocate($LAYOUT());
            }
            """);
    }

    private void emitAllocatorAllocateArray() {
        String allocatorParam = safeParameterName("allocator");
        String elementCountParam = safeParameterName("elementCount");
        appendIndentedLines(STR."""

            public static MemorySegment allocateArray(long \{elementCountParam}, SegmentAllocator \{allocatorParam}) {
                return \{allocatorParam}.allocate(MemoryLayout.sequenceLayout(\{elementCountParam}, $LAYOUT()));
            }
            """);
    }

    private void emitReinterpret() {
        appendIndentedLines("""

            public static MemorySegment reinterpret(MemorySegment addr, Arena arena, Consumer<MemorySegment> cleanup) {
                return reinterpret(addr, 1, arena, cleanup);
            }

            public static MemorySegment reinterpret(MemorySegment addr, long elementCount, Arena arena, Consumer<MemorySegment> cleanup) {
                return addr.reinterpret($LAYOUT().byteSize() * elementCount, arena, cleanup);
            }
            """);
    }

    private void emitLayoutDecl() {
        appendIndentedLines(STR."""

            private static final GroupLayout $LAYOUT = \{structOrUnionLayoutString(structType)};

            public static final GroupLayout $LAYOUT() {
                return $LAYOUT;
            }
            """);
    }

    private String emitOffsetFieldDecl(Declaration field) {
        String offsetFieldName = STR."\{field.name()}$OFFSET";
        appendIndentedLines(STR."""
            private static final long \{offsetFieldName} = \{ClangOffsetOf.getOrThrow(field) / 8};
            """);
        return offsetFieldName;
    }

    private String emitLayoutFieldDecl(Declaration.Variable field) {
        String layoutFieldName = STR."\{field.name()}$LAYOUT";
        String layoutType = Utils.layoutCarrierFor(field.type()).getSimpleName();
        appendIndentedLines(STR."""
            private static final \{layoutType} \{layoutFieldName} = (\{layoutType})$LAYOUT.select(\{fieldElementPaths(field.name())});
            """);
        return layoutFieldName;
    }

    private String emitSizeFieldDecl(Declaration field) {
        String sizeFieldName = STR."\{field.name()}$SIZE";
        appendIndentedLines(STR."""
            private static final long \{sizeFieldName} = \{ClangSizeOf.getOrThrow(field) / 8};
            """);
        return sizeFieldName;
    }

    private String structOrUnionLayoutString(Type type) {
        return switch (type) {
            case Declared d when Utils.isStructOrUnion(type) -> structOrUnionLayoutString(0, d.tree(), 0);
            default -> throw new UnsupportedOperationException(type.toString());
        };
    }

    private static long recordMemberOffset(Declaration member) {
        if (member instanceof Variable) {
            return ClangOffsetOf.get(member).orElseThrow();
        } else {
            // anonymous struct
            return AnonymousStruct.getOrThrow((Scoped) member).offset().orElseThrow();
        }
    }

    private String structOrUnionLayoutString(long base, Declaration.Scoped scoped, int indent) {
        List<String> memberLayouts = new ArrayList<>();

        boolean isStruct = scoped.kind() == Scoped.Kind.STRUCT;

        long align = ClangAlignOf.getOrThrow(scoped) / 8;
        long offset = base;

        long size = 0L; // bits
        for (Declaration member : scoped.members()) {
            if (!Skip.isPresent(member)) {
                long nextOffset = recordMemberOffset(member);
                long delta = nextOffset - offset;
                if (delta > 0) {
                    memberLayouts.add(paddingLayoutString(delta / 8, indent + 1));
                    offset += delta;
                    if (isStruct) {
                        size += delta;
                    }
                }
                String memberLayout;
                if (member instanceof Variable var) {
                    memberLayout = layoutString(var.type(), align);
                    memberLayout = STR."\{indentString(indent + 1)}\{memberLayout}.withName(\"\{member.name()}\")";
                } else {
                    // anon struct
                    memberLayout = structOrUnionLayoutString(offset, (Scoped) member, indent + 1);
                }
                memberLayouts.add(memberLayout);
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
        long expectedSize = ClangSizeOf.getOrThrow(scoped);
        if (size != expectedSize) {
            long trailPadding = isStruct ?
                    (expectedSize - size) / 8 :
                    expectedSize / 8;
            memberLayouts.add(paddingLayoutString(trailPadding, indent + 1));
        }

        String prefix = isStruct ?
                STR."\{indentString(indent)}MemoryLayout.structLayout(\n" :
                STR."\{indentString(indent)}MemoryLayout.unionLayout(\n";
        String suffix = STR."\n\{indentString(indent)})";
        String layoutString = memberLayouts.stream()
                .collect(Collectors.joining(",\n", prefix, suffix));

        // the name is only useful for clients accessing the layout, jextract doesn't care about it
        String name = scoped.name().isEmpty() ?
                AnonymousStruct.anonName(scoped) : scoped.name();
        return STR."\{layoutString}.withName(\"\{name}\")";
    }
}
