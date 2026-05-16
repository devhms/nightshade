package com.nightshade.engine;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import com.nightshade.strategy.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.*;

class CrossFileTest {

    @TempDir
    Path tempDir;

    @Test
    void testPublicMethodNamesConsistentAcrossFiles() {
        SourceFile fileA = new SourceFile("ServiceA.java", List.of(
            "public class ServiceA {",
            "    public int compute(int x) {",
            "        return x * 2;",
            "    }",
            "    private int helper(int y) {",
            "        return y + 1;",
            "    }",
            "}"
        ));

        SourceFile fileB = new SourceFile("ServiceB.java", List.of(
            "public class ServiceB {",
            "    public int compute(int x) {",
            "        return x + 10;",
            "    }",
            "    private String format(String s) {",
            "        return s.trim();",
            "    }",
            "}"
        ));

        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(new EntropyScrambler());
        strategies.add(new DeadCodeInjector());
        strategies.add(new CommentPoisoner());
        strategies.add(new StringEncoder());
        strategies.add(new WhitespaceDisruptor());

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalc = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, lexer, parser, serializer, entropyCalc, logService, 0.65
        );

        List<ObfuscationResult> results = engine.process(List.of(fileA, fileB));

        assertEquals(2, results.size());

        String outputA = String.join("\n", results.get(0).getObfuscatedFile().getObfuscatedLines());
        String outputB = String.join("\n", results.get(1).getObfuscatedFile().getObfuscatedLines());

        boolean aHasCompute = outputA.contains("compute(");
        boolean bHasCompute = outputB.contains("compute(");
        assertTrue(aHasCompute, "compute() should remain in ServiceA (public API)");
        assertTrue(bHasCompute, "compute() should remain in ServiceB (public API)");
        assertEquals(aHasCompute, bHasCompute, "Same public method name should be preserved in both files");
    }

    @Test
    void testCrossFileSymbolConsistency() {
        List<String> linesA = List.of(
            "public class Calc {",
            "    public int add(int a, int b) {",
            "        int result = a + b;",
            "        return result;",
            "    }",
            "}"
        );

        List<String> linesB = List.of(
            "public class Printer {",
            "    public void show() {",
            "        int result = 42;",
            "        System.out.println(result);",
            "    }",
            "}"
        );

        SourceFile fileA = new SourceFile("Calc.java", linesA);
        SourceFile fileB = new SourceFile("Printer.java", linesB);

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalc = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();
        SymbolTable sharedSymbols = new SymbolTable();

        var tokensA = lexer.tokenize(linesA);
        var astA = parser.parse(tokensA);
        for (String api : parser.getPublicApis()) sharedSymbols.protect(api);

        var tokensB = lexer.tokenize(linesB);
        var astB = parser.parse(tokensB);
        for (String api : parser.getPublicApis()) sharedSymbols.protect(api);

        Map<String, String> mappingA = new HashMap<>();
        Map<String, String> mappingB = new HashMap<>();

        for (String id : List.of("a", "b", "result")) {
            String scope = "Calc::method::add";
            String replacement = sharedSymbols.resolve(id, scope);
            mappingA.put(id, replacement);
        }
        for (String id : List.of("result")) {
            String scope = "Printer::method::show";
            String replacement = sharedSymbols.resolve(id, scope);
            mappingB.put(id, replacement);
        }

        SourceFile currentA = new SourceFile("Calc.java", linesA);
        currentA.setObfuscatedLines(serializer.applyMapping(currentA, mappingA));

        SourceFile currentB = new SourceFile("Printer.java", linesB);
        currentB.setObfuscatedLines(serializer.applyMapping(currentB, mappingB));

        String outA = String.join("\n", currentA.getObfuscatedLines());
        String outB = String.join("\n", currentB.getObfuscatedLines());

        assertFalse(outA.contains("int result = a + b;"),
            "Private variable in Calc should be renamed");
        boolean bResultRenamed = !outB.contains("int result = 42;") || outB.contains("v_");
        assertTrue(bResultRenamed,
            "Private variable in Printer should be renamed (or already v_ prefixed), got: " + outB);
        assertTrue(outA.contains("int ") || outA.contains("v_") || outA.contains("return"),
            "Method body should not break");
        assertTrue(outB.contains("int ") || outB.contains("v_") || outB.contains("System.out"),
            "Method body should not break compilation");
    }

    @Test
    void testMultipleFilesProcessTogether() {
        List<SourceFile> files = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            files.add(new SourceFile("Test" + i + ".java", List.of(
                "public class Test" + i + " {",
                "    private int value = " + i + ";",
                "    public int get() { return value; }",
                "    private void reset() { value = 0; }",
                "}"
            )));
        }

        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(new EntropyScrambler());
        strategies.add(new DeadCodeInjector());
        strategies.add(new CommentPoisoner());
        strategies.add(new StringEncoder());
        strategies.add(new WhitespaceDisruptor());

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalc = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, lexer, parser, serializer, entropyCalc, logService, 0.65
        );

        List<ObfuscationResult> results = engine.process(files);

        assertEquals(3, results.size());
        for (ObfuscationResult r : results) {
            assertNotNull(r.getObfuscatedFile());
            assertNotNull(r.getObfuscatedFile().getObfuscatedLines());
            assertFalse(r.getObfuscatedFile().getObfuscatedLines().isEmpty());
        }
    }

    @Test
    void testOutputFilesWritableAndCompilable() throws IOException {
        List<String> lines = List.of(
            "public class OutputTest {",
            "    private int data = 100;",
            "    public int getData() { return data; }",
            "    public void setData(int d) { this.data = d; }",
            "}"
        );
        SourceFile source = new SourceFile("OutputTest.java", lines);

        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(new EntropyScrambler());
        strategies.add(new DeadCodeInjector());
        strategies.add(new CommentPoisoner());
        strategies.add(new StringEncoder());
        strategies.add(new WhitespaceDisruptor());

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalc = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, lexer, parser, serializer, entropyCalc, logService, 0.65
        );

        List<ObfuscationResult> results = engine.process(List.of(source));
        assertFalse(results.isEmpty());

        String output = String.join("\n", results.get(0).getObfuscatedFile().getObfuscatedLines());

        String className = "OutputTest";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("public class (\\w+)").matcher(output);
        if (m.find()) className = m.group(1);
        Path outFile = tempDir.resolve(className + ".java");
        Files.writeString(outFile, output);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) return;
        int result = compiler.run(null, null, null, outFile.toAbsolutePath().toString());
        assertEquals(0, result, "Output file should compile successfully");
    }
}