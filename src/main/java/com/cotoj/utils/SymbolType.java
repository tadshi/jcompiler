package com.cotoj.utils;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.DefNode;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.VarDefNode;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;

public enum SymbolType {
    VARIABLE,
    FUNCTION,
    PARAM,
    LABEL; // Unused

    public static SymbolType fromString(String string) {
        return switch (string) {
            case "PROC" -> FUNCTION;
            case "VAR" -> VARIABLE;
            case "PARA" -> throw new CError(ErrorType.UNEXPECTED_TOKEN, string); // This is on purpose
            default -> throw new RuntimeException("Why " + string + " should occur here?");
        };
    }

    public static SymbolType fromDef(DefNode def) {
        return switch (def) {
            case ArrayDefNode _arr -> VARIABLE;
            case VarDefNode _var -> VARIABLE;
            case FuncDefNode _func -> FUNCTION;
        };
    }
}
