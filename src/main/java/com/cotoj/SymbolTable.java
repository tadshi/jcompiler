package com.cotoj;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

import org.objectweb.asm.Label;

import com.front.cerror.ErrorType;

import java.util.List;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.ArrayFuncParamNode;
import com.cotoj.adaptor.DefNode;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.FuncParamNode;
import com.cotoj.adaptor.SimpleFuncParamNode;
import com.cotoj.adaptor.VarDefNode;
import com.cotoj.utils.IdentEntry;
import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;
import com.cotoj.utils.SymbolType;
import com.front.cerror.CError;
import com.front.gunit.ConstDef;
import com.front.gunit.ConstExp;
import com.front.gunit.ConstInitValList;
import com.front.gunit.FuncDef;
import com.front.gunit.FuncFParam;
import com.front.gunit.Ident;
import com.front.gunit.ObjectClass;
import com.front.gunit.VarDef;

public class SymbolTable {
    private final Stack<IdentEntry> table;
    private final Stack<Integer> context;
    private final Map<String, IdentEntry> ident_table;
    
    private record LabelPair(Label label, int level) {};
    LabelPair breakLabelPair;
    Stack<LabelPair> contLabelPairs;

    public SymbolTable() {
        table = new Stack<>();
        context = new Stack<>();
        ident_table = new HashMap<>();
        breakLabelPair = null;
        contLabelPairs = new Stack<>();
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
            if (SymbolType.fromDef(entry.getDef()).equals(symbolType)) {
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
        DefNode def;
        if (ident.getDimension() > 0) {
            // This Logic is not true but we cannot get more info from AST.
            ArrayDefNode arrayDef = new ArrayDefNode(ident.getName(), getLevel() == 0 ? new Owner.Static() : new Owner.Local(), true);
            var dimList = varDef.getConstExpList();
            if (dimList.size() != ident.getDimension()) {
                throw new RuntimeException("No, so what it its dim indeed?!?!");
            }
            for (ConstExp exp : dimList) {
                arrayDef.addDimension(ConstExpParser.parseConstExp(exp, this));
            }
            def = arrayDef;
        } else {
            def = new VarDefNode(ident.getName(), getLevel() == 0 ?  new Owner.Static() : new Owner.Local(), true);
        }
        IdentEntry entry = new IdentEntry(def, getLevel());
        addEntry(entry);
        return entry;
    }

    public IdentEntry addConstDef(ConstDef constDef) {
        Ident ident = constDef.getIdent();
        ObjectClass initVal = constDef.getConstInitVal().getConstForm();
        IdentEntry entry;

        // Note that every constant in our lang can be inferred at compile-time
        // And in this way every constant is a non-mut variable.
        // However, there do exists a non-mut variable which is not a constant,
        // Even though we have not supported it yet.
        // Be careful to not be confused by the difference between frontend and backend.
        if (ident.getDimension() > 0) {
            if (!(initVal instanceof ConstInitValList)) {
                throw new RuntimeException("Why you init a const array without a List?");
            }
            ArrayDefNode arrayDef = new ArrayDefNode(ident.getName(), getLevel() == 0 ?  new Owner.Static() : new Owner.Local(), false);
            var dimList = constDef.getConstExps();
            if (dimList.size() != ident.getDimension()) {
                throw new RuntimeException("No, so what it its dim indeed?!?!");
            }
            for (ConstExp exp : dimList) {
                arrayDef.addDimension(ConstExpParser.parseConstExp(exp, this));
            }
            List<Integer> initVals = ConstExpParser.parseConstList((ConstInitValList)initVal, this);
            if (arrayDef.getDimSizes().stream().reduce((a, b) -> a * b).get() != initVals.size()) {
                throw new CError(ErrorType.INVALID_CONST_LIST, "The const init list size is only " + initVals.size() + ".");
            }
            entry = new IdentEntry(arrayDef, getLevel(), initVals);
        } else {
            if (!(initVal instanceof ConstExp)) {
                throw new RuntimeException("Why you init a const without a constExp?");
            }
            VarDefNode varDef = new VarDefNode(ident.getName(), getLevel() == 0 ?  new Owner.Static() : new Owner.Local(), false);
            entry = new IdentEntry(varDef, getLevel(), ConstExpParser.parseConstExp((ConstExp)initVal, this));
        }
        addEntry(entry);
        return entry;
    }

    public IdentEntry addFuncDef(FuncDef funcDef) {
        Ident ident = funcDef.getIdent();
        FuncDefNode funcDefNode = new FuncDefNode(ident.getName(), new Owner.Main(), ReturnType.fromFuncType(funcDef.getFuncType()));
        for (FuncFParam funcFParam : funcDef.getFuncFParams().getFuncFParams()) {
            FuncParamNode paramNode = switch (funcFParam.getType()) {
                case NONARRAY -> new SimpleFuncParamNode(funcFParam.getIdent().getName());
                case ARRAY1D -> new ArrayFuncParamNode(funcFParam.getIdent().getName());
                case ARRAYMULTID -> {
                    var marrParam = new ArrayFuncParamNode(funcFParam.getIdent().getName());
                    for (ConstExp constExp : funcFParam.getConstExp()) {
                        marrParam.addDim(ConstExpParser.parseConstExp(constExp, this));
                    }
                    yield marrParam;
                }
            };
            funcDefNode.addParam(paramNode);
        }
        IdentEntry entry = new IdentEntry(funcDefNode, getLevel());
        return entry;
    }

    public void loadFunctionParams(String funcName) {
        IdentEntry entry = getEntry(funcName, SymbolType.FUNCTION);
        if (!(entry.getDef() instanceof FuncDefNode)) {
            throw new RuntimeException("No, this is not a function.");
        }
        FuncDefNode funcDef = ((FuncDefNode)entry.getDef());
        for (FuncParamNode param: funcDef.getParams()) {
            addEntry(new IdentEntry(param.toDef(), getLevel()));
        }
    }

    public void registerLoop() {
        // TODO
    }
}
