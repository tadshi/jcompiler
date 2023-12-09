package com.cotoj;

import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.MethodHelper;
import com.cotoj.utils.SymbolType;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.*;

public interface ExpSummoner extends Opcodes {
    private static void summonLVal(LVal lVal, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        Ident ident = lVal.getIdent();
        IdentEntry entry = table.getEntry(ident.getName(), SymbolType.fromString(ident.getKind()));
        if (entry.isArray()) {
            if (ident.getDimension() != lVal.getExps().size()) {
                throw new CError(ErrorType.EXPECT_TOKEN, "[");
            }
            List<Integer> const_index = lVal.getExps().stream()
                                                    .map(exp -> exp.getAddExp())
                                                    .map(addExp -> parseAddExp(addExp, table))
                                                    .collect(Collectors.toList());
            return entry.indexCompileTimeValue(const_index);
        } else {
            if (entry.isConst()) {
                mv.visitLdcInsn(entry.getCompileTimeValue());
            }
        }
        
    }
    
    private static void summonPrimaryExp(PrimaryExp primaryExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        ObjectClass misteryExp = primaryExp.getWrappedExp();
        if (misteryExp instanceof Exp) {
            summonAddExp(((Exp)misteryExp).getAddExp(), mv, helper, table);
        } else if (misteryExp instanceof LVal) {
            summonLVal((LVal)misteryExp, mv, helper, table);
        } else if (misteryExp instanceof GNumber) {
            int number = ((GNumber)misteryExp).getNumber();
            mv.visitLdcInsn(number);
            helper.reportUseOpStack(1);
        } else {
            throw new RuntimeException(misteryExp.getClass() + " should not found in PrimaryExp");
        }
    }

    private static void summonUnaryExp(UnaryExp unaryExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        ObjectClass misteryExp = unaryExp.getUnaryExp();
        if (misteryExp instanceof OpExp) {
            OpExp opExp = ((OpExp)misteryExp);
            if (opExp.getUnaryOp().getOp() == "PLUS +") {
                summonUnaryExp(opExp.getUnaryExp(), mv, helper, table);
            } else if (opExp.getUnaryOp().getOp() == "MINU -") {
                summonUnaryExp(opExp.getUnaryExp(), mv, helper, table);
                mv.visitInsn(INEG);
            } else {
                throw new CError(ErrorType.UNEXPECTED_TOKEN, opExp.getUnaryOp().getOp());
            }
            helper.popUse();
            helper.reportUseOpStack(1);
        } else if (misteryExp instanceof PrimaryExp) {
            PrimaryExp primaryExp = ((PrimaryExp)misteryExp);
            summonPrimaryExp(primaryExp, mv, helper, table);
        } else {
            throw new CError(ErrorType.EXP_PARSE_FAIL,  misteryExp.getClass().toString());
        }
    }

    private static void summonMulExp(MulExp mulExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        if (mulExp.getMulExp() == null) {
            summonUnaryExp(mulExp.getUnaryExp(), mv, helper, table);
        } else {
            summonMulExp(mulExp.getMulExp(), mv, helper, table);
            summonUnaryExp(mulExp.getUnaryExp(), mv, helper, table);
            if (mulExp.getCh() == "MULT *") {
                mv.visitInsn(IMUL);
            } else if (mulExp.getCh() == "DIV /") {
                mv.visitInsn(IDIV);
            } else if (mulExp.getCh() == "MOD %") {
                mv.visitInsn(IREM);
            } else {
                throw new RuntimeException(mulExp.getCh());
            }
            helper.popUse();
            helper.popUse();
            helper.reportUseOpStack(1);
        }
    }

    // Well, if you do not deal with the complexity abide in the grammar, then
    // every single layer will suffer from the complexity.
    // This can be a good lesson...
    private static void summonAddExp(AddExp addExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        if (addExp.getAddExp() == null) {
            summonMulExp(addExp.getMulExp(), mv, helper, table);
        } else {
            summonAddExp(addExp.getAddExp(), mv, helper, table);
            summonMulExp(addExp.getMulExp(), mv, helper, table);
            if (addExp.getCh() == "PLUS +") {
                mv.visitInsn(IADD);
            } else if  (addExp.getCh() == "MINU -") {
                mv.visitInsn(ISUB);
            } else {
                throw new RuntimeException(addExp.getCh());
            }
            helper.popUse();
            helper.popUse();
            helper.reportUseOpStack(1);
        }
    }

    // Evaluate an Exp at Runtime.
    public static void summonExp(Exp exp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        summonAddExp(exp.getAddExp(), mv, helper, table);
    }    
}