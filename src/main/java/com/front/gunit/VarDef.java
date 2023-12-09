package com.front.gunit;
import java.util.ArrayList;

public class VarDef extends ObjectClass{
    private final ArrayList<ConstExp> constExpList = new ArrayList<>();
    private Ident ident;
    private InitVal initVal;

    public void setInitVal(InitVal initVal){
        this.initVal = initVal;
    }
    
    public void addConstExp(ConstExp constExp){
        constExpList.add(constExp);
    }

    public void setIdent(Ident ident){
        this.ident = ident;
    }

    public Ident getIdent() {
        return ident;
    }

    public InitVal getInitVal() {
        return initVal;
    }

    public boolean isArray() {
        return !constExpList.isEmpty();
    }
    
    public ArrayList<ConstExp> getConstExpList() {
        return constExpList;
    }
}
