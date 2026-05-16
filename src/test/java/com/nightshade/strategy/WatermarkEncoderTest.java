package com.nightshade.strategy;

import com.nightshade.model.SourceFile;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WatermarkEncoderTest {

    private WatermarkEncoder encoder;
    private SymbolTable symbols;

    @BeforeEach
    void setUp() {
        encoder = new WatermarkEncoder();
        encoder.setEnabled(true);
        encoder.setAuthorId("test-author");
        symbols = new SymbolTable();
    }

    @Test
    void testEmbedsWatermarkBits() {
        List<String> lines = new ArrayList<>();
        lines.add("public class TestClass {");
        for (int i = 0; i < 20; i++) {
            lines.add("    private int val" + i + " = " + i + ";");
        }
        lines.add("}");
        
        SourceFile source = new SourceFile("TestClass.java", lines);
        ObfuscationResult result = encoder.apply(source, null, symbols);
        
        List<String> out = result.getObfuscatedFile().getObfuscatedLines();
        
        int tabCount = 0;
        for (String line : out) {
            if (line.startsWith("\t")) {
                tabCount++;
            }
        }
        
        assertTrue(tabCount > 0, "Watermark should have embedded at least one tab character");
        assertTrue(result.getWhitespaceChanges() > 0, "Obfuscation result should track whitespace changes");
    }

    @Test
    void testWatermarkIsInvisible() {
        List<String> lines = new ArrayList<>();
        lines.add("public class TestClass {");
        for (int i = 0; i < 20; i++) {
            lines.add("    private int val" + i + " = " + i + ";");
        }
        lines.add("}");
        
        SourceFile source = new SourceFile("TestClass.java", lines);
        ObfuscationResult result = encoder.apply(source, null, symbols);
        
        List<String> out = result.getObfuscatedFile().getObfuscatedLines();
        
        for (int i = 0; i < lines.size(); i++) {
            String originalTrimmed = lines.get(i).trim();
            String outTrimmed = out.get(i).replace("\t", "    ").trim();
            assertEquals(originalTrimmed, outTrimmed, "Content should be identical after normalizing watermark");
        }
    }
}
