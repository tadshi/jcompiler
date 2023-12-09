package com.front.gunit;
public class MulExp extends Exp{
    private UnaryExp unaryExp;
    private String ch;
    private MulExp mulExp;

    public void setUnaryExp(UnaryExp unaryExp){
        this.unaryExp = unaryExp;
    }

    public void setMulExp(MulExp mulExp){
        this.mulExp = mulExp;
    }

    public void setCh(String ch){
        this.ch = ch;
    }

    public String getCh() {
        return ch;
    }

    public MulExp getMulExp() {
        return mulExp;
    }

    public UnaryExp getUnaryExp() {
        return unaryExp;
    }
    
}
