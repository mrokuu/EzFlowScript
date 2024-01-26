package com.example.ezflowscript;


import java.util.List;

abstract class Expr {
    Expr() {
    }

    abstract <R> R accept(Visitor<R> var1);

    static class Variable extends Expr {
        final Token name;

        Variable(Token name) {
            this.name = name;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitVariableExpr(this);
        }
    }

    static class Unary extends Expr {
        final Token operator;
        final Expr right;

        Unary(Token operator, Expr right) {
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitUnaryExpr(this);
        }
    }

    static class This extends Expr {
        final Token keyword;

        This(Token keyword) {
            this.keyword = keyword;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitThisExpr(this);
        }
    }

    static class Super extends Expr {
        final Token keyword;
        final Token method;

        Super(Token keyword, Token method) {
            this.keyword = keyword;
            this.method = method;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitSuperExpr(this);
        }
    }

    static class Set extends Expr {
        final Expr object;
        final Token name;
        final Expr value;

        Set(Expr object, Token name, Expr value) {
            this.object = object;
            this.name = name;
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitSetExpr(this);
        }
    }

    static class Logical extends Expr {
        final Expr left;
        final Token operator;
        final Expr right;

        Logical(Expr left, Token operator, Expr right) {
            this.left = left;
            this.operator = operator;
            this.right = right;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLogicalExpr(this);
        }
    }

    static class Literal extends Expr {
        final Object value;

        Literal(Object value) {
            this.value = value;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitLiteralExpr(this);
        }
    }

    static class Grouping extends Expr {
        final Expr expression;

        Grouping(Expr expression) {
            this.expression = expression;
        }

        <R> R accept(Visitor<R> visitor) {
            return visitor.visitGroupingExpr(this);
        }
    }




    interface Visitor<R> {
        R visitAssignExpr(Assign var1);

        R visitBinaryExpr(Binary var1);

        R visitCallExpr(Call var1);

        R visitGetExpr(Get var1);

        R visitGroupingExpr(Grouping var1);

        R visitLiteralExpr(Literal var1);

        R visitLogicalExpr(Logical var1);

        R visitSetExpr(Set var1);

        R visitSuperExpr(Super var1);

        R visitThisExpr(This var1);

        R visitUnaryExpr(Unary var1);

        R visitVariableExpr(Variable var1);
    }
}
