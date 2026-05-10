# Nightshade v2.0 Tier 1 + 2 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement Tier 1 + Tier 2 enhancements with five strategies (A–E), early-exit entropy threshold, enhanced diff view, and CLI extensions while preserving current functionality.

**Architecture:** Keep the existing pipeline (Lexer → Parser → Strategies → Serializer) and GUI controller, but adjust strategy ordering/weights and add early-exit logic. UI remains JavaFX with a single `MainController` and synchronized diff panes. Strategy changes remain isolated to strategy classes and engine; CLI extends args parsing only.

**Tech Stack:** Java 21+, JavaFX 21, Maven, Java I/O (BufferedReader/Writer), no external runtime deps.

---

## File/Module Map (New + Modified)

**Existing (modify):**
- `pom.xml` — ensure compiler release 21 and shade plugin intact; add surefire plugin for tests.
- `src/main/java/com/nightshade/engine/ObfuscationEngine.java` — early-exit by entropy threshold + ordered strategies.
- `src/main/java/com/nightshade/engine/EntropyCalculator.java` — update weights for strategies D/E.
- `src/main/java/com/nightshade/strategy/EntropyScrambler.java` — ensure scope-aware renaming is active (already implemented).
- `src/main/java/com/nightshade/strategy/DeadCodeInjector.java` — contextual dead code already present; verify logging hooks.
- `src/main/java/com/nightshade/strategy/CommentPoisoner.java` — add docstring poisoning for Python/JS; expand bank.
- `src/main/java/com/nightshade/strategy/StringEncoder.java` — confirm JS support, align with strategy D.
- `src/main/java/com/nightshade/strategy/WhitespaceDisruptor.java` — confirm weighted contribution in entropy.
- `src/main/java/com/nightshade/model/ObfuscationResult.java` — track per-file strategy execution + early-exit trigger.
- `src/main/java/com/nightshade/controller/MainController.java` — enhanced diff markers + stats dashboard; expose strategy order.
- `src/main/java/com/nightshade/CLI.java` — add `--entropy-threshold` and `--dry-run`.
- `src/main/resources/com/nightshade/fxml/main.fxml` — diff view markers + stats panel.
- `src/main/resources/com/nightshade/css/nightshade.css` — styling for diff markers + strategy badges.

**New (create):**
- `src/main/java/com/nightshade/engine/DiffUtil.java` — lightweight diff metadata builder for UI markers.
- `src/main/java/com/nightshade/model/DiffMarker.java` — marker DTO for line-based changes.
- `src/main/java/com/nightshade/model/StrategyRun.java` — per-strategy execution stats per file.
- `src/test/java/com/nightshade/engine/EntropyCalculatorTest.java`
- `src/test/java/com/nightshade/engine/ObfuscationEngineThresholdTest.java`
- `src/test/java/com/nightshade/strategy/StringEncoderTest.java`
- `src/test/java/com/nightshade/strategy/CommentPoisonerDocstringTest.java`
- `src/test/java/com/nightshade/strategy/WhitespaceDisruptorTest.java`

---

## Task 1: Add test scaffold + surefire plugin

**Files:**
- Modify: `pom.xml`
- Create: `src/test/java/com/nightshade/engine/EntropyCalculatorTest.java`

- [ ] **Step 1: Add surefire plugin**

Edit `pom.xml` and add the surefire plugin below the compiler plugin:

```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <version>3.2.5</version>
</plugin>
```

- [ ] **Step 2: Create entropy test (fails initially)**

Create `src/test/java/com/nightshade/engine/EntropyCalculatorTest.java`:

```java
package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntropyCalculatorTest {
    @Test
    void weightsIncludeStringAndWhitespace() {
        SourceFile s = new SourceFile("/tmp/a.java", List.of("class A {}"));
        ObfuscationResult r = new ObfuscationResult(s, s, 0.0);
        r.setRenamedIdentifiers(1); r.setTotalIdentifiers(1); // 0.5
        r.setDeadBlocksInjected(0); r.setTotalMethods(1);      // 0.0
        r.setCommentsPoisoned(0); r.setTotalComments(1);        // 0.0
        r.setStringsEncoded(1); // +0.15
        r.setWhitespaceChanges(1); // +0.1

        EntropyCalculator calc = new EntropyCalculator();
        assertEquals(0.75, calc.calculate(r), 1e-6);
    }
}
```

- [ ] **Step 3: Run tests (expect FAIL)**

Run: `mvn test -q`
Expected: failure because weights don’t match 0.15/0.1 yet.

---

## Task 2: Update entropy weights for D/E

**Files:**
- Modify: `src/main/java/com/nightshade/engine/EntropyCalculator.java`
- Test: `src/test/java/com/nightshade/engine/EntropyCalculatorTest.java`

