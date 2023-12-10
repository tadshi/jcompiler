package com.front.gunit;
public class PrimaryExp extends ObjectClass {
    private ObjectClass wrappedExp = new ObjectClass();

    public void setWrappedExp(ObjectClass primaryExp){
        this.wrappedExp = primaryExp;
    }

    public ObjectClass getWrappedExp() {
        return wrappedExp;
    }
}
