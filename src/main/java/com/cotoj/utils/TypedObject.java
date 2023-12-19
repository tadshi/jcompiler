package com.cotoj.utils;


public record TypedObject (ReturnType type, Object object) {
    public TypedObject map(Object newObject) {
        return new TypedObject(type, newObject);
    }
}
