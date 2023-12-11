package com.front.gunit;
public class LAndExp extends ObjectClass{
    private EqExp eqExp;
    private String ch;
    private LAndExp lAndExp;

    public void setEqExp(EqExp eqexp){
        this.eqExp = eqexp;
    }

    public void setLAndExp(LAndExp lAndExp){
        this.lAndExp = lAndExp;
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

    public LAndExp getlAndExp() {
        return lAndExp;
    }
}
