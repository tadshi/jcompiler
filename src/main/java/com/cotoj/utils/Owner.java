package com.cotoj.utils;

public sealed interface Owner {
    public record Static(String className, boolean isInterface) implements Owner {}

    public record Local() implements Owner {}

    public record Class(String className, boolean isInterface) implements Owner {}

    public static Owner builtinStatic() {
        return new Static("com/oto/Static", false);
    }

    public static Owner builtinMain() {
        return new Static("com/oto/Main", false);
    }
    
    // public record ExVarLib(String exVarOwner, String exVarType, 
    //                     String exVarName, String methodName) implements Owner {
    //     public String getVarOwner() {
    //         if (exVarType.startsWith("L") && exVarType.endsWith(";")) {
    //             return exVarType.substring(1, exVarType.length() - 1);
    //         } else {
    //             throw new RuntimeException(exVarType + " is NOT a java class!");
    //         }
    //     }
    // }
}