package com.front.gunit;
public class FuncDef extends ObjectClass{
    FuncType funcType;
    Ident ident;
    FuncFParams funcFParams;
    Block block;


    public void setFuncType(FuncType funcType){
        this.funcType = funcType;
    }

    public void setIdent(Ident ident){
        this.ident = ident;
    }

    public void setFuncFParams(FuncFParams funcFParams){
        this.funcFParams = funcFParams;
    }

    public void setBlock(Block block){
        this.block = block;
    }
}
