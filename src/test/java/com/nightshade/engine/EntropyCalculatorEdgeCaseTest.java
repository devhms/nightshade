package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EntropyCalculatorEdgeCaseTest {

    @Test
    void testAllZerosReturnsZero() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(0); result.setTotalIdentifiers(0);
        result.setDeadBlocksInjected(0); result.setTotalMethods(0);
        result.setCommentsPoisoned(0);   result.setTotalComments(0);
        result.setStringsEncoded(0);     result.setWhitespaceChanges(0);

        double score = calc.calculate(result);
        assertEquals(0.0, score, 0.001);
    }

    @Test
    void testZeroDenominatorsHandledGracefully() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(5); result.setTotalIdentifiers(0);
        result.setDeadBlocksInjected(3); result.setTotalMethods(0);
        result.setCommentsPoisoned(2);  result.setTotalComments(0);
        result.setStringsEncoded(1);     result.setWhitespaceChanges(1);

        double score = calc.calculate(result);
        assertTrue(score >= 0.0 && score <= 1.0,
            "Score should be clamped even with zero denominators, got: " + score);
    }

    @Test
    void testNegativeValuesHandledGracefully() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(-1);
        result.setTotalIdentifiers(10);
        result.setDeadBlocksInjected(-1);
        result.setTotalMethods(5);
        result.setCommentsPoisoned(-1);
        result.setTotalComments(5);
        result.setStringsEncoded(0);
        result.setWhitespaceChanges(0);

        double score = calc.calculate(result);
        assertTrue(score >= 0.0, "Score should not be negative with negative numerators");
    }

    @Test
    void testVeryLargeValuesClamped() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(Integer.MAX_VALUE);
        result.setTotalIdentifiers(1);
        result.setDeadBlocksInjected(Integer.MAX_VALUE);
        result.setTotalMethods(1);
        result.setCommentsPoisoned(Integer.MAX_VALUE);
        result.setTotalComments(1);
        result.setStringsEncoded(1);
        result.setWhitespaceChanges(1);

        double score = calc.calculate(result);
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void testStringEncodingBonusOnlyWhenPositive() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult base = new ObfuscationResult(original, modified, 0.0);
        base.setRenamedIdentifiers(0); base.setTotalIdentifiers(10);
        base.setDeadBlocksInjected(0); base.setTotalMethods(1);
        base.setCommentsPoisoned(0);   base.setTotalComments(1);
        base.setStringsEncoded(0);     base.setWhitespaceChanges(0);
        double baseScore = calc.calculate(base);

        ObfuscationResult withString = new ObfuscationResult(original, modified, 0.0);
        withString.setRenamedIdentifiers(0); withString.setTotalIdentifiers(10);
        withString.setDeadBlocksInjected(0); withString.setTotalMethods(1);
        withString.setCommentsPoisoned(0);   withString.setTotalComments(1);
        withString.setStringsEncoded(5);     withString.setWhitespaceChanges(0);
        double withStringScore = calc.calculate(withString);

        assertEquals(0.05, withStringScore - baseScore, 0.001,
            "String encoding bonus should be exactly 0.05 per encoding");
    }

    @Test
    void testWhitespaceBonusOnlyWhenPositive() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult base = new ObfuscationResult(original, modified, 0.0);
        base.setRenamedIdentifiers(0); base.setTotalIdentifiers(10);
        base.setDeadBlocksInjected(0); base.setTotalMethods(1);
        base.setCommentsPoisoned(0);   base.setTotalComments(1);
        base.setStringsEncoded(0);     base.setWhitespaceChanges(0);
        double baseScore = calc.calculate(base);

        ObfuscationResult withWs = new ObfuscationResult(original, modified, 0.0);
        withWs.setRenamedIdentifiers(0); withWs.setTotalIdentifiers(10);
        withWs.setDeadBlocksInjected(0); withWs.setTotalMethods(1);
        withWs.setCommentsPoisoned(0);   withWs.setTotalComments(1);
        withWs.setStringsEncoded(0);     withWs.setWhitespaceChanges(3);
        double withWsScore = calc.calculate(withWs);

        assertEquals(0.05, withWsScore - baseScore, 0.001,
            "Whitespace bonus should be exactly 0.05 total, not per change");
    }

    @Test
    void testMaximumBonusesCapped() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(10); result.setTotalIdentifiers(10);
        result.setDeadBlocksInjected(10); result.setTotalMethods(10);
        result.setCommentsPoisoned(10);   result.setTotalComments(10);
        result.setStringsEncoded(9999);   result.setWhitespaceChanges(9999);

        double score = calc.calculate(result);
        assertEquals(1.0, score, 0.001,
            "Score with max bonuses + max ratios should be capped at 1.0");
    }

    @Test
    void testPartialRatiosWeighted() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(5);   result.setTotalIdentifiers(10);
        result.setDeadBlocksInjected(5);  result.setTotalMethods(10);
        result.setCommentsPoisoned(5);    result.setTotalComments(10);
        result.setStringsEncoded(0);      result.setWhitespaceChanges(0);

        double score = calc.calculate(result);
        double expected = (0.5 * 0.5) + (0.5 * 0.3) + (0.5 * 0.2);
        assertEquals(expected, score, 0.001,
            "50% coverage should yield 0.5 * (0.5+0.3+0.2) = 0.5");
    }

    @Test
    void testIdenticalObjectsProduceSameScore() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult r1 = new ObfuscationResult(original, modified, 0.0);
        r1.setRenamedIdentifiers(3); r1.setTotalIdentifiers(5);
        r1.setDeadBlocksInjected(2); r1.setTotalMethods(4);
        r1.setCommentsPoisoned(1);   r1.setTotalComments(3);
        r1.setStringsEncoded(1);     r1.setWhitespaceChanges(1);

        ObfuscationResult r2 = new ObfuscationResult(original, modified, 0.0);
        r2.setRenamedIdentifiers(3); r2.setTotalIdentifiers(5);
        r2.setDeadBlocksInjected(2); r2.setTotalMethods(4);
        r2.setCommentsPoisoned(1);   r2.setTotalComments(3);
        r2.setStringsEncoded(1);     r2.setWhitespaceChanges(1);

        double score1 = calc.calculate(r1);
        double score2 = calc.calculate(r2);
        assertEquals(score1, score2, 0.001,
            "Identical results must produce identical scores");
    }

    @Test
    void testTotalIdentifiersZeroWithRenamedNonZero() {
        EntropyCalculator calc = new EntropyCalculator();
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(5);
        result.setTotalIdentifiers(0);
        result.setDeadBlocksInjected(0); result.setTotalMethods(1);
        result.setCommentsPoisoned(0);   result.setTotalComments(1);
        result.setStringsEncoded(0);     result.setWhitespaceChanges(0);

        double score = calc.calculate(result);
        assertTrue(score >= 0.0 && score <= 1.0,
            "Should handle zero totalIdentifiers gracefully");
    }
}