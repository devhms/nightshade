package com.nightshade.util;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void testWriteRunLog() throws IOException {
        Path outputDir = tempDir.resolve("output");

        SourceFile original = new SourceFile("/test/Test.java", List.of("public class Test {}"));
        SourceFile modified = new SourceFile("/test/Test.java", List.of("public class Test {}"));

        ObfuscationResult r1 = new ObfuscationResult(original, modified, 0.75);
        r1.setRenamedIdentifiers(5);
        r1.setDeadBlocksInjected(2);
        r1.setCommentsPoisoned(3);
        r1.setStringsEncoded(1);

        ObfuscationResult r2 = new ObfuscationResult(original, modified, 0.60);
        r2.setRenamedIdentifiers(3);
        r2.setDeadBlocksInjected(1);
        r2.setCommentsPoisoned(2);
        r2.setStringsEncoded(0);

        FileUtil util = new FileUtil();
        util.writeRunLog(List.of(r1, r2), outputDir.toFile());

        File logFile = outputDir.resolve("nightshade_run.log").toFile();
        assertTrue(logFile.exists());

        String content = Files.readString(logFile.toPath());
        assertTrue(content.contains("Nightshade v3.5.0"));
        assertTrue(content.contains("Test.java"));
        assertTrue(content.contains("TOTAL"));
    }

    @Test
    void testAppendLog() throws IOException {
        Path outputDir = tempDir.resolve("output");

        FileUtil util = new FileUtil();
        util.appendLog(outputDir.toFile(), "INFO", "Test message");

        File logFile = outputDir.resolve("nightshade_run.log").toFile();
        assertTrue(logFile.exists());

        String content = Files.readString(logFile.toPath());
        assertTrue(content.contains("Test message"));
        assertTrue(content.contains("[INFO]"));
    }

    @Test
    void testWriteCreatesDirectoryStructure() throws IOException {
        Path inputDir = Files.createDirectory(tempDir.resolve("input"));
        Files.writeString(inputDir.resolve("Test.java"), "public class Test {}");

        Path outputDir = tempDir.resolve("output");

        SourceFile original = new SourceFile(inputDir.resolve("Test.java").toAbsolutePath().toString(),
            List.of("public class Test {}"));
        SourceFile modified = new SourceFile(inputDir.resolve("Test.java").toAbsolutePath().toString(),
            List.of("public class Test { }"));

        ObfuscationResult result = new ObfuscationResult(original, modified, 0.5);
        result.setRenamedIdentifiers(0);

        FileUtil util = new FileUtil();
        util.write(result, inputDir.toFile(), outputDir.toFile());

        assertTrue(Files.exists(outputDir.resolve("Test.java")));
        String written = Files.readString(outputDir.resolve("Test.java"));
        assertTrue(written.contains("public class Test { }"));
    }

    @Test
    void testMultipleWriteOperations() throws IOException {
        Path inputDir = Files.createDirectory(tempDir.resolve("input"));
        Path outputDir = tempDir.resolve("output");

        SourceFile original1 = new SourceFile(inputDir.resolve("A.java").toAbsolutePath().toString(),
            List.of("public class A {}"));
        SourceFile modified1 = new SourceFile(inputDir.resolve("A.java").toAbsolutePath().toString(),
            List.of("public class A {}"));
        ObfuscationResult result1 = new ObfuscationResult(original1, modified1, 0.5);

        SourceFile original2 = new SourceFile(inputDir.resolve("B.java").toAbsolutePath().toString(),
            List.of("public class B {}"));
        SourceFile modified2 = new SourceFile(inputDir.resolve("B.java").toAbsolutePath().toString(),
            List.of("public class B {}"));
        ObfuscationResult result2 = new ObfuscationResult(original2, modified2, 0.6);

        FileUtil util = new FileUtil();
        util.write(result1, inputDir.toFile(), outputDir.toFile());
        util.write(result2, inputDir.toFile(), outputDir.toFile());

        assertTrue(Files.exists(outputDir.resolve("A.java")));
        assertTrue(Files.exists(outputDir.resolve("B.java")));
    }
}