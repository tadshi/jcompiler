package com.cotoj.utils;

import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.FuncType;

public enum ReturnType {
    VOID("V"),
    INTEGER("I");
    // To be continued...
    private String typeString;

    private ReturnType(String typeString) {
        this.typeString = typeString;
    }

    public String toTypeString() {
        return typeString;
    }

    public static ReturnType fromFuncType(FuncType funcType) {
        return switch (funcType.getName()) {
            case "INTTK" -> INTEGER;
            case "VOIDTK" -> VOID;
            default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, funcType.getName());
        };
    }
}
