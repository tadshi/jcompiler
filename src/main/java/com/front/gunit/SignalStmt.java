package com.front.gunit;

public class SignalStmt extends Stmt implements StmtTrait{
    private Ident ident;

    public void setIdent(Ident ident){ this.ident = ident;}

    public Ident getIdent(){ return ident;}
}
