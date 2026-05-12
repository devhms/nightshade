package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.strategy.DeadCodeInjector;
import com.nightshade.strategy.EntropyScrambler;
import com.nightshade.strategy.PoisonStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AstDriftTest {

    @Test
    void reparseAfterLineChangePreventsDrift() {
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
        List<SourceFile> files = new ArrayList<>();
        files.add(sourceFile);

        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(new DeadCodeInjector());
        strategies.add(new EntropyScrambler());

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalculator = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(strategies, lexer, parser, serializer, entropyCalculator, logService, 1.0);
        List<ObfuscationResult> results = engine.process(files);

        String joined = String.join("\n", results.get(0).getObfuscatedFile().getObfuscatedLines());

        assertAll("drift renaming",
            () -> assertFalse(joined.contains("count++"), "count identifier should be renamed"),
            () -> assertFalse(joined.contains("return count"), "count identifier should be renamed")
        );
    }
}
