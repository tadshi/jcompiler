package com.cotoj.adaptor;

import java.util.ArrayList;
import java.util.List;

import com.front.gunit.Exp;
import com.front.gunit.LOrExp;

// Only Right Value!
public class DotExp extends Exp {
    private final List<MethodInvokeDotter> dotList;

    public DotExp(LOrExp exp) {
        setLOrExp(exp);
        this.dotList = new ArrayList<>();
    }

    public void addDotter(MethodInvokeDotter dotter) {
        dotList.add(dotter);
    }
    
    public List<MethodInvokeDotter> getDotList() {
        return dotList;
    }
}
