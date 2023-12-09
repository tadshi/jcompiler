package com.front.gunit;
public class LValDecl extends Stmt{
    LVal lVal;
    Exp exp;

    public void setLVal(LVal lVal){
        this.lVal = lVal;
    }

    public void setExp(Exp exp){
        this.exp = exp;
    }
}
