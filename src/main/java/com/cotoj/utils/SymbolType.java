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
