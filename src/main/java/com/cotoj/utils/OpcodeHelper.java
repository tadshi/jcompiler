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

    public static int toArrayLoad(ReturnType type) {
        if (type.isPrimitive()) {
            return switch (type) {
                case ReturnType.Integer i -> IALOAD;
                case ReturnType.Boolean z -> BALOAD;
                case ReturnType.Float f -> FALOAD;
                default -> throw new RuntimeException("Cannot recognize " + type);
            };
        }
        return ALOAD;
    }

    public static int toArrayStore(ReturnType type) {
        if (type.isPrimitive()) {
            return switch (type) {
                case ReturnType.Integer i -> IASTORE;
                case ReturnType.Boolean z -> BASTORE;
                case ReturnType.Float f -> FASTORE;
                default -> throw new RuntimeException("Cannot recognize " + type);
            };
        }
        return AASTORE;
    }

    public static int toReturn(ReturnType type) {
        if (type.isPrimitive()) {
            return switch (type) {
                case ReturnType.Integer i -> IRETURN;
                case ReturnType.Boolean z -> IRETURN;
                case ReturnType.Float f -> FRETURN;
                default -> throw new RuntimeException("Cannot recognize " + type);
            };
        }
        return ARETURN;
    }
}
