package com.example.ezflowscript;

import java.util.HashMap;
import java.util.Map;

class EzFlowScriptInstance {
    private EzFlowScriptClass klass;
    private final Map<String, Object> fields = new HashMap();

    EzFlowScriptInstance(EzFlowScriptClass klass) {
        this.klass = klass;
    }

    Object get(Token name) {
        if (this.fields.containsKey(name.lexeme)) {
            return this.fields.get(name.lexeme);
        } else {
            EzFlowScriptFunction method = this.klass.findMethod(name.lexeme);
            if (method != null) {
                return method.bind(this);
            } else {
                throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
            }
        }
    }

    void set(Token name, Object value) {
        this.fields.put(name.lexeme, value);
    }

    public String toString() {
        return this.klass.name + " instance";
    }
}
