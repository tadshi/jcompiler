package com.cotoj.adaptor;

import java.util.Objects;

import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;

public sealed abstract class DefNode permits VarDefNode, ArrayDefNode, FuncDefNode {
    private final String name;
    private Owner owner;
    private final ReturnType type;

    public DefNode (String name, Owner owner, ReturnType type) {
        this.name = name;
        this.owner = owner;
        this.type = type;
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

    public void setOwner(Owner owner) {
        this.owner = owner;
    }

    public abstract String getDescriptor();

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefNode def) {
            return this.name.equals(def.name) &&
                    this.owner.equals(def.owner) &&
                    this.type.equals(def.type);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, owner, type);
    }
}
