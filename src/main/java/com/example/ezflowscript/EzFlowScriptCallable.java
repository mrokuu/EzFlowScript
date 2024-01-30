package com.example.ezflowscript;

import java.util.List;

interface EzFlowScriptCallable {
    int arity();
    Object call(Interpreter interpreter, List<Object> arguments);
}
