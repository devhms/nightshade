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
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CompilationSafetyTest {

    @TempDir
    Path tempDir;

    private boolean compiles(String code) {
        try {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("public class (\\w+)").matcher(code);
            if (m.find()) {
                String className = m.group(1);
                Path file = tempDir.resolve(className + ".java");
                Files.writeString(file, code);
                JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
                if (compiler == null) return true;
                int result = compiler.run(null, null, null, file.toAbsolutePath().toString());
                return result == 0;
            }
            Path file = tempDir.resolve("Test.java");
            Files.writeString(file, code);
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            if (compiler == null) return true;
            int result = compiler.run(null, null, null, file.toAbsolutePath().toString());
            return result == 0;
        } catch (IOException e) {
            return false;
        }
    }

    @Test
    void testEntropyScramblerProducesCompilableOutput() {
        String code =
            "public class Test {\n" +
            "    private int counter = 0;\n" +
            "    private String name = \"test\";\n" +
            "    public int getCounter() { return counter + 1; }\n" +
            "    public void setCounter(int c) { this.counter = c; }\n" +
            "}\n";

        SourceFile source = new SourceFile("Test.java", List.of(code.split("\n")));
        SymbolTable symbols = new SymbolTable();
        ASTNode ast = new ASTNode("PROGRAM");

        EntropyScrambler scrambler = new EntropyScrambler();
        scrambler.setEnabled(true);
        ObfuscationResult result = scrambler.apply(source, ast, symbols);

        String output = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertTrue(compiles(output), "EntropyScrambler output should be compilable:\n" + output);
    }

    @Test
    void testDeadCodeInjectorProducesCompilableOutput() {
        String code =
            "public class Test {\n" +
            "    public int calculate() {\n" +
            "        return 0;\n" +
            "    }\n" +
            "}\n";

        SourceFile source = new SourceFile("Test.java", List.of(code.split("\n")));
        SymbolTable symbols = new SymbolTable();
        ASTNode ast = new ASTNode("PROGRAM");

        DeadCodeInjector injector = new DeadCodeInjector();
        injector.setEnabled(true);
        ObfuscationResult result = injector.apply(source, ast, symbols);

        String output = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertTrue(compiles(output), "DeadCodeInjector output should be compilable:\n" + output);
    }

    @Test
    void testCommentPoisonerProducesCompilableOutput() {
        String code =
            "public class Test {\n" +
            "    /* This is a block comment */\n" +
            "    public void run() { }\n" +
            "    // This is a line comment\n" +
            "}\n";

        SourceFile source = new SourceFile("Test.java", List.of(code.split("\n")));
        SymbolTable symbols = new SymbolTable();
        ASTNode ast = new ASTNode("PROGRAM");

        CommentPoisoner poisoner = new CommentPoisoner();
        poisoner.setEnabled(true);
        ObfuscationResult result = poisoner.apply(source, ast, symbols);

        String output = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertTrue(compiles(output), "CommentPoisoner output should be compilable:\n" + output);
    }

    @Test
    void testWhitespaceDisruptorProducesCompilableOutput() {
        String code =
            "public class Test {\n" +
            "    public int add(int a, int b) {\n" +
            "        return a + b;\n" +
            "    }\n" +
            "}\n";

        SourceFile source = new SourceFile("Test.java", List.of(code.split("\n")));
        SymbolTable symbols = new SymbolTable();
        ASTNode ast = new ASTNode("PROGRAM");

        WhitespaceDisruptor disruptor = new WhitespaceDisruptor();
        disruptor.setEnabled(true);
        ObfuscationResult result = disruptor.apply(source, ast, symbols);

        String output = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertTrue(compiles(output), "WhitespaceDisruptor output should be compilable:\n" + output);
    }

    @Test
    void testAllStrategiesTogetherProduceCompilableOutput() {
        List<String> lines = List.of(
            "public class Test {",
            "    private int counter = 0;",
            "    public int getCounter() { return counter + 1; }",
            "    public void setCounter(int c) { this.counter = c; }",
            "    public void demo() { /* inline comment */ }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
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
        assertTrue(compiles(output), "Full pipeline output should be compilable:\n" + output);
    }

    @Test
    void testControlFlowFlattenerProducesCompilableOutput() throws IOException {
        String code =
            "public class Test {\n" +
            "    private int calculate(int x) {\n" +
            "        int a = x + 1;\n" +
            "        int b = a * 2;\n" +
            "        return b - 3;\n" +
            "    }\n" +
            "}\n";

        SourceFile source = new SourceFile("Test.java", List.of(code.split("\n")));
        SymbolTable symbols = new SymbolTable();
        ASTNode ast = new ASTNode("PROGRAM");

        ControlFlowFlattener flattener = new ControlFlowFlattener();
        flattener.setEnabled(true);
        ObfuscationResult result = flattener.apply(source, ast, symbols);

        String output = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        String className = "CFFTest";
        java.util.regex.Matcher m2 = java.util.regex.Pattern.compile("public class (\\w+)").matcher(output);
        if (m2.find()) className = m2.group(1);
        Path outFile = tempDir.resolve(className + ".java");
        Files.writeString(outFile, output);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) return;
        int result2 = compiler.run(null, null, null, outFile.toAbsolutePath().toString());
        assertEquals(0, result2, "ControlFlowFlattener output should be compilable:\n" + output);
    }

    @Test
    void testStringEncoderProducesCompilableOutput() {
        String code =
            "public class Test {\n" +
            "    public void print() {\n" +
            "        System.out.println(\"Hello World\");\n" +
            "        System.out.println(\"Another string\");\n" +
            "    }\n" +
            "}\n";

        SourceFile source = new SourceFile("Test.java", List.of(code.split("\n")));
        SymbolTable symbols = new SymbolTable();
        ASTNode ast = new ASTNode("PROGRAM");

        StringEncoder encoder = new StringEncoder();
        encoder.setEnabled(true);
        ObfuscationResult result = encoder.apply(source, ast, symbols);

        String output = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertTrue(compiles(output), "StringEncoder output should be compilable:\n" + output);
    }

    @Test
    void testSemanticInverterProducesCompilableOutput() {
        String code =
            "public class Test {\n" +
            "    public boolean check(int x) {\n" +
            "        return x > 10 && x < 100;\n" +
            "    }\n" +
            "}\n";

        SourceFile source = new SourceFile("Test.java", List.of(code.split("\n")));
        SymbolTable symbols = new SymbolTable();
        ASTNode ast = new ASTNode("PROGRAM");

        SemanticInverter inverter = new SemanticInverter();
        inverter.setEnabled(true);
        ObfuscationResult result = inverter.apply(source, ast, symbols);

        String output = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertTrue(compiles(output), "SemanticInverter output should be compilable:\n" + output);
    }
}