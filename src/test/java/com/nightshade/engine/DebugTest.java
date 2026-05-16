package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.strategy.*;
import java.util.*;

public class DebugTest {
    public static void main(String[] args) {
        List<String> lines = List.of(
            "public class Drift {",
            "    int count = 0;",
            "    int next() {",
            "        count++;",
            "        return count;",
            "    }",
            "}"
        );
        SourceFile sourceFile = new SourceFile("Drift.java", lines);
        
        // Test DeadCodeInjector directly
        DeadCodeInjector dci = new DeadCodeInjector();
        var tokens = new Lexer().tokenize(lines);
        var ast = new Parser().parse(tokens);
        var result = dci.apply(sourceFile, ast, new com.nightshade.model.SymbolTable());
        
        System.out.println("=== DeadCodeInjector result ===");
        System.out.println("Blocks injected: " + result.getDeadBlocksInjected());
        System.out.println("Total methods: " + result.getTotalMethods());
        for (String l : result.getObfuscatedFile().getObfuscatedLines()) {
            System.out.println(l);
        }
        
        // Now test the full pipeline
        System.out.println("\n=== Full pipeline output ===");
        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(dci);
        strategies.add(new EntropyScrambler());
        
        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, new Lexer(), new Parser(), new Serializer(),
            new EntropyCalculator(), new com.nightshade.util.LogService(), 1.0
        );
        
        var results = engine.process(List.of(sourceFile));
        String joined = String.join("\n", results.get(0).getObfuscatedFile().getObfuscatedLines());
        System.out.println(joined);
        System.out.println("\nContains v_? " + joined.contains("v_"));
        System.out.println("Renamed count: " + results.get(0).getRenamedIdentifiers());
        System.out.println("Total idents: " + results.get(0).getTotalIdentifiers());
    }
}