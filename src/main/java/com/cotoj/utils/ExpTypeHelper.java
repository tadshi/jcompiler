package com.cotoj.utils;

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

    public static void checkMatch(ReturnType expected, ReturnType result) {
        if (!(expected.equals(result))) {
            throw new CError(ErrorType.TYPE_MISMATCH, "Get " + result + " , expected " + expected + ".");
        }
    }
}
