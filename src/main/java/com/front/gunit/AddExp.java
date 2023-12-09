package com.front.gunit;
public class AddExp extends Exp{
    private MulExp mulExp;
    private String ch;
    private AddExp addExp;

    public void setMulExp(MulExp mulexp){
        this.mulExp = mulexp;
    }

    public void setAddExp(AddExp addExp){
        this.addExp = addExp;
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

    public MulExp getMulExp() {
        return mulExp;
    }
}
