package com.cotoj;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

import com.front.gunit.*;

public class JavaBytecodeSummoner {
    SymbolTable symbolTable;
    StaticSummoner staticSummoner;
    MainSummoner mainSummoner;
    
    public JavaBytecodeSummoner(Path logFolder) {
        symbolTable = new SymbolTable();
        try {
            staticSummoner = new StaticSummoner(logFolder.resolve("Static").toFile());
            mainSummoner = new MainSummoner(logFolder.resolve("Main").toFile());
        } catch (FileNotFoundException exp) {
            System.err.println("Fail to create log files.");
            exp.printStackTrace();
        }
    }

    public void summonBlock() {

    }
    
    public void summon(CompUnit compUnit) {
        List<Decl> decls = compUnit.getDecls();
        for (Decl decl : decls) {
            switch(decl) {
                case VarDecl varDecl -> {
                    for (VarDef varDef : varDecl.getVarDefs()) {
                        staticSummoner.parseStaticDef(varDef, symbolTable);
                    }
                }
                case ConstDecl constDecl -> {
                    for (ConstDef constDef : constDecl.getConstDefs()) {
                        staticSummoner.parseStaticFinalDef(constDef, symbolTable);
                    }
                }
                default -> throw new RuntimeException("How do you turn this on?");
            }
        }
        staticSummoner.masterUp();
        
    }
}
