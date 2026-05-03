package com.nightshade.engine;

import com.nightshade.model.ASTNode;
import com.nightshade.model.Token;
import com.nightshade.model.TokenType;

import java.util.List;

/**
 * Converts a flat Token list into a simplified AST sufficient for
 * all five poisoning strategies.
 *
 * This is NOT a full Java parser. It needs only to:
 *  - Identify class and method boundaries (for dead code injection)
 *  - Tag identifier tokens with scope info (for scope-aware renaming)
 *  - Identify comment tokens (for comment poisoning)
 *  - Identify string literal tokens (for string encoding)
 *
 * Strategy:
 *  - Tracks brace depth to detect method/class boundaries
 *  - Assigns scope paths for scope-aware renaming
 *  - Never crashes — logs a warning and continues on unparseable structures
 *
 * Node types produced:
 *  CLASS_DECL, METHOD_DECL, BLOCK, STATEMENT, FIELD_DECL, COMMENT_NODE, PROGRAM
 */
public class Parser {

    public ASTNode parse(List<Token> tokens) {
        ASTNode program = new ASTNode("PROGRAM");
        program.setScopePath("global");

        try {
            parseProgram(tokens, program);
        } catch (Exception e) {
            // Never crash the pipeline — return whatever we have
            System.err.println("[WARN] Parser encountered unexpected structure: " + e.getMessage());
        }

        return program;
    }

    private void parseProgram(List<Token> tokens, ASTNode program) {
        int i = 0;
        int braceDepth = 0;
        String currentClassName = "Unknown";
        String currentMethodName = null;
        int methodCount = 0;
        ASTNode currentMethod = null;
        boolean inMethod = false;
        int methodStartDepth = 0;

        while (i < tokens.size()) {
            Token t = tokens.get(i);

            // Track comments — always attach to program with scope
            if (t.getType() == TokenType.COMMENT) {
                ASTNode commentNode = new ASTNode("COMMENT_NODE", t);
                commentNode.setScopePath(currentClassName + "." +
                    (currentMethodName != null ? currentMethodName : "class"));
                program.addChild(commentNode);
                i++;
                continue;
            }

            // Class declaration detection
            if (t.getType() == TokenType.KEYWORD &&
                (t.getValue().equals("class") || t.getValue().equals("interface") ||
                 t.getValue().equals("enum") || t.getValue().equals("record"))) {

                // Next non-whitespace IDENTIFIER is the class name
                for (int j = i + 1; j < tokens.size(); j++) {
                    if (tokens.get(j).getType() == TokenType.IDENTIFIER) {
                        currentClassName = tokens.get(j).getValue();
                        ASTNode classNode = new ASTNode("CLASS_DECL", tokens.get(j));
                        classNode.setScopePath(currentClassName);
                        program.addChild(classNode);
                        break;
                    }
                }
            }

            // Method detection: look for pattern IDENTIFIER ( ... ) { at brace depth 1
            if (braceDepth == 1 && t.getType() == TokenType.IDENTIFIER &&
                i + 1 < tokens.size()) {

                // Check if this looks like a method: identifier followed eventually by (
                boolean looksLikeMethod = false;
                for (int j = i + 1; j < Math.min(i + 10, tokens.size()); j++) {
                    Token peek = tokens.get(j);
                    if (peek.getType() == TokenType.WHITESPACE) continue;
                    if (peek.getType() == TokenType.SYMBOL && peek.getValue().equals("(")) {
                        looksLikeMethod = true;
                    }
                    break;
                }

                if (looksLikeMethod && !inMethod) {
                    currentMethodName = t.getValue();
                    currentMethod = new ASTNode("METHOD_DECL", t);
                    currentMethod.setScopePath(currentClassName + "." + currentMethodName);
                    currentMethod.setMethodIndex(methodCount++);
                    program.addChild(currentMethod);
                }
            }

            // Brace tracking — detect method body entry/exit
            if (t.getType() == TokenType.SYMBOL) {
                if (t.getValue().equals("{")) {
                    braceDepth++;
                    if (currentMethod != null && !inMethod && braceDepth == 2) {
                        inMethod = true;
                        methodStartDepth = braceDepth;
                        ASTNode block = new ASTNode("BLOCK");
                        block.setScopePath(currentClassName + "." + currentMethodName);
                        if (currentMethod != null) currentMethod.addChild(block);
                    }
                } else if (t.getValue().equals("}")) {
                    if (inMethod && braceDepth == methodStartDepth) {
                        inMethod = false;
                        currentMethodName = null;
                        currentMethod = null;
                    }
                    braceDepth = Math.max(0, braceDepth - 1);
                }
            }

            // Tag all identifier tokens with scope path
            if (t.getType() == TokenType.IDENTIFIER) {
                ASTNode idNode = new ASTNode("STATEMENT", t);
                String scope = currentClassName + "." +
                    (currentMethodName != null ? currentMethodName : "class");
                idNode.setScopePath(scope);
                program.addChild(idNode);
            }

            // Tag string literals for StringEncoder
            if (t.getType() == TokenType.LITERAL && t.getValue().startsWith("\"")) {
                ASTNode litNode = new ASTNode("STRING_LITERAL", t);
                litNode.setScopePath(currentClassName + "." +
                    (currentMethodName != null ? currentMethodName : "class"));
                program.addChild(litNode);
            }

            i++;
        }
    }
}
