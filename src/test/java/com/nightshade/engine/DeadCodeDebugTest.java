package com.nightshade.engine;

import com.nightshade.model.*;
import com.nightshade.strategy.*;
import org.junit.jupiter.api.Test;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DeadCodeDebugTest {
    @Test
    void debugDeadCodeOutput() {
        List<String> lines = List.of(
            "public class Main {",
            "    public int calculate() {",
            "        return 0;",
            "    }",
            "}"
        );
        
        System.out.println("=== INPUT LINES ===");
        for (int i = 0; i < lines.size(); i++) {
            System.out.println("  " + i + ": [" + lines.get(i) + "]");
        }
        
        SourceFile source = new SourceFile("Main.java", lines);
        DeadCodeInjector dci = new DeadCodeInjector();
        
        var tokens = new Lexer().tokenize(lines);
        System.out.println("\n=== TOKENS ===");
        for (Token t : tokens) {
            System.out.println("  " + t.getLineNumber() + ": " + t.getType() + " [" + t.getValue() + "]");
        }
        
        var ast = new Parser().parse(tokens);
        var result = dci.apply(source, ast, new SymbolTable());

        System.out.println("\n=== RESULT ===");
        System.out.println("Blocks injected: " + result.getDeadBlocksInjected());
        System.out.println("Total methods: " + result.getTotalMethods());
        for (int i = 0; i < result.getObfuscatedFile().getObfuscatedLines().size(); i++) {
            System.out.printf("%2d: %s%n", i, result.getObfuscatedFile().getObfuscatedLines().get(i));
        }

        assertTrue(result.getDeadBlocksInjected() >= 1, "Should inject at least 1 block");
    }

    @Test
    void debugAstDrift() {
        List<String> lines = List.of(
            "public class Drift {",
            "    int count = 0;",
            "    int next() {",
            "        count++;",
            "        return count;",
            "    }",
            "}"
        );
        SourceFile sf = new SourceFile("Drift.java", lines);
        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(new DeadCodeInjector());
        strategies.add(new EntropyScrambler());

        var engine = new ObfuscationEngine(
            strategies, new Lexer(), new Parser(),
            new Serializer(), new EntropyCalculator(),
            new com.nightshade.util.LogService(), 1.0
        );

        var results = engine.process(List.of(sf));

        assertTrue(results.get(0).getDeadBlocksInjected() >= 1, "Should report at least 1 block");
        String joined = String.join("\n", results.get(0).getObfuscatedFile().getObfuscatedLines());
        assertTrue(joined.contains("v_"), "Should contain v_ dead code variables");
    }

    @Test
    void debugPackagePrivateMethod() {
        List<String> lines = List.of(
            "public class Test {",
            "    int next() {",
            "        return 1;",
            "    }",
            "}"
        );
        SourceFile sf = new SourceFile("Test.java", lines);
        DeadCodeInjector dci = new DeadCodeInjector();
        var result = dci.apply(sf, new ASTNode("PROGRAM"), new SymbolTable());

        String joined = String.join("\n", result.getObfuscatedFile().getObfuscatedLines());
        assertTrue(result.getDeadBlocksInjected() >= 1, "Should inject block for package-private method");
        assertTrue(joined.contains("v_"), "Should contain v_");
        assertTrue(joined.contains("if (false)"), "Should contain if (false) predicate");
    }
}