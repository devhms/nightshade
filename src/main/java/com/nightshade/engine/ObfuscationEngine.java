package com.nightshade.engine;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import com.nightshade.strategy.PoisonStrategy;
import com.nightshade.util.LogService;

import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full poisoning pipeline for a list of SourceFiles.
 *
 * Pipeline per file:
 *   1. Lex the raw source → token list
 *   2. Parse token list → AST
 *   3. Run each enabled strategy in order → chain of ObfuscationResults
 *   4. Merge per-strategy stats into one final ObfuscationResult
 *   5. Calculate entropy score for the merged result
 *
 * Threading:
 *   - All heavy work runs on the CALLING thread (background task in UI).
 *   - LogService.log() is called here; it marshals to FX thread internally.
 *   - NEVER calls Platform.runLater() directly here — LogService handles it.
 *
 * OOP: STRATEGY pattern — the List<PoisonStrategy> is injected in the
 * constructor, enabling any combination without changing the engine.
 */
public class ObfuscationEngine {

    private final List<PoisonStrategy> strategies;
    private final Lexer lexer;
    private final Parser parser;
    private final Serializer serializer;
    private final EntropyCalculator entropyCalc;
    private final LogService logService;
    private final double entropyThreshold;

    public ObfuscationEngine(List<PoisonStrategy> strategies,
                             Lexer lexer,
                             Parser parser,
                             Serializer serializer,
                             EntropyCalculator entropyCalc,
                             LogService logService,
                             double entropyThreshold) {
        this.strategies    = new ArrayList<>(strategies);
        this.lexer         = lexer;
        this.parser        = parser;
        this.serializer    = serializer;
        this.entropyCalc   = entropyCalc;
        this.logService    = logService;
        this.entropyThreshold = entropyThreshold;
    }

    /**
     * Processes all source files through the enabled strategy pipeline.
     *
     * @param files  Source files discovered by FileWalker
     * @return List of ObfuscationResult (one per file)
     */
    public List<ObfuscationResult> process(List<SourceFile> files) {
        List<ObfuscationResult> results = new ArrayList<>();
        SymbolTable symbols = new SymbolTable();

        logService.log("Starting Nightshade poisoning pipeline...");
        logService.log("Session salt: " + symbols.getSessionSalt().substring(0, 8) + "...");
        logService.log("Strategies enabled: " + countEnabled() + "/" + strategies.size());
        logService.log("Files to process: " + files.size());

        for (int i = 0; i < files.size(); i++) {
            SourceFile file = files.get(i);
            logService.log("Processing [" + (i + 1) + "/" + files.size() + "] " + file.getFileName());

            try {
                ObfuscationResult result = processOne(file, symbols);
                results.add(result);
                logService.logSuccess(String.format("Done: %s | entropy=%.3f | renamed=%d dead=%d comments=%d strings=%d",
                    file.getFileName(),
                    result.getEntropyScore(),
                    result.getRenamedIdentifiers(),
                    result.getDeadBlocksInjected(),
                    result.getCommentsPoisoned(),
                    result.getStringsEncoded()));
            } catch (Exception e) {
                logService.logError("Failed to process " + file.getFileName() + ": " + e.getMessage());
                // Non-fatal — include an unchanged result so file is still written
                ObfuscationResult unchanged = new ObfuscationResult(file, file, 0.0);
                results.add(unchanged);
            }
        }

        logService.log("Pipeline complete. " + results.size() + " files processed.");
        return results;
    }

    private ObfuscationResult processOne(SourceFile original, SymbolTable symbols) {
        // Step 1 + 2: Lex + Parse the ORIGINAL source
        var tokens = lexer.tokenize(original.getRawLines());
        var ast    = parser.parse(tokens);

        // Step 3: Chain strategies — each receives the OUTPUT of the previous
        SourceFile current = original;
        List<ObfuscationResult> partialResults = new ArrayList<>();

        for (PoisonStrategy strategy : strategies) {
            if (!strategy.isEnabled()) continue;
            logService.logDebug("  Applying: " + strategy.getName());
            ObfuscationResult partial = strategy.apply(current, ast, symbols);
            partialResults.add(partial);
            // Next strategy operates on the OBFUSCATED output of this one
            current = partial.getObfuscatedFile();

            // Early-exit entropy threshold check
            ObfuscationResult currentMerged = mergeResults(original, current, partialResults);
            double currentEntropy = entropyCalc.calculate(currentMerged);
            if (currentEntropy >= entropyThreshold) {
                logService.logDebug("  [EARLY EXIT] Entropy threshold reached: " + String.format("%.3f", currentEntropy));
                break;
            }
        }

        // Step 4: Merge stats from all partial results into one
        ObfuscationResult merged = mergeResults(original, current, partialResults);

        // Step 5: Calculate final entropy score
        double entropy = entropyCalc.calculate(merged);

        return new ObfuscationResult(original, current, entropy) {{
            setRenamedIdentifiers(merged.getRenamedIdentifiers());
            setDeadBlocksInjected(merged.getDeadBlocksInjected());
            setCommentsPoisoned(merged.getCommentsPoisoned());
            setStringsEncoded(merged.getStringsEncoded());
            setWhitespaceChanges(merged.getWhitespaceChanges());
            setTotalIdentifiers(merged.getTotalIdentifiers());
            setTotalMethods(merged.getTotalMethods());
            setTotalComments(merged.getTotalComments());
        }};
    }

    private ObfuscationResult mergeResults(SourceFile original, SourceFile finalOutput,
                                            List<ObfuscationResult> partials) {
        ObfuscationResult merged = new ObfuscationResult(original, finalOutput, 0.0);
        for (ObfuscationResult p : partials) {
            merged.setRenamedIdentifiers(merged.getRenamedIdentifiers()   + p.getRenamedIdentifiers());
            merged.setDeadBlocksInjected(merged.getDeadBlocksInjected()  + p.getDeadBlocksInjected());
            merged.setCommentsPoisoned(merged.getCommentsPoisoned()       + p.getCommentsPoisoned());
            merged.setStringsEncoded(merged.getStringsEncoded()           + p.getStringsEncoded());
            merged.setWhitespaceChanges(merged.getWhitespaceChanges()     + p.getWhitespaceChanges());
            // Take max for totals (they're counted per-file, so summing would double-count)
            merged.setTotalIdentifiers(Math.max(merged.getTotalIdentifiers(), p.getTotalIdentifiers()));
            merged.setTotalMethods(Math.max(merged.getTotalMethods(),         p.getTotalMethods()));
            merged.setTotalComments(Math.max(merged.getTotalComments(),       p.getTotalComments()));
        }
        return merged;
    }

    private long countEnabled() {
        return strategies.stream().filter(PoisonStrategy::isEnabled).count();
    }

    public List<PoisonStrategy> getStrategies() {
        return strategies;
    }
}
