package com.front.gunit;

public class PutItemStmt extends ObjectClass implements StmtTrait{
    //在列表中加入元素
    private Ident ident;

    private Exp exp;

    public void setIdent(Ident ident){
        this.ident = ident;
    }

    public void setExp(Exp exp){
        this.exp = exp;
    }

    public Ident getIdent(){ return ident;}

    public Exp getExp(){return exp;}
}
