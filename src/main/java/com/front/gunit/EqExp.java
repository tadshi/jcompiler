package com.front.gunit;
public class EqExp extends ObjectClass{
    private RelExp relExp;
    private String ch;
    private EqExp eqExp;

    public void setRelExp(RelExp relExp){
        this.relExp = relExp;
    }

    public void setEqExp(EqExp eqExp){
        this.eqExp = eqExp;
    }

    public void setCh(String ch){
        this.ch = ch;
    }

    public String getCh() {
        return ch;
    }

    public EqExp getEqExp() {
        return eqExp;
    }

    public RelExp getRelExp() {
        return relExp;
    }
}
