package com.front.gunit;
import java.util.ArrayList;

public class FuncRParams extends ObjectClass{
    private ArrayList<Exp> exps = new ArrayList<>();

    public void addExp(Exp exp){
        exps.add(exp);
    }

    public ArrayList<Exp> getExps() {
        return exps;
    }
}
