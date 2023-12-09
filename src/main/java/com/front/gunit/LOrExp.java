package com.front.gunit;
public class LOrExp extends Exp{
    LAndExp lAndExp;
    String ch;
    LOrExp lOrExp;

    public void setLAndExp(LAndExp lAndexp){
        this.lAndExp = lAndexp;
    }

    public void setLOrExp(LOrExp lOrExp){
        this.lOrExp = lOrExp;
    }

    public void setCh(String ch){
        this.ch = ch;
    }
}
