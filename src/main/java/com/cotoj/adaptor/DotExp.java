package com.cotoj.adaptor;

import java.util.ArrayList;
import java.util.List;

import com.front.gunit.AddExp;
import com.front.gunit.Exp;

// Only Right Value!
public class DotExp extends Exp {
    private final List<Dotter> dotList;

    public DotExp(AddExp exp) {
        setAddExp(exp);
        this.dotList = new ArrayList<>();
    }

    public void addDotter(Dotter dotter) {
        dotList.add(dotter);
    }
    
    public List<Dotter> getDotList() {
        return dotList;
    }
}
