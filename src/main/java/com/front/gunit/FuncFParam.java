package com.front.gunit;

import java.util.ArrayList;
import java.util.List;

public class FuncFParam extends ObjectClass{
    private FuncParamType type = FuncParamType.NONARRAY;
    private List<ConstExp> constExp = new ArrayList<>();
    private Ident ident;

    public enum FuncParamType {
        NONARRAY, 
        ARRAY1D,
        ARRAYMULTID
    }

    public void setType(FuncParamType type) {
        this.type = type;   
    }

    public void addConstExp(ConstExp constExp){
        this.constExp.add(constExp);
    }

    public void setIdent(Ident ident){
        this.ident = ident;
    }

    public List<ConstExp> getConstExp() {
        return constExp;
    }

    public Ident getIdent() {
        return ident;
    }

    public FuncParamType getType() {
        return type;
    }
}
