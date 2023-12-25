package com.cotoj;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.ExpSummoner.TypePair;
import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.DefNode;
import com.cotoj.adaptor.DotExp;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.FuncParamNode;
import com.cotoj.adaptor.MethodInvokeDotter;
import com.cotoj.adaptor.Mimic;
import com.cotoj.adaptor.SimpleFuncParamNode;
import com.cotoj.adaptor.StaticAccessExp;
import com.cotoj.adaptor.VarDefNode;
import com.cotoj.adaptor.VariableFuncParamNode;
import com.cotoj.utils.AutoPacker;
import com.cotoj.utils.ClassMaker;
import com.cotoj.utils.ExpTypeHelper;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.JavaType;
import com.cotoj.utils.MethodHelper;
import com.cotoj.utils.OpcodeHelper;
import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;
import com.cotoj.utils.SymbolType;
import com.cotoj.utils.ThreadHelper;
import com.cotoj.utils.ReturnType.JavaClass;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.*;

public class MainSummoner extends ClassMaker implements Opcodes {
    private FuncDefNode currentFunc = null;
    private Map<String, ClassWriter> threads;
    
    public MainSummoner() {
        super();
        this.threads = new HashMap<>();
        cv.visit(V21, ACC_PUBLIC + ACC_ABSTRACT, "com/oto/Main", null, "java/lang/Object", null);
    }
    
    public MainSummoner(File logFile) throws FileNotFoundException {
        super(logFile);
        this.threads = new HashMap<>();
        cv.visit(V21, ACC_PUBLIC + ACC_ABSTRACT, "com/oto/Main", null, "java/lang/Object", null);
    }

