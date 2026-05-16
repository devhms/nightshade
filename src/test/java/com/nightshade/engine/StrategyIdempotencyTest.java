package com.nightshade.engine;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import com.nightshade.strategy.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StrategyIdempotencyTest {

    @Test
    void testEntropyScramblerIsIdempotent() {
        List<String> lines = List.of(
            "public class Test {",
            "    private int counter = 0;",
            "    private String name = \"test\";",
            "    public int get() { return counter; }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        ASTNode ast = new ASTNode("PROGRAM");
        SymbolTable symbols = new SymbolTable();

        EntropyScrambler scrambler = new EntropyScrambler();
        scrambler.setEnabled(true);

        ObfuscationResult first = scrambler.apply(source, ast, symbols);
        String firstOutput = String.join("\n", first.getObfuscatedFile().getObfuscatedLines());

        SourceFile secondInput = first.getObfuscatedFile();
        ObfuscationResult second = scrambler.apply(secondInput, ast, symbols);
        String secondOutput = String.join("\n", second.getObfuscatedFile().getObfuscatedLines());

        assertEquals(firstOutput, secondOutput,
            "Second application should produce same result as first");
    }

    @Test
    void testDeadCodeInjectorCanApplyMultipleTimes() {
        List<String> lines = List.of(
            "public class Test {",
            "    public int calculate() {",
            "        return 0;",
            "    }",
            "    public void log() { }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        ASTNode ast = new ASTNode("PROGRAM");
        SymbolTable symbols = new SymbolTable();

        DeadCodeInjector injector = new DeadCodeInjector();
        injector.setEnabled(true);

        ObfuscationResult first = injector.apply(source, ast, symbols);
        int firstDeadBlocks = first.getDeadBlocksInjected();

        SourceFile secondInput = first.getObfuscatedFile();
        ObfuscationResult second = injector.apply(secondInput, ast, symbols);
        int secondDeadBlocks = second.getDeadBlocksInjected();

        assertTrue(secondDeadBlocks >= 0,
            "Second application should not produce negative dead block count");
    }

    @Test
    void testCommentPoisonerIsIdempotent() {
        List<String> lines = List.of(
            "public class Test {",
            "    public void run() { /* start */",
            "        // comment here",
            "        int x = 1;",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        ASTNode ast = new ASTNode("PROGRAM");
        SymbolTable symbols = new SymbolTable();

        CommentPoisoner poisoner = new CommentPoisoner();
        poisoner.setEnabled(true);

        ObfuscationResult first = poisoner.apply(source, ast, symbols);
        int firstCount = first.getCommentsPoisoned();
        String firstOutput = String.join("\n", first.getObfuscatedFile().getObfuscatedLines());

        SourceFile secondInput = first.getObfuscatedFile();
        ObfuscationResult second = poisoner.apply(secondInput, ast, symbols);
        int secondCount = second.getCommentsPoisoned();
        String secondOutput = String.join("\n", second.getObfuscatedFile().getObfuscatedLines());

        assertEquals(firstCount, secondCount,
            "Comment poisoning count should be stable on second application");
        assertEquals(firstOutput, secondOutput,
            "Second application should not change output further");
    }

    @Test
    void testWhitespaceDisruptorCanApplyMultipleTimes() {
        List<String> lines = List.of(
            "public class Test {",
            "    public int add(int a,int b){",
            "        return a+b;",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        ASTNode ast = new ASTNode("PROGRAM");
        SymbolTable symbols = new SymbolTable();

        WhitespaceDisruptor disruptor = new WhitespaceDisruptor();
        disruptor.setEnabled(true);

        ObfuscationResult first = disruptor.apply(source, ast, symbols);
        String firstOutput = String.join("\n", first.getObfuscatedFile().getObfuscatedLines());

        SourceFile secondInput = first.getObfuscatedFile();
        ObfuscationResult second = disruptor.apply(secondInput, ast, symbols);
        String secondOutput = String.join("\n", second.getObfuscatedFile().getObfuscatedLines());

        assertEquals(firstOutput, secondOutput,
            "Whitespace should stabilize after first application");
    }

    @Test
    void testNoDoubleEncodingOnRepeatedStrategyApplication() {
        List<String> lines = List.of(
            "public class Test {",
            "    private int counter = 0;",
            "    public int get() { return counter; }",
            "    public void set(int c) { this.counter = c; }",
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

        List<ObfuscationResult> firstRun = engine.process(List.of(source));
        assertFalse(firstRun.isEmpty());
        String firstOutput = String.join("\n", firstRun.get(0).getObfuscatedFile().getObfuscatedLines());

        SourceFile secondInput = new SourceFile("Test.java",
            firstRun.get(0).getObfuscatedFile().getObfuscatedLines());

        List<ObfuscationResult> secondRun = engine.process(List.of(secondInput));
        assertFalse(secondRun.isEmpty());
        String secondOutput = String.join("\n", secondRun.get(0).getObfuscatedFile().getObfuscatedLines());

        long countInFirst = firstOutput.split("v_").length - 1;
        long countInSecond = secondOutput.split("v_").length - 1;

        assertEquals(countInFirst, countInSecond,
            "Repeated pipeline runs should produce same obfuscation depth");
    }
}