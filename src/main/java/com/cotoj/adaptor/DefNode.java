package com.cotoj.adaptor;

import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;

public sealed abstract class DefNode permits VarDefNode, ArrayDefNode, FuncDefNode {
    private String name;
    private Owner owner;
    private ReturnType type;

    public DefNode (String name, Owner owner, ReturnType type) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Owner getOwner() {
        return owner;
    }

    public ReturnType getType() {
        return type;
    }

    public abstract String getDescriptor();
}
