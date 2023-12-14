package com.cotoj;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.MethodHelper;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.Exp;
import com.front.gunit.InitVal;
import com.front.gunit.InitValList;
import com.front.gunit.VarDef;

public interface ArrayInitHelper extends Opcodes {
    private static void initArray(ArrayDefNode arrayDef, InitValList initList, MethodVisitor mv, MethodHelper helper, int level, SymbolTable table) {
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
                    helper.dup(mv);
                    mv.visitLdcInsn(i);
                    helper.reportUseOpStack(1, "I");
                    ExpSummoner.summonExp(exp, mv, helper, table);
                    mv.visitInsn(IASTORE);
                    helper.reportPopOpStack(3);
                }
                case InitValList subInitList -> {
                    mv.visitInsn(DUP);
                    mv.visitLdcInsn(i);
                    mv.visitInsn(AALOAD);
                    helper.reportUsedStack(2);
                    helper.reportUseOpStack(1, arrayDef.getIndexedTypeString(level + 1));
                    initArray(arrayDef, subInitList, mv, helper, level + 1, table);
                    mv.visitInsn(POP);
                    helper.reportPopOpStack(1);
                }
                default -> throw new RuntimeException("Failed in parsing arr init.");
            }
        }
    }

    private static void initFinalArray(ArrayDefNode arrayDef, IdentEntry entry, MethodVisitor mv, MethodHelper helper, int shift, int level) {
        int perBlockShift = 1;
        for (int index  = level + 1; index < arrayDef.getDimSizes().size(); ++index) {
            perBlockShift *= arrayDef.getDimSizes().get(index);
        }
        for (int i = 0; i <= arrayDef.getDimSizes().get(level); ++i) {
            mv.visitInsn(DUP);
            mv.visitLdcInsn(i);
            if (level != arrayDef.getDimSizes().size() - 1) {
                mv.visitInsn(AALOAD);
                helper.reportUsedStack(2);
                helper.reportUseOpStack(1, arrayDef.getIndexedTypeString(level + 1));
                initFinalArray(arrayDef, entry, mv, helper, shift + perBlockShift * i, level + 1);
                mv.visitInsn(POP);
                helper.reportPopOpStack(1);
            } else {
                mv.visitLdcInsn(entry.getCompileTimeValues().get(shift + i));
                mv.visitInsn(IASTORE);
                helper.reportUsedStack(3);
            }
        }
    }

    public static void buildFinalArray(ArrayDefNode arrayDef, IdentEntry entry, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        for (int dimSize : arrayDef.getDimSizes()) {
            mv.visitIntInsn(SIPUSH, dimSize);
        }
        helper.reportUsedStack(arrayDef.getDimSizes().size());
        mv.visitMultiANewArrayInsn(arrayDef.getDescriptor(), arrayDef.getDimSizes().size());
        helper.reportUseOpStack(1, arrayDef.getTypeString());
        initFinalArray(arrayDef, entry, mv, helper, 0, 0);
    }

    public static void buildArray(ArrayDefNode arrayDef, VarDef astDef,MethodVisitor mv, MethodHelper helper, SymbolTable table) {  
        for (int dimSize : arrayDef.getDimSizes()) {
            mv.visitIntInsn(SIPUSH, dimSize);
        }
        helper.reportUsedStack(arrayDef.getDimSizes().size());
        mv.visitMultiANewArrayInsn(arrayDef.getDescriptor(), arrayDef.getDimSizes().size());
        helper.reportUseOpStack(1, arrayDef.getTypeString());
        InitVal initVal = astDef.getInitVal();
        if (initVal != null) {
            if (!(initVal.getInitForm() instanceof InitValList)) {
                throw new CError(ErrorType.UNEXPECTED_TOKEN, "Expect a init list");
            }
            // This function used DUP. DO NOT reorder!
            initArray(arrayDef, ((InitValList)initVal.getInitForm()), mv, helper, 0, table);
        }
    }
}
