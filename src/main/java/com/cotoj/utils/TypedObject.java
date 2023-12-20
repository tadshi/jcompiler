package com.cotoj.utils;

import java.util.List;
import java.util.Map;

import com.front.cerror.CError;
import com.front.cerror.ErrorType;

public record TypedObject (ReturnType type, Object object) {
    public TypedObject map(Object newObject) {
        return new TypedObject(type, newObject);
    }

    public Integer asInteger() {
        if (type() instanceof ReturnType.Integer) {
            return ((Integer)object());
        } else if (type() instanceof ReturnType.Float) {
            throw new RuntimeException("Cannot down-cast implicitly.");
        } else {
            throw new RuntimeException("This is not a integer.");
        }
    }

    public Float asFloat() {
        if (type() instanceof ReturnType.Integer) {
            return ((Integer)object()).floatValue();
        } else if (type() instanceof ReturnType.Float) {
            return ((Float)object());
        } else {
            throw new RuntimeException("This is not a float.");
        }
    }

    // Caller must check the type.
    @SuppressWarnings("unchecked")
    public <T> List<T> asUncheckList() {
        return ((List<T>)object);
    }

    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> asUncheckDict() {
        return ((Map<K, V>)object);
    }

    public Boolean asBool() {
        if (type() instanceof ReturnType.Integer) {
            return ((Integer)object()) != 0;
        } else if (type() instanceof ReturnType.Float) {
            return ((Float)object()) != 0;
        } else if (type() instanceof ReturnType.Boolean) {
            return ((Boolean)object());
        } else if (JavaType.STRING.equals(type())) {
            return !((String)object()).isEmpty();
        } else if (type() instanceof ReturnType.List list) {
            return !asUncheckList().isEmpty();
        } else if (type() instanceof ReturnType.Dict dict) {
            return !asUncheckDict().isEmpty();
        }
        throw new CError(ErrorType.TYPE_MISMATCH, "Cannot cast this to boolean.");
    }
}
