package org.openjdk.jextract.impl;

import org.openjdk.jextract.Declaration;
import org.openjdk.jextract.Declaration.Constant;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Declaration.Variable;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Array;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.Type.Delegated;
import org.openjdk.jextract.Type.Delegated.Kind;
import org.openjdk.jextract.Type.Function;
import org.openjdk.jextract.Type.Primitive;
import org.openjdk.jextract.impl.DeclarationImpl.AnonymousStruct;
import org.openjdk.jextract.impl.DeclarationImpl.ClangAlignOf;
import org.openjdk.jextract.impl.DeclarationImpl.ClangSizeOf;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;

import java.lang.invoke.MethodType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class LayoutUtils {

    public static String layoutString(Type type, String runtimeHelperClass) {
        return layoutString(type, Long.MAX_VALUE, runtimeHelperClass);
    }

    private static String layoutString(Type type, long align, String runtimeHelperClass) {
        return switch (type) {
            case Primitive p -> primitiveLayoutString(p, align, runtimeHelperClass);
            case Declared d when d.tree().kind() == Scoped.Kind.ENUM -> layoutString(((Constant)d.tree().members().get(0)).type(), align, runtimeHelperClass);
            case Declared d when Utils.isStructOrUnion(d) -> STR."\{JavaName.getFullNameOrThrow(d.tree())}.$LAYOUT()";
            case Delegated d when d.kind() == Kind.POINTER -> STR."\{runtimeHelperClass}.C_POINTER";
            case Delegated d -> layoutString(d.type(), align, runtimeHelperClass);
            case Function _ -> STR."\{runtimeHelperClass}.C_POINTER";
            case Array a -> STR."MemoryLayout.sequenceLayout(\{a.elementCount().orElse(0L)}, \{layoutString(a.elementType(), align, runtimeHelperClass)})";
            default -> throw new UnsupportedOperationException();
        };
    }

    public static String functionDescriptorString(int textBoxIndent, Type.Function functionType, String runtimeHelperClass) {
        final MethodType type = Utils.methodTypeFor(functionType);
        boolean noArgs = type.parameterCount() == 0;
        StringBuilder builder = new StringBuilder();
        if (!type.returnType().equals(void.class)) {
            builder.append("FunctionDescriptor.of(");
            builder.append("\n");
            builder.append(STR."\{indentString(textBoxIndent + 1)}\{layoutString(functionType.returnType(), runtimeHelperClass)}");
            if (!noArgs) {
                builder.append(",");
            }
        } else {
            builder.append("FunctionDescriptor.ofVoid(");
        }
        if (!noArgs) {
            builder.append("\n");
            String delim = "";
            for (Type arg : functionType.argumentTypes()) {
                builder.append(delim);
                builder.append(STR."\{indentString(textBoxIndent + 1)}\{layoutString(arg, runtimeHelperClass)}");
                delim = ",\n";
            }
            builder.append("\n");
        }
        builder.append(STR."\{indentString(textBoxIndent)})");
        return builder.toString();
    }

    private static String indentString(int size) {
        return " ".repeat(size * 4);
    }

    private static String primitiveLayoutString(Primitive primitiveType, long align, String runtimeHelperClass) {
        return switch (primitiveType.kind()) {
            case Bool -> STR."\{runtimeHelperClass}.C_BOOL";
            case Char -> STR."\{runtimeHelperClass}.C_CHAR";
            case Short -> alignIfNeeded(STR."\{runtimeHelperClass}.C_SHORT", 2, align);
            case Int -> alignIfNeeded(STR."\{runtimeHelperClass}.C_INT", 4, align);
            case Long -> alignIfNeeded(STR."\{runtimeHelperClass}.C_LONG", TypeImpl.IS_WINDOWS ? 4 : 8, align);
            case LongLong -> alignIfNeeded(STR."\{runtimeHelperClass}.C_LONG_LONG", 8, align);
            case Float -> alignIfNeeded(STR."\{runtimeHelperClass}.C_FLOAT", 4, align);
            case Double -> alignIfNeeded(STR."\{runtimeHelperClass}.C_DOUBLE", 8, align);
            case LongDouble -> TypeImpl.IS_WINDOWS ?
                        alignIfNeeded(STR."\{runtimeHelperClass}.C_LONG_DOUBLE", 8, align) :
                        paddingLayoutString(8);
            case HalfFloat, Char16, WChar -> paddingLayoutString(2); // unsupported
            case Float128, Int128 -> paddingLayoutString(16); // unsupported
            default -> throw new UnsupportedOperationException(primitiveType.toString());
        };
    }

    private static String alignIfNeeded(String layoutPrefix, long align, long expectedAlign) {
        return align > expectedAlign ?
                STR."\{layoutPrefix}.withByteAlignment(\{expectedAlign})" :
                layoutPrefix;
    }

    private static String paddingLayoutString(long size) {
        return STR."MemoryLayout.paddingLayout(\{size})";
    }

    public static String structOrUnionLayoutString(Type type, String runtimeHelperClass) {
        return switch (type) {
            case Declared d when Utils.isStructOrUnion(type) -> structOrUnionLayoutString(0, d.tree(), runtimeHelperClass);
            default -> throw new UnsupportedOperationException(type.toString());
        };
    }

    private static String structOrUnionLayoutString(long base, Declaration.Scoped scoped, String runtimeHelperClass) {
        List<String> memberLayouts = new ArrayList<>();

        boolean isStruct = scoped.kind() == Scoped.Kind.STRUCT;

        long align = ClangAlignOf.getOrThrow(scoped) / 8;
        long offset = base;

        long size = 0L; // bits
        for (Declaration member : scoped.members()) {
            if (member instanceof Scoped nested && nested.kind() == Scoped.Kind.BITFIELDS) {
                // skip
            } else {
                long nextOffset = DeclarationImpl.recordMemberOffset(member).getAsLong();
                long delta = nextOffset - offset;
                if (delta > 0) {
                    memberLayouts.add(STR."MemoryLayout.paddingLayout(\{delta / 8})");
                    offset += delta;
                    if (isStruct) {
                        size += delta;
                    }
                }
                String memberLayout;
                if (member instanceof Variable var) {
                    memberLayout = layoutString(var.type(), align, runtimeHelperClass);
                    memberLayout = STR."\{memberLayout}.withName(\"\{member.name()}\")";
                } else {
                    // anon struct
                    memberLayout = structOrUnionLayoutString(offset, (Scoped)member, runtimeHelperClass);
                }
                if ((ClangAlignOf.getOrThrow(member) / 8) > align) {
                    memberLayout = STR."\{memberLayout}.withByteAlignment(\{align})";
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
            memberLayouts.add(STR."MemoryLayout.paddingLayout(\{(expectedSize - size) / 8})");
        }

        String indentNewLine = STR."\n\{indentString(1)}";
        String prefix = isStruct ? "MemoryLayout.structLayout(" :
                                   "MemoryLayout.unionLayout(";
        String layoutString = memberLayouts.stream()
                .collect(Collectors.joining("," + indentNewLine, prefix + indentNewLine, "\n)"));

        // the name is only useful for clients accessing the layout, jextract doesn't care about it
        String name = scoped.name().isEmpty() ?
                AnonymousStruct.anonName(scoped) :
                scoped.name();
        return STR."\{layoutString}.withName(\"\{name}\")";
    }
}
