package com.cotoj;

import java.util.List;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.ArrayFuncParamNode;
import com.cotoj.adaptor.DefNode;
import com.cotoj.adaptor.DotExp;
import com.cotoj.adaptor.MethodInvokeDotter;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.FuncParamNode;
import com.cotoj.adaptor.SimpleFuncParamNode;
import com.cotoj.adaptor.StaticAccessExp;
import com.cotoj.adaptor.VarDefNode;
import com.cotoj.adaptor.VariableFuncParamNode;
import com.cotoj.utils.AutoPacker;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.JavaType;
import com.cotoj.utils.MethodHelper;
import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;
import com.cotoj.utils.SymbolType;
import com.cotoj.utils.ThreadHelper;
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

    private static void loadFuncParam(List<FuncParamNode> funcParams, List<Exp> callParams, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        int defParamIndex = 0;
        boolean hasVariableParam = funcParams.size() > 1 && (funcParams.getLast() instanceof VariableFuncParamNode);
        int varParamIndex = 0;
        if ((hasVariableParam && funcParams.size() < callParams.size() - 1) || (!hasVariableParam && funcParams.size() != callParams.size())) {
            throw new CError(ErrorType.FUNC_PARAM_FAIL, "The count of params do not match the function signature,");
        }
        for (Exp callParam : callParams) {
            FuncParamNode defParam = funcParams.get(defParamIndex);
            ReturnType paramType = switch (defParam) {
                case SimpleFuncParamNode spara -> summonExp(callParam, mv, helper, table);
                case ArrayFuncParamNode apara -> {
                    ObjectClass arrayExp = callParam.getAddExp().getMulExp().getUnaryExp().getWrappedExp();
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
                case VariableFuncParamNode vParam -> {
                    if (varParamIndex == 0) {
                        mv.visitIntInsn(SIPUSH, callParams.size() - defParamIndex);
                        mv.visitTypeInsn(ANEWARRAY, vParam.getType().toTypeString());
                        helper.reportUseOpStack(1, "[" + vParam.getType().toTypeString());
                    }
                    helper.dup(mv);
                    mv.visitLdcInsn(varParamIndex);
                    helper.reportUseOpStack(1, "I");
                    ReturnType type = summonExp(callParam, mv, helper, table);
                    ++varParamIndex;
                    --defParamIndex;
                    yield type;
                }
            };
            if (!paramType.getClass().equals(defParam.getType().getClass())) {
                if (AutoPacker.isPacked(paramType, defParam.getType())) {
                    AutoPacker.summonPack(paramType, mv, helper, table);
                } else {
                    throw new CError(ErrorType.TYPE_MISMATCH, "Expect " + paramType + ", found " + defParam.getType());
                }
            }
            if (varParamIndex > 0) {
                if (defParam.getType().isPrimitive()) {
                    mv.visitInsn(IASTORE);
                } else {
                    mv.visitInsn(AASTORE);
                }
                helper.reportPopOpStack(3);
            }
            ++defParamIndex;
        }
        if (hasVariableParam && varParamIndex == 0) {
            VariableFuncParamNode vParam = ((VariableFuncParamNode)funcParams.getLast());
            mv.visitIntInsn(SIPUSH, callParams.size() - defParamIndex);
            mv.visitTypeInsn(ANEWARRAY, vParam.getType().toTypeString());
            helper.reportUseOpStack(1, "[" + vParam.getType().toTypeString());
        }
    }

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
                        case Owner.Class(String clazz, boolean isInt) -> {
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitFieldInsn(GETFIELD, clazz, varDef.getName(), varDef.getDescriptor());
                        }
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
                    case Owner.Class(String clazz, boolean isInt) -> {
                        mv.visitVarInsn(ALOAD, 0);
                        mv.visitFieldInsn(GETFIELD, clazz, arrayDef.getName(), arrayDef.getDescriptor());
                    }          
                    default -> throw new RuntimeException("No, we cannot deal with" + owner);
                }
                helper.reportUseOpStack(1, arrayDef.getTypeString());
                for (Exp index : indexes) {
                    ExpSummoner.summonExp(index, mv, helper, table);
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
            case Exp exp -> summonExp(exp, mv, helper, table);
            case LVal lval -> summonLVal(lval, mv, helper, table);
            case GNumber number -> {
                mv.visitLdcInsn(number.getNumber());
                // TODO
                helper.reportUseOpStack(1, "I"); 
                yield new ReturnType.Integer();
            }
            case StaticAccessExp sa -> {
                mv.visitFieldInsn(GETSTATIC, sa.getClassType().toTypeString(), sa.getFieldName(), sa.getFieldType().toDescriptor());
                helper.reportUseOpStack(1, sa.getFieldType().toTypeString());
                yield sa.getFieldType();
            }
            case StringLiteral str -> {
                mv.visitLdcInsn(str.getLiteralString());
                helper.reportUseOpStack(1, JavaType.STRING.toTypeString());
                yield JavaType.STRING;
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
                IdentEntry entry = table.getEntry(funcCall.getIdent().getName(), SymbolType.FUNCTION);
                if (entry == null) {
                    throw new CError(ErrorType.IDENT_NOT_EXISTS, funcCall.getIdent().getName() + "is a Teapot - 14.");
                }
                if (!(entry.getDef() instanceof FuncDefNode)) {
                    throw new CError(ErrorType.UNEXPECTED_TOKEN, entry.getName() + " is not a function!");
                }
                FuncDefNode funcDef = ((FuncDefNode)entry.getDef());
                List<Exp> rparams = funcCall.getFuncRParams().getExps();
                switch (funcDef.getOwner()) {
                    case Owner.Static(String clazz, boolean isInt) -> {
                        loadFuncParam(funcDef.getParams(), rparams, mv, helper, table);
                        mv.visitMethodInsn(INVOKESTATIC, clazz, funcDef.getName(), funcDef.getDescriptor(), isInt);
                    }
                    case Owner.Class(String clazz, boolean isInt) -> {
                        mv.visitTypeInsn(NEW, clazz);
                        mv.visitInsn(DUP);
                        loadFuncParam(funcDef.getParams(), rparams, mv, helper, table);
                        mv.visitMethodInsn(INVOKESPECIAL, clazz, "<init>", 
                                            ThreadHelper.getInitDescriptor(funcDef), isInt);
                        mv.visitMethodInsn(INVOKEINTERFACE, "java/lang/Runnable", "run", "()V", true);
                    }
                    default -> throw new RuntimeException("A function cannot be possessed by " + funcDef.getOwner());
                }
                helper.reportPopOpStack(rparams.size());
                ReturnType retType = funcDef.getReturnType();
                if (!(retType instanceof ReturnType.Void)) {
                    helper.reportUseOpStack(1, retType.toTypeString());
                }
                yield retType;
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
                case "*" -> mv.visitInsn(IMUL);
                case "/" -> mv.visitInsn(IDIV);
                case "%" -> mv.visitInsn(IREM);
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
                case "+" -> mv.visitInsn(IADD);
                case "-" -> mv.visitInsn(ISUB);
                default -> throw new RuntimeException(addExp.getCh());
            }
            helper.reportPopOpStack(2);
            helper.reportUseOpStack(1, lType.toTypeString());
            return lType;
        }
    }

    public static ReturnType summonDotExp(DotExp dotExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        ReturnType curType = summonAddExp(dotExp.getAddExp(), mv, helper, table);
        for (MethodInvokeDotter dotter : dotExp.getDotList()) {
            loadFuncParam(dotter.getDefParams(), dotter.getCallParams(), mv, helper, table);
            StringBuilder sb = new StringBuilder("(");
            for (var defParam : dotter.getDefParams()) {
                sb.append(defParam.getDescriptor());
            }
            sb.append(')');
            sb.append(dotter.getReturnType().toDescriptor());
            mv.visitMethodInsn(INVOKEVIRTUAL, curType.toTypeString(), dotter.getIdentName(), sb.toString(), dotter.isInterface());
            curType = dotter.getReturnType();
            helper.reportPopOpStack(dotter.getDefParams().size() + 1);
            helper.reportUseOpStack(1, curType.toTypeString());
        }
        return curType;
    }

    // Evaluate an Exp at Runtime.
    public static ReturnType summonExp(Exp exp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        if (exp instanceof DotExp dotExp) {
            return summonDotExp(dotExp, mv, helper, table);
        }
        else {
            return summonAddExp(exp.getAddExp(), mv, helper, table);
        }
    }

    private static ReturnType summonRelExp(RelExp relExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        if (relExp.getRelExp() == null) {
            return summonAddExp(relExp.getAddExp(), mv, helper, table);
        } else {
            mv.visitInsn(ICONST_1);
            Label endLabel = new Label();
            helper.reportUseOpStack(1, "I");
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
            helper.reportPopOpStack(2);
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
            Label label = new Label();
            mv.visitInsn(ICONST_1);
            helper.reportUseOpStack(1, "I");
            ReturnType lType = summonEqxp(eqExp.getEqExp(), mv, helper, table);
            ReturnType rType = summonRelExp(eqExp.getRelExp(), mv, helper, table);
            if (!lType.getClass().equals(rType.getClass())) {
                throw new CError(ErrorType.TYPE_MISMATCH, "Left " + lType + ", right " + rType);
            }
            switch(eqExp.getCh()) {
                case "==" -> mv.visitJumpInsn(IF_ICMPEQ, label);
                case "!=" -> mv.visitJumpInsn(IF_ICMPNE, label);
                default -> throw new RuntimeException("Not sure what is " + eqExp.getCh());
            }
            mv.visitInsn(POP);
            mv.visitInsn(ICONST_0);
            helper.reportPopOpStack(2);
            helper.visitFrame(mv);
            mv.visitLabel(label);
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
            helper.reportPopOpStack(1);
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
            helper.reportPopOpStack(1);
            return TypePair.Yup(summonLAndExp(lOrExp.getlAndExp(), mv, helper, table, skipPoint).type);
        }
    }

    public static ReturnType summonThread(FuncCall funcCall, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        IdentEntry entry = table.getEntry(funcCall.getIdent().getName(), SymbolType.FUNCTION);
        if (entry == null) {
            throw new CError(ErrorType.NOT_A_THREAD, "No thread named " + funcCall.getIdent().getName() + " can be found.");
        }
        FuncDefNode funcDef = ((FuncDefNode)entry.getDef());
        Owner.Class owner = ((Owner.Class)funcDef.getOwner());
        mv.visitTypeInsn(NEW, owner.className());
        mv.visitInsn(DUP);
        helper.reportUseOpStack(2, null);
        loadFuncParam(funcDef.getParams(), funcCall.getFuncRParams().getExps(), mv, helper, table);
        mv.visitMethodInsn(INVOKESPECIAL, owner.className(), "<init>", 
                            ThreadHelper.getInitDescriptor(funcDef), owner.isInterface());
        helper.reportPopOpStack(2 + funcCall.getFuncRParams().getExps().size());
        helper.reportUseOpStack(1, owner.className());
        return new ReturnType.JavaClass(owner.className());
    }
}