- [ ] **Step 1: Update weights and bonus logic**

Replace the bonus section with fixed weights for D/E:

```java
private static final double WEIGHT_D = 0.15; // string encoding
private static final double WEIGHT_E = 0.10; // whitespace disruption (lightest)
```

And update `calculate`:

```java
double scoreD = result.getStringsEncoded() > 0 ? WEIGHT_D : 0.0;
double scoreE = result.getWhitespaceChanges() > 0 ? WEIGHT_E : 0.0;
return Math.min(1.0, scoreA + scoreB + scoreC + scoreD + scoreE);
```

- [ ] **Step 2: Run tests (expect PASS)**

Run: `mvn test -q`
Expected: PASS.

- [ ] **Step 3: Commit**

```bash
git add pom.xml src/main/java/com/nightshade/engine/EntropyCalculator.java src/test/java/com/nightshade/engine/EntropyCalculatorTest.java
git commit -m "test: add entropy weight coverage"
```

---

## Task 3: Strategy ordering + early-exit entropy threshold

**Files:**
- Modify: `src/main/java/com/nightshade/engine/ObfuscationEngine.java`
- Modify: `src/main/java/com/nightshade/model/ObfuscationResult.java`
- Create: `src/main/java/com/nightshade/model/StrategyRun.java`
- Create: `src/test/java/com/nightshade/engine/ObfuscationEngineThresholdTest.java`

- [ ] **Step 1: Add StrategyRun model**

Create `src/main/java/com/nightshade/model/StrategyRun.java`:

```java
package com.nightshade.model;

public class StrategyRun {
    private final String name;
    private final boolean applied;
    private final double entropyAfter;

    public StrategyRun(String name, boolean applied, double entropyAfter) {
        this.name = name;
        this.applied = applied;
        this.entropyAfter = entropyAfter;
    }

    public String getName() { return name; }
    public boolean isApplied() { return applied; }
    public double getEntropyAfter() { return entropyAfter; }
}
```

- [ ] **Step 2: Extend ObfuscationResult with strategy runs and early-exit trigger**

Add fields + getters/setters:

```java
private java.util.List<StrategyRun> strategyRuns = new java.util.ArrayList<>();
private String earlyExitStrategy;

public java.util.List<StrategyRun> getStrategyRuns() { return java.util.Collections.unmodifiableList(strategyRuns); }
public void setStrategyRuns(java.util.List<StrategyRun> runs) { this.strategyRuns = new java.util.ArrayList<>(runs); }
public String getEarlyExitStrategy() { return earlyExitStrategy; }
public void setEarlyExitStrategy(String name) { this.earlyExitStrategy = name; }
```

- [ ] **Step 3: Add threshold logic + ordering in ObfuscationEngine**

Add a constructor parameter `double entropyThreshold` with default 1.0.
Order strategies explicitly: EntropyScrambler → DeadCodeInjector → CommentPoisoner → StringEncoder → WhitespaceDisruptor.
After each applied strategy, compute entropy and break if >= threshold.

Pseudo insertion in `processOne` loop:

```java
List<StrategyRun> runs = new ArrayList<>();
for (PoisonStrategy strategy : strategies) {
  if (!strategy.isEnabled()) { runs.add(new StrategyRun(strategy.getName(), false, currentEntropy)); continue; }
  ObfuscationResult partial = strategy.apply(current, ast, symbols);
  current = partial.getObfuscatedFile();
  partialResults.add(partial);
  ObfuscationResult merged = mergeResults(original, current, partialResults);
  double currentEntropy = entropyCalc.calculate(merged);
  runs.add(new StrategyRun(strategy.getName(), true, currentEntropy));
  if (currentEntropy >= entropyThreshold) {
     merged.setEarlyExitStrategy(strategy.getName());
     break;
  }
}
```

- [ ] **Step 4: Add unit test for early exit**

Create `src/test/java/com/nightshade/engine/ObfuscationEngineThresholdTest.java`:

```java
package com.nightshade.engine;

import com.nightshade.model.SourceFile;
import com.nightshade.strategy.EntropyScrambler;
import com.nightshade.strategy.DeadCodeInjector;
import com.nightshade.strategy.CommentPoisoner;
import com.nightshade.strategy.StringEncoder;
import com.nightshade.strategy.WhitespaceDisruptor;
import com.nightshade.strategy.PoisonStrategy;
import com.nightshade.util.LogService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ObfuscationEngineThresholdTest {
    @Test
    void exitsAfterEntropyScramblerAtThreshold() {
        SourceFile s = new SourceFile("/tmp/Test.java", List.of("class Test { int a; int b; }"));
        List<PoisonStrategy> strategies = List.of(
            new EntropyScrambler(), new DeadCodeInjector(), new CommentPoisoner(), new StringEncoder(), new WhitespaceDisruptor()
        );
        ObfuscationEngine engine = new ObfuscationEngine(
            strategies, new Lexer(), new Parser(), new Serializer(), new EntropyCalculator(), new LogService(), 0.6
        );
        var result = engine.process(List.of(s)).get(0);
        assertTrue(result.getEarlyExitStrategy() != null);
    }
}
```

