package com.example.ezflowscript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static com.example.ezflowscript.TokenType.*;


class Parser {
    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList();

        while(!this.isAtEnd()) {
            statements.add(this.declaration());
        }

        return statements;
    }

    private Expr expression() {
        return this.assignment();
    }

    private Stmt declaration() {
        try {
            return this.match(TokenType.VAR) ? this.varDeclaration() : this.statement();
        } catch (ParseError var2) {
            this.synchronize();
            return null;
        }
    }

    private Stmt classDeclaration() {
        Token name = this.consume(TokenType.IDENTIFIER, "Expect class name.");
        Expr.Variable superclass = null;
        if (this.match(TokenType.LESS)) {
            this.consume(TokenType.IDENTIFIER, "Expect superclass name.");
            superclass = new Expr.Variable(this.previous());
        }

        this.consume(TokenType.LEFT_BRACE, "Expect '{' before class body.");
        List<Stmt.Function> methods = new ArrayList();

        while(!this.check(TokenType.RIGHT_BRACE) && !this.isAtEnd()) {
            methods.add(this.function("method"));
        }

        this.consume(TokenType.RIGHT_BRACE, "Expect '}' after class body.");
        return new Stmt.Class(name, superclass, methods);
    }

    private Stmt statement() {
        if (this.match(TokenType.CLASS)) {
            return this.classDeclaration();
        } else if (this.match(TokenType.FUN)) {
            return this.function("function");
        } else if (this.match(TokenType.FOR)) {
            return this.forStatement();
        } else if (this.match(TokenType.IF)) {
            return this.ifStatement();
        } else if (this.match(TokenType.PRINT)) {
            return this.printStatement();
        } else if (this.match(TokenType.RETURN)) {
            return this.returnStatement();
        } else if (this.match(TokenType.WHILE)) {
            return this.whileStatement();
        } else {
            return (Stmt)(this.match(TokenType.LEFT_BRACE) ? new Stmt.Block(this.block()) : this.expressionStatement());
        }
    }

    private Stmt forStatement() {
        this.consume(TokenType.LEFT_PAREN, "Expect '(' after 'for'.");
        Stmt initializer;
        if (this.match(TokenType.SEMICOLON)) {
            initializer = null;
        } else if (this.match(TokenType.VAR)) {
            initializer = this.varDeclaration();
        } else {
            initializer = this.expressionStatement();
        }

        Expr condition = null;
        if (!this.check(TokenType.SEMICOLON)) {
            condition = this.expression();
        }

        this.consume(TokenType.SEMICOLON, "Expect ';' after loop condition.");
        Expr increment = null;
        if (!this.check(TokenType.RIGHT_PAREN)) {
            increment = this.expression();
        }

        this.consume(TokenType.RIGHT_PAREN, "Expect ')' after for clauses.");
        Stmt body = this.statement();
        if (increment != null) {
            body = new Stmt.Block(Arrays.asList((Stmt)body, new Stmt.Expression(increment)));
        }

        if (condition == null) {
            condition = new Expr.Literal(true);
        }

        body = new Stmt.While((Expr)condition, (Stmt)body);
        if (initializer != null) {
            body = new Stmt.Block(Arrays.asList(initializer, (Stmt)body));
        }

        return (Stmt)body;
    }

    private Stmt ifStatement() {
        this.consume(TokenType.LEFT_PAREN, "Expect '(' after 'if'.");
        Expr condition = this.expression();
        this.consume(TokenType.RIGHT_PAREN, "Expect ')' after if condition.");
        Stmt thenBranch = this.statement();
        Stmt elseBranch = null;
        if (this.match(TokenType.ELSE)) {
            elseBranch = this.statement();
        }

        return new Stmt.If(condition, thenBranch, elseBranch);
    }

    private Stmt printStatement() {
        Expr value = this.expression();
        this.consume(TokenType.SEMICOLON, "Expect ';' after value.");
        return new Stmt.Print(value);
    }

    private Stmt returnStatement() {
        Token keyword = this.previous();
        Expr value = null;
        if (!this.check(TokenType.SEMICOLON)) {
            value = this.expression();
        }

        this.consume(TokenType.SEMICOLON, "Expect ';' after return value.");
        return new Stmt.Return(keyword, value);
    }

    private Stmt varDeclaration() {
        Token name = this.consume(TokenType.IDENTIFIER, "Expect variable name ");
        Expr initializer = null;
        if (this.match(TokenType.EQUAL)) {
            initializer = this.expression();
        }

        this.consume(TokenType.SEMICOLON, "Expect ';' after variable declaration ");
        return new Stmt.Var(name, initializer);
    }

    private Stmt whileStatement() {
        this.consume(TokenType.LEFT_PAREN, "Expect '(' after 'while'.");
        Expr condition = this.expression();
        this.consume(TokenType.RIGHT_PAREN, "Expect ')' after condition.");
        Stmt body = this.statement();
        return new Stmt.While(condition, body);
    }

    private Stmt expressionStatement() {
        Expr expr = this.expression();
        this.consume(TokenType.SEMICOLON, "Expect ';' after expression.");
        return new Stmt.Expression(expr);
    }

    private Stmt.Function function(String kind) {
        Token name = this.consume(TokenType.IDENTIFIER, "Expect " + kind + " name.");
        this.consume(TokenType.LEFT_PAREN, "Expect '(' after " + kind + " name.");
        List<Token> parameters = new ArrayList();
        if (!this.check(TokenType.RIGHT_PAREN)) {
            do {
                if (parameters.size() >= 255) {
                    this.error(this.peek(), "Can't have more than 255 parameters.");
                }

                parameters.add(this.consume(TokenType.IDENTIFIER, "Expect parameter name."));
            } while(this.match(TokenType.COMMA));
        }

        this.consume(TokenType.RIGHT_PAREN, "Expect ')' after parameters.");
        this.consume(TokenType.LEFT_BRACE, "Expect '{' brefore " + kind + " body");
        List<Stmt> body = this.block();
        return new Stmt.Function(name, parameters, body);
    }

    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList();

        while(!this.check(TokenType.RIGHT_BRACE) && !this.isAtEnd()) {
            statements.add(this.declaration());
        }

        this.consume(TokenType.RIGHT_BRACE, "Expect '}' after block.");
        return statements;
    }

    private Expr assignment() {
        Expr expr = this.or();
        if (this.match(TokenType.EQUAL)) {
            Token equals = this.previous();
            Expr value = this.assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }

            if (expr instanceof Expr.Get) {
                Expr.Get get = (Expr.Get)expr;
                return new Expr.Set(get.object, get.name, value);
            }

            this.error(equals, "Invalid assignment target.");
        }

        return expr;
    }

    private Expr or() {
        Object expr;
        Token operator;
        Expr right;
        for(expr = this.and(); this.match(TokenType.OR); expr = new Expr.Logical((Expr)expr, operator, right)) {
            operator = this.previous();
            right = this.and();
        }

        return (Expr)expr;
    }

    private Expr and() {
        Object expr;
        Token operator;
        Expr right;
        for(expr = this.equality(); this.match(TokenType.AND); expr = new Expr.Logical((Expr)expr, operator, right)) {
            operator = this.previous();
            right = this.equality();
        }

        return (Expr)expr;
    }

    private Expr equality() {
        Object expr;
        Token operator;
        Expr right;
        for(expr = this.comparison(); this.match(TokenType.BANG_EQUAL, TokenType.EQUAL_EQUAL); expr = new Expr.Binary((Expr)expr, operator, right)) {
            operator = this.previous();
            right = this.comparison();
        }

        return (Expr)expr;
    }

    private Expr comparison() {
        Object expr;
        Token operator;
        Expr right;
        for(expr = this.term(); this.match(TokenType.GREATER, TokenType.GREATER_EQUAL, TokenType.LESS, TokenType.LESS_EQUAL); expr = new Expr.Binary((Expr)expr, operator, right)) {
            operator = this.previous();
            right = this.term();
        }

        return (Expr)expr;
    }

    private Expr term() {
        Object expr;
        Token operator;
        Expr right;
        for(expr = this.factor(); this.match(TokenType.MINUS, TokenType.PLUS); expr = new Expr.Binary((Expr)expr, operator, right)) {
            operator = this.previous();
            right = this.factor();
        }

        return (Expr)expr;
    }

    private Expr factor() {
        Object expr;
        Token operator;
        Expr right;
        for(expr = this.unary(); this.match(TokenType.SLASH, TokenType.STAR); expr = new Expr.Binary((Expr)expr, operator, right)) {
            operator = this.previous();
            right = this.unary();
        }

        return (Expr)expr;
    }

    private Expr unary() {
        if (this.match(TokenType.BANG, TokenType.MINUS)) {
            Token operator = this.previous();
            Expr right = this.unary();
            return new Expr.Unary(operator, right);
        } else {
            return this.call();
        }
    }

    private Expr finishCall(Expr callee) {
        List<Expr> arguments = new ArrayList();
        if (!this.check(TokenType.RIGHT_PAREN)) {
            do {
                if (arguments.size() >= 255) {
                    this.error(this.peek(), "Can't have more than 255 arguments.");
                }

                arguments.add(this.expression());
            } while(this.match(TokenType.COMMA));
        }

        Token paren = this.consume(TokenType.RIGHT_PAREN, "Expect ')' after arguments.");
        return new Expr.Call(callee, paren, arguments);
    }

    private Expr call() {
        Expr expr = this.primary();

        while(true) {
            while(!this.match(TokenType.LEFT_PAREN)) {
                if (!this.match(TokenType.DOT)) {
                    return (Expr)expr;
                }

                Token name = this.consume(TokenType.IDENTIFIER, "Expect property name after '.' .");
                expr = new Expr.Get((Expr)expr, name);
            }

            expr = this.finishCall((Expr)expr);
        }
    }

    private Expr primary() {
        if (this.match(TokenType.NUMBER, TokenType.STRING)) {
            return new Expr.Literal(this.previous().literal);
        } else if (this.match(TokenType.SUPER)) {
            Token keyword = this.previous();
            this.consume(TokenType.DOT, "Expect '.' after 'super'.");
            Token method = this.consume(TokenType.IDENTIFIER, "Expect superclass method name.");
            return new Expr.Super(keyword, method);
        } else if (this.match(TokenType.THIS)) {
            return new Expr.This(this.previous());
        } else if (this.match(TokenType.IDENTIFIER)) {
            return new Expr.Variable(this.previous());
        } else if (this.match(TokenType.TRUE)) {
            return new Expr.Literal(true);
        } else if (this.match(TokenType.FALSE)) {
            return new Expr.Literal(false);
        } else if (this.match(TokenType.NIL)) {
            return new Expr.Literal((Object)null);
        } else if (this.match(TokenType.LEFT_PAREN)) {
            Expr expr = this.expression();
            this.consume(TokenType.RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        } else {
            throw this.error(this.peek(), "Expect Expression");
        }
    }

    private boolean match(TokenType... types) {
        TokenType[] var2 = types;
        int var3 = types.length;

        for(int var4 = 0; var4 < var3; ++var4) {
            TokenType type = var2[var4];
            if (this.check(type)) {
                this.advance();
                return true;
            }
        }

        return false;
    }

    private Token consume(TokenType type, String message) {
        if (this.check(type)) {
            return this.advance();
        } else {
            throw this.error(this.peek(), message);
        }
    }

    private boolean check(TokenType type) {
        if (this.isAtEnd()) {
            return false;
        } else {
            return this.peek().type == type;
        }
    }

    private Token advance() {
        if (!this.isAtEnd()) {
            ++this.current;
        }

        return this.previous();
    }

    private boolean isAtEnd() {
        return this.peek().type == TokenType.EOF;
    }

    private Token peek() {
        return (Token)this.tokens.get(this.current);
    }

    private Token previous() {
        return (Token)this.tokens.get(this.current - 1);
    }

    private ParseError error(Token token, String message) {
        EzFlowScriptApplication.error(token, message);
        return new ParseError();
    }

    private void synchronize() {
        this.advance();

        while(!this.isAtEnd()) {
            if (this.previous().type == TokenType.SEMICOLON) {
                return;
            }

            switch (this.peek().type) {
                case CLASS:
                case FUN:
                case VAR:
                case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
                default:
                    this.advance();
            }
        }

    }

    private static class ParseError extends RuntimeException {
        private ParseError() {
        }
    }
}