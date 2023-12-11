package com.cotoj.adaptor;

import com.cotoj.utils.Owner;

public final class VarDefNode extends DefNode {
    private boolean mut;

    public VarDefNode(String name, Owner owner, boolean mut) {
        super(name, owner);
        this.mut = mut;
    }

    public boolean isMut() {
        return mut;
    }

    public String getTypeString() {
        return "I";
    }

    @Override
    public String getDescriptor() {
        return "I";
    }
}
