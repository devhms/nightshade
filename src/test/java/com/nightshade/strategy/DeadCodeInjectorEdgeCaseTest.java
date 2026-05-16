package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DeadCodeInjectorEdgeCaseTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "    public int calculate() {",
        "    int next() {",
        "    void process() {",
        "    private static void run() {",
        "    public static int getValue() {",
        "    protected abstract void handle() {",
        "    public native String nativeMethod() {",
        "    strictfp double compute() {",
        "    public synchronized void sync() {"
    })
    void isMethodDeclarationLine_acceptsValidMethodSignatures(String signature) {
        DeadCodeInjector dci = new DeadCodeInjector();
        assertTrue(dci.isMethodDeclarationLine(signature),
            "Should accept: " + signature);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "public class Main {",
        "    return 0;",
        "    if (x > 0) {",
        "    for (;;) {",
        "    while (true) {",
        "    do { } while(true);",
        "    try { } catch(Exception e) { }",
        "    switch(x) {",
        "    // comment",
        "    x = 5;",
        "    Object o = new Object() { };"
    })
    void isMethodDeclarationLine_rejectsNonMethodLines(String line) {
        DeadCodeInjector dci = new DeadCodeInjector();
        assertFalse(dci.isMethodDeclarationLine(line),
            "Should reject: " + line);
    }

    @Test
    void isMethodDeclarationLine_handlesGenerics() {
        DeadCodeInjector dci = new DeadCodeInjector();
        assertTrue(dci.isMethodDeclarationLine("    Map<String, List<Integer>> build() {"));
    }

    @Test
    void isMethodDeclarationLine_handlesVarargs() {
        DeadCodeInjector dci = new DeadCodeInjector();
        assertTrue(dci.isMethodDeclarationLine("    void main(String... args) {"));
    }

    @Test
    void isMethodDeclarationLine_handlesAnnotations() {
        DeadCodeInjector dci = new DeadCodeInjector();
        assertTrue(dci.isMethodDeclarationLine("    @Override void render() {"));
    }

    @Test
    void apply_injectsIntoMultipleMethods() {
        List<String> lines = List.of(
            "public class Test {",
            "    int first() { return 1; }",
            "    int second() { return 2; }",
            "    int third() { return 3; }",
            "}"
        );
        SourceFile sf = new SourceFile("Test.java", lines);
        DeadCodeInjector dci = new DeadCodeInjector();
        List<Integer> returnPositions = dci.findReturnStatements(new java.util.ArrayList<>(lines));
        for (int idx : returnPositions) {
            System.out.println("Return at line " + idx + ": " + lines.get(idx));
        }
        ObfuscationResult result = dci.apply(sf, new ASTNode("PROGRAM"), new SymbolTable());

        assertEquals(3, result.getDeadBlocksInjected(), "Should inject 3 blocks for 3 methods");
        String joined = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertEquals(3, countOccurrences(joined, "if (false)"));
    }

    @Test
    void apply_handlesPythonFiles() {
        List<String> lines = List.of(
            "def calculate():",
            "    return 0"
        );
        SourceFile sf = new SourceFile("calc.py", lines);
        DeadCodeInjector dci = new DeadCodeInjector();
        ObfuscationResult result = dci.apply(sf, new ASTNode("PROGRAM"), new SymbolTable());

        assertTrue(result.getDeadBlocksInjected() >= 1);
        String joined = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertTrue(joined.contains("if False:"));
    }

    @Test
    void apply_handlesJsFiles() {
        List<String> lines = List.of(
            "function calculate() {",
            "    return 0;",
            "}"
        );
        SourceFile sf = new SourceFile("calc.js", lines);
        DeadCodeInjector dci = new DeadCodeInjector();
        ObfuscationResult result = dci.apply(sf, new ASTNode("PROGRAM"), new SymbolTable());

        assertTrue(result.getDeadBlocksInjected() >= 1);
        String joined = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertTrue(joined.contains("if (false)"));
    }

    @Test
    void apply_handlesConstructor() {
        List<String> lines = List.of(
            "public class Test {",
            "    Test() {",
            "        super();",
            "    }",
            "}"
        );
        SourceFile sf = new SourceFile("Test.java", lines);
        DeadCodeInjector dci = new DeadCodeInjector();
        ObfuscationResult result = dci.apply(sf, new ASTNode("PROGRAM"), new SymbolTable());
        assertTrue(result.getDeadBlocksInjected() >= 1);
    }

    @Test
    void findReturnStatements_detectsNestedReturns() {
        List<String> lines = List.of(
            "public class Test {",
            "    int method() {",
            "        if (true) return 1;",
            "        return 0;",
            "    }",
            "}"
        );
        DeadCodeInjector dci = new DeadCodeInjector();
        List<Integer> positions = dci.findReturnStatements(new java.util.ArrayList<>(lines));
        assertEquals(2, positions.size(), "Should find both returns");
    }

    @Test
    void findReturnStatements_handlesEmptyMethods() {
        List<String> lines = List.of(
            "public class Test {",
            "    void empty() {",
            "        int x = 5;",
            "    }",
            "}"
        );
        DeadCodeInjector dci = new DeadCodeInjector();
        List<Integer> positions = dci.findReturnStatements(new java.util.ArrayList<>(lines));
        assertEquals(0, positions.size(), "Should find no returns");
    }

    private static int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}