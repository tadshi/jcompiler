package com.front.gunit;
import java.util.ArrayList;

public class Block extends ObjectClass{
    ArrayList<BlockItem> blockItems = new ArrayList<>();

    public void addBlockItem(BlockItem blockItem){
        blockItems.add(blockItem);
    }
}
