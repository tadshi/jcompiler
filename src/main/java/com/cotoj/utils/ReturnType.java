package com.cotoj.utils;

import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.FuncType;
import com.front.gunit.GBool;
import com.front.gunit.GDict;
import com.front.gunit.GFloat;
import com.front.gunit.GList;
import com.front.gunit.GLock;
import com.front.gunit.GNumber;
import com.front.gunit.GSemaphore;
import com.front.gunit.GString;
import com.front.gunit.Ident;
import com.front.gunit.ObjectClass;

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
    public final record Float() implements ReturnType {
        public String toTypeString() {
            return "F";
        }
        public boolean isPrimitive() {
            return true;
        }
    };
    public final record Boolean() implements ReturnType {
        public String toTypeString() {
            return "Z";
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
    public final record List(ReturnType contentType) implements ReturnType {
        public String toTypeString() {
            return "java/util/List";
        }
        public String toDescriptor() {
            return "Ljava/util/List;";
        }
        public boolean isPrimitive() {
            return false;
        }
        public boolean equals(Object obj) {
            return obj instanceof ReturnType.List && contentType.equals(((List)obj).contentType);
        }
    }
    public final record Dict(ReturnType keyType, ReturnType valueType) implements ReturnType {
        public String toTypeString() {
            return "java/util/Map";
        }
        public String toDescriptor() {
            return "Ljava/util/Map;";
        }
        public boolean isPrimitive() {
            return false;
        }
        public boolean equals(Object obj) {
            return obj instanceof ReturnType.Dict && 
                    keyType.equals(((ReturnType.Dict)obj).keyType) &&
                    valueType.equals(((ReturnType.Dict)obj).valueType);
        }
    }

    public abstract String toTypeString();

    public boolean isPrimitive();

    public default String toDescriptor() {
        return toTypeString();
    }

    @SuppressWarnings("unused")
    private static ReturnType fromLegacyString(String tokenString) {
        return switch (tokenString) {
            case "INTTK" -> new Integer();
            case "VOIDTK" -> new Void();
            case "FLOATTK" -> new Float();
            case "BOOLTK"-> new Boolean();
            case "STRINGTK" -> JavaType.STRING;
            case "LOCKTK" -> JavaType.LOCK;
            case "SEMAPHORETK" -> JavaType.SEM;
            default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, "Cannot parse " + tokenString + "as a type.");
        };
    }

    private static ReturnType fromFrontendType(ObjectClass gObj) {
        return switch (gObj) {
            case null -> new Void();
            case GNumber gn -> new Integer();
            case GFloat gf -> new Float();
            case GBool gz -> new Boolean();
            case GString gs -> JavaType.STRING;
            case GLock gl -> JavaType.LOCK;
            case GSemaphore gsem -> JavaType.SEM;
            case GList glist -> new List(fromFrontendType(glist.getType()));
            case GDict gdict -> new Dict(fromFrontendType(gdict.getKeyType()), fromFrontendType(gdict.getValueType()));
            default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, gObj.getClass().toString());
        };
    }

    public static ReturnType fromIdent(Ident ident) {
        try {
            return fromFrontendType(ident.getDataType());
        } catch (CError err) {}
        if (ident.getType().startsWith("java/") || ident.getType().startsWith("com/")) {
            return new JavaClass(ident.getType());
        }
        throw new CError(ErrorType.UNEXPECTED_TOKEN, ident.getType());
    }

    public static ReturnType fromFuncType(FuncType funcType) {
        return fromFrontendType(funcType.getDataType());
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
