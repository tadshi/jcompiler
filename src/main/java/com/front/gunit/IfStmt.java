package com.front.gunit;
public class IfStmt extends ObjectClass implements StmtTrait {
    private Cond cond;
    private Stmt stmt;
    private ElseStmt elseStmt;

    public void setCond(Cond cond){
        this.cond = cond;
    }

    public void setStmt(Stmt stmt){
        this.stmt = stmt;
    }

    public void setElseStmt(ElseStmt elseStmt){
        this.elseStmt = elseStmt;
    }
    
    public Cond getCond() {
        return cond;
    }

    public ElseStmt getElseStmt() {
        return elseStmt;
    }

    public Stmt getStmt() {
        return stmt;
    }
}
