package com.cotoj;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.cotoj.adaptor.Mimic;
import com.front.gunit.*;

public final class JavaBytecodeSummoner {
    private final SymbolTable symbolTable;
    private final StaticSummoner staticSummoner;
    private final MainSummoner mainSummoner;
    private boolean masterUp;
    
    public JavaBytecodeSummoner(Path logFolder) {
        symbolTable = new SymbolTable();
        try {
            staticSummoner = new StaticSummoner(logFolder.resolve("Static").toFile(), symbolTable);
            mainSummoner = new MainSummoner(logFolder.resolve("Main").toFile());
        } catch (FileNotFoundException exp) {
            exp.printStackTrace();
            throw new RuntimeException("Fail to create log files.");
        }
        this.masterUp = false;
    }

    public void summon(CompUnit compUnit) {
        if (masterUp) {
            throw new RuntimeException("Today we're on strike!");
        }
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
            }
        }
        staticSummoner.masterUp();
        for (FuncDef funcDef : compUnit.getFuncDefs()) {
            mainSummoner.summonFunc(funcDef, symbolTable);
        }
        mainSummoner.summonFunc(Mimic.mimicFuncDef(compUnit.getMainFuncDef()), symbolTable);
        mainSummoner.masterUp();
        masterUp = true;
    }

    public void save(Path outputFolder) {
        try {
            Path classFileFolder = outputFolder.resolve("com/oto");
            Files.createDirectories(outputFolder.resolve("com/oto"));
            staticSummoner.save(classFileFolder.resolve("Static.class"));
            mainSummoner.save(classFileFolder.resolve("Main.class"));
        } catch(IOException err) {
            err.printStackTrace();
            System.err.println("Fail to save classfiles.");
        }
    }
}
