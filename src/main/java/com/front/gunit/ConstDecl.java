package com.front.gunit;
import java.util.ArrayList;

public final class ConstDecl extends Decl{
    private ArrayList<ConstDef> constDefs = new ArrayList<>();

    public void addconstDef(ConstDef constDef){
        constDefs.add(constDef);
    }

    public ArrayList<ConstDef> getConstDefs() {
        return constDefs;
    }
}
