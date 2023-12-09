package com.front.gunit;
import java.util.ArrayList;

public class InitValList extends ObjectClass{
    ArrayList<InitVal> initVals = new ArrayList<>();

    public void addInitVal(InitVal initVal){
        initVals.add(initVal);
    }
}
