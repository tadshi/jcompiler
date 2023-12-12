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
    };
    public final record Integer() implements ReturnType {
        public String toTypeString() {
            return "I";
        }
    };
    public final record JavaClass(String typeString) implements ReturnType {
        public String toTypeString() {
            return typeString;
        }
        public String toDescriptor() {
            return "L" + typeString + ";";
        }
    }

    public abstract String toTypeString();

    public default String toDescriptor() {
        return toTypeString();
    }

    public static ReturnType fromIdent(Ident ident) {
        return switch (ident.getType()) {
            case "INT" -> new Integer();
            case "CONST" -> new Integer(); // TODO
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
