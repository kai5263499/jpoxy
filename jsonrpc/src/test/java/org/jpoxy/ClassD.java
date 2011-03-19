package org.jpoxy;

import java.util.HashMap;

public class ClassD {

    public ClassD() {
    }

    public int add(HashMap params) {
        int a = Integer.parseInt((String)params.get("a"));
        int b = Integer.parseInt((String)params.get("b"));
        return a + b;
    }
}
