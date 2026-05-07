package com.nightshade.engine;

import com.nightshade.model.Token;
import com.nightshade.model.TokenType;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    @Test
    void testTokenizesJavaHelloWorld() {
        Lexer lexer = new Lexer();
        List<String> lines = List.of(
            "public class Hello {",
            "    public static void main(String[] args) {",
            "        System.out.println(\"Hello\");",
            "    }",
            "}"
        );
        List<Token> tokens = lexer.tokenize(lines);

        boolean hasPublic = false, hasClass = false, hasStatic = false, hasVoid = false;
        boolean hasHello = false, hasMain = false, hasOut = false, hasPrintln = false;
        boolean hasLiteralHello = false;

        for (Token t : tokens) {
            if (t.getType() == TokenType.KEYWORD) {
                if ("public".equals(t.getValue())) hasPublic = true;
                if ("class".equals(t.getValue())) hasClass = true;
                if ("static".equals(t.getValue())) hasStatic = true;
                if ("void".equals(t.getValue())) hasVoid = true;
            } else if (t.getType() == TokenType.IDENTIFIER) {
                if ("Hello".equals(t.getValue())) hasHello = true;
                if ("main".equals(t.getValue())) hasMain = true;
                if ("out".equals(t.getValue())) hasOut = true;
                if ("println".equals(t.getValue())) hasPrintln = true;
            } else if (t.getType() == TokenType.LITERAL) {
                if ("\"Hello\"".equals(t.getValue())) hasLiteralHello = true;
            }
        }

        assertTrue(hasPublic);
        assertTrue(hasClass);
        assertTrue(hasStatic);
        assertTrue(hasVoid);
        assertTrue(hasHello);
        assertTrue(hasMain);
        assertTrue(hasOut);
        assertTrue(hasPrintln);
        assertTrue(hasLiteralHello);
    }

    @Test
    void testTokenizesPythonComment() {
        Lexer lexer = new Lexer();
        List<String> lines = List.of(
            "# this is a comment",
            "x = 10"
        );
        List<Token> tokens = lexer.tokenize(lines);

        Token firstToken = null;
        for (Token t : tokens) {
            if (t.getType() != TokenType.WHITESPACE) {
                firstToken = t;
                break;
            }
        }
        
        assertNotNull(firstToken);
        assertEquals(TokenType.COMMENT, firstToken.getType());
        
        boolean hasX = false;
        for (Token t : tokens) {
            if (t.getType() == TokenType.IDENTIFIER && "x".equals(t.getValue())) {
                hasX = true;
            }
        }
        assertTrue(hasX);
    }

    @Test
    void testTokenizesStringLiterals() {
        Lexer lexer = new Lexer();
        List<String> lines = List.of(
            "String s = \"hello world\";"
        );
        List<Token> tokens = lexer.tokenize(lines);

        int literalCount = 0;
        for (Token t : tokens) {
            if (t.getType() == TokenType.LITERAL) {
                literalCount++;
                assertEquals("\"hello world\"", t.getValue());
            }
        }
        assertEquals(1, literalCount);
    }
}
