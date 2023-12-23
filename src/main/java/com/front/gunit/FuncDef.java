package com.front.gunit;
public class FuncDef extends ObjectClass{
    private boolean isThread = false;
    private boolean isRoutine = false;
    private FuncType funcType;
    private Ident ident;
    private FuncFParams funcFParams;
    private Block block;

    public void setIsThread() {this.isThread = true;}

    public void setRoutine(boolean isRoutine) {
        this.isRoutine = isRoutine;
    }

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

    public boolean isThread() {
        return isThread;
    }

    public Block getBlock() {
        return block;
    }

    public FuncFParams getFuncFParams() {
        return funcFParams;
    }

    public FuncType getFuncType() {
        return funcType;
    }

    public Ident getIdent() {
        return ident;
    }

    public boolean isRoutine() {
        return isRoutine;
    }
}
