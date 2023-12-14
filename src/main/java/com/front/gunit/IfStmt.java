package com.front.gunit;
public class IfStmt extends ObjectClass implements StmtTrait {
    private Cond cond;
    private Stmt stmt;
    private Stmt elseStmt;

    public void setCond(Cond cond){
        this.cond = cond;
    }

    public void setStmt(Stmt stmt){
        this.stmt = stmt;
    }

    public void setElseStmt(Stmt elseStmt){
        this.elseStmt = elseStmt;
    }
    
    public Cond getCond() {
        return cond;
    }

    public Stmt getElseStmt() {
        return elseStmt;
    }

    public Stmt getStmt() {
        return stmt;
    }
}
