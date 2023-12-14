package com.cotoj.adaptor;

import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;

public final class VariableFuncParamNode extends FuncParamNode {
    
    public VariableFuncParamNode(String name, ReturnType type) {
        super(name, type);
    }

    @Override
    public DefNode toDef() {
        ArrayDefNode ret = new ArrayDefNode(getName(), new Owner.Local(), getType(), true);
        ret.addDimension(0);
        return ret;
    }

    @Override
    public String getDescriptor() {
        return "[" + this.getType().toDescriptor();
    }
}
