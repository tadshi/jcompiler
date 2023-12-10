package com.front.gunit;
public class UnaryExp extends ObjectClass {
    private ObjectClass wrappedExp;

    public void setWrappedExp(ObjectClass wrappedUnaryExp){
        this.wrappedExp = wrappedUnaryExp;
    }

    public ObjectClass getWrappedExp() {
        return wrappedExp;
    }
}
