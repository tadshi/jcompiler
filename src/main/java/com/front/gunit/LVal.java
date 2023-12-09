package com.front.gunit;
import java.util.ArrayList;

public class LVal extends ObjectClass{
    private ArrayList<Exp> exps = new ArrayList<>();
    private Ident ident;

    public void addExp(Exp exp){
        exps.add(exp);
    }

    public void setIdent(Ident ident){
        this.ident = ident;
    }

    public Ident getIdent() {
        return ident;
    }

    public ArrayList<Exp> getExps() {
        return exps;
    }
}
