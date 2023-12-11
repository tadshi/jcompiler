package com.cotoj.utils;

public interface JavaType {
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
}
