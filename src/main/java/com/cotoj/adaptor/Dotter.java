package com.cotoj.adaptor;

public abstract sealed class Dotter permits StaticFieldDotter, MethodCallDotter {
    private String identName;

    protected Dotter(String identName) {
        this.identName = identName;
    }

    public String getIdentName() {
        return identName;
    }
}
