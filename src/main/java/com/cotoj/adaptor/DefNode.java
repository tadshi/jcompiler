package com.cotoj.adaptor;

import com.cotoj.utils.Owner;

public abstract class DefNode {
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
}
