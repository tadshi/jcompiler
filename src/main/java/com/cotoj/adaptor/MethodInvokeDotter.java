package com.cotoj.adaptor;

import java.util.ArrayList;
import java.util.List;

import com.cotoj.utils.ReturnType;
import com.front.gunit.Exp;

public final class MethodInvokeDotter {
    private final String identName;
    private final ReturnType returnType;
    private List<FuncParamNode> defParams;
    private List<Exp> callParams;
    boolean isInterface;

    public MethodInvokeDotter(String identName, ReturnType returnType, boolean isInterface) {
        this.identName = identName;
        this.returnType = returnType;
        this.isInterface = isInterface;
        this.defParams = new ArrayList<>();
        this.callParams = new ArrayList<>();
    }

    public void addDefParam(FuncParamNode node) {
        defParams.add(node);
    }

    public void addCallParam(Exp callParam) {
        callParams.add(callParam);
    }

    public List<Exp> getCallParams() {
        return callParams;
    }

    public List<FuncParamNode> getDefParams() {
        return defParams;
    }
    
    public boolean isInterface() {
        return isInterface;
    }
    public String getIdentName() {
        return identName;
    }

    public ReturnType getReturnType() {
        return returnType;
    }
}
