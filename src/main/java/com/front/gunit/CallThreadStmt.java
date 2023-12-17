package com.front.gunit;

public class CallThreadStmt extends ObjectClass implements StmtTrait{
    private FuncCall funcCall;

    public void setFuncCall(FuncCall funcCall){ this.funcCall = funcCall;}

    public FuncCall getFuncCall(){ return funcCall;}
}
