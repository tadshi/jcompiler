package com.cotoj.utils;

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public interface AutoPacker {
    public static void summonPack(ReturnType unpackedType, MethodVisitor mv, MethodHelper helper) {
        ReturnType packedType = getPackedType(unpackedType);
        if (packedType == null) {
            return;
        }
        helper.reportPopOpStack(1);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, packedType.toTypeString(), "valueOf", 
                        "(" + unpackedType.toDescriptor() +")" + packedType.toDescriptor(), false);
        helper.reportUseOpStack(1, packedType.toTypeString());
    }

    public static void summonInlineUnpack(ReturnType unpackedType, MethodVisitor mv) {
        ReturnType packedType = getPackedType(unpackedType);
        if (packedType == null) {
            return;
        }
        String methodName;
        if (JavaType.BOOLEAN.equals(packedType)) {
            methodName = "booleanValue";
        } else if (JavaType.INTEGER.equals(packedType)) {
            methodName = "intValue";
        } else if (JavaType.FLOAT.equals(packedType)) {
            methodName = "floatValue";
        } else {
            throw new RuntimeException("This should not occur.");
        }
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, packedType.toTypeString(), methodName, "()" + unpackedType.toDescriptor(), false);
    }

    private static ReturnType getPackedType(ReturnType unpackedType) {
        return switch (unpackedType) {
            case ReturnType.Integer() -> JavaType.INTEGER;
            case ReturnType.Boolean() -> JavaType.BOOLEAN;
            case ReturnType.Float() -> JavaType.FLOAT;
            default -> null;
        };
    }

    @SuppressWarnings("unused")
    private static ReturnType getUnpackedType(ReturnType packedType) {
        if (!(packedType instanceof ReturnType.JavaClass)) {
            return null;
        }
        if (JavaType.BOOLEAN.equals(packedType)) {
            return new ReturnType.Boolean();
        } else if (JavaType.INTEGER.equals(packedType)) {
            return new ReturnType.Integer();
        } else if (JavaType.FLOAT.equals(packedType)) {
            return new ReturnType.Float();
        } else {
            return null;
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
