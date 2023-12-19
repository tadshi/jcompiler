package com.cotoj.adaptor;

import com.cotoj.utils.ReturnType;
import com.front.gunit.AddExp;
import com.front.gunit.Exp;
import com.front.gunit.FuncDef;
import com.front.gunit.FuncFParam;
import com.front.gunit.FuncFParams;
import com.front.gunit.FuncType;
import com.front.gunit.Ident;
import com.front.gunit.LVal;
import com.front.gunit.StringLiteral;
import com.front.gunit.MainFuncDef;
import com.front.gunit.MulExp;
import com.front.gunit.ObjectClass;
import com.front.gunit.PrimaryExp;
import com.front.gunit.UnaryExp;
import com.front.gunit.FuncFParam.FuncParamType;

public interface Mimic {
    private static AddExp mimicFromPrimaryExp(ObjectClass exp) {
        PrimaryExp pExp = new PrimaryExp();
        pExp.setWrappedExp(exp);
        UnaryExp uExp = new UnaryExp();
        uExp.setWrappedExp(pExp);
        MulExp mExp = new MulExp();
        mExp.setUnaryExp(uExp);
        AddExp aExp = new AddExp();
        aExp.setMulExp(mExp);
        return aExp;
        
    }

    public static AddExp mimicAddExp(String identName, ReturnType type) {
        Ident ident = new Ident("VAR", type.toTypeString());
        ident.setIdent(identName, -1);
        LVal lval = new LVal();
        lval.setIdent(ident);
        return mimicFromPrimaryExp(lval);
    }

    public static AddExp mimicAddExp(String literalString) {
        StringLiteral literal = new StringLiteral(literalString);
        return mimicFromPrimaryExp(literal);
    }

    public static AddExp mimicAddExp(StaticAccessExp sa) {
        return mimicFromPrimaryExp(sa);
    }

    public static Exp wrap(AddExp addExp) {
        Exp ret = new Exp();
        ret.setAddExp(addExp);
        return ret;
    }

    public static FuncDef mimicFuncDef(MainFuncDef mainFuncDef) {
        FuncDef ret = new FuncDef();
        ret.setBlock(mainFuncDef.getBlock());
        FuncFParams params = new FuncFParams();
        FuncFParam param = new FuncFParam();
        Ident args = new Ident("VAR", "STRINGTK");
        args.setIdent("args", -1);
        param.setIdent(args);
        param.setType(FuncParamType.ARRAY1D);
        params.addFuncFParam(param);
        ret.setFuncFParams(params);
        FuncType intType = new FuncType();
        intType.setName("INTTK");
        ret.setFuncType(intType);
        Ident funcIdent = new Ident();
        funcIdent.setIdent("__main", -1);
        ret.setIdent(funcIdent);
        return ret;
    }
}
