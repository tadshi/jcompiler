package com.cotoj.adaptor;

import java.util.ArrayList;
import java.util.List;


import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;

public final class FuncDefNode extends DefNode {
    private ReturnType returnType;
    private List<FuncParamNode> params;
    private boolean isThread;

    public FuncDefNode(String name, Owner owner, ReturnType returnType, boolean isThread) {
        super(name, owner, null); // This is on purpose. Returntype may not be real Type...
        this.returnType = returnType;
        this.params = new ArrayList<>();
        this.isThread = isThread;
    }

    public void addParam(FuncParamNode param) {
        if (params.size() > 0 && params.getLast() instanceof VariableFuncParamNode) {
            throw new CError(ErrorType.UNEXPECTED_TOKEN, "Variable param must be the last one!");
        }
        params.add(param);
    }

    public List<FuncParamNode> getParams() {
        return params;
    }

    @Deprecated
    @Override
    public ReturnType getType() {
        throw new RuntimeException("No, you cannot call this.");
    }

    public ReturnType getReturnType() {
        return returnType;
    }

    public boolean isThread() {
        return isThread;
    }

    @Override
    public String getDescriptor() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (FuncParamNode param : params) {
            sb.append(param.getDescriptor());
        }
        sb.append(')');
        sb.append(returnType.toDescriptor());
        return sb.toString();
    }
}
