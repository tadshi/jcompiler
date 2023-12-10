package com.cotoj.adaptor;

public abstract class FuncParamNode {
    private String name;

    public FuncParamNode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract String getTypeString();
}
