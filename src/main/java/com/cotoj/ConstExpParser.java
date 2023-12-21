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
import com.front.gunit.EqExp;
import com.front.gunit.Exp;
import com.front.gunit.GBool;
import com.front.gunit.GFloat;
import com.front.gunit.GNumber;
import com.front.gunit.GString;
import com.front.gunit.Ident;
import com.front.gunit.LAndExp;
import com.front.gunit.LOrExp;
import com.front.gunit.LVal;
import com.front.gunit.MulExp;
import com.front.gunit.ObjectClass;
import com.front.gunit.OpExp;
import com.front.gunit.PrimaryExp;
import com.front.gunit.RelExp;
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
                                                    .map(exp -> exp.getLOrExp())
                                                    .map(lOrExp -> parseLOrExp(lOrExp, table))
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
            case Exp e -> parseLOrExp(((Exp)misteryExp).getLOrExp(), table);
            case LVal lVal -> parseLVal((LVal)misteryExp, table);
            case GNumber number -> new TypedObject(new ReturnType.Integer(), number.getNumber());
            case GBool bool -> new TypedObject(new ReturnType.Boolean(), bool.getBool());
            case GFloat f -> new TypedObject(new ReturnType.Float(), f.getNumber());
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
            if ("!".equals(opExp.getUnaryOp().getOp())) {
                return new TypedObject(new ReturnType.Boolean(), !operand.asBool());
            }
            if (operand.type() instanceof ReturnType.Integer) {
                Integer gInteger = ((Integer)operand.object());
                return switch (opExp.getUnaryOp().getOp()) {
                    case "+" -> operand;
                    case "-" -> operand.map(-gInteger);
                    default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, opExp.getUnaryOp().getOp());
                };
            } else if (operand.type() instanceof ReturnType.Float) {
                Float gFloat = ((Float)operand.object());
                return switch (opExp.getUnaryOp().getOp()) {
                    case "+" -> operand;
                    case "-" -> operand.map(-gFloat);
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
                float gFloatA = operandA.asFloat();
                float gFloatB = operandB.asFloat();
                if (gFloatB == 0 && ("DIV /".equals(exp.getCh()) || "MOD %".equals(exp.getCh()))) {
                    throw new CError(ErrorType.DIVIDE_BY_ZERO, "Nope.");
                }
                return new TypedObject(finalType, switch (exp.getCh()) {
                    case "*" -> gFloatA * gFloatB;
                    case "/" -> gFloatA / gFloatB;
                    case "%" -> gFloatA % gFloatB;
                    default -> throw new RuntimeException(exp.getCh());
                });
            } else if (finalType instanceof ReturnType.Integer) {
                int gIntA = ((Integer)operandA.object());
                int gIntB = ((Integer)operandB.object());
                return new TypedObject(finalType, switch (exp.getCh()) {
                    case "*" -> gIntA * gIntB;
                    case "/" -> gIntA / gIntB;
                    case "%" -> gIntA % gIntB;
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
                float gFloatA = operandA.asFloat();
                float gFloatB = operandB.asFloat();
                return new TypedObject(finalType, switch (addExp.getCh()) {
                    case "+" ->  gFloatA + gFloatB;
                    case "-" -> gFloatA - gFloatB;
                    default -> throw new RuntimeException(addExp.getCh());
                });
            } else if (finalType instanceof ReturnType.Integer) {
                int gIntA = ((Integer)operandA.object());
                int gIntB = ((Integer)operandB.object());
                return new TypedObject(finalType, switch (addExp.getCh()) {
                    case "+" ->  gIntA + gIntB;
                    case "-" -> gIntA - gIntB;
                    default -> throw new RuntimeException(addExp.getCh());
                });
            }
            throw new RuntimeException("Internal Error Occured.");
        }
    }

    public static TypedObject parseRelExp(RelExp relExp, SymbolTable symbolTable) {
        if (relExp.getRelExp() == null) {
            return parseAddExp(relExp.getAddExp(), symbolTable);
        }
        TypedObject operandA = parseRelExp(relExp.getRelExp(), symbolTable);
        TypedObject operandB = parseAddExp(relExp.getAddExp(), symbolTable);
        ReturnType finalType = ExpTypeHelper.checkCompare(operandA.type(), operandB.type());
        if (finalType instanceof ReturnType.Float) {
            float gFloatA = operandA.asFloat();
            float gFloatB = operandB.asFloat();
            return new TypedObject(new ReturnType.Boolean(), switch(relExp.getCh()) {
                case ">=" -> gFloatA >= gFloatB;
                case "<=" -> gFloatA <= gFloatB;
                case ">" -> gFloatA > gFloatB;
                case "<" -> gFloatA < gFloatB;
                default -> throw new RuntimeException("Not sure how to compare as " + relExp.getCh());
            });
        } else if (finalType instanceof ReturnType.Integer) {
            Integer gIntA = ((Integer)operandA.object());
            Integer gIntB = ((Integer)operandB.object());
            return new TypedObject(new ReturnType.Boolean(), switch(relExp.getCh()) {
                case ">=" -> gIntA >= gIntB;
                case "<=" -> gIntA <= gIntB;
                case ">" -> gIntA > gIntB;
                case "<" -> gIntA < gIntB;
                default -> throw new RuntimeException("Not sure how to compare as " + relExp.getCh());
            });
        } else if (JavaType.STRING.equals(finalType)) {
            String gStringA = ((String)operandA.object());
            String gStringB = ((String)operandB.object());
            int compareResult = gStringA.compareTo(gStringB);
            return new TypedObject(new ReturnType.Boolean(), switch(relExp.getCh()) {
                case ">=" -> compareResult >= 0;
                case "<=" -> compareResult <= 0;
                case ">" -> compareResult > 0;
                case "<" -> compareResult < 0;
                default -> throw new RuntimeException("Not sure how to compare as " + relExp.getCh());
            });
        }
        throw new RuntimeException("InternalError: Unsupported Type.");
    }

    public static TypedObject parseEqExp(EqExp eqExp, SymbolTable table) {
        if (eqExp.getEqExp() == null) {
            return parseRelExp(eqExp.getRelExp(), table);
        }
        TypedObject operandA = parseEqExp(eqExp, table);
        TypedObject operandB = parseRelExp(eqExp.getRelExp(), table);
        ReturnType finalType = ExpTypeHelper.checkEqual(operandA.type(), operandB.type());
        if (finalType instanceof ReturnType.Float) {
            float gFloatA = operandA.asFloat();
            float gFloatB = operandB.asFloat();
            return new TypedObject(new ReturnType.Boolean(), switch (eqExp.getCh()) {
                case "==" -> gFloatA == gFloatB;
                case "!=" -> gFloatA != gFloatB;
                default -> throw new RuntimeException("Cannot recognize " + eqExp.getCh());
            });
        } else {
            return new TypedObject(new ReturnType.Boolean(), operandA.object().equals(operandB.object()));
        }
    }

    public static TypedObject parseLAndExp(LAndExp lAndExp, SymbolTable table) {
        if (lAndExp.getlAndExp() == null) {
            return parseEqExp(lAndExp.getEqExp(), table);
        }
        Boolean operandA = parseLAndExp(lAndExp.getlAndExp(), table).asBool();
        Boolean operandB = parseEqExp(lAndExp.getEqExp(), table).asBool();
        return new TypedObject(new ReturnType.Boolean(), operandA && operandB);
    }

    public static TypedObject parseLOrExp(LOrExp lOrExp, SymbolTable table) {
        if (lOrExp.getlOrExp() == null) {
            return parseLAndExp(lOrExp.getlAndExp(), table);
        }
        Boolean operandA = parseLOrExp(lOrExp.getlOrExp(), table).asBool();
        Boolean operandB = parseLAndExp(lOrExp.getlAndExp(), table).asBool();
        return new TypedObject(new ReturnType.Boolean(), operandA || operandB);
    }

    public static TypedObject parseConstExp(ConstExp constExp, SymbolTable table) {
        return parseLOrExp(constExp.getLOrExp(), table);
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

    // public static Map<Object, Object> parseConstDict(ConstInitValList constList, ReturnType.Dict dictType, SymbolTable table) {}
}
