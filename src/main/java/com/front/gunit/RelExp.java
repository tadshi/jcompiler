package com.front.gunit;
public class RelExp extends ObjectClass {
    private AddExp addExp;
    private String ch;
    private RelExp relExp;

    public void setAddExp(AddExp addexp){
        this.addExp = addexp;
    }

    public void setRelExp(RelExp relExp){
        this.relExp = relExp;
    }

    public void setCh(String ch){
        this.ch = ch;
    }

    public AddExp getAddExp() {
        return addExp;
    }

    public String getCh() {
        return ch;
    }

    public RelExp getRelExp() {
        return relExp;
    }
}
