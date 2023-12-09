package com.front.gunit;
import java.util.ArrayList;

public class ConstDecl extends Decl{
    ArrayList<ConstDef> constDefs = new ArrayList<>();

    public void addconstDef(ConstDef constDef){
        constDefs.add(constDef);
    }
}
