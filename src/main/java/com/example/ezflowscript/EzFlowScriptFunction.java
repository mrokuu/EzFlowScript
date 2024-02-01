package com.example.ezflowscript;

import java.util.List;

public class EzFlowScriptFunction implements EzFlowScriptCallable {

    private final Environment closure;
    private final Stmt.Function declaration;
    private final boolean isInitializer;

    EzFlowScriptFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.isInitializer = isInitializer;
        this.closure = closure;
        this.declaration = declaration;
    }

    EzFlowScriptFunction bind(EzFlowScriptInstance instance) {
        Environment environment = new Environment(this.closure);
        environment.define("this", instance);
        return new EzFlowScriptFunction(this.declaration, environment, this.isInitializer);
    }

    public Object call(Interpreter interpreter, List<Object> arguments) {
        Environment environment = new Environment(this.closure);

        for(int i = 0; i < this.declaration.params.size(); ++i) {
            environment.define(((Token)this.declaration.params.get(i)).lexeme, arguments.get(i));
        }

        try {
            interpreter.executeBlock(this.declaration.body, environment);
        } catch (Return var5) {
            if (this.isInitializer) {
                return this.closure.getAt(0, "this");
            }

            return var5.value;
        }

        return this.isInitializer ? this.closure.getAt(0, "this") : null;
    }

    public int arity() {
        return this.declaration.params.size();
    }

    public String toString() {
        return "<fn " + this.declaration.name.lexeme + ">";
    }
}
