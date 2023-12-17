package com.front.gunit;

public class ReturnStmt extends ObjectClass implements StmtTrait {
    private Exp exp;
    boolean isLastReturn = false;

    public void setExp(Exp exp){
        this.exp = exp;
    }

    public void setLastReturn(){this.isLastReturn = true;}

    public Exp getExp() {
        return exp;
    }

    public boolean isLastReturn() {
        return isLastReturn;
    }
}
