package com.cotoj;

import java.io.File;
import java.io.FileNotFoundException;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.DotExp;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.MethodInvokeDotter;
import com.cotoj.adaptor.Mimic;
import com.cotoj.adaptor.SimpleFuncParamNode;
import com.cotoj.adaptor.StaticAccessExp;
import com.cotoj.adaptor.VarDefNode;
import com.cotoj.adaptor.VariableFuncParamNode;
import com.cotoj.utils.ClassMaker;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.JavaType;
import com.cotoj.utils.MethodHelper;
import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;
import com.cotoj.utils.SymbolType;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.*;

public class MainSummoner extends ClassMaker implements Opcodes {
    private FuncDefNode currentFunc = null;
    
    public MainSummoner(File logFile) throws FileNotFoundException {
        super(logFile);
        cv.visit(V17, ACC_PUBLIC + ACC_INTERFACE + ACC_ABSTRACT, "com/oto/Main", null, "java/lang/Object", null);
    }

    private void summonStmt(StmtTrait stmt, MethodVisitor mv, SymbolTable table, MethodHelper helper) {
        switch (stmt) {
            case LValDecl assignStmt -> {
                LVal lVal = assignStmt.getlVal();
                IdentEntry entry = table.getEntry(lVal.getIdent().getName(), SymbolType.fromString(lVal.getIdent().getKind()));
                ReturnType lValType = entry.getType();
                ReturnType rValType;
                if (!entry.isMut()) {
                    throw new CError(ErrorType.ASSIGN_TO_CONST, "You cannot assign to a non-mut variable.");
                }
                switch (entry.getDef()) {
                    case VarDefNode varDef -> {
                        rValType = ExpSummoner.summonExp(assignStmt.getExp(), mv, helper, table);
                        switch (varDef.getOwner()) {
                            case Owner.Static sClass -> mv.visitFieldInsn(PUTSTATIC, sClass.className(), varDef.getName(), varDef.getDescriptor());
                            case Owner.Local() -> mv.visitVarInsn(ISTORE, helper.getVarIndex(varDef));
                            default -> throw new RuntimeException("We cannot recognize " + varDef.getOwner() + "in lval.");
                        }
                        helper.reportPopOpStack(1);
                    }
                    case ArrayDefNode arrayDef -> {
                        if (arrayDef.getDimSizes().size() - 1 != lVal.getExps().size()) {
                            throw new CError(ErrorType.ARRAY_DIM_ERROR, "Expect " + arrayDef.getDimSizes().size() + ", found " + lVal.getExps().size());
                        }
                        switch (arrayDef.getOwner()) {
                            case Owner.Static sClass -> mv.visitFieldInsn(GETSTATIC, sClass.className(), arrayDef.getName(), arrayDef.getDescriptor());
                            case Owner.Local() -> mv.visitVarInsn(ALOAD, helper.getVarIndex(arrayDef));
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
                        rValType = ExpSummoner.summonExp(assignStmt.getExp(), mv, helper, table);
                        mv.visitInsn(IASTORE);
                        helper.reportPopOpStack(3);
                    }
                    case FuncDefNode funcDef -> throw new CError(ErrorType.UNEXPECTED_TOKEN, "No, you cannot assign a function.");
                }
                if (!(lValType.getClass().equals(rValType.getClass()))) {
                    throw new CError(ErrorType.TYPE_MISMATCH, "Left " + lValType + " while right " + rValType);
                }
            }
            case ExpStmt expStmt -> {
                Exp exp = expStmt.getExp();
                if (exp != null) {
                    ReturnType expType = ExpSummoner.summonExp(exp, mv, helper, table);
                    if (!(expType instanceof ReturnType.Void)) {
                        mv.visitInsn(POP);
                        helper.reportPopOpStack(1);
                    }
                }
                // else nop?
            }
            case Block block -> {
                summonBlock(block, mv, table, helper);
            }
            case IfStmt ifStmt -> {
                Label start = new Label();
                Label else_stmt = new Label();
                ExpSummoner.summonLOrExp(ifStmt.getCond().getlOrExp(), mv, helper, table, start);
                mv.visitJumpInsn(IFEQ, else_stmt);
                helper.reportPopOpStack(1);
                summonStmt(ifStmt.getStmt().getWrappedStmt(), mv, table, helper);
                if (ifStmt.getElseStmt() == null) {
                    mv.visitLabel(else_stmt);
                    helper.visitFrame(mv);
                } else {
                    Label end = new Label();
                    mv.visitJumpInsn(GOTO, end);
                    mv.visitLabel(else_stmt);
                    helper.visitFrame(mv);
                    summonStmt(ifStmt.getElseStmt().getWrappedStmt(), mv, table, helper);
                    mv.visitLabel(end);
                    helper.visitFrame(mv);
                }
            }
            case WhileStmt whileStmt -> {
                Label start = new Label();
                Label end = new Label();

                mv.visitInsn(NOP);
                mv.visitLabel(start);
                helper.visitFrame(mv);
                ExpSummoner.summonLOrExp(whileStmt.getCond().getlOrExp(), mv, helper, table, end);
                mv.visitJumpInsn(IFEQ, end);
                helper.reportPopOpStack(1);
                //Summon loop
                StmtTrait loopers = whileStmt.getStmt().getWrappedStmt();
                table.pushContext();
                table.registerLoop(start, end);
                if (loopers instanceof Block block) {
                    summonBlock(block, mv, table, helper);
                } else {
                    summonStmt(loopers, mv, table, helper);
                }
                for (IdentEntry entry : table.popContext()) {
                    if (entry.getDef().getOwner() instanceof Owner.Local) {
                        helper.releaseLocal(entry.getDef());
                    }
                }
                mv.visitJumpInsn(GOTO, start);
                mv.visitLabel(end);
                helper.visitFrame(mv);
            }
            case BreakStmt _stmt -> {
                mv.visitJumpInsn(GOTO, table.getBreakLabel());
                helper.visitFrame(mv);
            }
            case ContinueStmt _stmt -> {
                mv.visitJumpInsn(GOTO, table.getContinueLabel());
                helper.visitFrame(mv);
            }
            case ReturnStmt returnStmt -> {
                if (returnStmt.getExp() != null) {
                    ExpSummoner.summonExp(returnStmt.getExp(), mv, helper, table);
                    mv.visitInsn(IRETURN);
                    helper.reportPopOpStack(1);
                } else {
                    if (!(currentFunc.getReturnType() instanceof ReturnType.Void)) {
                        throw new CError(ErrorType.WRONG_RETURN_TYPE, "This is a non-void function.");
                    }
                    mv.visitInsn(RETURN);
                }
                // helper.visitFrame(mv);
            }
            case LValGetint getIntStmt -> {
                LValDecl lValDecl = new LValDecl();
                lValDecl.setLVal(getIntStmt.getlVal());
                DotExp dotExp = new DotExp(Mimic.mimicAddExp("__jScanner", new ReturnType.JavaClass("java/util/Scanner")));
                MethodInvokeDotter dotter = new MethodInvokeDotter("nextint", new ReturnType.Integer(), false);
                dotExp.addDotter(dotter);
                lValDecl.setExp(dotExp);
                summonStmt(lValDecl, mv, table, helper);
            }
            case PrintStmt printStmt -> {
                ReturnType pstream = new ReturnType.JavaClass("java/io/PrintStream");
                // Suddenly found that we can almost support java8 if we take class into type system
                DotExp dotExp = new DotExp(Mimic.mimicAddExp(new StaticAccessExp(new ReturnType.JavaClass("java/lang/System"), 
                                                            "out", pstream)));
                MethodInvokeDotter minv = new MethodInvokeDotter("printf", pstream, false);
                minv.addDefParam(new SimpleFuncParamNode("formatString", JavaType.STRING));
                minv.addDefParam(new VariableFuncParamNode("formats", JavaType.OBJECT));
                minv.addCallParam(Mimic.wrap(Mimic.mimicAddExp(printStmt.getFormaString())));
                for (Exp param : printStmt.getExps()) {
                    minv.addCallParam(param);
                }
                dotExp.addDotter(minv);
                ExpSummoner.summonDotExp(dotExp, mv, helper, table);
                mv.visitInsn(POP);
                helper.reportPopOpStack(1);
            }
            default -> throw new RuntimeException("Cannot recognise " + stmt.getClass() + " as a statement.");
        }
    }

    private void summonDecl(Decl decl, MethodVisitor mv, SymbolTable table, MethodHelper helper) {
        switch (decl) {
            case ConstDecl constDecl -> {
                for (ConstDef constDef : constDecl.getConstDefs()) {
                    IdentEntry entry = table.addConstDef(constDef);
                    helper.registerLocal(entry.getDef());
                    switch (entry.getDef()) {
                        case VarDefNode varDef -> {
                            mv.visitLdcInsn(entry.getCompileTimeValue());
                            mv.visitVarInsn(ISTORE, helper.getVarIndex(varDef));
                            helper.reportUseOpStack(1, varDef.getType().toTypeString());
                        }
                        case ArrayDefNode arrayDef -> {
                            ArrayInitHelper.buildFinalArray(arrayDef, entry, mv, helper, table);
                            mv.visitVarInsn(ASTORE, helper.getVarIndex(arrayDef));
                            helper.reportPopOpStack(1);
                        }
                        case FuncDefNode funcDef -> throw new RuntimeException("Why function?");
                    }
                }
            }
            case VarDecl varDecl -> {
                for (VarDef varDef : varDecl.getVarDefs()) {
                    IdentEntry entry = table.addVarDef(varDef);
                    helper.registerLocal(entry.getDef());
                    if (varDef.getInitVal() == null) {
                        continue;
                    }
                    switch (entry.getDef()) {
                        case VarDefNode simpleDef -> {
                            ExpSummoner.summonExp(((Exp)varDef.getInitVal().getInitForm()), mv, helper, table);
                            mv.visitVarInsn(ISTORE, helper.getVarIndex(simpleDef));
                        }
                        case ArrayDefNode arrayDef -> {
                            ArrayInitHelper.buildArray(arrayDef, varDef, mv, helper, table);
                            mv.visitVarInsn(ASTORE, helper.getVarIndex(arrayDef));
                        }
                        case FuncDefNode funcDef -> throw new RuntimeException("Why function?");
                    }
                    helper.reportPopOpStack(1);
                }
            }
        }
    }

    private void summonBlock(Block block, MethodVisitor mv, SymbolTable table, MethodHelper helper) {
        table.pushContext();
        for (BlockItem blockItem : block.getBlockItems()) {
            switch(blockItem.getWrappedBlockItem()) {
                case Decl decl -> summonDecl(decl, mv, table, helper);
                case Stmt stmt -> summonStmt(stmt.getWrappedStmt(), mv, table, helper);
                default -> throw new RuntimeException("No, you cannot have " + blockItem.getWrappedBlockItem().getClass() +
                            " in your block.");
            };
        }
        for (IdentEntry entry : table.popContext()) {
            if (entry.getDef().getOwner() instanceof Owner.Local) {
                helper.releaseLocal(entry.getDef());
            }
        }
    }

    public void summonFunc(FuncDef funcDef, SymbolTable table) {
        IdentEntry entry = table.addFuncDef(funcDef, Owner.builtinMain());
        FuncDefNode funcDefNode = ((FuncDefNode)entry.getDef());
        this.currentFunc = funcDefNode;
        MethodVisitor mv = cv.visitMethod(ACC_STATIC + ACC_PRIVATE, funcDefNode.getName(), funcDefNode.getDescriptor(), null, null);
        table.pushContext();
        table.loadFunctionParams(entry.getName());
        MethodHelper helper = new MethodHelper(funcDefNode);
        mv.visitCode();
        summonBlock(funcDef.getBlock(), mv, table, helper);
        if (funcDefNode.getReturnType() instanceof ReturnType.Void) {
            mv.visitInsn(RETURN);
        } else {
            // mv.visitInsn(NOP);
        }
        for (IdentEntry popEntry : table.popContext()) {
            helper.releaseLocal(popEntry.getDef());
        }
        helper.visitMaxs(mv);
        mv.visitEnd();
    }

    @Override
    public void masterUp() {
        MethodVisitor mv = cv.visitMethod(ACC_STATIC + ACC_PUBLIC, "main", "([Ljava/lang/String;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "com/oto/Main", "__main", "([Ljava/lang/String;)I", true);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        super.masterUp();
    }
}
