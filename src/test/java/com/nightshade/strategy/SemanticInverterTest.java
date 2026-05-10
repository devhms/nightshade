package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import com.nightshade.model.Token;
import com.nightshade.model.TokenType;
import com.nightshade.engine.Lexer;
import com.nightshade.engine.Parser;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SemanticInverterTest {

    @Test
    void testVariablesRenamedToMisleadingTerms() {
        List<String> lines = List.of(
            "public class Test {",
            "    int counter = 0;",
            "    int result = counter + 1;",
            "    public void compute(int data) {",
            "        int value = data * 2;",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(lines);
        Parser parser = new Parser();
        ASTNode ast = parser.parse(tokens);
        SymbolTable symbols = new SymbolTable();

        for (Token t : tokens) {
            if (t.getType() == TokenType.IDENTIFIER && symbols.isUserDefined(t.getValue())) {
                symbols.resolve(t.getValue());
            }
        }

        SemanticInverter inverter = new SemanticInverter();
        ObfuscationResult result = inverter.apply(source, ast, symbols);

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        String allContent = String.join(" ", obfuscated);

        assertFalse(allContent.contains("counter"));
        assertFalse(allContent.contains("result"));
        assertFalse(allContent.contains("data"));
        assertFalse(allContent.contains("value"));
        assertTrue(result.getRenamedIdentifiers() > 0);
    }

    @Test
    void testKeywordsNotRenamed() {
        List<String> lines = List.of(
            "public class Test {",
            "    public static void main(String[] args) {",
            "        int counter = 0;",
            "        if (counter > 0) {",
            "            return;",
            "        }",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(lines);
        Parser parser = new Parser();
        ASTNode ast = parser.parse(tokens);
        SymbolTable symbols = new SymbolTable();

        for (Token t : tokens) {
            if (t.getType() == TokenType.IDENTIFIER && symbols.isUserDefined(t.getValue())) {
                symbols.resolve(t.getValue());
            }
        }

        SemanticInverter inverter = new SemanticInverter();
        ObfuscationResult result = inverter.apply(source, ast, symbols);

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        String allContent = String.join(" ", obfuscated);

        assertTrue(allContent.contains("public"));
        assertTrue(allContent.contains("class"));
        assertTrue(allContent.contains("static"));
        assertTrue(allContent.contains("void"));
        assertTrue(allContent.contains("if"));
        assertTrue(allContent.contains("return"));
    }

    @Test
    void testClassNameNotRenamed() {
        List<String> lines = List.of(
            "public class MyApp {",
            "    int value = 10;",
            "}"
        );
        SourceFile source = new SourceFile("MyApp.java", lines);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(lines);
        Parser parser = new Parser();
        ASTNode ast = parser.parse(tokens);
        SymbolTable symbols = new SymbolTable();

        for (Token t : tokens) {
            if (t.getType() == TokenType.IDENTIFIER && symbols.isUserDefined(t.getValue())) {
                symbols.resolve(t.getValue());
            }
        }

        SemanticInverter inverter = new SemanticInverter();
        ObfuscationResult result = inverter.apply(source, ast, symbols);

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        String allContent = String.join(" ", obfuscated);

        assertTrue(allContent.contains("MyApp"));
    }

    @Test
    void testDeterministicRenaming() {
        List<String> lines = List.of(
            "public class Test {",
            "    int counter = 0;",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(lines);
        Parser parser = new Parser();
        ASTNode ast = parser.parse(tokens);
        SymbolTable symbols = new SymbolTable();

        for (Token t : tokens) {
            if (t.getType() == TokenType.IDENTIFIER && symbols.isUserDefined(t.getValue())) {
                symbols.resolve(t.getValue());
            }
        }

        SemanticInverter inverter1 = new SemanticInverter();
        ObfuscationResult result1 = inverter1.apply(source, ast, symbols);

        SemanticInverter inverter2 = new SemanticInverter();
        ObfuscationResult result2 = inverter2.apply(source, ast, symbols);

        List<String> lines1 = result1.getObfuscatedFile().getObfuscatedLines();
        List<String> lines2 = result2.getObfuscatedFile().getObfuscatedLines();

        assertEquals(lines1.get(1), lines2.get(1));
    }

    @Test
    void testEnabledByDefaultIsFalse() {
        SemanticInverter inverter = new SemanticInverter();
        assertFalse(inverter.isEnabled());
    }

    @Test
    void testSetEnabledMakesItRun() {
        List<String> lines = List.of(
            "public class Test {",
            "    int x = 1;",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(lines);
        Parser parser = new Parser();
        ASTNode ast = parser.parse(tokens);
        SymbolTable symbols = new SymbolTable();

        for (Token t : tokens) {
            if (t.getType() == TokenType.IDENTIFIER && symbols.isUserDefined(t.getValue())) {
                symbols.resolve(t.getValue());
            }
        }

        SemanticInverter inverter = new SemanticInverter();
        inverter.setEnabled(true);

        ObfuscationResult result = inverter.apply(source, ast, symbols);

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        String allContent = String.join(" ", obfuscated);

        assertFalse(allContent.contains("int x"));
    }
}