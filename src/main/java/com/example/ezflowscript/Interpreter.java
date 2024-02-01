package com.example.ezflowscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
    final Environment globals = new Environment();
    private Environment environment;
    private final Map<Expr, Integer> locals;

    Interpreter() {
        this.environment = this.globals;
        this.locals = new HashMap();
        this.globals.define("clock", new EzFlowScriptCallable() {
            public int arity() {
                return 0;
            }

            public Object call(Interpreter interpreter, List<Object> arguments) {
                return (double)System.currentTimeMillis() / 1000.0;
            }

            public String toString() {
                return "<native fn>";
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            Iterator var2 = statements.iterator();

            while(var2.hasNext()) {
                Stmt statement = (Stmt)var2.next();
                this.execute(statement);
            }
        } catch (RuntimeError var4) {
            EzFlowScriptApplication.runtimeError(var4);
        }

    }

    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = this.evaluate(expr.left);
        if (expr.operator.type == TokenType.OR) {
            if (this.isTruthy(left)) {
                return left;
            }
        } else if (!this.isTruthy(left)) {
            return left;
        }

        return this.evaluate(expr.right);
    }

    public Object visitSetExpr(Expr.Set expr) {
        Object object = this.evaluate(expr.object);
        if (!(object instanceof EzFlowScriptInstance)) {
            throw new RuntimeError(expr.name, "Only instances have fields.");
        } else {
            Object value = this.evaluate(expr.value);
            ((EzFlowScriptInstance)object).set(expr.name, value);
            return value;
        }
    }

    public Object visitSuperExpr(Expr.Super expr) {
        int distance = (Integer)this.locals.get(expr);
        EzFlowScriptClass superclass = (EzFlowScriptClass)this.environment.getAt(distance, "super");
        EzFlowScriptInstance object = (EzFlowScriptInstance)this.environment.getAt(distance - 1, "this");
        EzFlowScriptFunction method = superclass.findMethod(expr.method.lexeme);
        if (method == null) {
            throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
        } else {
            return method.bind(object);
        }
    }

    public Object visitThisExpr(Expr.This expr) {
        return this.lookUpVariable(expr.keyword, expr);
    }

    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = this.evaluate(expr.right);
        switch (expr.operator.type) {
            case BANG:
                return !this.isTruthy(right);
            case MINUS:
                return -(Double)right;
            default:
                return null;
        }
    }

    public Object visitVariableExpr(Expr.Variable expr) {
        return this.lookUpVariable(expr.name, expr);
    }

    private Object lookUpVariable(Token name, Expr expr) {
        Integer distance = (Integer)this.locals.get(expr);
        return distance != null ? this.environment.getAt(distance, name.lexeme) : this.globals.get(name);
    }

    private void checkNumberOperand(Token operator, Object operand) {
        if (!(operand instanceof Double)) {
            throw new RuntimeError(operator, "Operand must be a number.");
        }
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (!(left instanceof Double) || !(right instanceof Double)) {
            throw new RuntimeError(operator, "Operands must be numbers.");
        }
    }

    private boolean isTruthy(Object object) {
        if (object == null) {
            return false;
        } else {
            return object instanceof Boolean ? (Boolean)object : true;
        }
    }

    private boolean isEqual(Object a, Object b) {
        if (a == null && b == null) {
            return true;
        } else {
            return a == null ? false : a.equals(b);
        }
    }

    private String stringify(Object object) {
        if (object == null) {
            return "nil";
        } else if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }

            return text;
        } else {
            return object.toString();
        }
    }

    public Object visitGroupingExpr(Expr.Grouping expr) {
        return this.evaluate(expr.expression);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    void resolve(Expr expr, int depth) {
        this.locals.put(expr, depth);
    }

    void executeBlock(List<Stmt> statements, Environment environment) {
        Environment previous = this.environment;

        try {
            this.environment = environment;
            Iterator var4 = statements.iterator();

            while(var4.hasNext()) {
                Stmt statement = (Stmt)var4.next();
                this.execute(statement);
            }
        } finally {
            this.environment = previous;
        }

    }

    public Void visitBlockStmt(Stmt.Block stmt) {
        this.executeBlock(stmt.statements, new Environment(this.environment));
        return null;
    }

    public Void visitExpressionStmt(Stmt.Expression stmt) {
        this.evaluate(stmt.expression);
        return null;
    }

    public Void visitClassStmt(Stmt.Class stmt) {
        Object superclass = null;
        if (stmt.superclass != null) {
            superclass = this.evaluate(stmt.superclass);
            if (!(superclass instanceof EzFlowScriptClass)) {
                throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
            }
        }

        this.environment.define(stmt.name.lexeme, (Object)null);
        if (stmt.superclass != null) {
            this.environment = new Environment(this.environment);
            this.environment.define("super", superclass);
        }

        Map<String, EzFlowScriptFunction> methods = new HashMap();
        Iterator var4 = stmt.methods.iterator();

        while(var4.hasNext()) {
            Stmt.Function method = (Stmt.Function)var4.next();
            EzFlowScriptFunction function = new EzFlowScriptFunction(method, this.environment, method.name.lexeme.equals("init"));
            methods.put(method.name.lexeme, function);
        }

        EzFlowScriptClass klass = new EzFlowScriptClass(stmt.name.lexeme, (EzFlowScriptClass)superclass, methods);
        if (superclass != null) {
            this.environment = this.environment.enclosing;
        }

        this.environment.assign(stmt.name, klass);
        return null;
    }

    public Void visitFunctionStmt(Stmt.Function stmt) {
        EzFlowScriptFunction function = new EzFlowScriptFunction(stmt, this.environment, false);
        this.environment.define(stmt.name.lexeme, function);
        return null;
    }

    public Void visitIfStmt(Stmt.If stmt) {
        if (this.isTruthy(this.evaluate(stmt.condition))) {
            this.execute(stmt.thenBranch);
        } else if (stmt.elseBranch != null) {
            this.execute(stmt.elseBranch);
        }

        return null;
    }

    public Void visitPrintStmt(Stmt.Print stmt) {
        Object value = this.evaluate(stmt.expression);
        System.out.println(this.stringify(value));
        return null;
    }

    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if (stmt.value != null) {
            value = this.evaluate(stmt.value);
        }

        throw new Return(value);
    }

    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = this.evaluate(stmt.initializer);
        }

        this.environment.define(stmt.name.lexeme, value);
        return null;
    }

    public Void visitWhileStmt(Stmt.While stmt) {
        while(this.isTruthy(this.evaluate(stmt.condition))) {
            this.execute(stmt.body);
        }

        return null;
    }

    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = this.evaluate(expr.value);
        Integer distance = (Integer)this.locals.get(expr);
        if (distance != null) {
            this.environment.assignAt(distance, expr.name, value);
        } else {
            this.globals.assign(expr.name, value);
        }

        return value;
    }

    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = this.evaluate(expr.left);
        Object right = this.evaluate(expr.right);
        switch (expr.operator.type) {
            case MINUS:
                this.checkNumberOperands(expr.operator, left, right);
                return (Double)left - (Double)right;
            case GREATER:
                this.checkNumberOperands(expr.operator, left, right);
                return (Double)left > (Double)right;
            case GREATER_EQUAL:
                this.checkNumberOperands(expr.operator, left, right);
                return (Double)left >= (Double)right;
            case LESS:
                this.checkNumberOperands(expr.operator, left, right);
                return (Double)left < (Double)right;
            case LESS_EQUAL:
                this.checkNumberOperands(expr.operator, left, right);
                return (Double)left <= (Double)right;
            case BANG_EQUAL:
                return !this.isEqual(left, right);
            case EQUAL_EQUAL:
                return this.isEqual(left, right);
            case PLUS:
                if (left instanceof Double && right instanceof Double) {
                    return (Double)left + (Double)right;
                } else {
                    if (left instanceof String && right instanceof String) {
                        return (String)left + (String)right;
                    }

                    throw new RuntimeError(expr.operator, "Operands must be two numbers or Strings");
                }
            case SLASH:
                this.checkNumberOperands(expr.operator, left, right);
                return (Double)left / (Double)right;
            case STAR:
                this.checkNumberOperands(expr.operator, left, right);
                return (Double)left * (Double)right;
            default:
                return null;
        }
    }

    public Object visitCallExpr(Expr.Call expr) {
        Object callee = this.evaluate(expr.callee);
        List<Object> arguments = new ArrayList();
        Iterator var4 = expr.arguments.iterator();

        while(var4.hasNext()) {
            Expr argument = (Expr)var4.next();
            arguments.add(this.evaluate(argument));
        }

        if (!(callee instanceof EzFlowScriptCallable function)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        } else if (arguments.size() != function.arity()) {
            Token var10002 = expr.paren;
            int var10003 = function.arity();
            throw new RuntimeError(var10002, "Expected " + var10003 + " arguments but got " + arguments.size() + ".");
        } else {
            return function.call(this, arguments);
        }
    }

    public Object visitGetExpr(Expr.Get expr) {
        Object object = this.evaluate(expr.object);
        if (object instanceof EzFlowScriptInstance) {
            return ((EzFlowScriptInstance)object).get(expr.name);
        } else {
            throw new RuntimeError(expr.name, "Only instances have properties.");
        }
    }
}