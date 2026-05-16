package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
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
import static org.junit.jupiter.api.Assertions.*;

class PipelineIntegrationTest {

    @Test
    void testFullPipelineProducesOutput() {
        List<String> lines = List.of(
            "// Initial file",
            "public class Hello {",
            "    public static void main(String[] args) {",
            "        int count = 0;",
            "        System.out.println(\"Hello: \" + count);",
            "        return;",
            "    }",
            "}"
        );
        SourceFile sourceFile = new SourceFile("Hello.java", lines);
        List<SourceFile> files = new ArrayList<>();
        files.add(sourceFile);

        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(new EntropyScrambler());
        strategies.add(new DeadCodeInjector());
        strategies.add(new CommentPoisoner());
        strategies.add(new StringEncoder());
        strategies.add(new WhitespaceDisruptor());

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalculator = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, lexer, parser, serializer, entropyCalculator, logService, 0.65
        );
        List<ObfuscationResult> results = engine.process(files);

        assertFalse(results.isEmpty());
        ObfuscationResult result = results.get(0);

        assertTrue(result.getEntropyScore() > 0.0);

        boolean isDifferent = false;
        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();

        if (lines.size() != obfuscated.size()) {
            isDifferent = true;
        } else {
            for (int i = 0; i < lines.size(); i++) {
                if (!lines.get(i).equals(obfuscated.get(i))) {
                    isDifferent = true;
                    break;
                }
            }
        }

        assertTrue(isDifferent);
    }

    @TempDir
    Path pipelineTempDir;

    @Test
    void testFullPipelineOutputCompiles() throws IOException {
        List<String> lines = List.of(
            "public class CompileTest {",
            "    private int value = 42;",
            "    public int get() { return value; }",
            "    public void set(int v) { this.value = v; }",
            "    public static void main(String[] args) {",
            "        CompileTest t = new CompileTest();",
            "        System.out.println(t.get());",
            "    }",
            "}"
        );
        SourceFile sourceFile = new SourceFile("CompileTest.java", lines);

        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(new EntropyScrambler());
        strategies.add(new DeadCodeInjector());
        strategies.add(new CommentPoisoner());
        strategies.add(new StringEncoder());
        strategies.add(new WhitespaceDisruptor());

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalculator = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, lexer, parser, serializer, entropyCalculator, logService, 0.65
        );
        List<ObfuscationResult> results = engine.process(List.of(sourceFile));

        assertFalse(results.isEmpty());
        String output = String.join("\n", results.get(0).getObfuscatedFile().getObfuscatedLines());

        java.util.regex.Matcher m = java.util.regex.Pattern.compile("public class (\\w+)").matcher(output);
        String className = m.find() ? m.group(1) : "CompileTest";
        Path outFile = pipelineTempDir.resolve(className + ".java");
        Files.writeString(outFile, output);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) return;
        int result = compiler.run(null, null, null, outFile.toAbsolutePath().toString());
        assertEquals(0, result, "Pipeline output should compile successfully:\n" + output);
    }

    @Test
    void testFullPipelineWithAllStrategiesEnabled() {
        List<String> lines = List.of(
            "public class FullPipelineTest {",
            "    private int data = 0;",
            "    public int compute(int x) {",
            "        int temp = x * 2;",
            "        return temp + data;",
            "    }",
            "    private void update(int d) { this.data = d; }",
            "}"
        );
        SourceFile sourceFile = new SourceFile("FullPipelineTest.java", lines);

        List<PoisonStrategy> strategies = new ArrayList<>();
        EntropyScrambler es = new EntropyScrambler(); es.setEnabled(true);
        DeadCodeInjector dci = new DeadCodeInjector(); dci.setEnabled(true);
        CommentPoisoner cp = new CommentPoisoner(); cp.setEnabled(true);
        StringEncoder se = new StringEncoder(); se.setEnabled(true);
        WhitespaceDisruptor wd = new WhitespaceDisruptor(); wd.setEnabled(true);
        strategies.addAll(List.of(es, dci, cp, se, wd));

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalculator = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, lexer, parser, serializer, entropyCalculator, logService, 0.65
        );
        List<ObfuscationResult> results = engine.process(List.of(sourceFile));

        assertEquals(1, results.size());
        ObfuscationResult r = results.get(0);
        assertTrue(r.getEntropyScore() >= 0.0);
        assertFalse(r.getObfuscatedFile().getObfuscatedLines().isEmpty());
        assertEquals("FullPipelineTest.java", r.getObfuscatedFile().getFileName());
    }

    @Test
    void testEmptyFileListHandledGracefully() {
        List<PoisonStrategy> strategies = List.of(new EntropyScrambler());
        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalc = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, lexer, parser, serializer, entropyCalc, logService, 0.65
        );
        List<ObfuscationResult> results = engine.process(List.of());

        assertTrue(results.isEmpty());
    }

    @Test
    void testPipelineReportsEntropyScore() {
        List<String> lines = List.of(
            "public class EntropyTest {",
            "    private int alpha = 1;",
            "    private int beta = 2;",
            "    public int calc() { return alpha + beta; }",
            "}"
        );
        SourceFile sourceFile = new SourceFile("EntropyTest.java", lines);

        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(new EntropyScrambler());

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalculator = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, lexer, parser, serializer, entropyCalculator, logService, 0.65
        );
        List<ObfuscationResult> results = engine.process(List.of(sourceFile));

        assertFalse(results.isEmpty());
        double score = results.get(0).getEntropyScore();
        assertTrue(score >= 0.0, "Entropy score must be non-negative");
        assertTrue(score <= 1.0, "Entropy score must be capped at 1.0");
    }
}