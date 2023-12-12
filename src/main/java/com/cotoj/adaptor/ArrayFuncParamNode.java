package com.cotoj.adaptor;

import java.util.ArrayList;
import java.util.List;

import com.cotoj.utils.Owner;
import com.cotoj.utils.ReturnType;

public final class ArrayFuncParamNode extends FuncParamNode {
    private int dimLimit;
    private List<Integer> dimSizes;

    public ArrayFuncParamNode(String name, ReturnType type) {
        super(name, type);
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
    public String getDescriptor() {
        return "[".repeat(dimSizes.size()) + getType().toDescriptor();
    }

    public ArrayDefNode toArrayDef() {
        ArrayDefNode ret = new ArrayDefNode(getName(), new Owner.Local(), getType(), true);
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
