package com.cotoj.adaptor;

public abstract sealed class FuncParamNode permits SimpleFuncParamNode, ArrayFuncParamNode {
    private String name;

    public FuncParamNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String getDescriptor();

    public abstract DefNode toDef();
}
