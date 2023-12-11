package com.front.gunit;
public class ExpStmt extends ObjectClass implements StmtTrait {
    private Exp exp;
    
    public void setExp(Exp exp){
        this.exp = exp;
    }
    
    public Exp getExp() {
        return exp;
    }
}
