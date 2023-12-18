package com.front.gunit;

public class GBool extends ObjectClass{
    private boolean bool;

    public void setBool(Boolean bool){
        this.bool = bool;
    }

    @Override
    public String toString() {
        return "BOOLTK";
    }

    public Boolean getBool() {
        return bool;
    }
}
