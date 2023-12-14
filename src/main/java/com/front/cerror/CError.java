package com.front.cerror;

public class CError extends RuntimeException {
    private final ErrorType type;
    public CError(ErrorType type, String msg) {
        super(msg);
        this.type = type;
    }

    public ErrorType getType() {
        return type;
    }
}