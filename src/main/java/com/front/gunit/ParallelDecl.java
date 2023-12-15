package com.front.gunit;

import java.util.ArrayList;

public final class ParallelDecl extends Decl{
    private ParallelType parallelType;
    private ArrayList<VarDef> varDefs = new ArrayList<>();

    public void addVarDef(VarDef varDef){
        varDefs.add(varDef);
    }

    public void setParallelType(ParallelType parallelType) {this.parallelType = parallelType;}

    public ArrayList<VarDef> getVarDefs() {
        return varDefs;
    }

    public ParallelType getParallelType() {return parallelType;}
}
