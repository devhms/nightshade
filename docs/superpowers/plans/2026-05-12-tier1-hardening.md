# Tier 1 Hardening Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Validate and finalize Tier 1 hardening by adding targeted tests for token-safe renaming, dot-call guards, protected identifiers, and AST drift in the pipeline.

**Architecture:** Keep the existing token-aware serializer and AST reparse logic intact; add focused unit/integration tests that assert the intended invariants and prevent regression.

**Tech Stack:** Java 21, JUnit 5, Maven Surefire

---

## File Map
- Modify: `src/test/java/com/nightshade/strategy/EntropyScramblerTest.java`
- Modify: `src/test/java/com/nightshade/model/SymbolTableTest.java`
- Create: `src/test/java/com/nightshade/engine/SerializerTest.java`
- Create: `src/test/java/com/nightshade/engine/AstDriftTest.java`

---

### Task 1: Add token-safe renaming + dot-call guard test

**Files:**
- Modify: `src/test/java/com/nightshade/strategy/EntropyScramblerTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void testDoesNotRenameStringsCommentsOrDotCalls() {
    List<String> lines = List.of(
        "public class Test {",
        "    void run() {",
        "        String s = \"count\"; int count = 1; // count",
        "        java.util.List<String> myList = new java.util.ArrayList<>();",
        "        myList.add(\"x\");",
        "    }",
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
    List<String> obf = result.getObfuscatedFile().getObfuscatedLines();
    String joined = String.join("\n", obf);

    assertAll("token-safe renaming",
        () -> assertTrue(joined.contains("\"count\""), "string literal should remain"),
        () -> assertTrue(joined.contains("// count"), "comment should remain"),
        () -> assertFalse(joined.contains(" count = 1"), "variable name should be renamed"),
        () -> assertTrue(joined.contains("myList.add"), "dot-call method should not be renamed")
    );
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
mvn -Dtest=EntropyScramblerTest#testDoesNotRenameStringsCommentsOrDotCalls test
```
Expected: FAIL due to missing test or failing assertion if behavior regresses.

- [ ] **Step 3: Implement minimal changes (if needed)**

If the test fails because behavior is incorrect, update `Serializer.applyMapping` to preserve literals/comments and skip identifiers following `.`. Use the existing token stream:

```java
List<Token> tokens = lexer.tokenize(Collections.singletonList(line));
Token prevToken = null;
for (Token token : tokens) {
    boolean isMethodCall = prevToken != null
        && prevToken.getValue().equals(".")
        && token.getType() == TokenType.IDENTIFIER;
    if (token.getType() == TokenType.IDENTIFIER && !isMethodCall && mapping.containsKey(token.getValue())) {
        sb.append(mapping.get(token.getValue()));
    } else {
        sb.append(token.getValue());
    }
    prevToken = token;
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
mvn -Dtest=EntropyScramblerTest#testDoesNotRenameStringsCommentsOrDotCalls test
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/nightshade/strategy/EntropyScramblerTest.java src/main/java/com/nightshade/engine/Serializer.java
git commit -m "test: cover token-safe renaming and dot-call guard"
```

---

### Task 2: Add protected identifiers coverage

**Files:**
- Modify: `src/test/java/com/nightshade/model/SymbolTableTest.java`

- [ ] **Step 1: Write the failing test**

```java
@Test
void testAdditionalProtectedIdentifiers() {
    SymbolTable table = new SymbolTable();
    assertAll("protected identifiers",
        () -> assertFalse(table.isUserDefined("setTitle")),
        () -> assertFalse(table.isUserDefined("stream")),
        () -> assertFalse(table.isUserDefined("toUpperCase")),
        () -> assertFalse(table.isUserDefined("getItems"))
    );
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
mvn -Dtest=SymbolTableTest#testAdditionalProtectedIdentifiers test
```
Expected: FAIL if the protected list is missing entries.

- [ ] **Step 3: Implement minimal changes (if needed)**

Add missing identifiers to `PROTECTED_IDENTIFIERS` in `src/main/java/com/nightshade/model/SymbolTable.java`.

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
mvn -Dtest=SymbolTableTest#testAdditionalProtectedIdentifiers test
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/nightshade/model/SymbolTableTest.java src/main/java/com/nightshade/model/SymbolTable.java
git commit -m "test: verify expanded protected identifiers"
```

---

### Task 3: Add serializer unit test for token safety

**Files:**
- Create: `src/test/java/com/nightshade/engine/SerializerTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.nightshade.engine;

