package com.front.gunit;
public class Ident extends ObjectClass{
    boolean isShared = false;
    boolean isConst = false;
    //PROC/PARA/VAR
    String kind;
    //INTTK/FLOATTK/BOOLTK/STRINGTK/LISTTK/DICTTK/LOCKTK/SEMAPHORETK
    String type;

    //存着对象
    ObjectClass dataType;

    String name;
    int line;
    int dimension;

    public Ident(){

    }

    public Ident(String kind, String type){
        this.kind = kind;
        this.type = type;
    }

    public void setIsConst(){this.isConst = true;}

    public void setShared(){ this.isShared = true;}

    public void setIdent(String name, int line){
        this.name = name;
        this.line = line;
    }

    public void setDataType(ObjectClass dataType){this.dataType = dataType;}

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

    public boolean isShared() {return isShared;}

    public boolean isConst() {return isConst;}

    public ObjectClass getDataType() {return dataType;}
}
