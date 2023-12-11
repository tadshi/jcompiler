package com.front.gunit;
public class Stmt extends ObjectClass{
    private ObjectClass wrappedStmt;

    public void setWrappedStmt(ObjectClass stmt){
        this.wrappedStmt = stmt;
    }

    public ObjectClass getWrappedStmt() {
        return wrappedStmt;
    }
}
