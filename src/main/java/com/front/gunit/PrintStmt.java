package com.front.gunit;
import java.util.ArrayList;

public class PrintStmt extends Stmt{
    String formaString;
    ArrayList<Exp> exps = new ArrayList<>();
    public void setFormatString(String formString){
        this.formaString = formString;
    }
    public void addExp(Exp exp){
        exps.add(exp);
    }
}
