package com.front.gunit;
import java.util.ArrayList;

public class VarDecl extends Decl{
    private ArrayList<VarDef> varDefs = new ArrayList<>();

    public void addVarDef(VarDef varDef){
        varDefs.add(varDef);
    }

    public ArrayList<VarDef> getVarDefs() {
        return varDefs;
    }
}
