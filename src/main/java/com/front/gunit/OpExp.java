package com.front.gunit;
public class OpExp extends Exp{
    private UnaryExp unaryExp;
    private UnaryOp unaryOp;

    public void setUnaryExp(UnaryExp unaryExp){
        this.unaryExp = unaryExp;
    }

    public void setUnaryOp(UnaryOp unaryOp){
        this.unaryOp = unaryOp;
    }

    public UnaryExp getUnaryExp() {
        return unaryExp;
    }

    public UnaryOp getUnaryOp() {
        return unaryOp;
    }
}
