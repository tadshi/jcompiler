package com.front.gunit;

public class GDict extends ObjectClass{
    private ObjectClass keyType;
    private ObjectClass valueType;

    public GDict(ObjectClass keyType, ObjectClass valueType){
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public String toString() {
        return "DICTTK";
    }

    public ObjectClass getKeyType() {
        return keyType;
    }

    public ObjectClass getValueType() {return valueType;}
}
