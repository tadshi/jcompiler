package com.cotoj;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import com.front.cerror.ErrorType;

import java.util.List;

import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.SymbolType;
import com.front.cerror.CError;
import com.front.gunit.ConstDef;
import com.front.gunit.ConstExp;
import com.front.gunit.ConstInitVal;
import com.front.gunit.ConstInitValList;
import com.front.gunit.Ident;
import com.front.gunit.ObjectClass;
import com.front.gunit.VarDef;

public class SymbolTable {
    private final Stack<IdentEntry> table;
    private final Stack<Integer> context;
    private final Map<String, IdentEntry> ident_table;

    public SymbolTable() {
        table = new Stack<>();
        context = new Stack<>();
        ident_table = new HashMap<>();
    }

    private void popTable() {
        IdentEntry entry = table.peek();
        if (entry.getLast() != null) {
            ident_table.put(entry.getName(), entry.getLast());
        } else {
            ident_table.remove(entry.getName());
        }
        table.pop();
    }

    public void addEntry(IdentEntry entry) {
        table.push(entry);
        IdentEntry lastRecord = ident_table.get(entry.getName());
        entry.setLast(lastRecord);
        ident_table.put(entry.getName(), entry);
    }

    public IdentEntry getEntry(String name, SymbolType symbolType) {
        IdentEntry entry = ident_table.get(name);
        while (entry != null) {
            if (entry.getType() == symbolType) {
                return entry;
            }
            entry = entry.getLast();
        }
        return null;
    }

    public void pushContext() {
        context.push(context.size());
    }

    public void popContext() {
        while (table.size() > context.peek()) {
            popTable();
        }
        context.pop();
    }

    public int getLevel() {
        return context.size();
    }

    public IdentEntry addVarDef(VarDef varDef) {
        Ident ident = varDef.getIdent();
        IdentEntry entry = new IdentEntry(ident.getName(), SymbolType.fromString(ident.getKind()), getLevel());
        if (ident.getDimension() > 0) {
            var dimList = varDef.getConstExpList();
            if (dimList.size() != ident.getDimension()) {
                throw new RuntimeException("No, so what it its dim indeed?!?!");
            }
            for (ConstExp exp : dimList) {
                entry.addDimension(ConstExpParser.parseConstExp(exp, this));
            }
        }
        addEntry(entry);
        return entry;
    }

    public IdentEntry addConstDef(ConstDef constDef) {
        Ident ident = constDef.getIdent();
        ObjectClass initVal = constDef.getConstInitVal().getConstForm();
        IdentEntry entry;

        if (ident.getDimension() > 0) {
            if (!(initVal instanceof ConstInitValList)) {
                throw new RuntimeException("Why you init a const array without a List?");
            }
            List<Integer> initVals = ConstExpParser.parseConstList((ConstInitValList)initVal, this);
            entry = new IdentEntry(ident.getName(), SymbolType.fromString(ident.getKind()), getLevel(), initVals);
            var dimList = constDef.getConstExps();
            if (dimList.size() != ident.getDimension()) {
                throw new RuntimeException("No, so what it its dim indeed?!?!");
            }
            for (ConstExp exp : dimList) {
                entry.addDimension(ConstExpParser.parseConstExp(exp, this));
            }
            if (entry.getDimensions().stream().reduce((a, b) -> a * b).get() != initVals.size()) {
                throw new CError(ErrorType.INVALID_CONST_LIST, "The const init list size is only " + initVals.size() + ".");
            }
        } else {
            if (!(initVal instanceof ConstExp)) {
                throw new RuntimeException("Why you init a const without a constExp?");
            }
            entry = new IdentEntry(ident.getName(), SymbolType.fromString(ident.getKind()), getLevel(), ConstExpParser.parseConstExp((ConstExp)initVal, this));
        }
        addEntry(entry);
        return entry;
    }
}
