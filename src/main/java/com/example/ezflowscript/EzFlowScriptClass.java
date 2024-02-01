package com.example.ezflowscript;

import java.util.List;
import java.util.Map;

public class EzFlowScriptClass implements EzFlowScriptCallable  {
    final String name;
    final EzFlowScriptClass superClass;
    private final Map<String, EzFlowScriptFunction> methods;

    EzFlowScriptClass(String name, EzFlowScriptClass superClass, Map<String, EzFlowScriptFunction> methods) {
        this.name = name;
        this.superClass = superClass;
        this.methods = methods;
    }

    EzFlowScriptFunction findMethod(String name) {
        if (this.methods.containsKey(name)) {
            return (EzFlowScriptFunction)this.methods.get(name);
        } else {
            return this.superClass != null ? this.superClass.findMethod(name) : null;
        }
    }

    public Object call(Interpreter interpreter, List<Object> arguments) {
        EzFlowScriptInstance instance = new EzFlowScriptInstance(this);
        EzFlowScriptFunction initializer = this.findMethod("init");
        if (initializer != null) {
            initializer.bind(instance).call(interpreter, arguments);
        }

        return instance;
    }

    public int arity() {
        EzFlowScriptFunction initializer = this.findMethod("init");
        return initializer == null ? 0 : initializer.arity();
    }

    public String toString() {
        return this.name + " class";
    }
}
