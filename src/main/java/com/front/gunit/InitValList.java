package com.front.gunit;
import java.util.ArrayList;

public class InitValList extends ObjectClass{
    private ArrayList<InitVal> initVals = new ArrayList<>();

    public void addInitVal(InitVal initVal){
        initVals.add(initVal);
    }

    public ArrayList<InitVal> getInitVals() {
        return initVals;
    }
}
