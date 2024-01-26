package com.example.ezflowscript.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class GenerateAst {
    public GenerateAst() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: generate_ast <output directory>");
            System.exit(64);
        }

        String outputDir = args[0];
        defineAst(outputDir, "Expr", Arrays.asList("Assign  : Token name, Expr value", "Binary  : Expr left, Token operator, Expr right", "Call    : Expr callee, Token paren, List<Expr> arguments", "Get     : Expr object, Token name", "Grouping: Expr expression", "Literal : Object value", "Logical : Expr left, Token operator, Expr right", "Set     : Expr object, Token name, Expr value", "Super   : Token keyword, Token method", "This    : Token keyword", "Unary   : Token operator, Expr right", "Variable: Token name"));
        defineAst(outputDir, "Stmt", Arrays.asList("Block     : List<Stmt> statements", "Class     : Token name, Expr.Variable superclass, List<Stmt.Function> methods", "Expression: Expr expression", "Function  : Token name, List<Token> params, List<Stmt> body", "If        : Expr condition, Stmt thenBranch, Stmt elseBranch", "Print     : Expr expression", "Return    : Token keyword, Expr value", "Var       : Token name, Expr initializer", "While     : Expr condition, Stmt body"));
    }

    private static void defineAst(String outputDir, String baseName, List<String> types) throws IOException {
        String path = outputDir + "/" + baseName + ".java";
        PrintWriter writer = new PrintWriter(path, "UTF-8");
        writer.println("package myclasses;");
        writer.println();
        writer.println("import java.util.List;");
        writer.println();
        writer.println("abstract class " + baseName + "{");
        Iterator var5 = types.iterator();

        while(var5.hasNext()) {
            String type = (String)var5.next();
            String className = type.split(":")[0].trim();
            String fields = type.split(":")[1].trim();
        }

        writer.println();
        writer.println("  abstract <R> R accept(Visitor<R> visitor); ");
        writer.println("}");
        writer.close();
    }


}
