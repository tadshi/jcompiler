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
    LABEL; // Unused

    public static SymbolType fromString(String string) {
        return switch (string) {
            case "PROC" -> FUNCTION;
            case "VAR" -> VARIABLE;
            case "PARA" -> throw new CError(ErrorType.UNEXPECTED_TOKEN, string);
            default -> throw new RuntimeException("Why " + string + " should occur here?");
        };
    }

    public static SymbolType fromDef(DefNode def) {
        if (def instanceof ArrayDefNode || def instanceof VarDefNode) {
            return VARIABLE;
        } else if (def instanceof FuncDefNode) {
            return FUNCTION;
        } else {
            throw new RuntimeException("Why " + def.getClass() + " should occur here?");
        }
    }
}
