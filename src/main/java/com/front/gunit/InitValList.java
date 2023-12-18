package com.front.gunit;
import java.util.ArrayList;
import java.util.HashMap;

public class InitValList extends ObjectClass{
    private ArrayList<InitVal> initVals = new ArrayList<>();
    private HashMap<InitVal, InitVal> initValsMap = new HashMap<>();

    public void addInitVal(InitVal initVal){
        initVals.add(initVal);
    }

    public void addInitValMap(InitVal key, InitVal value) { initValsMap.put(key, value);}

    public ArrayList<InitVal> getInitVals() {
        return initVals;
    }

    public HashMap<InitVal, InitVal> getInitValsMap(){return initValsMap;}
}
