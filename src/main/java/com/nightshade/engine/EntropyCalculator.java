package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;

/**
 * Calculates an entropy score (0.0 – 1.0) per file representing how
 * thoroughly it has been poisoned.
 *
 * Formula (from spec):
 *   score = (renamedIdentifiers / totalIdentifiers) * 0.5
 *         + (deadBlocksInjected  / totalMethods)     * 0.3
 *         + (poisonedComments    / totalComments)     * 0.2
 *
 * Weights reflect research findings:
 *   - Variable renaming (0.5) is the strongest signal (arXiv:2512.15468)
 *   - Dead code (0.3) is medium — compiler-safe, preprocessing-proof
 *   - Comment poisoning (0.2) weakest — some pipelines strip comments
 *
 * String encoding and whitespace disruption are bonus strategies —
 * they contribute to the clamped final score but don't have dedicated
 * weight slots to preserve the original formula.
 */
public class EntropyCalculator {

    private static final double WEIGHT_A = 0.5;  // variable renaming
    private static final double WEIGHT_B = 0.3;  // dead code
    private static final double WEIGHT_C = 0.2;  // comment poisoning

    /**
     * Calculates the entropy score for a processed file.
     *
     * @param result The ObfuscationResult with stats already populated by strategies
     * @return entropy score clamped to [0.0, 1.0]
     */
    public double calculate(ObfuscationResult result) {
        double scoreA = safeDivide(result.getRenamedIdentifiers(), result.getTotalIdentifiers()) * WEIGHT_A;
        double scoreB = safeDivide(result.getDeadBlocksInjected(), result.getTotalMethods())    * WEIGHT_B;
        double scoreC = safeDivide(result.getCommentsPoisoned(),    result.getTotalComments())   * WEIGHT_C;

        // Bonus from string encoding and whitespace
        double bonus = 0.0;
        if (result.getStringsEncoded() > 0)    bonus += 0.05;
        if (result.getWhitespaceChanges() > 0) bonus += 0.05;

        return Math.max(0.0, Math.min(1.0, scoreA + scoreB + scoreC + bonus));
    }

    private double safeDivide(int numerator, int denominator) {
        if (denominator <= 0) return 0.0;
        return Math.min(1.0, (double) numerator / denominator);
    }
}
