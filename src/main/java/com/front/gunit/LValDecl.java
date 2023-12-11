package com.front.gunit;
public class LValDecl extends ObjectClass{
    private LVal lVal;
    private Exp exp;

    public void setLVal(LVal lVal){
        this.lVal = lVal;
    }

    public void setExp(Exp exp){
        this.exp = exp;
    }

    public Exp getExp() {
        return exp;
    }

    public LVal getlVal() {
        return lVal;
    }
}
