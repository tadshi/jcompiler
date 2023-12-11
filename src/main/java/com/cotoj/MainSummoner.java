package com.cotoj;

import java.io.File;
import java.io.FileNotFoundException;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.utils.ClassMaker;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.MethodHelper;
import com.front.gunit.FuncDef;

public class MainSummoner extends ClassMaker implements Opcodes {
    public MainSummoner(File logFile) throws FileNotFoundException {
        super(logFile);
        cv.visit(V17, ACC_PUBLIC + ACC_INTERFACE + ACC_ABSTRACT, "com/oto/Main", null, "java/lang/Object", null);
    }

    public void summonFunc(FuncDef funcDef, SymbolTable table) {
        IdentEntry entry = table.addFuncDef(funcDef);
        FuncDefNode funcDefNode = ((FuncDefNode)entry.getDef());
        MethodVisitor mv = cv.visitMethod(ACC_STATIC + ACC_PRIVATE, funcDef.getIdent().getName(), null, null, null);
        table.pushContext();
        table.loadFunctionParams(entry.getName());
        MethodHelper helper = new MethodHelper(funcDefNode);
        table.popContext();
    }

}
