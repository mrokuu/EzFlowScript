package com.example.ezflowscript;

import java.util.*;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>{
    private final Interpreter interpreter;
    private final Stack<Map<String, Boolean>> scopes = new Stack();
    private FunctionType currentFunction;
    private ClassType currentClass;

    Resolver(Interpreter interpreter) {
        this.currentFunction = Resolver.FunctionType.NONE;
        this.currentClass = Resolver.ClassType.NONE;
        this.interpreter = interpreter;
    }

    public Void visitBlockStmt(Stmt.Block stmt) {
        this.beginScope();
        this.resolve(stmt.statements);
        this.endScope();
        return null;
    }

    public Void visitClassStmt(Stmt.Class stmt) {
        ClassType enclosingClass = this.currentClass;
        this.currentClass = Resolver.ClassType.CLASS;
        this.declare(stmt.name);
        this.define(stmt.name);
        if (stmt.superclass != null && stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
            EzFlowScriptApplication.error(stmt.superclass.name, "A class can't inherit from itself.");
        }

        if (stmt.superclass != null) {
            this.currentClass = Resolver.ClassType.SUBCLASS;
            this.resolve((Expr)stmt.superclass);
        }

        if (stmt.superclass != null) {
            this.beginScope();
            ((Map)this.scopes.peek()).put("super", true);
        }

        this.beginScope();
        ((Map)this.scopes.peek()).put("this", true);

        Stmt.Function method;
        FunctionType declaration;
        for(Iterator var3 = stmt.methods.iterator(); var3.hasNext(); this.resolveFunction(method, declaration)) {
            method = (Stmt.Function)var3.next();
            declaration = Resolver.FunctionType.METHOD;
            if (method.name.lexeme.equals("init")) {
                declaration = Resolver.FunctionType.INITIALIZER;
            }
        }

        this.endScope();
        if (stmt.superclass != null) {
            this.endScope();
        }

        this.currentClass = enclosingClass;
        return null;
    }

    public Void visitExpressionStmt(Stmt.Expression stmt) {
        this.resolve(stmt.expression);
        return null;
    }

    public Void visitVarStmt(Stmt.Var stmt) {
        this.declare(stmt.name);
        if (stmt.initializer != null) {
            this.resolve(stmt.initializer);
        }

        this.define(stmt.name);
        return null;
    }

    public Void visitWhileStmt(Stmt.While stmt) {
        this.resolve(stmt.condition);
        this.resolve(stmt.body);
        return null;
    }

    public Void visitVariableExpr(Expr.Variable expr) {
        if (!this.scopes.isEmpty() && ((Map)this.scopes.peek()).get(expr.name.lexeme) == Boolean.FALSE) {
            EzFlowScriptApplication.error(expr.name, "Can't read local variable in its own initializer.");
        }

        this.resolveLocal(expr, expr.name);
        return null;
    }

    public Void visitAssignExpr(Expr.Assign expr) {
        this.resolve(expr.value);
        this.resolveLocal(expr, expr.name);
        return null;
    }

    public Void visitBinaryExpr(Expr.Binary expr) {
        this.resolve(expr.left);
        this.resolve(expr.right);
        return null;
    }

    public Void visitCallExpr(Expr.Call expr) {
        this.resolve(expr.callee);
        Iterator var2 = expr.arguments.iterator();

        while(var2.hasNext()) {
            Expr argument = (Expr)var2.next();
            this.resolve(argument);
        }

        return null;
    }

    public Void visitGetExpr(Expr.Get expr) {
        this.resolve(expr.object);
        return null;
    }

    public Void visitGroupingExpr(Expr.Grouping expr) {
        this.resolve(expr.expression);
        return null;
    }

    public Void visitLiteralExpr(Expr.Literal expr) {
        return null;
    }

    public Void visitLogicalExpr(Expr.Logical expr) {
        this.resolve(expr.left);
        this.resolve(expr.right);
        return null;
    }

    public Void visitSetExpr(Expr.Set expr) {
        this.resolve(expr.value);
        this.resolve(expr.object);
        return null;
    }

    public Void visitSuperExpr(Expr.Super expr) {
        if (this.currentClass == Resolver.ClassType.NONE) {
            EzFlowScriptApplication.error(expr.keyword, "Can't use 'super' outside of a class.");
        } else if (this.currentClass != Resolver.ClassType.SUBCLASS) {
            EzFlowScriptApplication.error(expr.keyword, "Can't use 'super' in a class with no superclass.");
        }

        this.resolveLocal(expr, expr.keyword);
        return null;
    }

    public Void visitThisExpr(Expr.This expr) {
        if (this.currentClass == Resolver.ClassType.NONE) {
            EzFlowScriptApplication.error(expr.keyword, "Can't use 'this' outside of a class");
            return null;
        } else {
            this.resolveLocal(expr, expr.keyword);
            return null;
        }
    }

    public Void visitUnaryExpr(Expr.Unary expr) {
        this.resolve(expr.right);
        return null;
    }

    public Void visitFunctionStmt(Stmt.Function stmt) {
        this.declare(stmt.name);
        this.define(stmt.name);
        this.resolveFunction(stmt, Resolver.FunctionType.FUNCTION);
        return null;
    }

    public Void visitIfStmt(Stmt.If stmt) {
        this.resolve(stmt.condition);
        this.resolve(stmt.thenBranch);
        if (stmt.elseBranch != null) {
            this.resolve(stmt.elseBranch);
        }

        return null;
    }

    public Void visitPrintStmt(Stmt.Print stmt) {
        this.resolve(stmt.expression);
        return null;
    }

    public Void visitReturnStmt(Stmt.Return stmt) {
        if (this.currentFunction == Resolver.FunctionType.NONE) {
            EzFlowScriptApplication.error(stmt.keyword, "Can't return from top-level code.");
        }

        if (stmt.value != null) {
            if (this.currentFunction == Resolver.FunctionType.INITIALIZER) {
                EzFlowScriptApplication.error(stmt.keyword, "Can't return a value from an initializer.");
            }

            this.resolve(stmt.value);
        }

        return null;
    }

    public void resolve(List<Stmt> statements) {
        Iterator var2 = statements.iterator();

        while(var2.hasNext()) {
            Stmt statement = (Stmt)var2.next();
            this.resolve(statement);
        }

    }

    private void resolve(Stmt stmt) {
        stmt.accept(this);
    }

    private void resolve(Expr expr) {
        expr.accept(this);
    }

    private void resolveFunction(Stmt.Function function, FunctionType type) {
        FunctionType enclosingFunction = this.currentFunction;
        this.currentFunction = type;
        this.beginScope();
        Iterator var4 = function.params.iterator();

        while(var4.hasNext()) {
            Token param = (Token)var4.next();
            this.declare(param);
            this.define(param);
        }

        this.resolve(function.body);
        this.endScope();
        this.currentFunction = enclosingFunction;
    }

    private void beginScope() {
        this.scopes.push(new HashMap());
    }

    private void endScope() {
        this.scopes.pop();
    }

    private void declare(Token name) {
        if (!this.scopes.isEmpty()) {
            Map<String, Boolean> scope = (Map)this.scopes.peek();
            if (scope.containsKey(name.lexeme)) {
                EzFlowScriptApplication.error(name, "Already a variable with this name in this scope.");
            }

            scope.put(name.lexeme, false);
        }
    }

    private void define(Token name) {
        if (!this.scopes.isEmpty()) {
            ((Map)this.scopes.peek()).put(name.lexeme, true);
        }
    }

    private void resolveLocal(Expr expr, Token name) {
        for(int i = this.scopes.size() - 1; i >= 0; --i) {
            if (((Map)this.scopes.get(i)).containsKey(name.lexeme)) {
                this.interpreter.resolve(expr, this.scopes.size() - i - 1);
                return;
            }
        }

    }

    public FunctionType getCurrentFunction() {
        return this.currentFunction;
    }

    public void setCurrentFunction(FunctionType currentFunction) {
        this.currentFunction = currentFunction;
    }

    private static enum FunctionType {
        NONE,
        FUNCTION,
        INITIALIZER,
        METHOD;

        private FunctionType() {
        }
    }

    private static enum ClassType {
        NONE,
        CLASS,
        SUBCLASS;

        private ClassType() {
        }
    }
}
