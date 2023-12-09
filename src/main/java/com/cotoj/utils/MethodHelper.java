package com.cotoj.utils;

import java.util.Stack;

import org.objectweb.asm.MethodVisitor;

public class MethodHelper {
    private int maxOperandStack;
    private int operandStack;
    private int maxLocal;
    private int local;
    Stack<Integer> stackRecord;

    public MethodHelper() {
        maxOperandStack = 1;
        operandStack = 1;
        maxLocal = 0;
        local = 0;
    }
    
    public void reportUseOpStack(int size) {
        operandStack += size;
        stackRecord.push(size);
        if (maxOperandStack < operandStack) {
            maxOperandStack = operandStack;
        }
    }

    public void popUse() {
        operandStack -= stackRecord.pop();
    }

    public void reportUsedOpStack(int size) {
        if (operandStack + size > maxOperandStack) {
            maxOperandStack = operandStack + size;
        }
    }

    public void reportUseLocal(int size) {
        local += size;
        if (local > maxLocal) {
            maxLocal = local;
        }
    }

    public void reportReleaseLocal(int size) {
        local -= size;
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
