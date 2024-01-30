package com.example.ezflowscript;

import java.util.List;

public class EzFlowScriptFunction implements EzFlowScriptCallable {

    private final Environment closure;

    private final Stmt.Function declaration;

    EzFlowScriptFunction(Stmt.Function declaration, Environment closure) {
        // closure is nothing but a reference to the environment
        // that was active during function declaration, not when it got called.
        this.closure = closure;
        this.declaration = declaration;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> arguments) {
        // we chain up the new environment to the environment present at
        // time of function declaration, i.e closure.
        Environment environment = new Environment(closure);

        for (int i = 0; i< declaration.params.size(); i++ ) {
            environment.define(declaration.params.get(i).lexeme,
                    arguments.get(i));
        }

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            return returnValue.value;
        }
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn " + declaration.name.lexeme + ">";
    }
}
