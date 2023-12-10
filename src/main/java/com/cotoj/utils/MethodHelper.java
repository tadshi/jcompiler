package com.cotoj.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.MethodVisitor;

import com.cotoj.adaptor.DefNode;

public class MethodHelper {
    private int maxOperandStack;
    private int operandStack;
    private int maxLocal;
    Map<DefNode, Integer> localMap;
    Set<Integer> freedLocal;

    public MethodHelper() {
        maxOperandStack = 0;
        operandStack = 0;
        maxLocal = 1;
        localMap = new HashMap<>();
        freedLocal = new HashSet<>();
    }
    
    public void reportUseOpStack(int size) {
        operandStack += size;
        if (maxOperandStack < operandStack) {
            maxOperandStack = operandStack;
        }
    }

    public void reportPopOpStack(int size) {
        operandStack -= size;
    }    

    // Do not get confused with reportUseOpStack!
    public void reportUsedStack(int usedSize) {
        if (operandStack + usedSize > maxOperandStack) {
            maxOperandStack = operandStack + usedSize;
        }
    }

    public void registerLocal(DefNode def) {
        if (freedLocal.isEmpty()) {
            localMap.put(def, maxLocal);
            maxLocal++;
        } else {
            Integer vaccum = freedLocal.iterator().next();
            freedLocal.remove(vaccum);
            localMap.put(def, vaccum);
        }
    }

    public void releaseLocal(DefNode def) {
        Integer index = localMap.get(def);
        if (index == null) {
            throw new RuntimeException("Cannot find definition " + def.getName());
        }
        freedLocal.add(index);
    }

    public int getVarIndex(DefNode def) {
        Integer index = localMap.get(def);
        if (index == null) {
            throw new RuntimeException("Cannot find definition " + def.getName());
        }
        return index;
    }

    public int getMaxLocal() {
        return maxLocal;
    }

    public int getMaxOperandStack() {
        return maxOperandStack;
    }

    public void visitMaxs(MethodVisitor mv) {
        mv.visitMaxs(maxOperandStack, maxLocal);
    }
}
