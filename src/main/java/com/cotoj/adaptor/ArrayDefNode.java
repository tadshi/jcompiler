package com.cotoj.adaptor;

import java.util.ArrayList;
import java.util.List;

import com.cotoj.utils.Owner;

public final class ArrayDefNode extends DefNode {
    private boolean mut;
    private List<Integer> dimSizes;
    
    public ArrayDefNode(String name, Owner owner, boolean mut) {
        super(name, owner);
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

    public String getIndexedTypeString(int level) {
        return "[".repeat(dimSizes.size() - level) + "I";
    }

    public String getTypeString() {
        return "[".repeat(dimSizes.size()) + "I";
    }

    @Override
    public String getDescriptor() {
        return "[".repeat(dimSizes.size()) + "I";
    }
}
