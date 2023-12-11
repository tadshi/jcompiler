package com.cotoj;

import java.util.List;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.ArrayFuncParamNode;
import com.cotoj.adaptor.DefNode;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.SimpleFuncParamNode;
import com.cotoj.adaptor.VarDefNode;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.MethodHelper;
import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;
import com.cotoj.utils.SymbolType;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.*;

public interface ExpSummoner extends Opcodes {
    private static void summonLVal(LVal lVal, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        Ident ident = lVal.getIdent();
        IdentEntry entry = table.getEntry(ident.getName(), SymbolType.fromString(ident.getKind()));
        DefNode def = entry.getDef();
        switch (entry.getDef()) {
            case VarDefNode varDef -> {
                if (entry.isConst()) {
                    mv.visitLdcInsn(entry.getCompileTimeValue());
                } else {
                    Owner owner = varDef.getOwner();
                    switch (owner) {
                        case Owner.Static() -> mv.visitFieldInsn(GETSTATIC, "com/oto/Static", varDef.getName(), varDef.getTypeString());
                        case Owner.Local() -> mv.visitVarInsn(ILOAD, helper.getVarIndex(varDef));
                        default -> throw new RuntimeException("No, we cannot deal with" + owner);
                    }
                }
                helper.reportUseOpStack(1, varDef.getTypeString());
            } 
            case ArrayDefNode arrayDef -> {
                List<Exp> indexes = lVal.getExps();
                if (indexes.size() != arrayDef.getDimSizes().size()) {
                    throw new CError(ErrorType.ARRAY_DIM_ERROR, "Found " + indexes.size() + 
                                        " , expect " + arrayDef.getDimSizes().size());
                }
                Owner owner = arrayDef.getOwner();
                switch (owner) {
                    case Owner.Static() -> mv.visitFieldInsn(GETSTATIC, "com/oto/Static", arrayDef.getName(), arrayDef.getTypeString());
                    case Owner.Local() -> mv.visitVarInsn(ALOAD, helper.getVarIndex(arrayDef));             
                    default -> throw new RuntimeException("No, we cannot deal with" + owner);
                }
                helper.reportUseOpStack(1, arrayDef.getTypeString());
                for (Exp index : indexes) {
                    ExpSummoner.summonAddExp(index.getAddExp(), mv, helper, table);
                }
                mv.visitInsn(IALOAD);
                helper.reportPopOpStack(indexes.size() + 1);
                helper.reportUseOpStack(1, ReturnType.INTEGER.toTypeString());
            } 
            default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, def.getClass() + " should not be used as LVal.");
        }
    }
    
    private static void summonPrimaryExp(PrimaryExp primaryExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        switch (primaryExp.getWrappedExp()) {
            case Exp exp -> summonAddExp(exp.getAddExp(), mv, helper, table);
            case LVal lval -> summonLVal(lval, mv, helper, table);
            case GNumber number -> {
                mv.visitLdcInsn(number.getNumber());
                helper.reportUseOpStack(1, ReturnType.INTEGER.toTypeString());
            }
            default -> throw new RuntimeException(primaryExp.getWrappedExp().getClass() + " should not found in PrimaryExp");
        }
    }

    private static void summonUnaryExp(UnaryExp unaryExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        switch (unaryExp.getWrappedExp()) {
            case PrimaryExp primaryExp -> summonPrimaryExp(primaryExp, mv, helper, table);
            case OpExp opExp -> {
                switch (opExp.getUnaryOp().getOp()) {
                    case "PLUS +" -> summonUnaryExp(opExp.getUnaryExp(), mv, helper, table);
                    case "MINU -" -> {
                        summonUnaryExp(opExp.getUnaryExp(), mv, helper, table);
                        mv.visitInsn(INEG);
                    }
                    default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, opExp.getUnaryOp().getOp());
                }
                helper.reportPopOpStack(1);
                helper.reportUseOpStack(1, ReturnType.INTEGER.toTypeString());
            } 
            case FuncCall funcCall -> {
                IdentEntry entry = table.getEntry(funcCall.getIdent().getName(), SymbolType.fromString(funcCall.getIdent().getKind()));
                if (!(entry.getDef() instanceof FuncDefNode)) {
                    throw new CError(ErrorType.UNEXPECTED_TOKEN, entry.getName() + " is not a function!");
                }
                FuncDefNode funcDef = ((FuncDefNode)entry.getDef());
                if (funcDef.getOwner() instanceof Owner.ExVarLib(String varOwner, String varType, String varName, String methodName)) {
                    mv.visitFieldInsn(GETSTATIC, varOwner, varName, varType);
                    helper.reportUseOpStack(1, funcDef.getTypeString());
                }
                List<Exp> rparams = funcCall.getFuncRParams().getExps();
                if (rparams.size() != funcDef.getParams().size()) {
                    throw new CError(ErrorType.FUNC_PARAM_FAIL, "Expect " + funcDef.getParams().size() + ", got " + rparams.size());
                }
                for (int i = 0; i < rparams.size(); ++i) {
                    switch (funcDef.getParams().get(i)) {
                        case SimpleFuncParamNode spara -> summonExp(rparams.get(i), mv, helper, table);
                        case ArrayFuncParamNode apara -> {
                            ObjectClass arrayExp = rparams.get(i).getAddExp().getMulExp().getUnaryExp().getWrappedExp();
                            if ((!(arrayExp instanceof PrimaryExp)) || (!(((PrimaryExp)arrayExp).getWrappedExp() instanceof LVal))) {
                                throw new RuntimeException("So, where is my precious? My precious...");
                            }
                            Ident arrayIdent = ((LVal)((PrimaryExp)arrayExp).getWrappedExp()).getIdent();
                            DefNode arrayDef = table.getEntry(arrayIdent.getName(), SymbolType.VARIABLE).getDef();
                            if (!(arrayDef instanceof ArrayDefNode)) {
                                throw new RuntimeException("You have to be an array!");
                            }
                            switch (arrayDef.getOwner()) {
                                case Owner.Static() -> mv.visitFieldInsn(GETSTATIC, "com/oto/Static", arrayDef.getName(), ((ArrayDefNode)arrayDef).getTypeString());
                                case Owner.Local() -> mv.visitVarInsn(ALOAD, helper.getVarIndex(arrayDef));             
                                default -> throw new RuntimeException("No, an array must beheld!");
                            }
                            helper.reportUseOpStack(1, ((ArrayDefNode)arrayDef).getTypeString());
                        }
                        default -> throw new RuntimeException("What is this param? " + funcDef.getParams().get(i));
                    }
                }
                switch (funcDef.getOwner()) {
                    case Owner.Main() -> mv.visitMethodInsn(INVOKESTATIC, "com/oto/Main", funcDef.getName(), funcDef.getTypeString(), true);
                    case Owner.ExVarLib exLib -> mv.visitMethodInsn(INVOKEVIRTUAL, exLib.getVarOwner(), 
                                                                    exLib.methodName(), funcDef.getTypeString(), false);
                    default -> throw new RuntimeException("A function cannot be possessed by " + funcDef.getOwner());
                }
                helper.reportPopOpStack(rparams.size());
                if (funcDef.getOwner() instanceof Owner.ExVarLib) {
                    helper.reportPopOpStack(1);
                }
            } 
            default -> throw new CError(ErrorType.EXP_PARSE_FAIL, unaryExp.getWrappedExp().getClass().toString());
        }
    }

    private static void summonMulExp(MulExp mulExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        if (mulExp.getMulExp() == null) {
            summonUnaryExp(mulExp.getUnaryExp(), mv, helper, table);
        } else {
            summonMulExp(mulExp.getMulExp(), mv, helper, table);
            summonUnaryExp(mulExp.getUnaryExp(), mv, helper, table);
            switch (mulExp.getCh()) {
                case "MULT *" -> mv.visitInsn(IMUL);
                case "DIV /" -> mv.visitInsn(IDIV);
                case "MOD %" -> mv.visitInsn(IREM);
                default -> throw new RuntimeException(mulExp.getCh());
            }
            helper.reportPopOpStack(2);
            helper.reportUseOpStack(1, ReturnType.INTEGER.toTypeString());
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
            switch (addExp.getCh()) {
                case "PLUS +" -> mv.visitInsn(IADD);
                case "MINU -" -> mv.visitInsn(ISUB);
                default -> throw new RuntimeException(addExp.getCh());
            }
            helper.reportPopOpStack(2);
            helper.reportUseOpStack(1, ReturnType.INTEGER.toTypeString());
        }
    }

    // Evaluate an Exp at Runtime.
    public static void summonExp(Exp exp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        summonAddExp(exp.getAddExp(), mv, helper, table);
    }    
}