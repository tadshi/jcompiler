package com.front.gunit;
import java.util.ArrayList;

public class FuncFParams extends ObjectClass{
    private ArrayList<FuncFParam> funcFParams = new ArrayList<>();

    public void addFuncFParam(FuncFParam funcFParam){
        funcFParams.add(funcFParam);
    }

    public ArrayList<FuncFParam> getFuncFParams() {
        return funcFParams;
    }
}
