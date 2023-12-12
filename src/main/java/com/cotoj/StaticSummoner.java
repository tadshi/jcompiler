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
import com.front.gunit.InitVal;
import com.front.gunit.InitValList;
import com.front.gunit.VarDef;

/**
 * StaticSummoner
 * Summon global variable / constant for the program.
 */
public class StaticSummoner extends ClassMaker implements Opcodes {
    MethodVisitor initVisitor;
    MethodHelper initHelper;
    public StaticSummoner(File logFile, SymbolTable table) throws FileNotFoundException {
        super(logFile);
        cv.visit(V17, ACC_PUBLIC + ACC_ABSTRACT, "com/oto/Static", null, "java/lang/Object", null);
        initVisitor = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        initVisitor.visitCode();
        initVisitor.visitVarInsn(ALOAD, 0);
        initVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        initHelper = new MethodHelper("com/oto/Static");

        cv.visitField(ACC_PUBLIC + ACC_STATIC, "__jScanner", "Ljava/util/Scanner;", null, null);
        initVisitor.visitTypeInsn(NEW, "java/util/Scanner");
        initVisitor.visitInsn(DUP);
        initVisitor.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
        initVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/util/Scanner", "<init>", "(Ljava/io/PrintStream;)V", false);
        initVisitor.visitFieldInsn(PUTSTATIC, "com/oto/Static", "__jScanner", "Ljava/util/Scanner;");
        initHelper.reportUsedStack(3);
        table.addDefNode(new VarDefNode("__jScanner", Owner.builtinStatic(), new ReturnType.JavaClass("java/util/Scanner"), false));
    }

    private void parseArrayInit(ArrayDefNode arrayDef, InitValList initList, int level, SymbolTable table) {
        int maxDim = arrayDef.getDimSizes().get(level);
        if (initList.getInitVals().size() > maxDim) {
            throw new CError(ErrorType.UNEXPECTED_TOKEN, "TOO MUCH initvalues");
        }
        for (int i = 0; i < initList.getInitVals().size(); ++i) {
            switch (initList.getInitVals().get(i).getInitForm()) {
                case Exp exp -> {
                    if (level != arrayDef.getDimSizes().size()) {
                        throw new CError(ErrorType.UNEXPECTED_TOKEN, "There should be more levels.");
                    }
                    initVisitor.visitInsn(DUP);
                    initHelper.reportUseOpStack(1, arrayDef.getIndexedTypeString(level));
                    initVisitor.visitLdcInsn(i);
                    initHelper.reportUseOpStack(1, "I");
                    ExpSummoner.summonExp(exp, initVisitor, initHelper, table);
                    initVisitor.visitInsn(IASTORE);
                    initHelper.reportPopOpStack(3);
                }
                case InitValList subInitList -> {
                    initVisitor.visitInsn(DUP);
                    initVisitor.visitLdcInsn(i);
                    initVisitor.visitInsn(AALOAD);
                    initHelper.reportUsedStack(2);
                    initHelper.reportUseOpStack(1, arrayDef.getIndexedTypeString(level + 1));
                    parseArrayInit(arrayDef, subInitList, level, table);
                    initVisitor.visitInsn(POP);
                    initHelper.reportPopOpStack(1);
                }
                default -> throw new RuntimeException("Failed in parsing arr init.");
            }
        }
    }

    public void parseStaticDef(VarDef varDef, SymbolTable table) {
        IdentEntry entry = table.addVarDef(varDef);
        DefNode def = entry.getDef();
        switch (def) {
            case ArrayDefNode arrayDef -> {
                cv.visitField(ACC_PUBLIC + ACC_STATIC, entry.getName(), arrayDef.getDescriptor(), null, null).visitEnd();
                for (int dimSize : arrayDef.getDimSizes()) {
                    initVisitor.visitIntInsn(SIPUSH, dimSize);
                }
                initHelper.reportUsedStack(arrayDef.getDimSizes().size());
                initVisitor.visitMultiANewArrayInsn(arrayDef.getDescriptor(), arrayDef.getDimSizes().size());
                initHelper.reportUseOpStack(1, arrayDef.getTypeString());
                InitVal initVal = varDef.getInitVal();
                if (initVal != null) {
                    if (!(initVal.getInitForm() instanceof InitValList)) {
                        throw new CError(ErrorType.UNEXPECTED_TOKEN, "Expect a init list");
                    }
                    // This function used DUP. DO NOT reorder!
                    parseArrayInit(arrayDef, ((InitValList)initVal.getInitForm()), 0, table);
                }
                initVisitor.visitFieldInsn(PUTSTATIC, "com/oto/Static", arrayDef.getName(), arrayDef.getDescriptor());
                initHelper.reportPopOpStack(1);
            }
            case VarDefNode _varDef -> {
                cv.visitField(ACC_PUBLIC + ACC_STATIC, entry.getName(), _varDef.getDescriptor(), null, null).visitEnd();
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

    public void parseFinalArrayInit(ArrayDefNode arrayDef, IdentEntry entry, int shift, int level) {
        int perBlockShift = 1;
        for (int index  = level + 1; index < arrayDef.getDimSizes().size(); ++index) {
            perBlockShift *= arrayDef.getDimSizes().get(index);
        }
        for (int i = 0; i <= arrayDef.getDimSizes().get(level); ++i) {
            initVisitor.visitInsn(DUP);
            initVisitor.visitLdcInsn(i);
            if (level != arrayDef.getDimSizes().size() - 1) {
                initVisitor.visitInsn(AALOAD);
                initHelper.reportUsedStack(2);
                initHelper.reportUseOpStack(1, arrayDef.getIndexedTypeString(level + 1));
                parseFinalArrayInit(arrayDef, entry, shift + perBlockShift * i, level + 1);
                initVisitor.visitInsn(POP);
                initHelper.reportPopOpStack(1);
            } else {
                initVisitor.visitInsn(DUP);
                initVisitor.visitLdcInsn(i);
                initVisitor.visitLdcInsn(entry.getCompileTimeValues().get(shift + i));
                initVisitor.visitInsn(IASTORE);
                initHelper.reportUsedStack(3);
            }
        }
    }

    public void parseStaticFinalDef(ConstDef constDef, SymbolTable table) {
        IdentEntry entry = table.addConstDef(constDef);
        DefNode def = entry.getDef();
        switch (def) {
            case ArrayDefNode arrayDef -> {
                cv.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, entry.getName(), arrayDef.getDescriptor(), null, null).visitEnd();
                for (int dimSize : arrayDef.getDimSizes()) {
                    initVisitor.visitIntInsn(SIPUSH, dimSize);
                }
                initHelper.reportUsedStack(arrayDef.getDimSizes().size());
                initVisitor.visitMultiANewArrayInsn(arrayDef.getDescriptor(), arrayDef.getDimSizes().size());
                initHelper.reportUseOpStack(1, arrayDef.getTypeString());
                parseFinalArrayInit(arrayDef, entry, 0, 0);
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

    @Override
    public void masterUp() {
        initHelper.visitFrame(initVisitor);
        initVisitor.visitInsn(RETURN);
        initHelper.visitMaxs(initVisitor);
        initVisitor.visitEnd();
        super.masterUp();
    }
}
