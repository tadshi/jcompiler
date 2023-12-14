package com.cotoj.adaptor;

import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;

public final class VarDefNode extends DefNode {
    private boolean mut;

    public VarDefNode(String name, Owner owner, ReturnType type, boolean mut) {
        super(name, owner, type);
        this.mut = mut;
    }

    public boolean isMut() {
        return mut;
    }

    public String getTypeString() {
        return getType().toTypeString();
    }

    @Override
    public String getDescriptor() {
        return getType().toDescriptor();
    }
}
