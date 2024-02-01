package com.example.ezflowscript;

import java.util.HashMap;
import java.util.Map;

class Environment {
    private final Map<String, Object> values = new HashMap();
    final Environment enclosing;

    Environment() {
        this.enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    void define(String name, Object value) {
        this.values.put(name, value);
    }

    Environment ancestor(int distance) {
        Environment environment = this;

        for(int i = 0; i < distance; ++i) {
            environment = environment.enclosing;
        }

        return environment;
    }

    Object getAt(int distance, String name) {
        return this.ancestor(distance).values.get(name);
    }

    void assignAt(int distance, Token name, Object value) {
        this.ancestor(distance).values.put(name.lexeme, value);
    }

    Object get(Token name) {
        if (this.values.containsKey(name.lexeme)) {
            return this.values.get(name.lexeme);
        } else if (this.enclosing != null) {
            return this.enclosing.get(name);
        } else {
            throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
        }
    }

    void assign(Token name, Object value) {
        if (this.values.containsKey(name.lexeme)) {
            this.values.put(name.lexeme, value);
        } else if (this.enclosing != null) {
            this.enclosing.assign(name, value);
        } else {
            throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
        }
    }
}