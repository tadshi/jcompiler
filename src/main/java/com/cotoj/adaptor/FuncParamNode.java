package com.cotoj.adaptor;

import com.cotoj.utils.ReturnType;

public abstract sealed class FuncParamNode permits SimpleFuncParamNode, ArrayFuncParamNode, VariableFuncParamNode {
    private final String name;
    private final ReturnType type;

    public FuncParamNode(String name, ReturnType type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public ReturnType getType() {
        return type;
    }

    public String getDescriptor() {
        return type.toDescriptor();
    }

    public abstract DefNode toDef();
}
