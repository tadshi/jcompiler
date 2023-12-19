package com.cotoj.utils;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.cotoj.SymbolTable;

public interface AutoPacker {
    public static void summonPack(ReturnType type, MethodVisitor mv, MethodHelper helper, SymbolTable table) {
        switch (type) {
            case ReturnType.Integer() -> mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            default -> throw new RuntimeException("Type " + type + "cannot be packed!");
        }
    }

    public static boolean isPacked(ReturnType premitive, ReturnType packed) {
        if (packed instanceof ReturnType.JavaClass javaType) {
            if (premitive instanceof ReturnType.Integer) {
                return (JavaType.INTEGER.equals(javaType) || JavaType.OBJECT.equals(javaType));
            } else if (premitive instanceof ReturnType.Float) {
                return (JavaType.FLOAT.equals(javaType) || JavaType.OBJECT.equals(javaType));
            }
        } 
        return false;
    }
}
