package com.nightshade.engine;

import com.nightshade.model.SourceFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class FileWalkerTest {

    @TempDir
    Path tempDir;

    @Test
    void testDirectoryWithMixedExtensions() throws IOException {
        Files.createFile(tempDir.resolve("Test.java"));
        Files.createFile(tempDir.resolve("script.py"));
        Files.createFile(tempDir.resolve("app.js"));
        Files.createFile(tempDir.resolve("readme.txt"));
        Files.createFile(tempDir.resolve("data.json"));

        FileWalker walker = new FileWalker();
        List<SourceFile> files = walker.walk(tempDir.toFile());

        assertEquals(3, files.size());
        assertTrue(files.stream().anyMatch(f -> f.getFileName().equals("Test.java")));
        assertTrue(files.stream().anyMatch(f -> f.getFileName().equals("script.py")));
        assertTrue(files.stream().anyMatch(f -> f.getFileName().equals("app.js")));
        assertFalse(files.stream().anyMatch(f -> f.getFileName().equals("readme.txt")));
    }

    @Test
    void testSingleFileInput() throws IOException {
        Path subDir = Files.createDirectory(tempDir.resolve("src"));
        Files.createFile(subDir.resolve("Hello.java"));

        FileWalker walker = new FileWalker();
        List<SourceFile> files = walker.walk(subDir.resolve("Hello.java").toFile());

        assertEquals(1, files.size());
        assertEquals("Hello.java", files.get(0).getFileName());
    }

    @Test
    void testEmptyDirectory() throws IOException {
        Files.createDirectory(tempDir.resolve("empty"));

        FileWalker walker = new FileWalker();
        List<SourceFile> files = walker.walk(tempDir.resolve("empty").toFile());

        assertTrue(files.isEmpty());
    }

    @Test
    void testNestedDirectoriesWithGitSkipped() throws IOException {
        Path gitDir = Files.createDirectory(tempDir.resolve(".git"));
        Files.createFile(gitDir.resolve("config"));

        Path srcDir = Files.createDirectory(tempDir.resolve("src"));
        Files.createFile(srcDir.resolve("Main.java"));

        Path nodeModules = Files.createDirectory(tempDir.resolve("node_modules"));
        Files.createFile(nodeModules.resolve("package.json"));

        Path target = Files.createDirectory(tempDir.resolve("target"));
        Files.createFile(target.resolve("classes.java"));

        Path pycache = Files.createDirectory(tempDir.resolve("__pycache__"));
        Files.createFile(pycache.resolve("cache.pyc"));

        FileWalker walker = new FileWalker();
        List<SourceFile> files = walker.walk(tempDir.toFile());

        assertEquals(1, files.size());
        assertEquals("Main.java", files.get(0).getFileName());
    }

    @Test
    void testFileWithNoExtensionSkipped() throws IOException {
        Files.createFile(tempDir.resolve("Makefile"));
        Files.createFile(tempDir.resolve("Dockerfile"));
        Files.createFile(tempDir.resolve("Test.java"));

        FileWalker walker = new FileWalker();
        List<SourceFile> files = walker.walk(tempDir.toFile());

        assertEquals(1, files.size());
        assertEquals("Test.java", files.get(0).getFileName());
    }

    @Test
    void testOutputDirectorySkipped() throws IOException {
        Path outputDir = Files.createDirectory(tempDir.resolve("_nightshade_output"));
        Files.createFile(outputDir.resolve("Poisoned.java"));

        Path srcDir = Files.createDirectory(tempDir.resolve("src"));
        Files.createFile(srcDir.resolve("Original.java"));

        FileWalker walker = new FileWalker();
        List<SourceFile> files = walker.walk(tempDir.toFile());

        assertEquals(1, files.size());
        assertEquals("Original.java", files.get(0).getFileName());
    }

    @Test
    void testDeeplyNestedStructure() throws IOException {
        Path level1 = Files.createDirectory(tempDir.resolve("level1"));
        Path level2 = Files.createDirectory(level1.resolve("level2"));
        Path level3 = Files.createDirectory(level2.resolve("level3"));
        Files.createFile(level3.resolve("Deep.java"));

        FileWalker walker = new FileWalker();
        List<SourceFile> files = walker.walk(tempDir.toFile());

        assertEquals(1, files.size());
        assertTrue(files.get(0).getAbsolutePath().contains("Deep.java"));
    }

    @Test
    void testMultipleJavaFilesSortedAlphabetically() throws IOException {
        Files.createFile(tempDir.resolve("Zebra.java"));
        Files.createFile(tempDir.resolve("Alpha.java"));
        Files.createFile(tempDir.resolve("Middle.java"));

        FileWalker walker = new FileWalker();
        List<SourceFile> files = walker.walk(tempDir.toFile());

        assertEquals(3, files.size());
        assertEquals("Alpha.java", files.get(0).getFileName());
        assertEquals("Middle.java", files.get(1).getFileName());
        assertEquals("Zebra.java", files.get(2).getFileName());
    }
}