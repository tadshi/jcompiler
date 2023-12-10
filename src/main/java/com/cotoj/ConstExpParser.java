package com.cotoj;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.SymbolType;
import com.front.cerror.CError;
import com.front.cerror.ErrorType;
import com.front.gunit.AddExp;
import com.front.gunit.ConstExp;
import com.front.gunit.ConstInitVal;
import com.front.gunit.ConstInitValList;
import com.front.gunit.Exp;
import com.front.gunit.GNumber;
import com.front.gunit.Ident;
import com.front.gunit.LVal;
import com.front.gunit.MulExp;
import com.front.gunit.ObjectClass;
import com.front.gunit.OpExp;
import com.front.gunit.PrimaryExp;
import com.front.gunit.UnaryExp;

public interface ConstExpParser {

    private static int parseLVal(LVal lVal, SymbolTable table) {
        Ident ident = lVal.getIdent();
        IdentEntry entry = table.getEntry(ident.getName(), SymbolType.fromString(ident.getKind()));
        if (ident.getDimension() == 0) {
            return entry.getCompileTimeValue();
        } else {
            if (ident.getDimension() != lVal.getExps().size()) {
                throw new CError(ErrorType.EXPECT_TOKEN, "[");
            }
            List<Integer> const_index = lVal.getExps().stream()
                                                    .map(exp -> exp.getAddExp())
                                                    .map(addExp -> parseAddExp(addExp, table))
                                                    .collect(Collectors.toList());
            return entry.indexCompileTimeValue(const_index);
        }
    }

    // <PrimaryExp> ::= (<Exp>) | <LVal> | <Number>
    private static int parsePrimaryExp(PrimaryExp exp, SymbolTable table) {
        ObjectClass misteryExp = exp.getWrappedExp();
        if (misteryExp instanceof Exp) {
            return parseAddExp(((Exp)misteryExp).getAddExp(), table);
        } else if (misteryExp instanceof LVal) {
            return parseLVal((LVal)misteryExp, table);
        } else if (misteryExp instanceof GNumber) {
            return  ((GNumber)misteryExp).getNumber();
        } else {
            throw new RuntimeException(misteryExp.getClass() + " should not found in PrimaryExp");
        }
    }

    // <UnaryExp> ::= <PrimaryExp> | <Ident> ([<FuncRParams]) | <UnaryOp> <UnaryExp>
    private static int parseUnaryExp(UnaryExp exp, SymbolTable table) {
        ObjectClass misteryExp = exp.getWrappedExp();
        if (misteryExp instanceof OpExp) {
            OpExp opExp = ((OpExp)misteryExp);
            switch (opExp.getUnaryOp().getOp()) {
                case "PLUS +" -> {return parseUnaryExp(opExp.getUnaryExp(), table);}
                case "MINU -" -> {return -parseUnaryExp(opExp.getUnaryExp(), table);}
                default -> throw new CError(ErrorType.UNEXPECTED_TOKEN, opExp.getUnaryOp().getOp());
            }
        } else if (misteryExp instanceof PrimaryExp) {
            PrimaryExp primaryExp = ((PrimaryExp)misteryExp);
            return parsePrimaryExp(primaryExp, table);
        } else {
            throw new CError(ErrorType.CONST_FAIL,  misteryExp.getClass().toString());
        }
    }

    private static int parseMulExp(MulExp exp, SymbolTable table) {
        if (exp.getMulExp() == null) {
            return parseUnaryExp(exp.getUnaryExp(), table);
        } else return switch (exp.getCh()) {
            case "MULT *" -> parseMulExp(exp.getMulExp(), table) * parseUnaryExp(exp.getUnaryExp(), table);
            case "DIV /" -> parseMulExp(exp.getMulExp(), table) / parseUnaryExp(exp.getUnaryExp(), table);
            case "MOD %" -> parseMulExp(exp.getMulExp(), table) % parseUnaryExp(exp.getUnaryExp(), table);
            default -> throw new RuntimeException(exp.getCh());
        };
    }

    private static int parseAddExp(AddExp addExp, SymbolTable table) {
        if (addExp.getAddExp() == null) {
            return parseMulExp(addExp.getMulExp(), table);
        } else return switch (addExp.getCh()) {
            case "PLUS +" -> parseAddExp(addExp.getAddExp(), table) + parseMulExp(addExp.getMulExp(), table);
            case "MINU -" -> parseAddExp(addExp.getAddExp(), table) - parseMulExp(addExp.getMulExp(), table);
            default -> throw new RuntimeException(addExp.getCh());
        };
    }

    public static int parseConstExp(ConstExp constExp, SymbolTable table) {
        return parseAddExp(constExp.getAddExp(), table);
    }

    public static List<Integer> parseConstList(ConstInitValList constList, SymbolTable table) {
        List<ConstInitVal> vals = constList.getConstInitVals();
        List<Integer> flattenedRet = new ArrayList<>();
        for (ConstInitVal val : vals) {
            ObjectClass realVal = val.getConstForm();
            if (realVal instanceof ConstExp) {
                flattenedRet.add(parseConstExp(((ConstExp)realVal), table));
            } else if (realVal instanceof ConstInitValList) {
                flattenedRet.addAll(parseConstList(((ConstInitValList)realVal), table));
            } else {
                throw new RuntimeException("Why you put " + realVal.getClass() + " in constInitVal...");
            }
        }
        return flattenedRet;
    }
}
