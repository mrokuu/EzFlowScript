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
