package com.front.gunit;

public class GList extends ObjectClass{
    private ObjectClass type;

    public GList(ObjectClass type){
        this.type = type;
    }

    @Override
    public String toString() {
        return "LISTTK";
    }

    public ObjectClass getType() {
        return type;
    }
}
