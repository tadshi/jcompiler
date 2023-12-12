package com.cotoj.adaptor;

import java.util.List;

public final class MethodCallDotter extends Dotter {
    private List<FuncParamNode> params;
    public MethodCallDotter(String identName) {
        super(identName);
    }

    public void addParam(FuncParamNode node) {
        addParam(node);
    }
    
    public List<FuncParamNode> getParams() {
        return params;
    }
}
