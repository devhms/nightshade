package com.nightshade.strategy;

import com.nightshade.model.SourceFile;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ControlFlowFlattenerTest {

    private ControlFlowFlattener flattener;
    private SymbolTable symbols;

    @BeforeEach
    void setUp() {
        flattener = new ControlFlowFlattener();
        flattener.setEnabled(true);
        symbols = new SymbolTable();
    }

    @Test
    void testFlattensPrivateMethod() {
        List<String> lines = Arrays.asList(
            "public class TestClass {",
            "    private int calculate(int x) {",
            "        int a = x + 1;",
            "        int b = a * 2;",
            "        return b - 3;",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("TestClass.java", lines);
        
        ObfuscationResult result = flattener.apply(source, null, symbols);
        List<String> out = result.getObfuscatedFile().getObfuscatedLines();
        
        String joined = String.join("\n", out);
        assertTrue(joined.contains("while (_ns_state != -1)"), "Should contain while loop");
        assertTrue(joined.contains("switch (_ns_state)"), "Should contain switch statement");
        assertTrue(joined.contains("case 0: int a = x + 1;"), "Should contain mapped case statements");
        assertTrue(joined.contains("return b - 3;"), "Should contain return outside loop");
        
        // Ensure sequential structure is broken
        assertFalse(joined.contains("int a = x + 1;\n        int b = a * 2;"), "Sequential statements should be broken up");
    }

    @Test
    void testSkipsPublicMethods() {
        List<String> lines = Arrays.asList(
            "public class TestClass {",
            "    public int calculate(int x) {",
            "        int a = x + 1;",
            "        int b = a * 2;",
            "        return b - 3;",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("TestClass.java", lines);
        
        ObfuscationResult result = flattener.apply(source, null, symbols);
        List<String> out = result.getObfuscatedFile().getObfuscatedLines();
        
        String joined = String.join("\n", out);
        assertFalse(joined.contains("switch (_ns_state)"), "Public method should not be flattened");
        assertEquals(lines, out, "Output should be identical to input");
    }

    @Test
    void testSkipsShortMethods() {
        List<String> lines = Arrays.asList(
            "public class TestClass {",
            "    private void log(String msg) {",
            "        System.out.println(msg);",
            "    }",
            "}"
        );
        SourceFile source = new SourceFile("TestClass.java", lines);
        
        ObfuscationResult result = flattener.apply(source, null, symbols);
        List<String> out = result.getObfuscatedFile().getObfuscatedLines();
        
        String joined = String.join("\n", out);
        assertFalse(joined.contains("switch (_ns_state)"), "Short method should not be flattened");
        assertEquals(lines, out, "Output should be identical to input");
    }
}
