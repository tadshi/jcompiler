package com.front.gunit;
public class Stmt extends ObjectClass {
    private StmtTrait wrappedStmt;

    public void setWrappedStmt(StmtTrait stmt){
        this.wrappedStmt = stmt;
    }

    public StmtTrait getWrappedStmt() {
        return wrappedStmt;
    }
}
