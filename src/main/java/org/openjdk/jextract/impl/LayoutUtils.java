package org.openjdk.jextract.impl;

import org.openjdk.jextract.Declaration.Constant;
import org.openjdk.jextract.Declaration.Scoped;
import org.openjdk.jextract.Type;
import org.openjdk.jextract.Type.Array;
import org.openjdk.jextract.Type.Declared;
import org.openjdk.jextract.Type.Delegated;
import org.openjdk.jextract.Type.Delegated.Kind;
import org.openjdk.jextract.Type.Function;
import org.openjdk.jextract.Type.Primitive;
import org.openjdk.jextract.impl.DeclarationImpl.JavaName;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemoryLayout;
import java.lang.invoke.MethodType;

public class LayoutUtils {
    public static String layoutString(Type type, String runtimeHelperClass) {
        return switch (type) {
            case Primitive p -> primitiveLayoutString(p, runtimeHelperClass);
            case Declared d when d.tree().kind() == Scoped.Kind.ENUM -> layoutString(((Constant)d.tree().members().get(0)).type(), runtimeHelperClass);
            case Declared d when Utils.isStructOrUnion(d) -> STR."\{JavaName.getFullNameOrThrow(d.tree())}.$LAYOUT()";
            case Delegated d when d.kind() == Kind.POINTER -> STR."\{runtimeHelperClass}.C_POINTER";
            case Delegated d -> layoutString(d.type(), runtimeHelperClass);
            case Function _ -> STR."\{runtimeHelperClass}.C_POINTER";
            case Array a -> STR."MemoryLayout.sequenceLayout(\{a.elementCount().orElse(0L)}, \{layoutString(a.elementType(), runtimeHelperClass)})";
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

    private static String primitiveLayoutString(Primitive primitiveType, String runtimeHelperClass) {
        return switch (primitiveType.kind()) {
            case Bool -> STR."\{runtimeHelperClass}.C_BOOL";
            case Char -> STR."\{runtimeHelperClass}.C_CHAR";
            case Short -> STR."\{runtimeHelperClass}.C_SHORT";
            case Int -> STR."\{runtimeHelperClass}.C_INT";
            case Long -> STR."\{runtimeHelperClass}.C_LONG";
            case LongLong -> STR."\{runtimeHelperClass}.C_LONG_LONG";
            case Float -> STR."\{runtimeHelperClass}.C_FLOAT";
            case Double -> STR."\{runtimeHelperClass}.C_DOUBLE";
            case LongDouble -> {
                if (TypeImpl.IS_WINDOWS) {
                    yield STR."\{runtimeHelperClass}.C_LONG_DOUBLE";
                } else {
                    throw new UnsupportedOperationException(primitiveType.toString());
                }
            }
            default -> throw new UnsupportedOperationException(primitiveType.toString());
        };
    }
}
