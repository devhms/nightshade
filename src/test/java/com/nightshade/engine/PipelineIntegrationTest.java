package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.strategy.*;
import org.junit.jupiter.api.Test;
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

        ObfuscationEngine engine = new ObfuscationEngine(strategies, lexer, parser, serializer, entropyCalculator, logService, 0.65);
        List<ObfuscationResult> results = engine.process(files);

        assertFalse(results.isEmpty());
        ObfuscationResult result = results.get(0);

        assertTrue(result.getEntropyScore() > 0.0);
        
        boolean isDifferent = false;
        List<String> obfuscated = result.getObfuscatedFile().getObfuscatedLines();
        
        // Ensure lines have changed due to formatting, poisoning, etc.
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
}
