package com.cotoj;

import java.io.File;
import java.io.FileNotFoundException;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.DefNode;
import com.cotoj.adaptor.VarDefNode;
import com.cotoj.utils.ClassMaker;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.MethodHelper;
import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.ConstDef;
import com.front.gunit.Exp;
import com.front.gunit.GLock;
import com.front.gunit.GSemaphore;
import com.front.gunit.InitVal;
import com.front.gunit.ParallelType;
import com.front.gunit.VarDef;
import java.util.concurrent.Semaphore;
/**
 * StaticSummoner
 * Summon global variable / constant for the program.
 */
public class StaticSummoner extends ClassMaker implements Opcodes {
    MethodVisitor initVisitor;
    MethodHelper initHelper;
    public StaticSummoner(File logFile, SymbolTable table) throws FileNotFoundException {
        super(logFile);
        cv.visit(V17, ACC_PUBLIC + ACC_FINAL, "com/oto/Static", null, "java/lang/Object", null);
        initVisitor = cv.visitMethod(ACC_PUBLIC + ACC_STATIC, "<clinit>", "()V", null, null);
        initVisitor.visitCode();
        // initVisitor.visitVarInsn(ALOAD, 0);
        // initVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        initHelper = new MethodHelper("com/oto/Static");

        cv.visitField(ACC_PUBLIC + ACC_STATIC, "__jScanner", "Ljava/util/Scanner;", null, null).visitEnd();
        initVisitor.visitTypeInsn(NEW, "java/util/Scanner");
        initVisitor.visitInsn(DUP);
        initVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "in", "Ljava/io/InputStream;");
        initVisitor.visitMethodInsn(INVOKESPECIAL, "java/util/Scanner", "<init>", "(Ljava/io/InputStream;)V", false);
        initVisitor.visitFieldInsn(PUTSTATIC, "com/oto/Static", "__jScanner", "Ljava/util/Scanner;");
        initHelper.reportUsedStack(3);
        table.addDefNode(new VarDefNode("__jScanner", Owner.builtinStatic(), new ReturnType.JavaClass("java/util/Scanner"), false), false);
    }

    public void parseStaticDef(VarDef varDef, SymbolTable table) {
        IdentEntry entry = table.addVarDef(varDef);
        DefNode def = entry.getDef();
        int access = ACC_PUBLIC + ACC_STATIC;
        if (varDef.getIdent().isShared()) {
            access += ACC_VOLATILE;
        }
        switch (def) {
            case ArrayDefNode arrayDef -> {
                cv.visitField(access, entry.getName(), arrayDef.getDescriptor(), null, null).visitEnd();
                ArrayInitHelper.buildArray(arrayDef, varDef, initVisitor, initHelper, table);
                initVisitor.visitFieldInsn(PUTSTATIC, "com/oto/Static", arrayDef.getName(), arrayDef.getDescriptor());
                initHelper.reportPopOpStack(1);
            }
            case VarDefNode _varDef -> {
                cv.visitField(access, entry.getName(), _varDef.getDescriptor(), null, null).visitEnd();
                InitVal initVal = varDef.getInitVal();
                if (initVal != null) {
                    if (!(initVal.getInitForm() instanceof Exp)) {
                        throw new CError(ErrorType.UNEXPECTED_TOKEN, "Expect a exp");
                    }
                    ExpSummoner.summonExp(((Exp)initVal.getInitForm()), initVisitor, initHelper, table);
                    initVisitor.visitFieldInsn(PUTSTATIC, "com/oto/Static", _varDef.getName(), _varDef.getDescriptor());
                    initHelper.reportPopOpStack(1);
                }
            }
            default -> throw new RuntimeException("No, I do not know this type of def.");
        }
    }

    public void parseStaticFinalDef(ConstDef constDef, SymbolTable table) {
        IdentEntry entry = table.addConstDef(constDef);
        DefNode def = entry.getDef();
        switch (def) {
            case ArrayDefNode arrayDef -> {
                cv.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, entry.getName(), arrayDef.getDescriptor(), null, null).visitEnd();
                ArrayInitHelper.buildFinalArray(arrayDef, entry, initVisitor, initHelper, table);
                initVisitor.visitFieldInsn(PUTSTATIC, "com/oto/Static", entry.getName(), arrayDef.getDescriptor());
                initHelper.reportPopOpStack(1);
            }
            case VarDefNode varDef -> {
                cv.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, entry.getName(), varDef.getDescriptor(), null, entry.getCompileTimeValue()).visitEnd();
                // I'm not sure
                initHelper.reportUsedStack(1);
            }
            default -> throw new RuntimeException("No, I do not know this type of const def.");
        }
    }

    public void parseStaticParallelDef(VarDef varDef, ParallelType pType, SymbolTable table) {
        IdentEntry entry = table.addVarDef(varDef);
        DefNode def = entry.getDef();
        ReturnType innerType = def.getType();
        cv.visitField(ACC_PUBLIC + ACC_STATIC, def.getName(), innerType.toDescriptor(), null, null).visitEnd();
        switch (pType.getDataType()) {
            case GLock lock -> {
                initVisitor.visitTypeInsn(NEW, innerType.toTypeString());
                initVisitor.visitInsn(DUP);
                initVisitor.visitMethodInsn(INVOKESPECIAL, innerType.toTypeString(), "<init>", "()V", false);
                initVisitor.visitFieldInsn(PUTSTATIC, Owner.builtinStatic().className(), def.getName(), innerType.toDescriptor());
                initHelper.reportUsedStack(2);
            }
            case GSemaphore sem -> {
                initVisitor.visitTypeInsn(NEW, innerType.toTypeString());
                initVisitor.visitInsn(DUP);
                initHelper.reportUseOpStack(2, null);
                if (varDef.getInitVal() != null) {
                    InitVal initVal = varDef.getInitVal();
                    if (!(initVal.getInitForm() instanceof Exp)) {
                        throw new CError(ErrorType.UNEXPECTED_TOKEN, "Expect a exp");
                    }
                    ReturnType rType = ExpSummoner.summonExp(((Exp)initVal.getInitForm()), initVisitor, initHelper, table);
                    if (!(rType instanceof ReturnType.Integer)) {
                        throw new CError(ErrorType.UNEXPECTED_TOKEN, "Init value for a sem must be a int");
                    }
                } else {
                    initVisitor.visitInsn(ICONST_0);
                    initHelper.reportUseOpStack(1, "I");
                }
                initVisitor.visitMethodInsn(INVOKESPECIAL, innerType.toTypeString(), "<init>", "(I)V", false);
                initVisitor.visitFieldInsn(PUTSTATIC, Owner.builtinStatic().className(), def.getName(), innerType.toDescriptor());
                initHelper.reportPopOpStack(3);
            }
            default -> throw new RuntimeException(def.getType() + " is not a parallel type.");
        }
    }
    
    @Override
    public void masterUp() {
        initVisitor.visitInsn(RETURN);
        initHelper.visitMaxs(initVisitor);
        initVisitor.visitEnd();
        super.masterUp();
    }
}
