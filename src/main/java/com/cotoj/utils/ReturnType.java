package com.cotoj.utils;

import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.FuncType;
import com.front.gunit.Ident;

public sealed interface ReturnType {
    public final record Void() implements ReturnType {
        public String toTypeString() {
            return "V";
        }
        public boolean isPrimitive() {
            return true;
        }
    };
    public final record Integer() implements ReturnType {
        public String toTypeString() {
            return "I";
        }
        public boolean isPrimitive() {
            return true;
        }
    };
    public final record JavaClass(String typeString) implements ReturnType {
        public String toTypeString() {
            return typeString;
        }
        public String toDescriptor() {
            return "L" + typeString + ";";
        }
        public boolean isPrimitive() {
            return false;
        }
        public boolean equals(Object obj) {
            return obj instanceof JavaClass && typeString.equals(((JavaClass)obj).typeString());
        }
    }

    public abstract String toTypeString();

    public boolean isPrimitive();

    public default String toDescriptor() {
        return toTypeString();
    }

    public static ReturnType fromIdent(Ident ident) {
        return switch (ident.getType()) {
            case "INTTK" -> new Integer();
            case "STRING" -> JavaType.STRING;
            default -> new JavaClass(ident.getType());
        };
    }

    public static ReturnType fromFuncType(FuncType funcType) {
        return switch (funcType.getName()) {
            case "INTTK" -> new Integer();
            case "VOIDTK" -> new Void();
            // default -> new JavaClass(ident.getType());
            default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, funcType.getName());
        };
    }

    public static String typeToDescriptor(String type) {
        if (type.startsWith("[") || type.length() == 1) {
            return type;
        }
        return "L" + type + ";";
    }

    public static String descriptorToType(String descriptor) {
        if (descriptor.startsWith("L")) {
            return descriptor.substring(1, descriptor.length() - 1);
        } else {
            return descriptor;
        }
    }

    @Deprecated
    @Override
    String toString();
}
