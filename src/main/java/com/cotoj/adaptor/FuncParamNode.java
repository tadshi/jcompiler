package com.cotoj.adaptor;

import com.cotoj.utils.ReturnType;

public abstract sealed class FuncParamNode permits SimpleFuncParamNode, ArrayFuncParamNode {
    private String name;
    private ReturnType type;

    public FuncParamNode(String name, ReturnType type) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public ReturnType getType() {
        return type;
    }

    public abstract String getDescriptor();

    public abstract DefNode toDef();
}
