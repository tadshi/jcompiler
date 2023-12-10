package com.front.gunit;
public class FuncCall extends ObjectClass{
    private Ident ident;
    private FuncRParams funcRParams;

    public void setIdent(Ident ident){
        this.ident = ident;
    }

    public void setFuncRParams(FuncRParams funcRParams){
        this.funcRParams = funcRParams;
    }

    public FuncRParams getFuncRParams() {
        return funcRParams;
    }

    public Ident getIdent() {
        return ident;
    }
    
}
