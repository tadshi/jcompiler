package com.cotoj.adaptor;

import java.util.List;

import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;

public final class FuncDefNode extends DefNode {
    private ReturnType returnType;
    private List<FuncParamNode> params;

    public FuncDefNode(String name, Owner owner, ReturnType returnType) {
        super(name, owner);
        this.returnType = returnType;
    }

    public void addParam(FuncParamNode param) {
        params.add(param);
    }

    public List<FuncParamNode> getParams() {
        return params;
    }

    public ReturnType getReturnType() {
        return returnType;
    }

    @Override
    public String getDescriptor() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (FuncParamNode param : params) {
            sb.append(param.getDescriptor());
        }
        sb.append(')');
        switch (returnType) {
            case VOID -> sb.append('V');
            case INTEGER -> sb.append('I');
        }
        return sb.toString();
    }
}
