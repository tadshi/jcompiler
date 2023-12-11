package com.cotoj.adaptor;

import com.cotoj.utils.Owner;

public final class SimpleFuncParamNode extends FuncParamNode {
    public SimpleFuncParamNode(String name) {
        super(name);
    }

    @Override
    public String getDescriptor() {
        return "I";
    }

    @Override
    public DefNode toDef() {
        return new VarDefNode(getName(), new Owner.Local(), true);
    }
}
