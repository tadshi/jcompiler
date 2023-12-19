package com.cotoj;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.cotoj.utils.ExpTypeHelper;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.JavaType;
import com.cotoj.utils.ReturnType;
import com.cotoj.utils.SymbolType;
import com.cotoj.utils.TypedObject;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.AddExp;
import com.front.gunit.ConstExp;
import com.front.gunit.ConstInitVal;
import com.front.gunit.ConstInitValList;
import com.front.gunit.Exp;
import com.front.gunit.GBool;
import com.front.gunit.GFloat;
import com.front.gunit.GNumber;
import com.front.gunit.GString;
import com.front.gunit.Ident;
import com.front.gunit.LVal;
import com.front.gunit.MulExp;
import com.front.gunit.ObjectClass;
import com.front.gunit.OpExp;
import com.front.gunit.PrimaryExp;
import com.front.gunit.UnaryExp;

public interface ConstExpParser {
    private static TypedObject parseLVal(LVal lVal, SymbolTable table) {
        Ident ident = lVal.getIdent();
        IdentEntry entry = table.getEntry(ident.getName(), SymbolType.fromString(ident.getKind()));
        if (ident.getDimension() == 0) {
            return new TypedObject(entry.getType(), entry.getCompileTimeValue());
        } else {
            if (ident.getDimension() != lVal.getExps().size()) {
                throw new CError(ErrorType.EXPECT_TOKEN, "[");
            }
            List<Integer> const_index = lVal.getExps().stream()
                                                    .map(exp -> exp.getAddExp())
                                                    .map(addExp -> parseAddExp(addExp, table))
                                                    .map(typedObj -> {if (typedObj.type() instanceof ReturnType.Integer)
                                                                        return (Integer)typedObj.object();
                                                                    throw new CError(ErrorType.TYPE_MISMATCH, "An arr index must be a int.");})
                                                    .collect(Collectors.toList());
            return new TypedObject(entry.getType(), entry.indexCompileTimeValue(const_index));
        }
    }

    // <PrimaryExp> ::= (<Exp>) | <LVal> | <Number>
    private static TypedObject parsePrimaryExp(PrimaryExp exp, SymbolTable table) {
        ObjectClass misteryExp = exp.getWrappedExp();
        return switch (misteryExp) {
            case Exp e -> parseAddExp(((Exp)misteryExp).getAddExp(), table);
            case LVal lVal -> parseLVal((LVal)misteryExp, table);
            case GNumber number -> new TypedObject(JavaType.INTEGER, number.getNumber());
            case GBool bool -> new TypedObject(JavaType.BOOLEAN, bool.getBool());
            case GFloat f -> new TypedObject(JavaType.FLOAT, f.getNumber());
            case GString str -> new TypedObject(JavaType.STRING, str.getString());
            default -> throw new RuntimeException(misteryExp.getClass() + " should not found in PrimaryExp");
        };
    }

