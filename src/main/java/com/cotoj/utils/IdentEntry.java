package com.cotoj.utils;

import java.util.ArrayList;
import java.util.List;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.DefNode;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.VarDefNode;

public class IdentEntry {
    private final String name;
    private final ReturnType type;
    private final DefNode def;
    private final int level;
    private final boolean mut;
    private IdentEntry last;
    private final List<Integer> compileTimeValue;

    public IdentEntry(DefNode def, int level, boolean mut) {
        this.name = def.getName();
        this.type = def.getType();
        this.def = def;
        this.level = level;
        this.mut = mut;
        this.last = null;
        this.compileTimeValue = null;
    }

    public IdentEntry(VarDefNode def, int level, int compileTimeValue) {
        this.name = def.getName();
        this.type = def.getType();
        this.def = def;
        this.level = level;
        this.mut = false;
        this.last = null;
        this.compileTimeValue = new ArrayList<>();
        this.compileTimeValue.add(compileTimeValue);
    }

    public IdentEntry(ArrayDefNode def, int level, List<Integer>compileTimeValues) {
        this.name = def.getName();
        this.type = def.getType();
        this.def = def;
        this.level = level;
        this.mut = false;
        this.last = null;
        this.compileTimeValue = new ArrayList<>(compileTimeValues);
    }

    public IdentEntry(FuncDefNode def, int level) {
        this.name = def.getName();
        this.type = null;
        this.def = def;
        this.level = level;
        this.mut = false;
        this.last = null;
        this.compileTimeValue = null;
    }

    public void setLast(IdentEntry last) {
        this.last = last;
    }

    public String getName() {
        return name;
    }

    public ReturnType getType() {
        return type;
    }

    public boolean isMut() {
        return mut;
    }

    public int getLevel() {
        return level;
    }

    public IdentEntry getLast() {
        return last;
    }

    public int getCompileTimeValue() {
        if ((def instanceof VarDefNode) && (compileTimeValue != null)) {
            return compileTimeValue.get(0);
        } else {
            throw new RuntimeException("No, this is not a const.");
        }
    }

    public List<Integer> getCompileTimeValues() {
        return compileTimeValue;
    }

    public int indexCompileTimeValue(List<Integer> indexes) {
        if (!(def instanceof ArrayDefNode)) {
            throw new RuntimeException("No, this is not an array.");
        }
        ArrayDefNode arrayDef = ((ArrayDefNode)def);
        if (arrayDef.getDimSizes().size() != indexes.size()) {
            throw new RuntimeException("No, the size of our array is not what you think.");
        }
        int offset = 0;
        for (int i = 0; i < indexes.size(); ++i) {
            if (arrayDef.getDimSizes().get(i) <= indexes.get(i)) {
                throw new RuntimeException("You shall not pass the line");
            }
            offset *= arrayDef.getDimSizes().get(i);
            offset += indexes.get(i);
        }
        return compileTimeValue.get(offset);
    }

    public boolean isArray() {
        return (def instanceof ArrayDefNode);
    }

    public boolean isConst() {
        return compileTimeValue != null;
    }

    public DefNode getDef() {
        return def;
    }

}
