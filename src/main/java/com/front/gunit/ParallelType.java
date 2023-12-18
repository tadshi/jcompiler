package com.front.gunit;

public class ParallelType extends ObjectClass{
    private String name;

    private ObjectClass dataType;

    public void setName(String name){
        this.name = name;
    }

    public void setDataType(ObjectClass dataType){this.dataType = dataType;}

    public String getName() {
        return name;
    }

    public ObjectClass getDataType() {return dataType;}
}
