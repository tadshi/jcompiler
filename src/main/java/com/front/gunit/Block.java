package com.front.gunit;
import java.util.ArrayList;

public class Block extends ObjectClass implements StmtTrait {
    private ArrayList<BlockItem> blockItems = new ArrayList<>();

    public void addBlockItem(BlockItem blockItem){
        blockItems.add(blockItem);
    }

    public ArrayList<BlockItem> getBlockItems() {
        return blockItems;
    }
}
