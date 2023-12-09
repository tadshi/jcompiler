package com.front.gunit;
import java.util.ArrayList;

public class CompUnit extends ObjectClass{
    ArrayList<Decl> decls = new ArrayList<>();
    ArrayList<FuncDef> funcDefs = new ArrayList<>();
    MainFuncDef mainFuncDef;

    public void addDecl(Decl decl) {
        decls.add(decl);
    }

    public void addFuncDef(FuncDef funcDef){
        funcDefs.add(funcDef);
    }

    public void setMainFuncDef(MainFuncDef mainFuncDef){
        this.mainFuncDef = mainFuncDef;
    }
}
