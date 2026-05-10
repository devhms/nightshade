package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PoisoningReportTest {

    @Test
    void testGeneratesValidMarkdownTable() {
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult r1 = new ObfuscationResult(original, modified, 0.75);
        r1.setRenamedIdentifiers(5);
        r1.setDeadBlocksInjected(2);
        r1.setCommentsPoisoned(3);
        r1.setStringsEncoded(1);
        r1.setWhitespaceChanges(2);

        String report = PoisoningReport.generate(List.of(r1));

        assertTrue(report.contains("# Nightshade Poisoning Report"));
        assertTrue(report.contains("## Summary"));
        assertTrue(report.contains("| Metric | Value |"));
        assertTrue(report.contains("| Files Processed | 1 |"));
        assertTrue(report.contains("| Identifiers Renamed | 5 |"));
        assertTrue(report.contains("| Avg Entropy Score | 0.750 |"));
    }

    @Test
    void testHandlesZeroResultsWithoutDivisionByZero() {
        String report = PoisoningReport.generate(List.of());

        assertTrue(report.contains("# Nightshade Poisoning Report"));
        assertTrue(report.contains("| Files Processed | 0 |"));
        assertTrue(report.contains("| Avg Entropy Score | 0.000 |"));
        assertTrue(report.contains("| Files Above Threshold | 0/0 (0%) |"));
    }

    @Test
    void testPerFileBreakdown() {
        SourceFile original1 = new SourceFile("A.java", List.of("class A {}"));
        SourceFile modified1 = new SourceFile("A.java", List.of("class A {}"));
        ObfuscationResult r1 = new ObfuscationResult(original1, modified1, 0.80);
        r1.setRenamedIdentifiers(3);
        r1.setDeadBlocksInjected(1);
        r1.setCommentsPoisoned(2);
        r1.setStringsEncoded(0);

        SourceFile original2 = new SourceFile("B.java", List.of("class B {}"));
        SourceFile modified2 = new SourceFile("B.java", List.of("class B {}"));
        ObfuscationResult r2 = new ObfuscationResult(original2, modified2, 0.60);
        r2.setRenamedIdentifiers(2);
        r2.setDeadBlocksInjected(0);
        r2.setCommentsPoisoned(1);
        r2.setStringsEncoded(1);

        String report = PoisoningReport.generate(List.of(r1, r2));

        assertTrue(report.contains("## Per-File Breakdown"));
        assertTrue(report.contains("| File | Entropy | Renamed | Dead | Comments | Strings |"));
        assertTrue(report.contains("| A.java |"));
        assertTrue(report.contains("| B.java |"));
    }

    @Test
    void testMIResistanceEstimate() {
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult r = new ObfuscationResult(original, modified, 0.70);
        r.setRenamedIdentifiers(10);
        r.setDeadBlocksInjected(5);
        r.setCommentsPoisoned(4);
        r.setStringsEncoded(3);

        String report = PoisoningReport.generate(List.of(r));

        assertTrue(report.contains("## Estimated MI Resistance"));
        assertTrue(report.contains("arXiv:2512.15468"));
        assertTrue(report.contains("resistance:"));
    }

    @Test
    void testMIResistanceScalesWithResults() {
        SourceFile original = new SourceFile("Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("Test.java", List.of("public class Test {}"));

        ObfuscationResult r = new ObfuscationResult(original, modified, 0.5);
        r.setRenamedIdentifiers(1);
        r.setDeadBlocksInjected(0);
        r.setCommentsPoisoned(0);
        r.setStringsEncoded(0);

        String report = PoisoningReport.generate(List.of(r));

        assertTrue(report.contains("resistance:"));
    }

    @Test
    void testAverageEntropyCalculation() {
        SourceFile original1 = new SourceFile("A.java", List.of("class A {}"));
        SourceFile modified1 = new SourceFile("A.java", List.of("class A {}"));
        ObfuscationResult r1 = new ObfuscationResult(original1, modified1, 0.80);

        SourceFile original2 = new SourceFile("B.java", List.of("class B {}"));
        SourceFile modified2 = new SourceFile("B.java", List.of("class B {}"));
        ObfuscationResult r2 = new ObfuscationResult(original2, modified2, 0.60);

        String report = PoisoningReport.generate(List.of(r1, r2));

        assertTrue(report.contains("| Avg Entropy Score | 0.700 |"));
    }
}