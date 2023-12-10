package com.cotoj.utils;

public sealed interface Owner { 
    public record Static() implements Owner {}

    public record Local() implements Owner {}

    public record Main() implements Owner {}
    
    public record ExVarLib(String exVarOwner, String exVarType, 
                        String exVarName, String methodName) implements Owner {
        public String getVarOwner() {
            if (exVarType.startsWith("L") && exVarType.endsWith(";")) {
                return exVarType.substring(1, exVarType.length() - 1);
            } else {
                throw new RuntimeException(exVarType + " is NOT a java class!");
            }
        }
    }
}