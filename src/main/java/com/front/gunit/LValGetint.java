package com.front.gunit;
public class LValGetint extends ObjectClass implements StmtTrait {
    private LVal lVal;

    public void setLVal(LVal lVal){
        this.lVal = lVal;
    }

    public LVal getlVal() {
        return lVal;
    }
}
