package com.front.gunit;
public class IfStmt extends Stmt{
    Cond cond;
    Stmt stmt;
    ElseStmt elseStmt;

    public void setCond(Cond cond){
        this.cond = cond;
    }

    public void setStmt(Stmt stmt){
        this.stmt = stmt;
    }

    public void setElseStmt(ElseStmt elseStmt){
        this.elseStmt = elseStmt;
    }
    
}