- [ ] **Step 5: Run tests**

Run: `mvn test -q`
Expected: PASS.

---

## Task 4: Strategy D — String Encoder (JS support + tests)

**Files:**
- Modify: `src/main/java/com/nightshade/strategy/StringEncoder.java`
- Create: `src/test/java/com/nightshade/strategy/StringEncoderTest.java`

- [ ] **Step 1: Add JS support**

Modify `encodeLine` to support `.js` output:

```java
private String encodeJavaScript(String content) {
    StringBuilder sb = new StringBuilder("String.fromCharCode(");
    for (int i = 0; i < content.length(); i++) {
        if (i > 0) sb.append(',');
        sb.append((int) content.charAt(i));
    }
    sb.append(")");
    return sb.toString();
}
```

Choose `encodeJava` for `.java`, `encodePython` for `.py`, `encodeJavaScript` for `.js`.

- [ ] **Step 2: Test string encoding**

Create `src/test/java/com/nightshade/strategy/StringEncoderTest.java`:

```java
package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class StringEncoderTest {
    @Test
    void encodesJavaString() {
        SourceFile s = new SourceFile("/tmp/A.java", List.of("class A { String x = \"Hi\"; }"));
        StringEncoder enc = new StringEncoder();
        var result = enc.apply(s, new ASTNode("PROGRAM"), new SymbolTable());
        String line = result.getObfuscatedFile().getObfuscatedLines().get(0);
        assertTrue(line.contains("new String(new char[]{"));
    }
}
```

- [ ] **Step 3: Run tests**

Run: `mvn test -q`
Expected: PASS.

---

## Task 5: Strategy C — Docstring poisoning (Python/JS)

**Files:**
- Modify: `src/main/java/com/nightshade/strategy/CommentPoisoner.java`
- Create: `src/test/java/com/nightshade/strategy/CommentPoisonerDocstringTest.java`

- [ ] **Step 1: Implement docstring detection**

Add a pass for Python triple-quote and JS JSDoc blocks:

```java
private static final Pattern PY_DOCSTRING = Pattern.compile("^(\\s*)(\"\"\".*?\"\"\"|''' .*? ''')\\s*$", Pattern.DOTALL);
private static final Pattern JS_DOCBLOCK = Pattern.compile("^(\\s*)/\\*\\*.*?\\*/\\s*$", Pattern.DOTALL);
```

Replace content with bank entries using same deterministic selection.

- [ ] **Step 2: Test docstring poisoning**

Create `src/test/java/com/nightshade/strategy/CommentPoisonerDocstringTest.java`:

```java
package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommentPoisonerDocstringTest {
    @Test
    void poisonsPythonDocstring() {
        SourceFile s = new SourceFile("/tmp/a.py", List.of("\"\"\"doc\"\"\""));
        CommentPoisoner p = new CommentPoisoner();
        var result = p.apply(s, new ASTNode("PROGRAM"), new SymbolTable());
        String line = result.getObfuscatedFile().getObfuscatedLines().get(0);
        assertTrue(line.contains("#") || line.contains("//"));
    }
}
```

- [ ] **Step 3: Run tests**

Run: `mvn test -q`
Expected: PASS.

---

## Task 6: Strategy E — Whitespace disruption weight + safeguards

**Files:**
- Modify: `src/main/java/com/nightshade/strategy/WhitespaceDisruptor.java`
- Create: `src/test/java/com/nightshade/strategy/WhitespaceDisruptorTest.java`

- [ ] **Step 1: Ensure non-destructive transformation**

Add guard to not alter empty or comment-only lines and keep Allman transform deterministic.

- [ ] **Step 2: Add unit test**

Create `src/test/java/com/nightshade/strategy/WhitespaceDisruptorTest.java`:

```java
package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class WhitespaceDisruptorTest {
    @Test
    void skipsPython() {
        SourceFile s = new SourceFile("/tmp/a.py", List.of("def x():", "    return 1"));
        WhitespaceDisruptor w = new WhitespaceDisruptor();
        var result = w.apply(s, new ASTNode("PROGRAM"), new SymbolTable());
        assertTrue(result.getWhitespaceChanges() == 0);
    }
}
```

- [ ] **Step 3: Run tests**

Run: `mvn test -q`
Expected: PASS.

