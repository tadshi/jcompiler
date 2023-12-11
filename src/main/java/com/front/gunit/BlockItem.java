package com.front.gunit;
public class BlockItem extends ObjectClass{
    private ObjectClass wrappedBlockItem;

    public void setWrappedBlockItem(ObjectClass blockItem){
        this.wrappedBlockItem = blockItem;
    }

    public ObjectClass getWrappedBlockItem() {
        return wrappedBlockItem;
    }
    
}
