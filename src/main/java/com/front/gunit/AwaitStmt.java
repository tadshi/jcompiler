package com.front.gunit;

public class AwaitStmt extends ObjectClass implements StmtTrait{
    private ObjectClass awaitItem;
    //ident里暂时没存东西，都存在awaitItem里了
    private Ident ident;

    public void setAwaitItem(ObjectClass awaitItem){ this.awaitItem = awaitItem;}

    // public void setIdent(Ident ident){this.ident = ident;}

    public ObjectClass getAwaitItem(){ return awaitItem;}

    public Ident getIdent() {return ident;}

}