    private void summonDictPut(VarDefNode varDef, Exp keyExp, Exp assignExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        switch (varDef.getOwner()) {
            case Owner.Static sClass -> mv.visitFieldInsn(GETSTATIC, sClass.className(), varDef.getName(), varDef.getDescriptor());
            case Owner.Local() -> mv.visitVarInsn(ALOAD, helper.getVarIndex(varDef));
            case Owner.Class clazz -> mv.visitFieldInsn(GETFIELD, clazz.className(), varDef.getName(), varDef.getDescriptor());
        }
        ReturnType.Dict dictType = ((ReturnType.Dict)varDef.getType());
        helper.reportUseOpStack(1, dictType.toTypeString());
        ExpTypeHelper.implicitCast(dictType.keyType(), ExpSummoner.summonExp(keyExp, mv, helper, table), mv, helper);
        AutoPacker.summonPack(dictType.keyType(), mv, helper);
        ExpTypeHelper.implicitCast(dictType.valueType(), ExpSummoner.summonExp(assignExp, mv, helper, table), mv, helper);
        AutoPacker.summonPack(dictType.valueType(), mv, helper);
        mv.visitMethodInsn(INVOKEINTERFACE, JavaType.DICT_INT.toTypeString(), "put", 
                            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
        mv.visitInsn(POP);
        helper.reportPopOpStack(3);
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
                // Is this a dict put?
                if (lValType instanceof ReturnType.Dict && lVal.getExps().size() > 0) {
                    if (lVal.getExps().size() != 1) {
                        throw new CError(ErrorType.UNEXPECTED_TOKEN, "A Dict cannot be indexed twice.");
                    }
                    summonDictPut(((VarDefNode)entry.getDef()), lVal.getExps().getFirst(), assignStmt.getExp(), mv, helper, table);
                    return;
                }
                // Now it is normal variable.
                switch (entry.getDef()) {
                    case VarDefNode varDef -> {
                        rValType = ExpSummoner.summonExp(assignStmt.getExp(), mv, helper, table);
                        ExpTypeHelper.implicitCast(lValType, rValType, mv, helper);
                        switch (varDef.getOwner()) {
                            case Owner.Static sClass -> mv.visitFieldInsn(PUTSTATIC, sClass.className(), varDef.getName(), varDef.getDescriptor());
                            case Owner.Local() -> mv.visitVarInsn(OpcodeHelper.toStore(lValType), helper.getVarIndex(varDef));
                            case Owner.Class clazz -> mv.visitFieldInsn(PUTFIELD, clazz.className(), varDef.getName(), varDef.getDescriptor());
                        }
                        helper.reportPopOpStack(1);
                    }
                    case ArrayDefNode arrayDef -> {
                        if (arrayDef.getDimSizes().size() != lVal.getExps().size()) {
                            throw new CError(ErrorType.ARRAY_DIM_ERROR, "Expect " + arrayDef.getDimSizes().size() + ", found " + lVal.getExps().size());
                        }
                        switch (arrayDef.getOwner()) {
                            case Owner.Static sClass -> mv.visitFieldInsn(GETSTATIC, sClass.className(), arrayDef.getName(), arrayDef.getDescriptor());
                            case Owner.Local() -> mv.visitVarInsn(ALOAD, helper.getVarIndex(arrayDef));
                            case Owner.Class clazz -> mv.visitFieldInsn(GETFIELD, clazz.className(), arrayDef.getName(), arrayDef.getDescriptor());
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
                        ExpTypeHelper.implicitCast(lValType, rValType, mv, helper);
                        mv.visitInsn(OpcodeHelper.toArrayStore(lValType));
                        helper.reportPopOpStack(3);
                    }
                    case FuncDefNode funcDef -> throw new CError(ErrorType.UNEXPECTED_TOKEN, "No, you cannot assign a function.");
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
                TypePair pair = ExpSummoner.summonLOrExp(ifStmt.getCond().getlOrExp(), mv, helper, table, start);
                mv.visitInsn(NOP);
                if (pair.used()) {
                    mv.visitLabel(start);
                    helper.visitFrame(mv);
                }
                mv.visitJumpInsn(IFEQ, else_stmt);
                helper.reportPopOpStack(1);
                summonStmt(ifStmt.getStmt().getWrappedStmt(), mv, table, helper);
                mv.visitInsn(NOP);
                if (ifStmt.getElseStmt() == null) {
                    mv.visitLabel(else_stmt);
                    helper.visitFrame(mv);
                } else {
                    Label end = new Label();
                    mv.visitJumpInsn(GOTO, end);
                    mv.visitLabel(else_stmt);
                    helper.visitFrame(mv);
                    summonStmt(ifStmt.getElseStmt().getWrappedStmt(), mv, table, helper);
                    mv.visitInsn(NOP);
                    mv.visitLabel(end);
                    helper.visitFrame(mv);
                }
            }
            case WhileStmt whileStmt -> {
                Label start = new Label();
                Label judge = new Label();
                Label end = new Label();

                mv.visitInsn(NOP);
                mv.visitLabel(start);
                helper.visitFrame(mv);
                ExpSummoner.summonLOrExp(whileStmt.getCond().getlOrExp(), mv, helper, table, judge);
                mv.visitLabel(judge);
                helper.visitFrame(mv);
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
                mv.visitInsn(NOP);
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
                ReturnType returnType = currentFunc.getReturnType();
                if (returnStmt.getExp() != null) {
                    ExpTypeHelper.implicitCast(returnType, ExpSummoner.summonExp(returnStmt.getExp(), mv, helper, table), mv, helper);
                    mv.visitInsn(OpcodeHelper.toReturn(returnType));
                    helper.reportPopOpStack(1);
                } else {
                    if (!(returnType instanceof ReturnType.Void)) {
                        throw new CError(ErrorType.WRONG_RETURN_TYPE, "This is a non-void function.");
                    }
                    mv.visitInsn(RETURN);
                }
                if (!returnStmt.isLastReturn()) {
                    helper.visitFrame(mv);
                }
            }
            case LValGetint getIntStmt -> {
                LValDecl lValDecl = new LValDecl();
                lValDecl.setLVal(getIntStmt.getlVal());
                DotExp dotExp = new DotExp(Mimic.mimicLOrExp("__jScanner", new ReturnType.JavaClass("java/util/Scanner")));
                MethodInvokeDotter dotter = new MethodInvokeDotter("nextInt", new ReturnType.Integer(), false);
                dotExp.addDotter(dotter);
                lValDecl.setExp(dotExp);
                summonStmt(lValDecl, mv, table, helper);
            }
            case PrintStmt printStmt -> {
                ReturnType pstream = new ReturnType.JavaClass("java/io/PrintStream");
                // Suddenly found that we can almost support java8 if we take class into type system
                DotExp dotExp = new DotExp(Mimic.mimicLOrExp(new StaticAccessExp(new ReturnType.JavaClass("java/lang/System"), 
                                                            "out", pstream)));
                MethodInvokeDotter minv = new MethodInvokeDotter("printf", pstream, false);
                minv.addDefParam(new SimpleFuncParamNode("formatString", JavaType.STRING));
                minv.addDefParam(new VariableFuncParamNode("formats", JavaType.OBJECT));
                minv.addCallParam(Mimic.wrap(Mimic.mimicLOrExp(printStmt.getFormaString())));
                for (Exp param : printStmt.getExps()) {
                    minv.addCallParam(param);
                }
                dotExp.addDotter(minv);
                ExpSummoner.summonDotExp(dotExp, mv, helper, table);
                mv.visitInsn(POP);
                helper.reportPopOpStack(1);
            }
            case AwaitStmt awaitStmt -> {
                switch (awaitStmt.getAwaitItem()) {
                    case Ident ident -> {
                        IdentEntry entry = table.getEntry(ident.getName(), SymbolType.VARIABLE);
                        if (entry == null) {
                            throw new CError(ErrorType.IDENT_NOT_EXISTS, "Parallel premitive " + ident.getName() + " do not exists.");
                        }
                        DefNode paraVarDef = entry.getDef();
                        if (!(paraVarDef.getType() instanceof JavaClass)) {
                            throw new CError(ErrorType.UNEXPECTED_TOKEN, "This is not a paralle primitive!");
                        }
                        JavaClass paraType = ((JavaClass)paraVarDef.getType());
                        mv.visitFieldInsn(GETSTATIC, Owner.builtinStatic().className(), paraVarDef.getName(), paraType.toDescriptor());
                        if (JavaType.LOCK.toTypeString().equals(paraType.toTypeString())) {
                            mv.visitMethodInsn(INVOKEINTERFACE, JavaType.LOCK_INT.toTypeString(), "lock", "()V", true);
                        } else if (JavaType.SEM.toTypeString().equals(paraType.toTypeString())) {
                            mv.visitMethodInsn(INVOKEVIRTUAL, JavaType.SEM.toTypeString(), "acquire", "()V", false);
                        }
                        helper.reportUsedStack(1);
                        break;
                    }
                    case FuncCall funcCall -> {
                        IdentEntry entry = table.getEntry(funcCall.getIdent().getName(), SymbolType.FUNCTION);
                        if (entry == null) {
                            throw new CError(ErrorType.IDENT_NOT_EXISTS, "Cannot find parallel function named" + funcCall.getIdent().getName());
                        }
                        FuncDefNode funcDef = ((FuncDefNode)entry.getDef());
                        if (!funcDef.isParallel()) {
                            throw new CError(ErrorType.NOT_A_THREAD, "This is not a thread function!");
                        }
                        if (funcDef.isRoutine()) {
                            mv.visitMethodInsn(INVOKESTATIC, JavaType.THREAD.toTypeString(), "ofVirtual", "()Ljava/lang/Thread$Builder$OfVirtual;", false);
                            helper.reportUseOpStack(1, "java/lang/Thread$Builder$OfVirtual");
                            ExpSummoner.summonThread(funcCall, mv, helper, table);
                            mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/Thread$Builder", "start", "(Ljava/lang/Runnable;)Ljava/lang/Thread;", true);
                            mv.visitMethodInsn(INVOKEVIRTUAL, JavaType.THREAD.toTypeString(), "join", "()V", false);
                            helper.reportPopOpStack(2);
                        } else {
                            mv.visitTypeInsn(NEW, JavaType.THREAD.toTypeString());
                            mv.visitInsn(DUP);
                            helper.reportUseOpStack(2, null);
                            ExpSummoner.summonThread(funcCall, mv, helper, table);
                            mv.visitMethodInsn(INVOKESPECIAL, JavaType.THREAD.toTypeString(), "<init>", "(Ljava/lang/Runnable;)" + JavaType.THREAD.toDescriptor(), false);
                            mv.visitInsn(DUP);
                            mv.visitMethodInsn(INVOKEVIRTUAL, JavaType.THREAD.toTypeString(), "start", "()V", false);
                            mv.visitMethodInsn(INVOKEVIRTUAL, JavaType.THREAD.toTypeString(), "join", "()V", false);
                            helper.reportPopOpStack(3);
                        }
                    }
                    default -> throw new RuntimeException("What's this awaitStmt?");
                }
            }
            case SignalStmt signalStmt -> {
                IdentEntry entry = table.getEntry(signalStmt.getIdent().getName(), SymbolType.VARIABLE);
                if (entry == null) {
                    throw new CError(ErrorType.IDENT_NOT_EXISTS, signalStmt.getIdent().getName());
                }
                DefNode paraVarDef = entry.getDef();
                if (!(paraVarDef.getType() instanceof JavaClass)) {
                    throw new CError(ErrorType.UNEXPECTED_TOKEN, "This is not a paralle primitive!");
                }
                JavaClass paraType = ((JavaClass)paraVarDef.getType());
                mv.visitFieldInsn(GETSTATIC, Owner.builtinStatic().className(), paraVarDef.getName(), paraType.toDescriptor());
                if (JavaType.LOCK.toTypeString().equals(paraType.toTypeString())) {
                    mv.visitMethodInsn(INVOKEINTERFACE, JavaType.LOCK_INT.toTypeString(), "unlock", "()V", true);
                } else if (JavaType.SEM.toTypeString().equals(paraType.toTypeString())) {
                    mv.visitMethodInsn(INVOKEVIRTUAL, JavaType.SEM.toTypeString(), "release", "()V", false);
                }
                helper.reportUsedStack(1);
                break;
            }
            case CallThreadStmt runStmt -> {
                FuncCall funcCall = runStmt.getFuncCall();
                FuncDefNode funcDef = ((FuncDefNode)table.getEntry(funcCall.getIdent().getName(), SymbolType.FUNCTION).getDef());
                if (funcDef.isRoutine()) {
                    mv.visitMethodInsn(INVOKESTATIC, JavaType.THREAD.toTypeString(), "ofVirtual", "()Ljava/lang/Thread$Builder$OfVirtual;", false);
                    helper.reportUseOpStack(1, "java/lang/Thread$Builder$OfVirtual");
                    ExpSummoner.summonThread(funcCall, mv, helper, table);
                    mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/Thread$Builder", "start", "(Ljava/lang/Runnable;)Ljava/lang/Thread;", true);
                    mv.visitInsn(POP);
                    helper.reportPopOpStack(2);
                } else {
                    mv.visitTypeInsn(NEW, JavaType.THREAD.toTypeString());
                    mv.visitInsn(DUP);
                    helper.reportUseOpStack(2, null);
                    ExpSummoner.summonThread(funcCall, mv, helper, table);
                    mv.visitMethodInsn(INVOKESPECIAL, JavaType.THREAD.toTypeString(), "<init>", "(Ljava/lang/Runnable;)V", false);
                    mv.visitMethodInsn(INVOKEVIRTUAL, JavaType.THREAD.toTypeString(), "start", "()V", false);
                    helper.reportPopOpStack(3);
                }
            }
            case PutItemStmt appendStmt -> {
                Ident listIdent = appendStmt.getIdent();
                IdentEntry listEntry = table.getEntry(listIdent.getName(), SymbolType.VARIABLE);
                if (!(listEntry.getDef() instanceof VarDefNode && listEntry.getDef().getType() instanceof ReturnType.List)) {
                    throw new CError(ErrorType.UNEXPECTED_TOKEN, "You can only put to a list.");
                }
                VarDefNode listDef = ((VarDefNode)listEntry.getDef());
                ReturnType.List listType = ((ReturnType.List)listDef.getType());
                switch (listDef.getOwner()) {
                    case Owner.Static sttc -> mv.visitFieldInsn(GETSTATIC, Owner.builtinStatic().className(), listDef.getName(), listDef.getDescriptor());
                    case Owner.Local() -> mv.visitVarInsn(ALOAD, helper.getVarIndex(listDef));
                    case Owner.Class clazz -> mv.visitFieldInsn(GETFIELD, clazz.className(), listDef.getName(), listDef.getDescriptor());
                }
                helper.reportUseOpStack(1, listDef.getTypeString());
                ExpTypeHelper.implicitCast(listType.contentType(), ExpSummoner.summonExp(appendStmt.getExp(), mv, helper, table), mv, helper);
                AutoPacker.summonPack(listType.contentType(), mv, helper);
                mv.visitMethodInsn(INVOKEINTERFACE, JavaType.LIST_INT.toTypeString(), "add", "(Ljava/lang/Object;)Z", true);
                mv.visitInsn(POP);
                helper.reportPopOpStack(2);
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
                            mv.visitVarInsn(OpcodeHelper.toStore(varDef.getType()), helper.getVarIndex(varDef));
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
                    if (entry.getDef().getType() instanceof ReturnType.List) {
                        ExpSummoner.summonList(((ReturnType.List)entry.getDef().getType()), varDef.getIdent().isShared(), varDef.getInitVal(), mv, helper, table);
                        mv.visitVarInsn(ASTORE, helper.getVarIndex(entry.getDef()));
                        helper.reportPopOpStack(1);
                        continue;
                    }
                    if (entry.getDef().getType() instanceof ReturnType.Dict) {
                        ExpSummoner.summonDict(((ReturnType.Dict)entry.getDef().getType()), varDef.getIdent().isShared(), varDef.getInitVal(), mv, helper, table);
                        mv.visitVarInsn(ASTORE, helper.getVarIndex(entry.getDef()));
                        helper.reportPopOpStack(1);
                        continue;
                    }
                    switch (entry.getDef()) {
                        case VarDefNode simpleDef -> {
                            if (varDef.getInitVal() == null) break;
                            ExpTypeHelper.implicitCast(simpleDef.getType(), ExpSummoner.summonExp(((Exp)varDef.getInitVal().getInitForm()), mv, helper, table), mv, helper);
                            mv.visitVarInsn(OpcodeHelper.toStore(simpleDef.getType()), helper.getVarIndex(simpleDef));
                            helper.reportPopOpStack(1);
                        }
                        case ArrayDefNode arrayDef -> {
                            ArrayInitHelper.buildArray(arrayDef, varDef, mv, helper, table);
                            mv.visitVarInsn(ASTORE, helper.getVarIndex(arrayDef));
                            helper.reportPopOpStack(1);
                        }
                        case FuncDefNode funcDef -> throw new RuntimeException("Why function?");
                    }
                }
            }
            case ParallelDecl para -> throw new CError(ErrorType.UNEXPECTED_TOKEN, "Sorry but parallel types can only be defined in global scope.");
        }
    }

