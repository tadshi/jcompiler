package com.front.gunit;
public class Ident extends ObjectClass{
    String kind;
    String type;
    String name;
    int line;
    int dimension;

    public Ident(){

    }

    public Ident(String kind, String type){
        this.kind = kind;
        this.type = type;
    }

    public void setIdent(String name, int line){
        this.name = name;
        this.line = line;
    }

    public void setDimension(int dimension){
        this.dimension = dimension;
    }

    public int getDimension() {
        return dimension;
    }

    public String getKind() {
        return kind;
    }
    
    public int getLine() {
        return line;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }
}
