package com.cotoj.adaptor;

import java.util.ArrayList;
import java.util.List;

import com.cotoj.utils.Owner;

public final class ArrayFuncParamNode extends FuncParamNode {
    private int dimLimit;
    private List<Integer> dimSizes;

    public ArrayFuncParamNode(String name) {
        super(name);
        this.dimLimit = 1;
        this.dimSizes = new ArrayList<>();
    }

    public void addDim(int size) {
        dimSizes.add(size);
        dimLimit++;
    }

    public int getDimLimit() {
        return dimLimit;
    }

    @Override
    public String getTypeString() {
        return "[".repeat(dimSizes.size()) + "I";
    }

    public ArrayDefNode toArrayDef() {
        ArrayDefNode ret = new ArrayDefNode(getName(), new Owner.Local(), true);
        ret.addDimension(0);
        for (int dim : dimSizes) {
            ret.addDimension(dim);
        }
        return ret;
    }

    @Override
    public DefNode toDef() {
        return toArrayDef();
    }
}
