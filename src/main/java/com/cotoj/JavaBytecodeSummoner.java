package com.cotoj;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

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
                case ParallelDecl parallelDecl -> {
                    for (VarDef varDef : parallelDecl.getVarDefs()) {
                        staticSummoner.parseStaticParallelDef(varDef, parallelDecl.getParallelType(), symbolTable);
                    }
                }
            }
        }
        staticSummoner.masterUp();
        for (FuncDef funcDef : compUnit.getFuncDefs()) {
            if (funcDef.isThread() || funcDef.isRoutine()) {
                mainSummoner.summonThreadFunc(funcDef, symbolTable);
            } else {
                mainSummoner.summonFunc(funcDef, symbolTable);
            }
        }
        mainSummoner.summonFunc(Mimic.mimicFuncDef(compUnit.getMainFuncDef()), symbolTable);
        mainSummoner.masterUp();
        masterUp = true;
    }

    public void save(Path outputFolder) {
        try {
            Path classFileFolder = outputFolder.resolve("com/oto");
            Files.createDirectories(outputFolder.resolve("com/oto"));
            Files.createDirectories(outputFolder.resolve("com/threads"));
            staticSummoner.save(classFileFolder.resolve("Static.class"));
            mainSummoner.save(classFileFolder.resolve("Main.class"), outputFolder.resolve("com/threads"));
        } catch(IOException err) {
            err.printStackTrace();
            System.err.println("Fail to save classfiles.");
        }
    }

    private void putJEntry(String entryName, JarOutputStream jStream) throws IOException {
            JarEntry entry = new JarEntry(entryName);
            entry.setTime(System.currentTimeMillis());
            jStream.putNextEntry(entry);
    }

    public void saveJar(Path outputPath) {
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(Attributes.Name.MAIN_CLASS, "com.oto.Main");
        try {
            File outputJar = outputPath.toFile();
            OutputStream oStream = new PrintStream(outputJar);
            JarOutputStream jStream = new JarOutputStream(oStream, manifest);
            putJEntry("com/oto/Main.class", jStream);
            jStream.write(mainSummoner.getBytes());
            jStream.closeEntry();
            putJEntry("com/oto/Static.class", jStream);
            jStream.write(staticSummoner.getBytes());
            jStream.closeEntry();
            for (var pair : mainSummoner.getThreads().entrySet()) {
                putJEntry("com/threads/Thread" + pair.getKey() + ".class", jStream);
                jStream.write(pair.getValue().toByteArray());
                jStream.closeEntry();
            }
            jStream.close();
        } catch (IOException err) {
            System.err.println("Fail to create jar file.");
        }
    }
}
