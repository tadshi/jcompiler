package com.cotoj.utils;

import com.front.cerror.CError;
import com.front.cerror.ErrorType;

public enum SymbolType {
    VARIABLE,
    FUNCTION,
    LABEL; // Unused

    public static SymbolType fromString(String string) {
        if (string == "PROC") {
            return FUNCTION;
        } else if (string == "VAR") {
            return VARIABLE;
        } else if (string == "PARA") {
            throw new CError(ErrorType.UNEXPECTED_TOKEN, string);
        } else {
            throw new RuntimeException("Why " + string + " should occur here?");
        }
    }
}
