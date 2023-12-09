package com.front.gunit;
public class RelExp extends Exp{
    AddExp addExp;
    String ch;
    RelExp relExp;

    public void setAddExp(AddExp addexp){
        this.addExp = addexp;
    }

    public void setRelExp(RelExp relExp){
        this.relExp = relExp;
    }

    public void setCh(String ch){
        this.ch = ch;
    }
}
