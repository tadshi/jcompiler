package com.cotoj.adaptor;

import com.cotoj.utils.Owner;

public sealed abstract class DefNode permits VarDefNode, ArrayDefNode, FuncDefNode {
    private String name;
    private Owner owner;

    public DefNode (String name, Owner owner) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Owner getOwner() {
        return owner;
    }

    public abstract String getDescriptor();
}
