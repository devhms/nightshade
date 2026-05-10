package com.nightshade.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class CompilationVerifierTest {

    @TempDir
    Path tempDir;

    @Test
    void testValidJavaFile() throws IOException {
        String validCode = """
            public class Test {
                public static void main(String[] args) {
                    System.out.println("Hello");
                }
            }
            """;
        Files.writeString(tempDir.resolve("Test.java"), validCode);

        CompilationVerifier verifier = new CompilationVerifier();
        boolean result = verifier.verify(tempDir.toFile());

        assertTrue(result);
    }

    @Test
    void testInvalidJavaSyntax() throws IOException {
        String invalidCode = """
            public class Test {
                public static void main(String[] args) {
                    System.out.println("Hello"
                }
            }
            """;
        Files.writeString(tempDir.resolve("Test.java"), invalidCode);

        CompilationVerifier verifier = new CompilationVerifier();
        boolean result = verifier.verify(tempDir.toFile());

        assertFalse(result);
    }

    @Test
    void testEmptyDirectory() throws IOException {
        Files.createDirectory(tempDir.resolve("empty"));

        CompilationVerifier verifier = new CompilationVerifier();
        boolean result = verifier.verify(tempDir.resolve("empty").toFile());

        assertTrue(result);
    }

    @Test
    void testNonJavaFilesOnly() throws IOException {
        Files.writeString(tempDir.resolve("script.py"), "print('hello')");
        Files.writeString(tempDir.resolve("app.js"), "console.log('hello');");

        CompilationVerifier verifier = new CompilationVerifier();
        boolean result = verifier.verify(tempDir.toFile());

        assertTrue(result);
    }

    @Test
    void testHasJavaFilesReturnsTrue() throws IOException {
        Files.writeString(tempDir.resolve("Main.java"), "public class Main {}");

        CompilationVerifier verifier = new CompilationVerifier();
        assertTrue(verifier.hasJavaFiles(tempDir.toFile()));
    }

    @Test
    void testHasJavaFilesReturnsFalseForNonJava() throws IOException {
        Files.writeString(tempDir.resolve("script.py"), "print('hello')");

        CompilationVerifier verifier = new CompilationVerifier();
        assertFalse(verifier.hasJavaFiles(tempDir.toFile()));
    }

    @Test
    void testSkipsOutputDirectories() throws IOException {
        Path outputDir = Files.createDirectory(tempDir.resolve("_nightshade_output"));
        Files.writeString(outputDir.resolve("Poisoned.java"), "public class Poisoned {}");

        Path srcDir = Files.createDirectory(tempDir.resolve("src"));
        Files.writeString(srcDir.resolve("Original.java"), "public class Original {}");

        CompilationVerifier verifier = new CompilationVerifier();
        boolean result = verifier.verify(tempDir.toFile());

        assertTrue(result);
    }

    @Test
    void testMultipleJavaFilesAllValid() throws IOException {
        Files.writeString(tempDir.resolve("A.java"), "public class A {}");
        Files.writeString(tempDir.resolve("B.java"), "public class B {}");
        Files.writeString(tempDir.resolve("C.java"), "public class C {}");

        CompilationVerifier verifier = new CompilationVerifier();
        boolean result = verifier.verify(tempDir.toFile());

        assertTrue(result);
    }

    @Test
    void testMultipleJavaFilesOneInvalid() throws IOException {
        Files.writeString(tempDir.resolve("A.java"), "public class A {}");
        Files.writeString(tempDir.resolve("B.java"), "public class B {");
        Files.writeString(tempDir.resolve("C.java"), "public class C {}");

        CompilationVerifier verifier = new CompilationVerifier();
        boolean result = verifier.verify(tempDir.toFile());

        assertFalse(result);
    }

    @Test
    void testNestedDirectories() throws IOException {
        Path nested = Files.createDirectories(tempDir.resolve("com/example"));
        Files.writeString(nested.resolve("App.java"), "package com.example; public class App {}");

        CompilationVerifier verifier = new CompilationVerifier();
        boolean result = verifier.verify(tempDir.toFile());

        assertTrue(result);
    }
}