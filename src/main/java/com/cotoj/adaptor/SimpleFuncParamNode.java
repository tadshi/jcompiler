package com.cotoj.adaptor;

import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;

public final class SimpleFuncParamNode extends FuncParamNode {
    public SimpleFuncParamNode(String name, ReturnType type) {
        super(name, type);
    }

    @Override
    public String getDescriptor() {
        return "I";
    }

    @Override
    public DefNode toDef() {
        return new VarDefNode(getName(), new Owner.Local(), getType(), true);
    }
}