    private void summonBlock(Block block, MethodVisitor mv, SymbolTable table, MethodHelper helper) {
        table.pushContext();
        for (BlockItem blockItem : block.getBlockItems()) {
            switch(blockItem.getWrappedBlockItem()) {
                case Decl decl -> summonDecl(decl, mv, table, helper);
                case Stmt stmt -> summonStmt(stmt.getWrappedStmt(), mv, table, helper);
                case ReturnStmt retStmt -> mv.visitInsn(RETURN);
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

    public void summonThreadFunc(FuncDef funcDef, SymbolTable table) {
        IdentEntry entry = table.addFuncDef(funcDef, new Owner.Class(ThreadHelper.getClassName(funcDef), false));
        FuncDefNode funcDefNode = ((FuncDefNode)entry.getDef());
        this.currentFunc = funcDefNode;
        var pack = ThreadHelper.summonThreadClass(funcDefNode);
        threads.put(funcDefNode.getName(), pack.cw());

        ClassVisitor threadCv = pack.cv();
        MethodVisitor threadMv = threadCv.visitMethod(ACC_PUBLIC, "<init>", ThreadHelper.getInitDescriptor(funcDefNode), null, null);
        threadMv.visitCode();
        threadMv.visitVarInsn(ALOAD, 0);
        threadMv.visitInsn(DUP);
        threadMv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        int index = 1;
        for (FuncParamNode param : funcDefNode.getParams()) {
            threadCv.visitField(ACC_PRIVATE, param.getName(), param.getDescriptor(), null, null).visitEnd();
            threadMv.visitInsn(DUP);
            threadMv.visitVarInsn(OpcodeHelper.toLoad(param), index);
            threadMv.visitFieldInsn(PUTFIELD, ThreadHelper.getClassName(funcDefNode), param.getName(), param.getDescriptor());
        }
        threadMv.visitInsn(POP);
        threadMv.visitInsn(RETURN);
        threadMv.visitMaxs(3, funcDefNode.getParams().size() + 1);
        threadMv.visitEnd();
        MethodVisitor runMv = threadCv.visitMethod(Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        MethodHelper helper = new MethodHelper(funcDefNode);
        table.pushContext();
        table.loadFunctionParamsAsFields(funcDefNode.getName());
        runMv.visitCode();
        summonBlock(funcDef.getBlock(), runMv, table, helper);
        for (IdentEntry popEntry : table.popContext()) {
            if (popEntry.getDef().getOwner() instanceof Owner.Local) {
                helper.releaseLocal(popEntry.getDef());
            }
        }
        helper.visitMaxs(runMv);
        runMv.visitEnd();
    }

    public void summonFunc(FuncDef funcDef, SymbolTable table) {
        IdentEntry entry = table.addFuncDef(funcDef, Owner.builtinMain());
        FuncDefNode funcDefNode = ((FuncDefNode)entry.getDef());
        this.currentFunc = funcDefNode;
        MethodVisitor mv = cv.visitMethod(ACC_STATIC + ACC_PUBLIC, funcDefNode.getName(), funcDefNode.getDescriptor(), null, null);
        table.pushContext();
        table.loadFunctionParams(entry.getName());
        MethodHelper helper = new MethodHelper(funcDefNode);
        mv.visitCode();
        summonBlock(funcDef.getBlock(), mv, table, helper);
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
        mv.visitMethodInsn(INVOKESTATIC, "com/oto/Main", "__main", "([Ljava/lang/String;)V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
        super.masterUp();
    }

    @Override
    @Deprecated
    public void save(Path path) throws IOException {
        save(path, path.resolve("../../threads"));
    }
    public void save(Path mainPath, Path subsPath) throws IOException {
        super.save(mainPath);
        for (var entry : threads.entrySet()) {
            File outputFile = subsPath.resolve("Thread" + entry.getKey() + ".class").toFile();
            OutputStream tStream = new PrintStream(outputFile);
            tStream.write(entry.getValue().toByteArray());
            tStream.close();
        }
    }

    public Map<String, ClassWriter> getThreads() {
        return threads;
    }
}
