package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CommentPoisonerTest {

    @Test
    void testReplacesComments() {
        List<String> lines = List.of(
            "// my original comment",
            "int x = 10;"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        SymbolTable symbols = new SymbolTable();
        ASTNode dummyAst = new ASTNode("PROGRAM");

        CommentPoisoner poisoner = new CommentPoisoner();
        ObfuscationResult result = poisoner.apply(source, dummyAst, symbols);

        boolean foundOriginal = false;
        boolean foundCode = false;
        for (String line : result.getObfuscatedFile().getObfuscatedLines()) {
            if (line.contains("my original comment")) {
                foundOriginal = true;
            }
            if (line.contains("int x = 10;")) {
                foundCode = true;
            }
        }
        
        assertFalse(foundOriginal, "Original comment should be removed.");
        assertTrue(foundCode, "Code line should be preserved.");
        assertTrue(result.getCommentsPoisoned() > 0);
    }

    @Test
    void testPreservesNonCommentLines() {
        List<String> lines = List.of(
            "public class Test {",
            "    public static void main(String[] args) {",
            "        System.out.println(\"Hello\");",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        SymbolTable symbols = new SymbolTable();
        ASTNode dummyAst = new ASTNode("PROGRAM");

        CommentPoisoner poisoner = new CommentPoisoner();
        ObfuscationResult result = poisoner.apply(source, dummyAst, symbols);

        assertEquals(lines.size(), result.getObfuscatedFile().getObfuscatedLines().size());
        for (int i = 0; i < lines.size(); i++) {
            assertEquals(lines.get(i), result.getObfuscatedFile().getObfuscatedLines().get(i));
        }
        assertEquals(0, result.getCommentsPoisoned());
    }
}
