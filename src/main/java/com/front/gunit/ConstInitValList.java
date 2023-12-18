package com.front.gunit;
import java.util.ArrayList;
import java.util.HashMap;

public class ConstInitValList extends ObjectClass{
    private ArrayList<ConstInitVal> constInitVals = new ArrayList<>();

    private HashMap<InitVal, InitVal> initValsMap = new HashMap<>();

    public void addConstInitVal(ConstInitVal constInitVal){
        constInitVals.add(constInitVal);
    }

    public void addInitValMap(InitVal key, InitVal value) { initValsMap.put(key, value);}

    public ArrayList<ConstInitVal> getConstInitVals() {
        return constInitVals;
    }

    public HashMap<InitVal, InitVal> getInitValsMap(){return initValsMap;}
}
