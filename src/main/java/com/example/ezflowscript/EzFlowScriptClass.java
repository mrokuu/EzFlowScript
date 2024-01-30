package com.example.ezflowscript;

import java.util.List;
import java.util.Map;

public class EzFlowScriptClass implements EzFlowScriptCallable  {

    final String name;
    private final Map<String, EzFlowScriptFunction> methods;

    EzFlowScriptClass(String name, Map<String, EzFlowScriptFunction> methods) {
        this.name = name;
        this.methods = methods;
    }

    EzFlowScriptFunction findMethod(String name) {
        if (methods.containsKey(name)) {
            return methods.get(name);
        }

        return null;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        EzFlowScriptInstance instance = new EzFlowScriptInstance(this);
        return instance;
    }

    @Override
    public int arity() {
        return 0;
    }

    @Override
    public String toString() {
        return name + " class";
    }
}
