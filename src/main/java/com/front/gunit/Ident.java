package com.front.gunit;
public class Ident extends ObjectClass{
    int isShared = 0;
    int isConst = 0;
    //PROC/PARA/VAR
    String kind;
    //INTTK/FLOATTK/BOOLTK/STRINGTK/LISTTK/DICTTK
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

    public void setIsConst(){this.isConst = 1;}

    public void setShared(){ this.isShared = 1;}

    public void setIdent(String name, int line){
        this.name = name;
        this.line = line;
    }

    public void setKind(String kind){this.kind = kind;}

    public void setType(String type){this.type = type;}

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

    public int getShare() {return isShared;}

    public int getIsConst() {return isConst;}
}
