package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DeadCodeInjectorTest {

    @Test
    void testInjectsDeadBlocks() {
        List<String> lines = List.of(
            "public class Main {",
            "    public int calculate() {",
            "        return 0;",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("Main.java", lines);
        SymbolTable symbols = new SymbolTable();
        ASTNode dummyAst = new ASTNode("PROGRAM");

        DeadCodeInjector injector = new DeadCodeInjector();
        ObfuscationResult result = injector.apply(source, dummyAst, symbols);

        assertTrue(result.getDeadBlocksInjected() >= 1);
        
        boolean foundDeadCodePredicate = false;
        for (String line : result.getObfuscatedFile().getObfuscatedLines()) {
            // Note: After Phase 2, this will check for opaque predicates.
            // Currently checks for "if (false)" or other opaque predicates we might inject
            if (line.contains("if (false)") || line.contains("if (") && line.contains(")")) {
                foundDeadCodePredicate = true;
            }
        }
        assertTrue(foundDeadCodePredicate);
    }

    @Test
    void testDeadBlocksAreContextual() {
        List<String> lines = List.of(
            "public class Reader {",
            "    public int readFile() {",
            "        File f = new File(\"test\");",
            "        InputStream stream = null;",
            "        return 1;",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("Reader.java", lines);
        SymbolTable symbols = new SymbolTable();
        ASTNode dummyAst = new ASTNode("PROGRAM");

        DeadCodeInjector injector = new DeadCodeInjector();
        ObfuscationResult result = injector.apply(source, dummyAst, symbols);

        // Domain 0 is filesystem/io. The injector should pick something else.
        boolean foundFsDomain = false;
        for (String line : result.getObfuscatedFile().getObfuscatedLines()) {
            if (line.contains("[FS]")) {
                foundFsDomain = true;
            }
        }
        assertFalse(foundFsDomain, "Should not inject domain 0 (FS) into a file that already has FS context.");
    }
}
