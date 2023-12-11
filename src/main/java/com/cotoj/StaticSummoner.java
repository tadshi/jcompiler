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
    public StaticSummoner(File logFile) throws FileNotFoundException {
        super(logFile);
        cv.visit(V17, ACC_PUBLIC + ACC_ABSTRACT, "com/oto/Static", null, "java/lang/Object", null);
        initVisitor = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        initVisitor.visitCode();
        initVisitor.visitVarInsn(ALOAD, 0);
        initVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        initHelper = new MethodHelper("com/oto/Static");
    }

    private void parseArrayInit(ArrayDefNode arrayDef, InitValList initList, int level, int shift, SymbolTable table) {
        int maxDim = arrayDef.getDimSizes().get(level);
        if (initList.getInitVals().size() > maxDim) {
            throw new CError(ErrorType.UNEXPECTED_TOKEN, "TOO MUCH initvalues");
        }
        int perBlockShift = 1;
        for (int index  = level + 1; index < arrayDef.getDimSizes().size(); ++index) {
            perBlockShift *= arrayDef.getDimSizes().get(index);
        }
        for (int i = 0; i < initList.getInitVals().size(); ++i) {
            switch (initList.getInitVals().get(i).getInitForm()) {
                case Exp exp -> {
                    if (level != arrayDef.getDimSizes().size()) {
                        throw new CError(ErrorType.UNEXPECTED_TOKEN, "There should be more levels.");
                    }
                    initVisitor.visitInsn(DUP);
                    initHelper.reportUseOpStack(1, arrayDef.getTypeString());
                    initVisitor.visitLdcInsn(shift + i);
                    initHelper.reportUseOpStack(1, ReturnType.INTEGER.toTypeString());
                    ExpSummoner.summonExp(exp, initVisitor, initHelper, table);
                    initVisitor.visitInsn(IASTORE);
                    initHelper.reportPopOpStack(3);
                }
                case InitValList subInitList -> parseArrayInit(arrayDef, subInitList, level, shift + perBlockShift * i, table);
                default -> throw new RuntimeException("Failed in parsing arr init.");
            }
        }
    }

    public void parseStaticDef(VarDef varDef, SymbolTable table) {
        IdentEntry entry = table.addVarDef(varDef);
        DefNode def = entry.getDef();
        switch (def) {
            case ArrayDefNode arrayDef -> {
                cv.visitField(ACC_PUBLIC + ACC_STATIC, entry.getName(), arrayDef.getTypeString(), null, null).visitEnd();
                for (int dimSize : arrayDef.getDimSizes()) {
                    initVisitor.visitIntInsn(SIPUSH, dimSize);
                }
                initHelper.reportUsedStack(arrayDef.getDimSizes().size());
                initVisitor.visitMultiANewArrayInsn(arrayDef.getTypeString(), arrayDef.getDimSizes().size());
                initHelper.reportUseOpStack(1, arrayDef.getTypeString());
                InitVal initVal = varDef.getInitVal();
                if (initVal != null) {
                    if (!(initVal.getInitForm() instanceof InitValList)) {
                        throw new CError(ErrorType.UNEXPECTED_TOKEN, "Expect a init list");
                    }
                    // This function used DUP. DO NOT reorder!
                    parseArrayInit(arrayDef, ((InitValList)initVal.getInitForm()), 0, 0, table);
                }
                initVisitor.visitFieldInsn(PUTSTATIC, "com/oto/Static", arrayDef.getName(), arrayDef.getTypeString());
                initHelper.reportPopOpStack(1);
            }
            case VarDefNode _varDef -> {
                cv.visitField(ACC_PUBLIC + ACC_STATIC, entry.getName(), _varDef.getTypeString(), null, null).visitEnd();
                InitVal initVal = varDef.getInitVal();
                if (initVal != null) {
                    if (!(initVal.getInitForm() instanceof Exp)) {
                        throw new CError(ErrorType.UNEXPECTED_TOKEN, "Expect a exp");
                    }
                    ExpSummoner.summonExp(((Exp)initVal.getInitForm()), initVisitor, initHelper, table);
                    initVisitor.visitFieldInsn(PUTSTATIC, "com/oto/Static", _varDef.getName(), _varDef.getTypeString());
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
                cv.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, entry.getName(), arrayDef.getTypeString(), null, null).visitEnd();
                for (int dimSize : arrayDef.getDimSizes()) {
                    initVisitor.visitIntInsn(SIPUSH, dimSize);
                }
                initHelper.reportUsedStack(arrayDef.getDimSizes().size());
                initVisitor.visitMultiANewArrayInsn(arrayDef.getTypeString(), arrayDef.getDimSizes().size());
                initHelper.reportUseOpStack(1, arrayDef.getTypeString());
                int index = 0;
                for (Integer compileTimeValue : entry.getCompileTimeValues()) {
                    initVisitor.visitInsn(DUP);
                    initVisitor.visitLdcInsn(index);
                    initVisitor.visitLdcInsn(compileTimeValue);
                    initVisitor.visitInsn(IASTORE);
                }
                initHelper.reportUsedStack(3);
                initVisitor.visitFieldInsn(PUTSTATIC, "com/oto/Static", entry.getName(), arrayDef.getTypeString());
                initHelper.reportPopOpStack(1);
            }
            case VarDefNode varDef -> {
                cv.visitField(ACC_PUBLIC + ACC_STATIC + ACC_FINAL, entry.getName(), varDef.getTypeString(), null, entry.getCompileTimeValue()).visitEnd();
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
