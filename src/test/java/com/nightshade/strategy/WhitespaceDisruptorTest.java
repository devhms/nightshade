package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class WhitespaceDisruptorTest {

    @Test
    void testConvertsKRsToAllmanStyle() {
        List<String> lines = List.of(
            "public class Test {",
            "    public void method() {",
            "        int x = 1;",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        WhitespaceDisruptor disruptor = new WhitespaceDisruptor();
        ObfuscationResult result = disruptor.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        assertTrue(result.getWhitespaceChanges() > 0);
    }

    @Test
    void testPythonFilesAreSkipped() {
        List<String> lines = List.of(
            "def hello():",
            "    print('hello')"
        );
        SourceFile source = new SourceFile("script.py", lines);
        source.setObfuscatedLines(lines);

        WhitespaceDisruptor disruptor = new WhitespaceDisruptor();
        ObfuscationResult result = disruptor.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        assertEquals(lines, obfuscated);
        assertEquals(0, result.getWhitespaceChanges());
    }

    @Test
    void testIndentationVaries() {
        List<String> lines = List.of(
            "public class Test {",
            "    public void method1() { int a = 1; }",
            "    public void method2() { int b = 2; }",
            "    public void method3() { int c = 3; }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        WhitespaceDisruptor disruptor = new WhitespaceDisruptor();
        ObfuscationResult result = disruptor.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        assertTrue(result.getWhitespaceChanges() > 0);
    }

    @Test
    void testMultipleBraceStyles() {
        List<String> lines = List.of(
            "public class Test {",
            "    public void method() { return; }",
            "    public void method2() {",
            "        return;",
            "    }",
            "    static { System.out.println(\"init\"); }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        source.setObfuscatedLines(lines);

        WhitespaceDisruptor disruptor = new WhitespaceDisruptor();
        ObfuscationResult result = disruptor.apply(source, new ASTNode("BLOCK"), new SymbolTable());

        assertTrue(result.getWhitespaceChanges() > 0);
    }
}