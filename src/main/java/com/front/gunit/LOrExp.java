package com.front.gunit;
public class LOrExp extends ObjectClass{
    private LAndExp lAndExp;
    private String ch;
    private LOrExp lOrExp;

    public void setLAndExp(LAndExp lAndexp){
        this.lAndExp = lAndexp;
    }

    public void setLOrExp(LOrExp lOrExp){
        this.lOrExp = lOrExp;
    }

    public void setCh(String ch){
        this.ch = ch;
    }

    public String getCh() {
        return ch;
    }
    
    public LAndExp getlAndExp() {
        return lAndExp;
    }

    public LOrExp getlOrExp() {
        return lOrExp;
    }
}