    // <UnaryExp> ::= <PrimaryExp> | <Ident> ([<FuncRParams]) | <UnaryOp> <UnaryExp>
    private static TypedObject parseUnaryExp(UnaryExp exp, SymbolTable table) {
        ObjectClass misteryExp = exp.getWrappedExp();
        if (misteryExp instanceof OpExp) {
            OpExp opExp = ((OpExp)misteryExp);
            TypedObject operand = parseUnaryExp(opExp.getUnaryExp(), table);
            if (operand.type() instanceof ReturnType.Integer) {
                Integer gInteger = ((Integer)operand.object());
                return switch (opExp.getUnaryOp().getOp()) {
                    case "PLUS +" -> operand;
                    case "MINU -" -> operand.map(-gInteger);
                    default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, opExp.getUnaryOp().getOp());
                };
            } else if (operand.type() instanceof ReturnType.Float) {
                Float gFloat = ((Float)operand.object());
                return switch (opExp.getUnaryOp().getOp()) {
                    case "PLUS +" -> operand;
                    case "MINU -" -> operand.map(-gFloat);
                    default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, opExp.getUnaryOp().getOp());
                };
            } else {
                throw new CError(ErrorType.TYPE_MISMATCH, "A value typed " + operand.type() + " cannot use + or -.");
            }
        } else if (misteryExp instanceof PrimaryExp) {
            PrimaryExp primaryExp = ((PrimaryExp)misteryExp);
            return parsePrimaryExp(primaryExp, table);
        } else {
            throw new CError(ErrorType.CONST_FAIL,  misteryExp.getClass().toString());
        }
    }

    private static TypedObject parseMulExp(MulExp exp, SymbolTable table) {
        if (exp.getMulExp() == null) {
            return parseUnaryExp(exp.getUnaryExp(), table);
        } else {
            TypedObject operandA = parseMulExp(exp.getMulExp(), table);
            TypedObject operandB = parseUnaryExp(exp.getUnaryExp(), table);
            ReturnType finalType = ExpTypeHelper.checkBasic(operandA.type(), operandB.type());
            if (finalType instanceof ReturnType.Float) {
                float gFloatA, gFloatB;
                if (operandA.type() instanceof ReturnType.Integer) {
                    gFloatA = ((Integer)operandA.object()).floatValue();
                } else {
                    gFloatA = ((Float)operandA.object());
                }
                if (operandB.type() instanceof ReturnType.Integer) {
                    gFloatB = ((Integer)operandB.object()).floatValue();
                } else {
                    gFloatB = ((Float)operandB.object());
                }
                if (gFloatB == 0 && ("DIV /".equals(exp.getCh()) || "MOD %".equals(exp.getCh()))) {
                    throw new CError(ErrorType.DIVIDE_BY_ZERO, "Nope.");
                }
                return new TypedObject(finalType, switch (exp.getCh()) {
                    case "MULT *" -> gFloatA * gFloatB;
                    case "DIV /" -> gFloatA / gFloatB;
                    case "MOD %" -> gFloatA % gFloatB;
                    default -> throw new RuntimeException(exp.getCh());
                });
            } else if (finalType instanceof ReturnType.Integer) {
                int gIntA = ((Integer)operandA.object());
                int gIntB = ((Integer)operandB.object());
                return new TypedObject(finalType, switch (exp.getCh()) {
                    case "MULT *" -> gIntA * gIntB;
                    case "DIV /" -> gIntA / gIntB;
                    case "MOD %" -> gIntA % gIntB;
                    default -> throw new RuntimeException(exp.getCh());
                });
            }
            throw new RuntimeException("Internal Error Occured.");
        }
    }

    private static TypedObject parseAddExp(AddExp addExp, SymbolTable table) {
        if (addExp.getAddExp() == null) {
            return parseMulExp(addExp.getMulExp(), table);
        } else {
            TypedObject operandA = parseAddExp(addExp.getAddExp(), table);
            TypedObject operandB = parseMulExp(addExp.getMulExp(), table);
            ReturnType finalType = ExpTypeHelper.checkBasic(operandA.type(), operandB.type());
            if (finalType instanceof ReturnType.Float) {
                float gFloatA, gFloatB;
                if (operandA.type() instanceof ReturnType.Integer) {
                    gFloatA = ((Integer)operandA.object()).floatValue();
                } else {
                    gFloatA = ((Float)operandA.object());
                }
                if (operandB.type() instanceof ReturnType.Integer) {
                    gFloatB = ((Integer)operandB.object()).floatValue();
                } else {
                    gFloatB = ((Float)operandB.object());
                }
                return new TypedObject(finalType, switch (addExp.getCh()) {
                    case "PLUS +" ->  gFloatA + gFloatB;
                    case "MINU -" -> gFloatA - gFloatB;
                    default -> throw new RuntimeException(addExp.getCh());
                });
            } else if (finalType instanceof ReturnType.Integer) {
                int gIntA = ((Integer)operandA.object());
                int gIntB = ((Integer)operandB.object());
                return new TypedObject(finalType, switch (addExp.getCh()) {
                    case "PLUS +" ->  gIntA + gIntB;
                    case "MINU -" -> gIntA - gIntB;
                    default -> throw new RuntimeException(addExp.getCh());
                });
            }
            throw new RuntimeException("Internal Error Occured.");
        }
    }

    public static TypedObject parseConstExp(ConstExp constExp, SymbolTable table) {
        return parseAddExp(constExp.getAddExp(), table);
    }

    public static List<Object> parseConstList(ConstInitValList constList, ReturnType expectedType, SymbolTable table) {
        List<ConstInitVal> vals = constList.getConstInitVals();
        List<Object> flattenedRet = new ArrayList<>();
        for (ConstInitVal val : vals) {
            ObjectClass realVal = val.getConstForm();
            if (realVal instanceof ConstExp) {
                TypedObject expValue = parseConstExp(((ConstExp)realVal), table);
                if (!(expValue.type().equals(expectedType))) {
                    throw new CError(ErrorType.TYPE_MISMATCH, "The type of initializer does not match the type of array.");
                }
                flattenedRet.add(expValue.object());
            } else if (realVal instanceof ConstInitValList) {
                flattenedRet.addAll(parseConstList(((ConstInitValList)realVal), expectedType, table));
            } else {
                throw new RuntimeException("Why you put " + realVal.getClass() + " in constInitVal...");
            }
        }
        return flattenedRet;
    }
}
