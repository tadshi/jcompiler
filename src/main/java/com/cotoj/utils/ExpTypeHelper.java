package com.cotoj.utils;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.front.cerror.CError;
import com.front.cerror.ErrorType;

public interface ExpTypeHelper {
    public static ReturnType checkBasic(ReturnType typeA, ReturnType typeB) {
        if ((typeA instanceof ReturnType.Float) || (typeB instanceof ReturnType.Float)) {
            if ((typeA instanceof ReturnType.Integer) || (typeB instanceof ReturnType.Integer)) {
                return new ReturnType.Float();
            }
            if ((typeA instanceof ReturnType.Float) && (typeB instanceof ReturnType.Float)) {
                return new ReturnType.Float();
            }
        } else if ((typeA instanceof ReturnType.Integer) && (typeB instanceof ReturnType.Integer)) {
            return new ReturnType.Integer();
        }
        throw new CError(ErrorType.TYPE_MISMATCH, "Cannot calculate between " + typeA + " and " + typeB + ".");
    }

    public static ReturnType checkCompare(ReturnType typeA, ReturnType typeB) {
        try {
            return checkBasic(typeA, typeB);
        } catch (CError err) {};
        if (JavaType.STRING.equals(typeA) && JavaType.STRING.equals(typeB)) {
            return JavaType.STRING;
        }
        throw new CError(ErrorType.TYPE_MISMATCH, "Cannot compare between " + typeA + " and " + typeB + ".");
    }

    public static ReturnType checkEqual(ReturnType typeA, ReturnType typeB) {
        try {
            return checkBasic(typeA, typeB);
        } catch (CError err) {};
        if (typeA instanceof ReturnType.Boolean && typeB instanceof ReturnType.Boolean) {
            return new ReturnType.Boolean();
        }
        if (JavaType.STRING.equals(typeA) && JavaType.STRING.equals(typeB)) {
            return JavaType.STRING;
        }
        if (typeA.equals(typeB)) {
            return JavaType.OBJECT;
        }
        throw new CError(ErrorType.TYPE_MISMATCH, "Cannot check equality between " + typeA + " and " + typeB + ".");
    }

    public static void checkMatch(ReturnType expected, ReturnType result) {
        if (!(expected.equals(result))) {
            throw new CError(ErrorType.TYPE_MISMATCH, "Get " + result + " , expected " + expected + ".");
        }
    }

    public static void checkInt(ReturnType result) {
        if (!(result instanceof ReturnType.Integer)) {
            throw new CError(ErrorType.TYPE_MISMATCH, "Need a int here, got " + result + ".");
        }
    }

    public static void intToFloat(ReturnType secondType, ReturnType firstType, MethodVisitor mv, MethodHelper helper) {
        if (secondType instanceof ReturnType.Float && firstType instanceof ReturnType.Float) {
            return;
        }
        if (firstType instanceof ReturnType.Integer) {
            mv.visitInsn(Opcodes.I2F);
        }
        if (secondType instanceof ReturnType.Integer) {
            mv.visitInsn(Opcodes.DUP_X1);
            mv.visitInsn(Opcodes.POP);
            mv.visitInsn(Opcodes.I2F);
            mv.visitInsn(Opcodes.DUP_X1);
            mv.visitInsn(Opcodes.POP);
            helper.reportUsedStack(1);
        }
        helper.reportPopOpStack(2);
        helper.reportUseOpStack(2, "F");
    }

    public static void anyToBool(ReturnType baseType, MethodVisitor mv, MethodHelper helper) {
        if (baseType instanceof ReturnType.Boolean) {
            return;
        } else if (baseType instanceof ReturnType.Integer) {
            Label falseLabel = new Label();
            Label endLabel = new Label();
            mv.visitJumpInsn(Opcodes.IFEQ, falseLabel);
            helper.reportPopOpStack(1);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitJumpInsn(Opcodes.GOTO, endLabel);
            mv.visitLabel(falseLabel);
            helper.visitFrame(mv);
            mv.visitInsn(Opcodes.ICONST_0);
            helper.reportUseOpStack(1, "I");
            mv.visitLabel(endLabel);
            helper.visitFrame(mv);
            return;
        } else if (baseType instanceof ReturnType.Float) {
            mv.visitInsn(Opcodes.FCMPG);
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.IAND);
        } else {
            if (JavaType.STRING.equals(baseType)) {
                mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, JavaType.STRING.toTypeString(), "isEmpty", "()Z", false);
            } else if (baseType instanceof ReturnType.List) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, JavaType.LIST_INT.toTypeString(), "isEmpty", "()Z", true);
            } else if (baseType instanceof ReturnType.Dict) {
                mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, JavaType.DICT_INT.toTypeString(), "isEmpty", "()Z", true);
            } else {
                throw new CError(ErrorType.TYPE_MISMATCH, baseType + " cannot be converted into bool implicitly.");
            }
            mv.visitInsn(Opcodes.ICONST_1);
            mv.visitInsn(Opcodes.IXOR);
        }
        helper.reportUsedStack(1);
        helper.reportPopOpStack(1);
        helper.reportUseOpStack(1, "I");
    }

    public static void implicitCast(ReturnType left, ReturnType right, MethodVisitor mv, MethodHelper helper) {
        if (left.equals(right)) {
            return;
        }
        if (left instanceof ReturnType.Float && right instanceof ReturnType.Integer) {
            mv.visitInsn(Opcodes.I2F);
            return;
        }
        if (left instanceof ReturnType.Integer && right instanceof ReturnType.Float) {
            mv.visitInsn(Opcodes.F2I);
            return;
        }
        throw new CError(ErrorType.TYPE_MISMATCH, "Cannot cast from " + right + " to " + left + ".");
    }
}
