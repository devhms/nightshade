package com.nightshade;

import com.nightshade.strategy.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CLITest {

    @Test
    void testAllStrategiesExist() {
        PoisonStrategy entropy = new EntropyScrambler();
        assertEquals("Variable Entropy Scrambling", entropy.getName());

        PoisonStrategy deadcode = new DeadCodeInjector();
        assertEquals("Dead Code Injection", deadcode.getName());

        PoisonStrategy comments = new CommentPoisoner();
        assertEquals("Semantic Comment Poisoning", comments.getName());

        PoisonStrategy strings = new StringEncoder();
        assertEquals("String Literal Encoding", strings.getName());

        PoisonStrategy whitespace = new WhitespaceDisruptor();
        assertEquals("Whitespace Pattern Disruption", whitespace.getName());

        PoisonStrategy semantic = new SemanticInverter();
        assertEquals("Semantic Inversion", semantic.getName());

        PoisonStrategy controlflow = new ControlFlowFlattener();
        assertEquals("Control Flow Flattening", controlflow.getName());

        PoisonStrategy watermark = new WatermarkEncoder();
        assertEquals("Watermark Encoder", watermark.getName());
    }

    @Test
    void testDefaultEnabledStates() {
        EntropyScrambler entropy = new EntropyScrambler();
        assertTrue(entropy.isEnabled());

        DeadCodeInjector deadcode = new DeadCodeInjector();
        assertTrue(deadcode.isEnabled());

        CommentPoisoner comments = new CommentPoisoner();
        assertTrue(comments.isEnabled());

        StringEncoder strings = new StringEncoder();
        assertTrue(strings.isEnabled());

        WhitespaceDisruptor whitespace = new WhitespaceDisruptor();
        assertTrue(whitespace.isEnabled());

        SemanticInverter semantic = new SemanticInverter();
        assertFalse(semantic.isEnabled());

        ControlFlowFlattener controlflow = new ControlFlowFlattener();
        assertFalse(controlflow.isEnabled());

        WatermarkEncoder watermark = new WatermarkEncoder();
        assertFalse(watermark.isEnabled());
    }

    @Test
    void testStrategyCanBeEnabled() {
        SemanticInverter semantic = new SemanticInverter();
        assertFalse(semantic.isEnabled());
        semantic.setEnabled(true);
        assertTrue(semantic.isEnabled());

        ControlFlowFlattener controlflow = new ControlFlowFlattener();
        assertFalse(controlflow.isEnabled());
        controlflow.setEnabled(true);
        assertTrue(controlflow.isEnabled());
    }
}