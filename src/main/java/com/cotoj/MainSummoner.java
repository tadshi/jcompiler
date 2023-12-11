package com.cotoj;

import java.io.File;
import java.io.FileNotFoundException;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.VarDefNode;
import com.cotoj.utils.ClassMaker;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.MethodHelper;
import com.cotoj.utils.Owner;
import com.cotoj.utils.SymbolType;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.Block;
import com.front.gunit.BlockItem;
import com.front.gunit.Decl;
import com.front.gunit.ConstDecl;
import com.front.gunit.ConstDef;
import com.front.gunit.VarDecl;
import com.front.gunit.VarDef;
import com.front.gunit.FuncDef;
import com.front.gunit.LVal;
import com.front.gunit.LValDecl;
import com.front.gunit.Stmt;

public class MainSummoner extends ClassMaker implements Opcodes {
    public MainSummoner(File logFile) throws FileNotFoundException {
        super(logFile);
        cv.visit(V17, ACC_PUBLIC + ACC_INTERFACE + ACC_ABSTRACT, "com/oto/Main", null, "java/lang/Object", null);
    }

    public void summonStmt(Stmt stmt, MethodVisitor mv, SymbolTable table, MethodHelper helper) {
        switch (stmt.getWrappedStmt()) {
            case LValDecl assignStmt -> {
                LVal lVal = assignStmt.getlVal();
                IdentEntry entry = table.getEntry(lVal.getIdent().getName(), SymbolType.fromString(lVal.getIdent().getKind()));
                switch (entry.getDef()) {
                    case VarDefNode varDef -> {
                        ExpSummoner.summonExp(assignStmt.getExp(), mv, helper, table);
                        switch (varDef.getOwner()) {
                            case Owner.Static() -> mv.visitFieldInsn(PUTSTATIC, "com/oto/Static", varDef.getName(), varDef.getDescriptor());
                            case Owner.Local() -> mv.visitIntInsn(ISTORE, helper.getVarIndex(varDef));
                            default -> throw new RuntimeException("We cannot recognize " + varDef.getOwner() + "in lval.");
                        }
                        helper.reportPopOpStack(1);
                    }
                    case ArrayDefNode arrayDef -> {
                        
                    }
                    case FuncDefNode funcDef -> throw new CError(ErrorType.UNEXPECTED_TOKEN, "No, you cannot assign a function.");
                }
                
            }
            default -> throw new RuntimeException("Cannot recognise " + stmt.getWrappedStmt().getClass() + " as a statement.");
        }
    }

    public void summonDecl(Decl decl, MethodVisitor mv, SymbolTable table, MethodHelper helper) {
        switch (decl) {
            case ConstDecl constDecl -> {
                for (ConstDef constDef : constDecl.getConstDefs()) {
                    IdentEntry entry = table.addConstDef(constDef);
                    helper.registerLocal(entry.getDef());
                }
            }
            case VarDecl varDecl -> {
                for (VarDef varDef : varDecl.getVarDefs()) {
                    IdentEntry entry = table.addVarDef(varDef);
                    helper.registerLocal(entry.getDef());
                }
            }
        }
    }

    public void summonBlock(Block block, MethodVisitor mv, SymbolTable table, MethodHelper helper) {
        for (BlockItem blockItem : block.getBlockItems()) {
            switch(blockItem.getWrappedBlockItem()) {
                case Decl decl -> summonDecl(decl, mv, table, helper);
                case Stmt stmt -> summonStmt(stmt, mv, table, helper);
                default -> throw new RuntimeException("No, you cannot have " + blockItem.getWrappedBlockItem().getClass() +
                            " in your block.");
            };
        }
    }

    public void summonFunc(FuncDef funcDef, SymbolTable table) {
        IdentEntry entry = table.addFuncDef(funcDef);
        FuncDefNode funcDefNode = ((FuncDefNode)entry.getDef());
        MethodVisitor mv = cv.visitMethod(ACC_STATIC + ACC_PRIVATE, funcDef.getIdent().getName(), null, null, null);
        table.pushContext();
        table.loadFunctionParams(entry.getName());
        MethodHelper helper = new MethodHelper(funcDefNode);
        summonBlock(funcDef.getBlock(), mv, table, helper);
        table.popContext();
    }

}
