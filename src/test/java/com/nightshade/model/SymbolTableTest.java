package com.nightshade.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SymbolTableTest {

    @Test
    void testKeywordsAreNotUserDefined() {
        SymbolTable table = new SymbolTable();
        assertFalse(table.isUserDefined("class"));
        assertFalse(table.isUserDefined("public"));
        assertFalse(table.isUserDefined("String"));
        assertFalse(table.isUserDefined("System"));
    }

    @Test
    void testUserVariablesAreUserDefined() {
        SymbolTable table = new SymbolTable();
        assertTrue(table.isUserDefined("myVar"));
        assertTrue(table.isUserDefined("counter"));
        assertTrue(table.isUserDefined("result"));
    }

    @Test
    void testResolveProducesDeterministicOutput() {
        SymbolTable table = new SymbolTable();
        String result1 = table.resolve("myVar", "MyClass.myMethod");
        String result2 = table.resolve("myVar", "MyClass.myMethod");
        assertEquals(result1, result2, "Resolving the same symbol in the same scope should yield identical results.");
    }

    @Test
    void testDifferentScopesProduceDifferentNames() {
        SymbolTable table = new SymbolTable();
        String result1 = table.resolve("result", "ClassA.method1");
        String result2 = table.resolve("result", "ClassA.method2");
        assertNotEquals(result1, result2, "Resolving the same symbol in different scopes should yield different results.");
    }

    @Test
    void testDifferentSessionsProduceDifferentNames() {
        SymbolTable table1 = new SymbolTable();
        SymbolTable table2 = new SymbolTable();
        String result1 = table1.resolve("myVar", "global");
        String result2 = table2.resolve("myVar", "global");
        assertNotEquals(result1, result2, "Resolving the same symbol in different sessions should yield different results due to salt.");
    }

    @Test
    void testProtectsUppercaseClassNames() {
        SymbolTable table = new SymbolTable();
        assertFalse(table.isUserDefined("MyClass"));
        assertFalse(table.isUserDefined("ArrayList"));
    }
}
