package com.front.gunit;
public class WhileStmt extends ObjectClass implements StmtTrait{
    private Cond cond;
    private Stmt stmt;

    public void setCond(Cond cond){
        this.cond = cond;
    }

    public void setStmt(Stmt stmt) {
        this.stmt = stmt;
    }

    public Cond getCond() {
        return cond;
    }

    public Stmt getStmt() {
        return stmt;
    }
}
