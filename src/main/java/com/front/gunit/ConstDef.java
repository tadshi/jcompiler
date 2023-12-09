package com.front.gunit;
import java.util.ArrayList;

public class ConstDef extends ObjectClass{
    private final ArrayList<ConstExp> constExps = new ArrayList<>();
    private ConstInitVal constInitVal;
    private Ident ident;

    public void addConstExp(ConstExp constExp){
        constExps.add(constExp);
    }

    public void setConstInitVal(ConstInitVal constInitVal){
        this.constInitVal = constInitVal;
    }

    public void setIdent(Ident ident){
        this.ident = ident;
    }
    
    public ArrayList<ConstExp> getConstExps() {
        return constExps;
    }

    public ConstInitVal getConstInitVal() {
        return constInitVal;
    }

    public Ident getIdent() {
        return ident;
    }
}
