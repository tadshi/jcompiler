package com.front.gunit;
public class FuncFParam extends ObjectClass{
    String type = "Non-array";
    //type = 0,,非数组,type = 1,一维数组;type = 2,有constexp
    ConstExp constExp;
    Ident ident;

    public void setType(String type) {
        this.type = type;   
    }

    public void setConstExp(ConstExp constExp){
        this.constExp = constExp;
    }

    public void setIdent(Ident ident){
        this.ident = ident;
    }
}
