package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import com.nightshade.model.Token;
import com.nightshade.engine.Lexer;
import com.nightshade.engine.Parser;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EntropyScramblerTest {

    @Test
    void testRenamesUserDefinedVariables() {
        List<String> lines = List.of(
            "public class Test {",
            "    int counter = 0;",
            "    int result = counter + 1;",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(lines);
        Parser parser = new Parser();
        ASTNode ast = parser.parse(tokens);
        SymbolTable symbols = new SymbolTable();

        EntropyScrambler scrambler = new EntropyScrambler();
        ObfuscationResult result = scrambler.apply(source, ast, symbols);

        assertTrue(result.getRenamedIdentifiers() > 0);
        
        boolean foundCounter = false;
        boolean foundResult = false;
        for (String line : result.getObfuscatedFile().getObfuscatedLines()) {
            if (line.contains("counter")) foundCounter = true;
            if (line.contains("result")) foundResult = true;
        }
        assertFalse(foundCounter);
        assertFalse(foundResult);
    }

    @Test
    void testDoesNotRenameKeywords() {
        List<String> lines = List.of(
            "public class Test {",
            "    int counter = 0;",
            "    int result = counter + 1;",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(lines);
        Parser parser = new Parser();
        ASTNode ast = parser.parse(tokens);
        SymbolTable symbols = new SymbolTable();

        EntropyScrambler scrambler = new EntropyScrambler();
        ObfuscationResult result = scrambler.apply(source, ast, symbols);

        boolean foundPublic = false, foundClass = false, foundInt = false;
        for (String line : result.getObfuscatedFile().getObfuscatedLines()) {
            if (line.contains("public")) foundPublic = true;
            if (line.contains("class")) foundClass = true;
            if (line.contains("int ")) foundInt = true;
        }
        assertTrue(foundPublic);
        assertTrue(foundClass);
        assertTrue(foundInt);
    }

    @Test
    void testDoesNotRenameClassNames() {
        List<String> lines = List.of(
            "public class Test {",
            "    int counter = 0;",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        Lexer lexer = new Lexer();
        List<Token> tokens = lexer.tokenize(lines);
        Parser parser = new Parser();
        ASTNode ast = parser.parse(tokens);
        SymbolTable symbols = new SymbolTable();

        EntropyScrambler scrambler = new EntropyScrambler();
        ObfuscationResult result = scrambler.apply(source, ast, symbols);

        boolean foundTest = false;
        for (String line : result.getObfuscatedFile().getObfuscatedLines()) {
            if (line.contains("Test")) foundTest = true;
        }
        assertTrue(foundTest);
    }
}
