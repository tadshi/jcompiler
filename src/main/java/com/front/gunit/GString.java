package com.front.gunit;

public class GString extends ObjectClass{
    private String string;

    public void setString(String string){
        this.string = string;
    }

    @Override
    public String toString() {
        return "STRINGTK";
    }

    public String  getString() {
        return string;
    }
}
