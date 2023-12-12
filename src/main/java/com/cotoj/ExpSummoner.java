package com.cotoj;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.ArrayFuncParamNode;
import com.cotoj.adaptor.DefNode;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.FuncParamNode;
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
    public record TypePair(ReturnType type, boolean used) {
        // Let's go Rrrrrrust!
        public static TypePair Yup(ReturnType type) {
            return new TypePair(type, true);
        }
        public static TypePair Nop(ReturnType type) {
            return new TypePair(type, false);
        }
    };
    private static ReturnType summonLVal(LVal lVal, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
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
                        case Owner.Static sClass -> mv.visitFieldInsn(GETSTATIC, sClass.className(), varDef.getName(), varDef.getDescriptor());
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
                    case Owner.Static sClass -> mv.visitFieldInsn(GETSTATIC, sClass.className(), arrayDef.getName(), arrayDef.getDescriptor());
                    case Owner.Local() -> mv.visitVarInsn(ALOAD, helper.getVarIndex(arrayDef));             
                    default -> throw new RuntimeException("No, we cannot deal with" + owner);
                }
                helper.reportUseOpStack(1, arrayDef.getTypeString());
                for (Exp index : indexes) {
                    ExpSummoner.summonAddExp(index.getAddExp(), mv, helper, table);
                }
                mv.visitInsn(IALOAD);
                helper.reportPopOpStack(indexes.size() + 1);
                helper.reportUseOpStack(1, arrayDef.getTypeString());
            } 
            default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, def.getClass() + " should not be used as LVal.");
        }
        return def.getType();
    }
    
    private static ReturnType summonPrimaryExp(PrimaryExp primaryExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        return switch (primaryExp.getWrappedExp()) {
            case Exp exp -> summonAddExp(exp.getAddExp(), mv, helper, table);
            case LVal lval -> summonLVal(lval, mv, helper, table);
            case GNumber number -> {
                mv.visitLdcInsn(number.getNumber());
                // TODO
                helper.reportUseOpStack(1, "I"); 
                yield new ReturnType.Integer();
            }
            default -> throw new RuntimeException(primaryExp.getWrappedExp().getClass() + " should not found in PrimaryExp");
        };
    }

    private static ReturnType summonUnaryExp(UnaryExp unaryExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        return switch (unaryExp.getWrappedExp()) {
            case PrimaryExp primaryExp -> summonPrimaryExp(primaryExp, mv, helper, table);
            case OpExp opExp -> {
                ReturnType retType = switch (opExp.getUnaryOp().getOp()) {
                    case "PLUS +" -> summonUnaryExp(opExp.getUnaryExp(), mv, helper, table);
                    case "MINU -" -> {
                        var type = summonUnaryExp(opExp.getUnaryExp(), mv, helper, table);
                        mv.visitInsn(INEG);
                        yield type;
                    }
                    default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, opExp.getUnaryOp().getOp());
                };
                helper.reportPopOpStack(1);
                helper.reportUseOpStack(1, retType.toTypeString());
                yield retType;
            } 
            case FuncCall funcCall -> {
                IdentEntry entry = table.getEntry(funcCall.getIdent().getName(), SymbolType.fromString(funcCall.getIdent().getKind()));
                if (!(entry.getDef() instanceof FuncDefNode)) {
                    throw new CError(ErrorType.UNEXPECTED_TOKEN, entry.getName() + " is not a function!");
                }
                FuncDefNode funcDef = ((FuncDefNode)entry.getDef());
                List<Exp> rparams = funcCall.getFuncRParams().getExps();
                if (rparams.size() != funcDef.getParams().size()) {
                    throw new CError(ErrorType.FUNC_PARAM_FAIL, "Expect " + funcDef.getParams().size() + ", got " + rparams.size());
                }
                for (int i = 0; i < rparams.size(); ++i) {
                    FuncParamNode defParam = funcDef.getParams().get(i);
                    ReturnType paramType = switch (defParam) {
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
                                case Owner.Static sClass -> {mv.visitFieldInsn(GETSTATIC, sClass.className(), arrayDef.getName(), ((ArrayDefNode)arrayDef).getDescriptor());}
                                case Owner.Local() -> {mv.visitVarInsn(ALOAD, helper.getVarIndex(arrayDef));}
                                default -> throw new RuntimeException("No, an array must beheld!");
                            }
                            helper.reportUseOpStack(1, ((ArrayDefNode)arrayDef).getTypeString());
                            yield arrayDef.getType();
                        }
                        default -> throw new RuntimeException("What is this param? " + defParam);
                    };
                    if (!paramType.getClass().equals(defParam.getType().getClass())) {
                        throw new CError(ErrorType.TYPE_MISMATCH, "Expect " + paramType + ", found " + defParam.getType());
                    }
                }
                switch (funcDef.getOwner()) {
                    case Owner.Static(String clazz, boolean isInt) -> {mv.visitMethodInsn(INVOKESTATIC, clazz, funcDef.getName(), funcDef.getDescriptor(), isInt);}
                    default -> throw new RuntimeException("A function cannot be possessed by " + funcDef.getOwner());
                }
                helper.reportPopOpStack(rparams.size());
                yield funcDef.getReturnType();
            } 
            default -> throw new CError(ErrorType.EXP_PARSE_FAIL, unaryExp.getWrappedExp().getClass().toString());
        };
    }

    private static ReturnType summonMulExp(MulExp mulExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        if (mulExp.getMulExp() == null) {
            return summonUnaryExp(mulExp.getUnaryExp(), mv, helper, table);
        } else {
            ReturnType lType = summonMulExp(mulExp.getMulExp(), mv, helper, table);
            ReturnType rType = summonUnaryExp(mulExp.getUnaryExp(), mv, helper, table);
            if (!lType.getClass().equals(rType.getClass())) {
                throw new CError(ErrorType.TYPE_MISMATCH, "Left " + lType + ", right " + rType);
            }
            switch (mulExp.getCh()) {
                case "MULT *" -> mv.visitInsn(IMUL);
                case "DIV /" -> mv.visitInsn(IDIV);
                case "MOD %" -> mv.visitInsn(IREM);
                default -> throw new RuntimeException(mulExp.getCh());
            }
            helper.reportPopOpStack(2);
            helper.reportUseOpStack(1, lType.toTypeString());
            return lType;
        }
    }

    // Well, if you do not deal with the complexity abide in the grammar, then
    // every single layer will suffer from the complexity.
    // This can be a good lesson...
    private static ReturnType summonAddExp(AddExp addExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        if (addExp.getAddExp() == null) {
            return summonMulExp(addExp.getMulExp(), mv, helper, table);
        } else {
            ReturnType lType = summonAddExp(addExp.getAddExp(), mv, helper, table);
            ReturnType rType = summonMulExp(addExp.getMulExp(), mv, helper, table);
            if (!lType.getClass().equals(rType.getClass())) {
                throw new CError(ErrorType.TYPE_MISMATCH, "Left " + lType + ", right " + rType);
            }
            switch (addExp.getCh()) {
                case "PLUS +" -> mv.visitInsn(IADD);
                case "MINU -" -> mv.visitInsn(ISUB);
                default -> throw new RuntimeException(addExp.getCh());
            }
            helper.reportPopOpStack(2);
            helper.reportUseOpStack(1, lType.toTypeString());
            return lType;
        }
    }

    // Evaluate an Exp at Runtime.
    public static ReturnType summonExp(Exp exp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        return summonAddExp(exp.getAddExp(), mv, helper, table);
    }

    private static ReturnType summonRelExp(RelExp relExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        if (relExp.getRelExp() == null) {
            return summonAddExp(relExp.getAddExp(), mv, helper, table);
        } else {
            mv.visitInsn(ICONST_1);
            Label endLabel = new Label();
            ReturnType lType = summonRelExp(relExp.getRelExp(), mv, helper, table);
            ReturnType rType = summonAddExp(relExp.getAddExp(), mv, helper, table);
            helper.visitFrame(mv);
            // Yes we can calculate those without branch
            // but at 7-8 instructions' cost so
            if (!lType.getClass().equals(rType.getClass())) {
                throw new CError(ErrorType.TYPE_MISMATCH, "Left " + lType + ", right " + rType);
            }
            switch(relExp.getCh()) {
                case ">=" -> mv.visitJumpInsn(IF_ICMPGE, endLabel);
                case "<=" -> mv.visitJumpInsn(IF_ICMPLE, endLabel);
                case ">" -> mv.visitJumpInsn(IF_ICMPGT, endLabel);
                case "<" -> mv.visitJumpInsn(IF_ICMPLT, endLabel);
                default -> throw new RuntimeException("Not sure what is " + relExp.getCh());
            }
            mv.visitInsn(POP);
            mv.visitInsn(ICONST_0);
            mv.visitLabel(endLabel);
            helper.visitFrame(mv);
            return new ReturnType.Integer();
        }
    }

    private static ReturnType summonEqxp(EqExp eqExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        if (eqExp.getEqExp() == null) {
            return summonRelExp(eqExp.getRelExp(), mv, helper, table);
        } else {
            ReturnType lType = summonEqxp(eqExp.getEqExp(), mv, helper, table);
            ReturnType rType = summonRelExp(eqExp.getRelExp(), mv, helper, table);
            if (!lType.getClass().equals(rType.getClass())) {
                throw new CError(ErrorType.TYPE_MISMATCH, "Left " + lType + ", right " + rType);
            }
            mv.visitInsn(IXOR);
            switch(eqExp.getCh()) {
                case "==" -> {
                    mv.visitInsn(INEG);
                    mv.visitInsn(ICONST_1);
                    mv.visitInsn(ISUB);
                }
                case "!=" -> {}
                default -> throw new RuntimeException("Not sure what is " + eqExp.getCh());
            }
            helper.reportPopOpStack(2);
            helper.reportUseOpStack(1, "I");
            return new ReturnType.Integer();
        }
    }

    private static TypePair summonLAndExp(LAndExp lAndExp, MethodVisitor mv, MethodHelper helper, SymbolTable table, Label skipPoint) {
        if (lAndExp.getlAndExp() == null) {
            return TypePair.Nop(summonEqxp(lAndExp.getEqExp(), mv, helper, table));
        } else {
            Label midPoint = new Label();
            TypePair pair = summonLAndExp(lAndExp.getlAndExp(), mv, helper, table, skipPoint);
            if (pair.used) {
                helper.visitFrame(mv);
                mv.visitLabel(midPoint);
            }
            helper.dup(mv);
            mv.visitJumpInsn(IFEQ, midPoint);
            return TypePair.Yup(summonEqxp(lAndExp.getEqExp(), mv, helper, table));
        }
    }

    public static TypePair summonLOrExp(LOrExp lOrExp, MethodVisitor mv, MethodHelper helper, SymbolTable table, Label skipPoint) {
        if (lOrExp.getlOrExp() == null) {
            return summonLAndExp(lOrExp.getlAndExp(), mv, helper, table, skipPoint);
        } else {
            Label midPoint = new Label();
            TypePair pair = summonLOrExp(lOrExp.getlOrExp(), mv, helper, table, midPoint);
            if (pair.used) {
                helper.visitFrame(mv);
                mv.visitLabel(midPoint);
            }
            helper.dup(mv);
            mv.visitJumpInsn(IFNE, skipPoint);
            return TypePair.Yup(summonLAndExp(lOrExp.getlAndExp(), mv, helper, table, skipPoint).type);
        }
    }
}