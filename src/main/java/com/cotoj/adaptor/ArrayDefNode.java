package com.cotoj.adaptor;

import java.util.ArrayList;
import java.util.List;

import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;

public final class ArrayDefNode extends DefNode {
    private boolean mut;
    private List<Integer> dimSizes;
    
    public ArrayDefNode(String name, Owner owner, ReturnType type, boolean mut) {
        super(name, owner, type);
        this.mut = mut;
        this.dimSizes = new ArrayList<>();
    }

    public void addDimension(int dimSize) {
        this.dimSizes.add(dimSize);
    }

    public boolean isMut() {
        return mut;
    }

    public List<Integer> getDimSizes() {
        return dimSizes;
    }

    public String getContentTypeString() {
        return getType().toTypeString();
    }

    public String getIndexedTypeString(int level) {
        return "[".repeat(dimSizes.size() - level) + getContentTypeString();
    }

    public String getTypeString() {
        return "[".repeat(dimSizes.size()) + getContentTypeString();
    }

    @Override
    public String getDescriptor() {
        return "[".repeat(dimSizes.size()) + getContentTypeString();
    }
}
