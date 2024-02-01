package com.example.ezflowscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
    private final String source;
    private final List<Token> tokens = new ArrayList();
    private int start = 0;
    private int current = 0;
    private int line = 1;
    private static final Map<String, TokenType> keywords = new HashMap();

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while(!this.isAtEnd()) {
            this.start = this.current;
            this.scanToken();
        }

        this.tokens.add(new Token(TokenType.EOF, "", (Object)null, this.line));
        return this.tokens;
    }

    private void scanToken() {
        char c = this.advance();
        switch (c) {
            case '\t':
            case '\r':
            case ' ':
                break;
            case '\n':
                ++this.line;
                break;
            case '!':
                this.addToken(this.match('=') ? TokenType.BANG_EQUAL : TokenType.BANG);
                break;
            case '"':
                this.string();
                break;
            case '(':
                this.addToken(TokenType.LEFT_PAREN);
                break;
            case ')':
                this.addToken(TokenType.RIGHT_PAREN);
                break;
            case '*':
                this.addToken(TokenType.STAR);
                break;
            case '+':
                this.addToken(TokenType.PLUS);
                break;
            case ',':
                this.addToken(TokenType.COMMA);
                break;
            case '-':
                this.addToken(TokenType.MINUS);
                break;
            case '.':
                this.addToken(TokenType.DOT);
                break;
            case '/':
                if (this.match('/')) {
                    while(this.peek() != '\n' && !this.isAtEnd()) {
                        this.advance();
                    }

                    return;
                } else {
                    if (this.match('*')) {
                        this.ccoment();
                    } else {
                        this.addToken(TokenType.SLASH);
                    }
                    break;
                }
            case ';':
                this.addToken(TokenType.SEMICOLON);
                break;
            case '<':
                this.addToken(this.match('=') ? TokenType.LESS_EQUAL : TokenType.LESS);
                break;
            case '=':
                this.addToken(this.match('=') ? TokenType.EQUAL_EQUAL : TokenType.EQUAL);
                break;
            case '>':
                this.addToken(this.match('=') ? TokenType.GREATER_EQUAL : TokenType.GREATER);
                break;
            case '{':
                this.addToken(TokenType.LEFT_BRACE);
                break;
            case '}':
                this.addToken(TokenType.RIGHT_BRACE);
                break;
            default:
                if (this.isDigit(c)) {
                    this.number();
                } else if (this.isAlpha(c)) {
                    this.identifer();
                } else {
                    EzFlowScriptApplication.error(this.line, "Unexpected Character");
                }
        }

    }

    private void ccoment() {
        boolean in_comment = false;

        while(true) {
            while(this.peek() == '/' && this.peekNext() == '*') {
                in_comment = true;
                this.advance(2);
            }

            if (this.peek() == '*' && this.peekNext() == '/' && !this.isAtEnd()) {
                in_comment = false;
                this.advance(2);
                break;
            }

            if (this.isAtEnd()) {
                break;
            }

            this.advance();
        }

        if (this.isAtEnd() && in_comment) {
            EzFlowScriptApplication.error(this.line, "Dangling comment");
        }

    }

    private void identifer() {
        while(this.isAlphaNumeric(this.peek())) {
            this.advance();
        }

        String text = this.source.substring(this.start, this.current);
        TokenType type = (TokenType)keywords.get(text);
        if (type == null) {
            type = TokenType.IDENTIFIER;
        }

        this.addToken(type);
    }

    private void number() {
        while(this.isDigit(this.peek())) {
            this.advance();
        }

        if (this.peek() == '.' && this.isDigit(this.peekNext())) {
            this.advance();

            while(this.isDigit(this.peek())) {
                this.advance();
            }
        }

        this.addToken(TokenType.NUMBER, Double.parseDouble(this.source.substring(this.start, this.current)));
    }

    private void string() {
        for(; this.peek() != '"' && !this.isAtEnd(); this.advance()) {
            if (this.peek() == '\n') {
                ++this.line;
            }
        }

        if (this.isAtEnd()) {
            EzFlowScriptApplication.error(this.line, "Unterminated String");
        } else {
            this.advance();
            String value = this.source.substring(this.start + 1, this.current - 1);
            this.addToken(TokenType.STRING, value);
        }
    }

    private boolean match(char expected) {
        if (this.isAtEnd()) {
            return false;
        } else if (this.source.charAt(this.current) != expected) {
            return false;
        } else {
            ++this.current;
            return true;
        }
    }

    private char peek() {
        return this.isAtEnd() ? '\u0000' : this.source.charAt(this.current);
    }

    private char peekNext() {
        return this.current + 1 >= this.source.length() ? '\u0000' : this.source.charAt(this.current + 1);
    }

    private boolean isAlpha(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c == '_';
    }

    boolean isAlphaNumeric(char c) {
        return this.isAlpha(c) || this.isDigit(c);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAtEnd() {
        return this.current >= this.source.length();
    }

    private char advance() {
        return this.source.charAt(this.current++);
    }

    private char advance(int steps) {
        this.current += steps;
        return this.source.charAt(this.current);
    }

    private void addToken(TokenType type) {
        this.addToken(type, (Object)null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = this.source.substring(this.start, this.current);
        this.tokens.add(new Token(type, text, literal, this.line));
    }

    static {
        keywords.put("and", TokenType.AND);
        keywords.put("class", TokenType.CLASS);
        keywords.put("else", TokenType.ELSE);
        keywords.put("false", TokenType.FALSE);
        keywords.put("fun", TokenType.FUN);
        keywords.put("for", TokenType.FOR);
        keywords.put("if", TokenType.IF);
        keywords.put("nil", TokenType.NIL);
        keywords.put("or", TokenType.OR);
        keywords.put("print", TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super", TokenType.SUPER);
        keywords.put("this", TokenType.THIS);
        keywords.put("true", TokenType.TRUE);
        keywords.put("var", TokenType.VAR);
        keywords.put("while", TokenType.WHILE);
    }
}