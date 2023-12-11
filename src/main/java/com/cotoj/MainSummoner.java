package com.cotoj;

import java.io.File;
import java.io.FileNotFoundException;

import org.objectweb.asm.Label;
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
import com.front.gunit.Exp;
import com.front.gunit.ExpStmt;
import com.front.gunit.ConstDecl;
import com.front.gunit.ConstDef;
import com.front.gunit.VarDecl;
import com.front.gunit.VarDef;
import com.front.gunit.WhileStmt;
import com.front.gunit.FuncDef;
import com.front.gunit.IfStmt;
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
                        if (arrayDef.getDimSizes().size() - 1 != lVal.getExps().size()) {
                            throw new CError(ErrorType.ARRAY_DIM_ERROR, "Expect " + arrayDef.getDimSizes().size() + ", found " + lVal.getExps().size());
                        }
                        switch (arrayDef.getOwner()) {
                            case Owner.Static() -> mv.visitFieldInsn(GETSTATIC, "com/oto/Static", arrayDef.getName(), arrayDef.getDescriptor());
                            case Owner.Local() -> mv.visitIntInsn(ALOAD, helper.getVarIndex(arrayDef));
                            default -> throw new RuntimeException("We cannot recognize " + arrayDef.getOwner() + "in lval.");
                        }
                        helper.reportUseOpStack(1, arrayDef.getDescriptor());
                        for (int index = 0; index < lVal.getExps().size() - 1; ++index) {
                            ExpSummoner.summonExp(lVal.getExps().get(index), mv, helper, table);
                            mv.visitInsn(AALOAD);
                            helper.reportPopOpStack(2);
                            helper.reportUseOpStack(1, arrayDef.getIndexedTypeString(index + 1));
                        }
                        ExpSummoner.summonExp(lVal.getExps().getLast(), mv, helper, table);
                        ExpSummoner.summonExp(assignStmt.getExp(), mv, helper, table);
                        mv.visitInsn(IASTORE);
                        helper.reportPopOpStack(3);
                    }
                    case FuncDefNode funcDef -> throw new CError(ErrorType.UNEXPECTED_TOKEN, "No, you cannot assign a function.");
                }
            }
            case ExpStmt expStmt -> {
                Exp exp = expStmt.getExp();
                if (exp != null) {
                    ExpSummoner.summonExp(exp, mv, helper, table);
                    mv.visitInsn(POP);
                    helper.reportPopOpStack(1);
                }
                // else nop?
            }
            case Block block -> {
                table.pushContext();
                summonBlock(block, mv, table, helper);
                table.popContext();
            }
            case IfStmt ifStmt -> {
                Label start = new Label();
                Label else_stmt = new Label();
                ExpSummoner.summonLOrExp(ifStmt.getCond().getlOrExp(), mv, helper, table, start);
                mv.visitJumpInsn(IFEQ, else_stmt);
                summonStmt(ifStmt.getStmt(), mv, table, helper);
                if (ifStmt.getElseStmt() == null) {
                    mv.visitLabel(else_stmt);
                    helper.visitFrame(mv);
                } else {
                    Label end = new Label();
                    mv.visitJumpInsn(GOTO, end);
                    mv.visitLabel(else_stmt);
                    helper.visitFrame(mv);
                    summonStmt(ifStmt.getElseStmt(), mv, table, helper);
                    mv.visitLabel(end);
                    helper.visitFrame(mv);
                }
            }
            case WhileStmt whileStmt -> {
                Label start = new Label();
                Label end = new Label();
                ExpSummoner.summonLOrExp(whileStmt.getCond().getlOrExp(), mv, helper, table, end);
                mv.visitJumpInsn(IFEQ, end);
                mv.visitLabel(start);
                helper.visitFrame(mv);
                summonStmt(whileStmt.getStmt(), mv, table, helper);
                ExpSummoner.summonLOrExp(whileStmt.getCond().getlOrExp(), mv, helper, table, end);
                mv.visitJumpInsn(IFNE, start);
                mv.visitLabel(end);
                helper.visitFrame(mv);
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