---

## Task 7: Enhanced diff markers + stats dashboard

**Files:**
- Create: `src/main/java/com/nightshade/engine/DiffUtil.java`
- Create: `src/main/java/com/nightshade/model/DiffMarker.java`
- Modify: `src/main/java/com/nightshade/controller/MainController.java`
- Modify: `src/main/resources/com/nightshade/fxml/main.fxml`
- Modify: `src/main/resources/com/nightshade/css/nightshade.css`

- [ ] **Step 1: Create DiffMarker model**

```java
package com.nightshade.model;

public class DiffMarker {
    public enum Type { INJECTED, RENAMED, COMMENT_POISONED }
    private final int line;
    private final Type type;

    public DiffMarker(int line, Type type) {
        this.line = line;
        this.type = type;
    }

    public int getLine() { return line; }
    public Type getType() { return type; }
}
```

- [ ] **Step 2: Create DiffUtil**

```java
package com.nightshade.engine;

import com.nightshade.model.DiffMarker;

import java.util.ArrayList;
import java.util.List;

public class DiffUtil {
    public List<DiffMarker> buildMarkers(List<String> original, List<String> obfuscated) {
        List<DiffMarker> markers = new ArrayList<>();
        int max = Math.min(original.size(), obfuscated.size());
        for (int i = 0; i < max; i++) {
            if (!original.get(i).equals(obfuscated.get(i))) {
                String line = obfuscated.get(i);
                if (line.contains("if (false)") || line.contains("[strategy:")) {
                    markers.add(new DiffMarker(i + 1, DiffMarker.Type.INJECTED));
                } else if (line.contains("v_") && line.matches(".*v_[a-z]+.*")) {
                    markers.add(new DiffMarker(i + 1, DiffMarker.Type.RENAMED));
                } else if (line.contains("//") || line.contains("#")) {
                    markers.add(new DiffMarker(i + 1, DiffMarker.Type.COMMENT_POISONED));
                }
            }
        }
        return markers;
    }
}
```

- [ ] **Step 3: Update UI to show markers**

Modify `main.fxml` to include a narrow marker column for each code pane. Add a `ListView<DiffMarker>` next to each TextArea. Bind markers in `MainController`.

- [ ] **Step 4: Style markers in CSS**

Add classes:

```css
.marker-injected { -fx-text-fill: #4CAF50; }
.marker-renamed { -fx-text-fill: #FFA500; }
.marker-comment { -fx-text-fill: #FF4444; }
```

- [ ] **Step 5: Run UI manually**

Run: `mvn javafx:run`
Expected: markers appear in diff view with colors.

---

## Task 8: CLI extensions (`--entropy-threshold`, `--dry-run`)

**Files:**
- Modify: `src/main/java/com/nightshade/CLI.java`

- [ ] **Step 1: Add new flags**

Add:

```java
case "--entropy-threshold" -> entropyThreshold = Double.parseDouble(args[++i]);
case "--dry-run" -> dryRun = true;
```

- [ ] **Step 2: Apply threshold and dry-run**

Pass `entropyThreshold` into `ObfuscationEngine` constructor.
If `dryRun` is true, skip `fileUtil.write` and only print stats.

- [ ] **Step 3: Run CLI**

Run:
`java -jar target/nightshade-all.jar --input ./sample-repo --dry-run --entropy-threshold 0.6`
Expected: no files written, stats printed.

---

## Task 9: Update README/docs (Tier 1+2)

**Files:**
- Create: `README.md`
- Create: `docs/RESEARCH.md`
- Create: `docs/STRATEGIES.md`

Include:
- Badges
- Before/after screenshot (use a single screenshot from enhanced diff)
- Strategy list + weights
- CLI usage examples

---

## Task 10: Final verification

- [ ] Run: `mvn clean test`
- [ ] Run: `mvn javafx:run` and visually confirm UI markers
- [ ] Run CLI dry-run with threshold
- [ ] Confirm `_nightshade_output/` remains untouched during dry-run

---

## Plan Self-Review

**Spec coverage:**
- All five strategies A–E included with weights (D=0.15, E=0.1).
- Early-exit threshold behavior defined and ordered by weight.
- CLI extended with `--entropy-threshold` and `--dry-run`.
- Enhanced diff markers implemented.
- Tier 1+2 docs added.

**Placeholder scan:** No TODO/TBD.

**Type consistency:** All new classes referenced are created in plan tasks.

---

Plan complete and saved to `docs/superpowers/plans/2026-05-03-nightshade-tier1-2-implementation-plan.md`.
Two execution options:

1. Subagent-Driven (recommended) — I dispatch a fresh subagent per task, review between tasks.
2. Inline Execution — execute tasks in this session using executing-plans.

Which approach?
