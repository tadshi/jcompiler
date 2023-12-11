package com.cotoj.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.adaptor.ArrayDefNode;
import com.cotoj.adaptor.DefNode;
import com.cotoj.adaptor.FuncDefNode;
import com.cotoj.adaptor.FuncParamNode;
import com.cotoj.adaptor.VarDefNode;

public class MethodHelper {
    private int maxOperandStack;
    private Stack<String> operandStack;
    private int maxLocal;
    private final boolean isStatic;
    private final String thisType;
    private Map<DefNode, Integer> localMap;
    private Stack<DefNode> localStack;
    private int localShift;
    private List<String> appendLocals;
    private boolean localCorrcupted;

    // Only for constructor!
    public MethodHelper(String constructedClassTypeString) {
        isStatic = true;
        thisType = constructedClassTypeString;
        maxOperandStack = 0;
        operandStack = new Stack<>();
        maxLocal = 1;
        localMap = new HashMap<>();
        localStack = new Stack<>();
        localShift = 0;
        localCorrcupted = false;
        appendLocals = new ArrayList<>();
        localMap.put(null, 0);
        localStack.push(null);
    }

    public MethodHelper(FuncDefNode node) {
        isStatic = false;
        thisType = null;
        maxOperandStack = 0;
        operandStack = new Stack<>();
        maxLocal = 0;
        localMap = new HashMap<>();
        localStack = new Stack<>();
        localShift = 0;
        localCorrcupted = false;
        appendLocals = new ArrayList<>();
        for (FuncParamNode param : node.getParams()) {
            localMap.put(param.toDef(), localStack.size());
            localStack.push(param.toDef());
            ++maxLocal;
        }
    }
    
    public void reportUseOpStack(int size, String type) {
        for (int i = 0; i < size; ++i) {
            operandStack.push(type);
        }
        if (maxOperandStack < operandStack.size()) {
            maxOperandStack = operandStack.size();
        }
    }

    public void reportPopOpStack(int size) {
        for (int i = 0; i < size; ++i) {
            operandStack.pop();
        }
    }    

    // Do not get confused with reportUseOpStack!
    public void reportUsedStack(int usedSize) {
        if (operandStack.size() + usedSize > maxOperandStack) {
            maxOperandStack = operandStack.size() + usedSize;
        }
    }

    public void registerLocal(DefNode def) {
        localMap.put(def, localMap.size());
        localStack.push(def);
        if (localMap.size() > maxLocal) {
            maxLocal = localMap.size();
        }
        if (!localCorrcupted) {
            if (localShift < 0) {
                localCorrcupted = true;
            } else {
                localShift++;
                appendLocals.add(switch (def) {
                    case VarDefNode varDef -> varDef.getDescriptor();
                    case ArrayDefNode arrayDef -> arrayDef.getDescriptor();
                    case FuncDefNode funcDef -> throw new RuntimeException("Why you put a function in the stack?");
                });
            }
        }
    }

    public void releaseLocal(DefNode def) {
        if (def != localStack.peek()) {
            throw new RuntimeException("Cannot pop non-top local!");
        }
        if (localStack.size() == 1 && isStatic) {
            throw new RuntimeException("You cannot release `this`!");
        }
        localMap.remove(localStack.pop());
        if (localShift > 0) {
            appendLocals.removeLast();
        }
        localShift--;
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

    public void dup(MethodVisitor mv) {
        reportUseOpStack(1, operandStack.peek());
        mv.visitInsn(Opcodes.DUP);
    }

    public void visitMaxs(MethodVisitor mv) {
        mv.visitMaxs(maxOperandStack, maxLocal);
    }

    private void visitFullFrame(MethodVisitor mv) {
        Object[] localObjects = localStack.stream().map(def -> def == null ? thisType : switch(def) {
            case VarDefNode varDef -> varDef.getDescriptor();
            case ArrayDefNode arrayDef -> arrayDef.getDescriptor();
            case FuncDefNode funcDef -> throw new RuntimeException("Why there is a function in the stack?");
        }).toArray();
        mv.visitFrame(Opcodes.F_FULL, localMap.size(), localObjects, operandStack.size(), operandStack.toArray());
    }

    public void visitFrame(MethodVisitor mv) {
        if (localCorrcupted) {
            visitFullFrame(mv);
        } else if (operandStack.size() == 0) {
            if (localShift == 0) {
                mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
            } else if (localShift > 0 && localShift <= 3) {
                mv.visitFrame(Opcodes.F_APPEND, localShift, appendLocals.toArray(), 0, null);
            } else if (localShift < 0 && localShift >= -3) {
                mv.visitFrame(Opcodes.F_CHOP, -localShift, null, 0, null);
            } else {
                visitFullFrame(mv);
            }
        } else if (operandStack.size() == 1 && localShift == 0) {
            mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, operandStack.toArray());
        } else {
            visitFullFrame(mv);
        }
        localShift = 0;
        localCorrcupted = false;
        appendLocals.clear();
    }
}
