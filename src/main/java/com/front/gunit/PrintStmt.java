package com.front.gunit;
import java.util.ArrayList;

public class PrintStmt extends ObjectClass implements StmtTrait {
    private String formaString;
    private ArrayList<Exp> exps = new ArrayList<>();
    public void setFormatString(String formString){
        this.formaString = formString;
    }
    public void addExp(Exp exp){
        exps.add(exp);
    }

    public ArrayList<Exp> getExps() {
        return exps;
    }

    public String getFormaString() {
        return formaString;
    }
}
