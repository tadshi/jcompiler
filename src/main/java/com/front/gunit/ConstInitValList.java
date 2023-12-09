package com.front.gunit;
import java.util.ArrayList;

public class ConstInitValList extends ObjectClass{
    private ArrayList<ConstInitVal> constInitVals = new ArrayList<>();

    public void addConstInitVal(ConstInitVal constInitVal){
        constInitVals.add(constInitVal);
    }

    public ArrayList<ConstInitVal> getConstInitVals() {
        return constInitVals;
    }
}
