package com.front.gunit;
import java.util.ArrayList;

public class CompUnit extends ObjectClass{
    private ArrayList<Decl> decls = new ArrayList<>();
    private ArrayList<FuncDef> funcDefs = new ArrayList<>();
    private MainFuncDef mainFuncDef;

    public void addDecl(Decl decl) {
        decls.add(decl);
    }

    public void addFuncDef(FuncDef funcDef){
        funcDefs.add(funcDef);
    }

    public void setMainFuncDef(MainFuncDef mainFuncDef){
        this.mainFuncDef = mainFuncDef;
    }

    public ArrayList<Decl> getDecls() {
        return decls;
    }

    public ArrayList<FuncDef> getFuncDefs() {
        return funcDefs;
    }

    public MainFuncDef getMainFuncDef() {
        return mainFuncDef;
    }
}
