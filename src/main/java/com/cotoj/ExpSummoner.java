package com.cotoj;

import java.util.List;
import java.util.Map.Entry;

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
import com.cotoj.utils.ExpTypeHelper;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.JavaType;
import com.cotoj.utils.MethodHelper;
import com.cotoj.utils.OpcodeHelper;
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
                    ObjectClass arrayExp = callParam.getLOrExp().getlAndExp().getEqExp().getRelExp().getAddExp().getMulExp().getUnaryExp().getWrappedExp();
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
                    AutoPacker.summonPack(paramType, mv, helper);
                } else {
                    throw new CError(ErrorType.TYPE_MISMATCH, "Expect " + paramType + ", found " + defParam.getType());
                }
            }
            if (varParamIndex > 0) {
                mv.visitInsn(OpcodeHelper.toArrayStore(defParam.getType()));
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
                        case Owner.Local() -> mv.visitVarInsn(OpcodeHelper.toLoad(varDef.getType()), helper.getVarIndex(varDef));
                        case Owner.Class(String clazz, boolean isInt) -> {
                            mv.visitVarInsn(ALOAD, 0);
                            mv.visitFieldInsn(GETFIELD, clazz, varDef.getName(), varDef.getDescriptor());
                        }
                        default -> throw new RuntimeException("No, we cannot deal with" + owner);
                    }
                }
                helper.reportUseOpStack(1, varDef.getTypeString());
                if (lVal.getExps().isEmpty()) {
                    return varDef.getType();
                }
                // This must be a List or a Dict.
                if (lVal.getExps().size() != 1) {
                    throw new CError(ErrorType.UNEXPECTED_TOKEN, "No, a list/dict cannot be multi-indexed.");
                }
                switch (varDef.getType()) {
                    case ReturnType.List(ReturnType cType) -> {
                        ExpTypeHelper.checkInt(summonExp(lVal.getExps().getFirst(), mv, helper, table));
                        mv.visitMethodInsn(INVOKEINTERFACE, JavaType.LIST_INT.toTypeString(), "get", "(I)Ljava/lang/Object;", true);
                        AutoPacker.summonInlineUnpack(cType, mv);
                        helper.reportPopOpStack(2);
                        helper.reportUseOpStack(1, cType.toTypeString());
                        return cType;
                    }
                    case ReturnType.Dict(ReturnType keyType, ReturnType valType) -> {
                        ExpTypeHelper.checkMatch(keyType, summonExp(lVal.getExps().getFirst(), mv, helper, table));
                        AutoPacker.summonPack(keyType, mv, helper);
                        mv.visitMethodInsn(INVOKEINTERFACE, JavaType.DICT_INT.toTypeString(), "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                        AutoPacker.summonInlineUnpack(valType, mv);
                        helper.reportPopOpStack(2);
                        helper.reportUseOpStack(1, valType.toTypeString());
                        return valType;
                    }
                    default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, "No, you cannot index a simple variable.");
                }
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
                int dim_index = 0;
                while (dim_index < indexes.size()) {
                    ExpSummoner.summonExp(indexes.get(dim_index), mv, helper, table);
                    mv.visitInsn(dim_index == indexes.size() - 1 ? OpcodeHelper.toArrayLoad(arrayDef.getType()) : AALOAD);
                    helper.reportPopOpStack(2);
                    helper.reportUseOpStack(1, arrayDef.getIndexedTypeString(dim_index + 1));
                }
                return arrayDef.getType();
            } 
            default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, def.getClass() + " should not be used as LVal.");
        }
    }
    
    private static ReturnType summonPrimaryExp(PrimaryExp primaryExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        return switch (primaryExp.getWrappedExp()) {
            case Exp exp -> summonExp(exp, mv, helper, table);
            case LVal lval -> summonLVal(lval, mv, helper, table);
            case GNumber number -> {
                mv.visitLdcInsn(number.getNumber());
                helper.reportUseOpStack(1, "I"); 
                yield new ReturnType.Integer();
            }
            case GBool gb -> {
                mv.visitLdcInsn(gb.getBool().booleanValue());
                helper.reportUseOpStack(1, "Z");
                yield new ReturnType.Boolean();
            }
            case GFloat gf -> {
                mv.visitLdcInsn(gf.getNumber());
                helper.reportUseOpStack(1, "F");
                yield new ReturnType.Float();
            }
            case GString gstr -> {
                mv.visitLdcInsn(gstr.getString());
                helper.reportUseOpStack(1, JavaType.STRING.toTypeString());
                yield JavaType.STRING;
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
                ReturnType opType = summonUnaryExp(opExp.getUnaryExp(), mv, helper, table);
                if (opType instanceof ReturnType.Integer) {
                    switch (opExp.getUnaryOp().getOp()) {
                        case "+" -> {}
                        case "-" -> {mv.visitInsn(INEG);}
                        default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, opExp.getUnaryOp().getOp());
                    };
                } else if (opType instanceof ReturnType.Float) {
                    switch (opExp.getUnaryOp().getOp()) {
                        case "+" -> {}
                        case "-" -> {mv.visitInsn(FNEG);}
                        default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, opExp.getUnaryOp().getOp());
                    };
                } else {
                    throw new CError(ErrorType.TYPE_MISMATCH, opType + " cannot be operated by +-.");
                }
                helper.reportPopOpStack(1);
                helper.reportUseOpStack(1, opType.toTypeString());
                yield opType;
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
            ReturnType finalType = ExpTypeHelper.checkBasic(lType, rType);
            if (finalType instanceof ReturnType.Integer) {
                switch (mulExp.getCh()) {
                    case "*" -> mv.visitInsn(IMUL);
                    case "/" -> mv.visitInsn(IDIV);
                    case "%" -> mv.visitInsn(IREM);
                    default -> throw new RuntimeException(mulExp.getCh());
                }
            } else if (finalType instanceof ReturnType.Float) {
                ExpTypeHelper.intToFloat(lType, rType, mv, helper);
                switch (mulExp.getCh()) {
                    case "*" -> mv.visitInsn(FMUL);
                    case "/" -> mv.visitInsn(FDIV);
                    case "%" -> mv.visitInsn(FREM);
                    default -> throw new RuntimeException(mulExp.getCh());
                }
            } else {
                throw new RuntimeException("Internal Error occured.");
            }
            helper.reportPopOpStack(2);
            helper.reportUseOpStack(1, finalType.toTypeString());
            return finalType;
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
            ReturnType finalType = ExpTypeHelper.checkBasic(lType, rType);
            if (finalType instanceof ReturnType.Integer) {
                switch (addExp.getCh()) {
                    case "+" -> mv.visitInsn(IADD);
                    case "-" -> mv.visitInsn(ISUB);
                    default -> throw new RuntimeException(addExp.getCh());
                }
            } else if (finalType instanceof ReturnType.Float) {
                ExpTypeHelper.intToFloat(lType, rType, mv, helper);
                switch (addExp.getCh()) {
                    case "+" -> mv.visitInsn(FADD);
                    case "-" -> mv.visitInsn(FSUB);
                    default -> throw new RuntimeException(addExp.getCh());
                }
            } else {
                throw new RuntimeException("Internal Error occured.");
            }
            helper.reportPopOpStack(2);
            helper.reportUseOpStack(1, lType.toTypeString());
            return lType;
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
            ReturnType finalType = ExpTypeHelper.checkCompare(lType, rType);
            if (finalType instanceof ReturnType.Integer) {
                switch(relExp.getCh()) {
                    case ">=" -> mv.visitJumpInsn(IF_ICMPGE, endLabel);
                    case "<=" -> mv.visitJumpInsn(IF_ICMPLE, endLabel);
                    case ">" -> mv.visitJumpInsn(IF_ICMPGT, endLabel);
                    case "<" -> mv.visitJumpInsn(IF_ICMPLT, endLabel);
                    default -> throw new RuntimeException("Not sure what is " + relExp.getCh());
                }
            } else if (finalType instanceof ReturnType.Float) {
                ExpTypeHelper.intToFloat(lType, rType, mv, helper);
                mv.visitInsn(FCMPG);
                switch(relExp.getCh()) {
                    case ">=" -> mv.visitJumpInsn(IFGE, endLabel);
                    case "<=" -> mv.visitJumpInsn(IFLE, endLabel);
                    case ">" -> mv.visitJumpInsn(IFGT, endLabel);
                    case "<" -> mv.visitJumpInsn(IFLT, endLabel);
                    default -> throw new RuntimeException("Not sure what is " + relExp.getCh());
                }
            } else if (JavaType.STRING.equals(finalType)) {
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Comparable", "compareTo", "(Ljava/lang/Object;)I", true);
                switch(relExp.getCh()) {
                    case ">=" -> mv.visitJumpInsn(IFGE, endLabel);
                    case "<=" -> mv.visitJumpInsn(IFLE, endLabel);
                    case ">" -> mv.visitJumpInsn(IFGT, endLabel);
                    case "<" -> mv.visitJumpInsn(IFLT, endLabel);
                    default -> throw new RuntimeException("Not sure what is " + relExp.getCh());
                }
            } else {
                throw new RuntimeException("Internal Error occured.");
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
            ReturnType finalType = ExpTypeHelper.checkEqual(lType, rType);
            if (finalType instanceof ReturnType.Integer || finalType instanceof ReturnType.Boolean) {
                switch(eqExp.getCh()) {
                    case "==" -> mv.visitJumpInsn(IF_ICMPEQ, label);
                    case "!=" -> mv.visitJumpInsn(IF_ICMPNE, label);
                    default -> throw new RuntimeException("Not sure what is " + eqExp.getCh());
                }
            } else if (finalType instanceof ReturnType.Float) {
                ExpTypeHelper.intToFloat(lType, rType, mv, helper);
                mv.visitInsn(FCMPG);
                switch(eqExp.getCh()) {
                    case "==" -> mv.visitJumpInsn(IFEQ, label);
                    case "!=" -> mv.visitJumpInsn(IFNE, label);
                    default -> throw new RuntimeException("Not sure what is " + eqExp.getCh());
                }
            } else {
                mv.visitMethodInsn(INVOKEVIRTUAL, JavaType.OBJECT.toTypeString(), "equals", "(Ljava/lang/Object)Z", false);
                switch(eqExp.getCh()) {
                    case "==" -> mv.visitJumpInsn(IFNE, label);
                    case "!=" -> mv.visitJumpInsn(IFEQ, label);
                    default -> throw new RuntimeException("Not sure what is " + eqExp.getCh());
                }
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
            ExpTypeHelper.anyToBool(pair.type(), mv, helper);
            helper.dup(mv);
            mv.visitJumpInsn(IFEQ, midPoint);
            helper.reportPopOpStack(1);
            ExpTypeHelper.anyToBool(summonEqxp(lAndExp.getEqExp(), mv, helper, table), mv, helper);
            return TypePair.Yup(new ReturnType.Boolean());
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
            ExpTypeHelper.anyToBool(pair.type(), mv, helper);
            helper.dup(mv);
            mv.visitJumpInsn(IFNE, skipPoint);
            helper.reportPopOpStack(1);
            ExpTypeHelper.anyToBool(summonLAndExp(lOrExp.getlAndExp(), mv, helper, table, skipPoint).type, mv, helper);
            return TypePair.Yup(new ReturnType.Boolean());
        }
    }

    public static ReturnType summonDotExp(DotExp dotExp, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        Label startLabel = new Label();
        TypePair pair = summonLOrExp(dotExp.getLOrExp(), mv, helper, table, startLabel);
        ReturnType curType = pair.type();
        if (dotExp.getDotList().isEmpty()) {
            throw new RuntimeException("Dotter list cannot be empty.");
        }
        if (pair.used()) {
            helper.visitFrame(mv);
            mv.visitLabel(startLabel);
        }
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
            Label label = new Label();
            TypePair pair = summonLOrExp(exp.getLOrExp(), mv, helper, table, label);
            if (pair.used) {
                helper.visitFrame(mv);
                mv.visitLabel(label);
            }
            return pair.type();
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

    public static void summonList(ReturnType.List type, boolean isShared, InitVal initVal, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        ReturnType listType = isShared ? JavaType.CONCLIST : JavaType.ARRLIST;
        mv.visitTypeInsn(NEW, listType.toTypeString());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, listType.toTypeString(), "<init>", "()V", false);
        helper.reportUseOpStack(1, listType.toTypeString());
        if (initVal == null) {
            return;
        }
        if (!(initVal.getInitForm() instanceof InitValList)) {
            throw new CError(ErrorType.UNEXPECTED_TOKEN, "You must use init-list to init list.");
        }
        for (InitVal listInitVal : ((InitValList)initVal.getInitForm()).getInitVals()) {
            if (!(listInitVal.getInitForm() instanceof Exp)) {
                throw new CError(ErrorType.UNEXPECTED_TOKEN, "List can only be initialized with 1-level init list.");
            }
            helper.dup(mv);
            ExpTypeHelper.checkMatch(type.contentType(), ExpSummoner.summonExp(((Exp)listInitVal.getInitForm()), mv, helper, table));
            AutoPacker.summonPack(type.contentType(), mv, helper);
            mv.visitMethodInsn(INVOKEINTERFACE, JavaType.LIST_INT.toTypeString(), "add", "(Ljava/lang/Object;)Z", true);
            mv.visitInsn(POP);
            helper.reportPopOpStack(2);
        }
    }

    public static void summonDict(ReturnType.Dict type, boolean isShared, InitVal initVal, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        ReturnType dictType = isShared ? JavaType.CONCDICT : JavaType.HASHDICT;
        mv.visitTypeInsn(NEW, dictType.toTypeString());
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, dictType.toTypeString(), "<init>", "()V", false);
        helper.reportUseOpStack(1, dictType.toTypeString());
        if (initVal == null) {
            return;
        }
        if (!(initVal.getInitForm() instanceof InitValList)) {
            throw new CError(ErrorType.UNEXPECTED_TOKEN, "You must use init-list to init list.");
        }
        for (Entry<InitVal, InitVal> dictPair : ((InitValList)initVal.getInitForm()).getInitValsMap().entrySet()) {
            if (!((dictPair.getKey().getInitForm() instanceof Exp) && (dictPair.getValue().getInitForm() instanceof Exp))) {
                throw new CError(ErrorType.UNEXPECTED_TOKEN, "List can only be initialized with 1-level init list.");
            }
            helper.dup(mv);
            ExpTypeHelper.checkMatch(type.keyType(), ExpSummoner.summonExp(((Exp)dictPair.getKey().getInitForm()), mv, helper, table));
            AutoPacker.summonPack(type.keyType(), mv, helper);
            ExpTypeHelper.checkMatch(type.valueType(), ExpSummoner.summonExp(((Exp)dictPair.getValue().getInitForm()), mv, helper, table));
            AutoPacker.summonPack(type.valueType(), mv, helper);
            mv.visitMethodInsn(INVOKEINTERFACE, JavaType.DICT_INT.toTypeString(), "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
            helper.reportPopOpStack(3);
        }
    }
}
