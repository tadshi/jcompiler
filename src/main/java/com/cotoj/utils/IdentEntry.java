package com.cotoj.utils;

import java.util.ArrayList;
import java.util.List;

public class IdentEntry {
    private final String name;
    private final int level;
    private final Owner owner; 
    private final SymbolType type;
    private final List<Integer> dimensions;
    private IdentEntry last;
    private final List<Integer> compileTimeValue;
    private final String linkPath;

    public IdentEntry(String name, SymbolType type, int level, Owner owner) {
        this.name = name;
        this.type = type;
        this.owner = owner;
        this.level = level;
        this.last = null;
        this.dimensions = new ArrayList<>();
        this.compileTimeValue = null;
        this.linkPath = null;
    }

    public IdentEntry(String name, SymbolType type, int level, int compileTimeValue) {
        this.name = name;
        this.type = type;
        this.level = level;
        this.last = null;
        this.owner = Owner.COMPILER;
        this.dimensions = new ArrayList<>();
        this.compileTimeValue = new ArrayList<>();
        this.compileTimeValue.add(compileTimeValue);
        this.linkPath = null;
    }

    public IdentEntry(String name, SymbolType type, int level, List<Integer>compileTimeValues) {
        this.name = name;
        this.type = type;
        this.level = level;
        this.last = null;
        this.owner = Owner.COMPILER;
        this.dimensions = new ArrayList<>();
        this.compileTimeValue = new ArrayList<>(compileTimeValues);
        this.linkPath = null;
    }

    public IdentEntry(String name, SymbolType type, String exString) {
        this.name = name;
        this.type = type;
        this.level = 0;
        this.last = null;
        this.owner = Owner.EXLIB;
        this.dimensions = new ArrayList<>();
        this.compileTimeValue = null;
        this.linkPath = exString;
    }

    public void addDimension(int dimension) {
        this.dimensions.add(dimension);
    }

    public void setLast(IdentEntry last) {
        this.last = last;
    }

    public String getName() {
        return name;
    }

    public int getLevel() {
        return level;
    }

    public SymbolType getType() {
        return type;
    }

    public IdentEntry getLast() {
        return last;
    }

    public int getCompileTimeValue() {
        if (dimensions.isEmpty()) {
            return compileTimeValue.get(0);
        } else {
            throw new RuntimeException("No, this is not an array.");
        }
    }

    public int indexCompileTimeValue(List<Integer> indexes) {
        if (dimensions.size() != indexes.size()) {
            throw new RuntimeException("No, the size of our array is not what you think.");
        }
        int offset = 0;
        for (int i = 0; i < dimensions.size(); ++i) {
            if (dimensions.get(i) <= indexes.get(i)) {
                throw new RuntimeException("You shall not pass the line");
            }
            offset *= dimensions.get(i);
            offset += indexes.get(i);
        }
        return compileTimeValue.get(offset);
    }

    public List<Integer> getDimensions() {
        return dimensions;
    }

    public boolean isArray() {
        return dimensions.isEmpty();
    }

    public boolean isConst() {
        return compileTimeValue != null;
    }

}
