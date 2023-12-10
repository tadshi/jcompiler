package com.cotoj.adaptor;

public class SimpleFuncParamNode extends FuncParamNode {
    public SimpleFuncParamNode(String name) {
        super(name);
    }

    @Override
    public String getTypeString() {
        return "I";
    }
}
