package com.cotoj.adaptor;

import com.cotoj.utils.ReturnType;
import com.front.gunit.ObjectClass;

public final class StaticAccessExp extends ObjectClass {
    private ReturnType classType;
    private String fieldName;
    private ReturnType fieldType;
    
    public StaticAccessExp(ReturnType classType, String fieldName, ReturnType fieldType) {
        this.classType = classType;
        this.fieldName = fieldName;
        this.fieldType = fieldType;
    }

    public ReturnType getClassType() {
        return classType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public ReturnType getFieldType() {
        return fieldType;
    }
}
