package com.cotoj.utils;

import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.ArrayFuncParamNode;
import com.cotoj.adaptor.FuncParamNode;
import com.cotoj.adaptor.SimpleFuncParamNode;
import com.cotoj.adaptor.VariableFuncParamNode;

public interface OpcodeHelper extends Opcodes{
    public static int toLoad(ReturnType type) {
        if (type.isPrimitive()) {
            return switch (type) {
                case ReturnType.Integer i -> ILOAD;
                case ReturnType.Boolean z -> ILOAD;
                case ReturnType.Float f -> FLOAD;
                default -> throw new RuntimeException("Cannot recognize " + type);
            };
        }
        return ALOAD;
    }

    public static int toLoad(FuncParamNode param) {
        return switch(param) {
            case SimpleFuncParamNode sp -> toLoad(sp.getType());
            case ArrayFuncParamNode ap -> ALOAD;
            case VariableFuncParamNode vp -> ALOAD;
        };
    }

    public static int toStore(ReturnType type) {
        if (type.isPrimitive()) {
            return switch (type) {
                case ReturnType.Integer i -> ISTORE;
                case ReturnType.Boolean z -> ISTORE;
                case ReturnType.Float f -> FSTORE;
                default -> throw new RuntimeException("Cannot recognize " + type);
            };
        }
        return ALOAD;
    }
}
