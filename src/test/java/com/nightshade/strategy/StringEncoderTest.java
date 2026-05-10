package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class StringEncoderTest {

    @Test
    void testEncodesSimpleStringLiteral() {
        List<String> lines = List.of(
            "public class Test {",
            "    String greeting = \"Hello, World!\";",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        StringEncoder encoder = new StringEncoder();
        ObfuscationResult result = encoder.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        String encodedLine = obfuscated.get(1);

        assertTrue(encodedLine.contains("new String(new char[]{"));
        assertFalse(encodedLine.contains("Hello, World!"));
        assertEquals(1, result.getStringsEncoded());
    }

    @Test
    void testSkipsStringsLongerThan80Chars() {
        String longString = "x".repeat(100);
        List<String> lines = List.of(
            "public class Test {",
            "    String longStr = \"" + longString + "\";",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        StringEncoder encoder = new StringEncoder();
        ObfuscationResult result = encoder.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        String line = obfuscated.get(1);

        assertTrue(line.contains(longString));
        assertEquals(0, result.getStringsEncoded());
    }

    @Test
    void testHandlesEscapeCharactersInStrings() {
        List<String> lines = List.of(
            "public class Test {",
            "    String escaped = \"Hello\\nWorld\\t!\";",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        StringEncoder encoder = new StringEncoder();
        ObfuscationResult result = encoder.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        String encodedLine = obfuscated.get(1);

        assertTrue(encodedLine.contains("new String(new char[]{"));
        assertEquals(1, result.getStringsEncoded());
    }

    @Test
    void testSkipsCommentOnlyLines() {
        List<String> lines = List.of(
            "public class Test {",
            "    // This is a comment",
            "    String msg = \"hello\";",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        StringEncoder encoder = new StringEncoder();
        ObfuscationResult result = encoder.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        assertTrue(obfuscated.get(1).contains("This is a comment"));
        assertEquals(1, result.getStringsEncoded());
    }

    @Test
    void testSkipsDeadCodeBlocks() {
        List<String> lines = List.of(
            "public class Test {",
            "    public void method() {",
            "        if (false) {",
            "            String secret = \"jdbc:mysql://prod-db\";",
            "        }",
            "        String normal = \"normal string\";",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        StringEncoder encoder = new StringEncoder();
        ObfuscationResult result = encoder.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        assertTrue(obfuscated.get(3).contains("jdbc:mysql://prod-db"));
        assertTrue(obfuscated.get(5).contains("new String(new char[]{"));
    }

    @Test
    void testSkipsNightshadeSkipBlocks() {
        List<String> lines = List.of(
            "public class Test {",
            "    @nightshade:skip",
            "    String preserved = \"keep this\";",
            "    @nightshade:resume",
            "    String encoded = \"encode this\";",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        StringEncoder encoder = new StringEncoder();
        ObfuscationResult result = encoder.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        assertTrue(obfuscated.get(2).contains("keep this"));
        assertTrue(obfuscated.get(4).contains("new String(new char[]{"));
    }

    @Test
    void testEmptyStringNotEncoded() {
        List<String> lines = List.of(
            "public class Test {",
            "    String empty = \"\";",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        StringEncoder encoder = new StringEncoder();
        ObfuscationResult result = encoder.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        assertEquals(0, result.getStringsEncoded());
    }

    @Test
    void testMultipleStringsInLine() {
        List<String> lines = List.of(
            "public class Test {",
            "    String a = \"x\", b = \"y\";",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        StringEncoder encoder = new StringEncoder();
        ObfuscationResult result = encoder.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        assertTrue(obfuscated.get(1).contains("new String(new char[]{"));
    }
}