import com.nightshade.model.SourceFile;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SerializerTest {

    @Test
    void applyMappingSkipsStringsCommentsAndDotCalls() {
        List<String> lines = List.of(
            "public class Test {",
            "  void run() {",
            "    String s = \"count\"; int count = 1; // count",
            "    myObj.process();",
            "  }",
            "}"
        );
        SourceFile source = new SourceFile("Test.java", lines);
        Map<String, String> mapping = Map.of("count", "v_aaaaaaa", "process", "v_bbbbbbb");

        Serializer serializer = new Serializer();
        List<String> out = serializer.applyMapping(source, mapping);
        String joined = String.join("\n", out);

        assertAll("applyMapping invariants",
            () -> assertTrue(joined.contains("\"count\"")),
            () -> assertTrue(joined.contains("// count")),
            () -> assertTrue(joined.contains("int v_aaaaaaa = 1")),
            () -> assertTrue(joined.contains("myObj.process()"))
        );
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
mvn -Dtest=SerializerTest#applyMappingSkipsStringsCommentsAndDotCalls test
```
Expected: FAIL if serializer is not token-safe.

- [ ] **Step 3: Implement minimal changes (if needed)**

Ensure `Serializer.applyMapping` uses tokenization per line and dot-call guard (see Task 1).

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
mvn -Dtest=SerializerTest#applyMappingSkipsStringsCommentsAndDotCalls test
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/nightshade/engine/SerializerTest.java src/main/java/com/nightshade/engine/Serializer.java
git commit -m "test: add serializer token-safety coverage"
```

---

### Task 4: Add AST drift regression test

**Files:**
- Create: `src/test/java/com/nightshade/engine/AstDriftTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.strategy.DeadCodeInjector;
import com.nightshade.strategy.EntropyScrambler;
import com.nightshade.strategy.PoisonStrategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AstDriftTest {

    @Test
    void reparseAfterLineChangePreventsDrift() {
        List<String> lines = List.of(
            "public class Drift {",
            "    int count = 0;",
            "    int next() {",
            "        count++;",
            "        return count;",
            "    }",
            "}"
        );
        SourceFile sourceFile = new SourceFile("Drift.java", lines);
        List<SourceFile> files = new ArrayList<>();
        files.add(sourceFile);

        List<PoisonStrategy> strategies = new ArrayList<>();
        strategies.add(new DeadCodeInjector());
        strategies.add(new EntropyScrambler());

        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalculator = new EntropyCalculator();
        com.nightshade.util.LogService logService = new com.nightshade.util.LogService();

        ObfuscationEngine engine = new ObfuscationEngine(strategies, lexer, parser, serializer, entropyCalculator, logService, 1.0);
        List<ObfuscationResult> results = engine.process(files);

        String joined = String.join("\n", results.get(0).getObfuscatedFile().getObfuscatedLines());

        assertAll("drift renaming",
            () -> assertFalse(joined.contains("count++"), "count identifier should be renamed"),
            () -> assertFalse(joined.contains("return count"), "count identifier should be renamed")
        );
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run:
```bash
mvn -Dtest=AstDriftTest#reparseAfterLineChangePreventsDrift test
```
Expected: FAIL if AST reparse is missing or wrong.

- [ ] **Step 3: Implement minimal changes (if needed)**

Ensure `ObfuscationEngine.processOne` reparses after line count change (already present):

```java
if (currentLineCount != previousLineCount) {
    tokens = lexer.tokenize(current.getObfuscatedLines());
    ast = parser.parse(tokens);
    for (String api : parser.getPublicApis()) {
        symbols.protect(api);
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run:
```bash
mvn -Dtest=AstDriftTest#reparseAfterLineChangePreventsDrift test
```
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/nightshade/engine/AstDriftTest.java src/main/java/com/nightshade/engine/ObfuscationEngine.java
git commit -m "test: cover AST drift after line changes"
```

---

### Task 5: Full test suite verification

**Files:**
- Test: full repository

- [ ] **Step 1: Run full test suite**

Run:
```bash
mvn test
```
Expected: PASS

- [ ] **Step 2: Commit final plan state (if any)**

```bash
git status
```
Ensure no unexpected changes remain.

---

## Self-Review Checklist
- All Tier 1 invariants are asserted via tests.
- Tests fail before fixes if a regression is introduced.
- Tests pass with current implementation.
- Full test suite passes.

## Execution Handoff
Plan complete and saved to `docs/superpowers/plans/2026-05-12-tier1-hardening.md`.

Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks.
2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints.

Which approach?
