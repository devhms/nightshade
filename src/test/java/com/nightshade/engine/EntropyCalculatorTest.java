package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EntropyCalculatorTest {

    @Test
    void testSafeDivideReturnsZeroWhenDenominatorIsZero() {
        EntropyCalculator calculator = new EntropyCalculator();

        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(5);
        result.setTotalIdentifiers(0);
        result.setDeadBlocksInjected(0);
        result.setTotalMethods(0);
        result.setCommentsPoisoned(0);
        result.setTotalComments(0);

        double score = calculator.calculate(result);

        assertEquals(0.0, score, 0.001);
    }

    @Test
    void testScoreIsClampedToOne() {
        EntropyCalculator calculator = new EntropyCalculator();

        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(100);
        result.setTotalIdentifiers(10);
        result.setDeadBlocksInjected(100);
        result.setTotalMethods(5);
        result.setCommentsPoisoned(100);
        result.setTotalComments(5);

        double score = calculator.calculate(result);

        assertTrue(score <= 1.0);
        assertEquals(1.0, score, 0.001);
    }

    @Test
    void testScoreIsClampedToMinimumZero() {
        EntropyCalculator calculator = new EntropyCalculator();

        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(0);
        result.setTotalIdentifiers(10);
        result.setDeadBlocksInjected(0);
        result.setTotalMethods(5);
        result.setCommentsPoisoned(0);
        result.setTotalComments(5);

        double score = calculator.calculate(result);

        assertTrue(score >= 0.0);
    }

    @Test
    void testWeightsSumToOne() {
        EntropyCalculator calculator = new EntropyCalculator();

        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(10);
        result.setTotalIdentifiers(10);
        result.setDeadBlocksInjected(10);
        result.setTotalMethods(10);
        result.setCommentsPoisoned(10);
        result.setTotalComments(10);

        double score = calculator.calculate(result);

        double expected = 0.5 + 0.3 + 0.2;
        assertEquals(expected, score, 0.001);
    }

    @Test
    void testStringEncodingBonus() {
        EntropyCalculator calculator = new EntropyCalculator();

        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult resultWithoutBonus = new ObfuscationResult(original, modified, 0.0);
        resultWithoutBonus.setRenamedIdentifiers(0);
        resultWithoutBonus.setTotalIdentifiers(10);
        resultWithoutBonus.setDeadBlocksInjected(0);
        resultWithoutBonus.setTotalMethods(1);
        resultWithoutBonus.setCommentsPoisoned(0);
        resultWithoutBonus.setTotalComments(1);
        resultWithoutBonus.setStringsEncoded(0);
        resultWithoutBonus.setWhitespaceChanges(0);

        double scoreWithoutBonus = calculator.calculate(resultWithoutBonus);

        ObfuscationResult resultWithBonus = new ObfuscationResult(original, modified, 0.0);
        resultWithBonus.setRenamedIdentifiers(0);
        resultWithBonus.setTotalIdentifiers(10);
        resultWithBonus.setDeadBlocksInjected(0);
        resultWithBonus.setTotalMethods(1);
        resultWithBonus.setCommentsPoisoned(0);
        resultWithBonus.setTotalComments(1);
        resultWithBonus.setStringsEncoded(1);
        resultWithBonus.setWhitespaceChanges(0);

        double scoreWithBonus = calculator.calculate(resultWithBonus);

        assertTrue(scoreWithBonus > scoreWithoutBonus);
        assertEquals(0.05, scoreWithBonus - scoreWithoutBonus, 0.001);
    }

    @Test
    void testWhitespaceChangesBonus() {
        EntropyCalculator calculator = new EntropyCalculator();

        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult resultWithoutBonus = new ObfuscationResult(original, modified, 0.0);
        resultWithoutBonus.setRenamedIdentifiers(0);
        resultWithoutBonus.setTotalIdentifiers(10);
        resultWithoutBonus.setDeadBlocksInjected(0);
        resultWithoutBonus.setTotalMethods(1);
        resultWithoutBonus.setCommentsPoisoned(0);
        resultWithoutBonus.setTotalComments(1);
        resultWithoutBonus.setStringsEncoded(0);
        resultWithoutBonus.setWhitespaceChanges(0);

        double scoreWithoutBonus = calculator.calculate(resultWithoutBonus);

        ObfuscationResult resultWithBonus = new ObfuscationResult(original, modified, 0.0);
        resultWithBonus.setRenamedIdentifiers(0);
        resultWithBonus.setTotalIdentifiers(10);
        resultWithBonus.setDeadBlocksInjected(0);
        resultWithBonus.setTotalMethods(1);
        resultWithBonus.setCommentsPoisoned(0);
        resultWithBonus.setTotalComments(1);
        resultWithBonus.setStringsEncoded(0);
        resultWithBonus.setWhitespaceChanges(1);

        double scoreWithBonus = calculator.calculate(resultWithBonus);

        assertTrue(scoreWithBonus > scoreWithoutBonus);
        assertEquals(0.05, scoreWithBonus - scoreWithoutBonus, 0.001);
    }

    @Test
    void testBothBonusesApplied() {
        EntropyCalculator calculator = new EntropyCalculator();

        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(0);
        result.setTotalIdentifiers(10);
        result.setDeadBlocksInjected(0);
        result.setTotalMethods(1);
        result.setCommentsPoisoned(0);
        result.setTotalComments(1);
        result.setStringsEncoded(1);
        result.setWhitespaceChanges(1);

        double score = calculator.calculate(result);

        assertEquals(0.1, score, 0.001);
    }

    @Test
    void testVariableRenamingIsHighestWeighted() {
        EntropyCalculator calculator = new EntropyCalculator();

        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.0);
        result.setRenamedIdentifiers(10);
        result.setTotalIdentifiers(10);
        result.setDeadBlocksInjected(10);
        result.setTotalMethods(10);
        result.setCommentsPoisoned(10);
        result.setTotalComments(10);

        double score = calculator.calculate(result);

        double expected = (10.0/10.0 * 0.5) + (10.0/10.0 * 0.3) + (10.0/10.0 * 0.2);
        assertEquals(expected, score, 0.001);
    }
}