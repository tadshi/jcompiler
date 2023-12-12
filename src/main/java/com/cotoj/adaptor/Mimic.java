package com.cotoj.adaptor;

import com.cotoj.utils.ReturnType;
import com.front.gunit.AddExp;
import com.front.gunit.Ident;
import com.front.gunit.LVal;
import com.front.gunit.MulExp;
import com.front.gunit.PrimaryExp;
import com.front.gunit.UnaryExp;

public interface Mimic {
    public static AddExp mimicAddExp(String identName, ReturnType type) {
        Ident ident = new Ident("VAR", type.toTypeString());
        ident.setIdent(identName, -1);
        LVal lval = new LVal();
        lval.setIdent(ident);
        PrimaryExp pExp = new PrimaryExp();
        pExp.setWrappedExp(lval);
        UnaryExp uExp = new UnaryExp();
        uExp.setWrappedExp(pExp);
        MulExp mExp = new MulExp();
        mExp.setUnaryExp(uExp);
        AddExp aExp = new AddExp();
        aExp.setMulExp(mExp);
        return aExp;
    }
}
