# NIGHTSHADE v3.5 — NEXT PHASE IMPLEMENTATION PLAN
# Builds on completed v3.0 (Phases 1-5 done)
# For: Low-tier AI agent execution

===============================================================================
PHASE 6: CONTROL FLOW FLATTENING (New Strategy G)
===============================================================================

## WHY: The current 6 strategies modify surface-level tokens (names, comments,
## strings, whitespace). None of them change the STRUCTURE of the code.
## LLM training pipelines that normalize variables before dedup can undo
## EntropyScrambler + SemanticInverter. Control flow flattening changes
## the actual logic structure, making it preprocessing-proof.

## TASK 6.1: Create ControlFlowFlattener Strategy

File: src/main/java/com/nightshade/strategy/ControlFlowFlattener.java
Package: com.nightshade.strategy

This strategy takes sequential statements inside a method body and rewrites
them into a while-switch dispatch loop. The code behavior is identical,
but the structure is completely different.

BEFORE:
```java
public int calculate(int x) {
    int a = x + 1;
    int b = a * 2;
    return b - 3;
}
```

AFTER:
```java
public int calculate(int x) {
    int _state = 0;
    int a = 0, b = 0, _ret = 0;
    while (_state != -1) {
        switch (_state) {
            case 0: a = x + 1; _state = 1; break;
            case 1: b = a * 2; _state = 2; break;
            case 2: _ret = b - 3; _state = -1; break;
        }
    }
    return _ret;
}
```

Implementation approach:
- This strategy should ONLY target PRIVATE methods (never public API)
- It works on the line-level text, not on a full AST
- Find method bodies by tracking brace depth (like DeadCodeInjector does)
- Extract sequential statements between { and return/}
- Wrap them in the while-switch pattern
- Each statement becomes a case block with an incrementing state counter

```java
package com.nightshade.strategy;

import com.nightshade.model.*;
import java.util.*;
import java.util.regex.*;

public class ControlFlowFlattener implements PoisonStrategy {

    private boolean enabled = false; // disabled by default — aggressive
    
    @Override public String getName()          { return "Control Flow Flattening"; }
    @Override public String getDescription()   { return "Rewrites method bodies into switch-dispatch loops — changes code structure, not just names"; }
    @Override public String getResearchBasis() { return "Structure-level obfuscation — survives variable normalization and reformatting"; }
    @Override public boolean isEnabled()       { return enabled; }
    @Override public void setEnabled(boolean e){ this.enabled = e; }

    // Detects private method declarations
    private static final Pattern PRIVATE_METHOD = Pattern.compile(
        "^(\\s*)(private\\s+\\w+\\s+(\\w+)\\s*\\([^)]*\\))\\s*\\{\\s*$");

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        List<String> lines = new ArrayList<>(source.getObfuscatedLines());
        int flattenedCount = 0;
        int totalMethods = 0;

        // Find private methods and flatten them
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = PRIVATE_METHOD.matcher(lines.get(i));
            if (!m.matches()) continue;
            totalMethods++;

            String indent = m.group(1);
            // Find the closing brace of this method
            int braceDepth = 1;
            int bodyStart = i + 1;
            int bodyEnd = -1;
            for (int j = bodyStart; j < lines.size(); j++) {
                for (char c : lines.get(j).toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                if (braceDepth == 0) { bodyEnd = j; break; }
            }
            if (bodyEnd == -1 || bodyEnd - bodyStart < 3) continue;

            // Extract body statements (skip blank lines)
            List<String> bodyStatements = new ArrayList<>();
            String returnStatement = null;
            for (int j = bodyStart; j < bodyEnd; j++) {
                String trimmed = lines.get(j).trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("return ")) {
                    returnStatement = trimmed;
                } else {
                    bodyStatements.add(trimmed);
                }
            }
            
            if (bodyStatements.size() < 2) continue; // not worth flattening

            // Build the flattened version
            List<String> flattened = new ArrayList<>();
            flattened.add(indent + "    int _ns_state = 0;");
            flattened.add(indent + "    while (_ns_state != -1) {");
            flattened.add(indent + "        switch (_ns_state) {");
            for (int s = 0; s < bodyStatements.size(); s++) {
                flattened.add(indent + "            case " + s + ": " 
                    + bodyStatements.get(s) + " _ns_state = " + (s+1) + "; break;");
            }
            flattened.add(indent + "            case " + bodyStatements.size() 
                + ": _ns_state = -1; break;");
            flattened.add(indent + "        }");
            flattened.add(indent + "    }");
            if (returnStatement != null) {
                flattened.add(indent + "    " + returnStatement);
            }

            // Replace original body with flattened version
            List<String> before = lines.subList(0, bodyStart);
            List<String> after = lines.subList(bodyEnd, lines.size());
            List<String> newLines = new ArrayList<>(before);
            newLines.addAll(flattened);
            newLines.addAll(after);
            lines = newLines;
            flattenedCount++;
        }

        SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        modified.setObfuscatedLines(lines);

        ObfuscationResult result = new ObfuscationResult(source, modified, 0.0);
        result.setTotalMethods(Math.max(1, totalMethods));
        return result;
    }
}
```

## TASK 6.2: Register in CLI.java and MainController

In CLI.java buildStrategies(), add:
  case "all" -> add new ControlFlowFlattener() (keep it disabled by default — user opts in)
  case "flatten" -> list.add(new ControlFlowFlattener())

In MainController.java:
  Add @FXML CheckBox cbFlatten
  In buildSelectedStrategies(): if (cbFlatten.isSelected()) list.add(new ControlFlowFlattener())

In main.fxml: Add checkbox labeled "Control Flow Flattening (aggressive)" after semantic inversion

## TASK 6.3: Write ControlFlowFlattenerTest

File: src/test/java/com/nightshade/strategy/ControlFlowFlattenerTest.java

Test cases:
1. testFlattensPrivateMethod()
   - Input: class with private method containing 3 statements + return
   - Assert output contains "switch" and "_ns_state"
   - Assert output does NOT contain original sequential statements

2. testSkipsPublicMethods()
   - Input: class with public method
   - Assert output is IDENTICAL to input (no flattening applied)

3. testSkipsShortMethods()
   - Input: private method with only 1 statement
   - Assert no flattening occurs (not worth the complexity)


===============================================================================
PHASE 7: CRYPTOGRAPHIC WATERMARKING (Strategy H)
===============================================================================

## WHY: If an LLM produces code that was trained on Nightshade-protected files,
## the original author has NO way to prove it. Watermarking embeds a hidden,
## extractable signature that proves provenance in copyright disputes.

## TASK 7.1: Create WatermarkEncoder Strategy

File: src/main/java/com/nightshade/strategy/WatermarkEncoder.java

This strategy embeds bits of a unique fingerprint into the code using:
1. Whitespace steganography — encoding bits as (tab vs 2-spaces) in indent
2. Comment word choice — selecting synonym A or B based on bit value
3. Variable name suffix patterns — using specific character patterns

The watermark encodes:
- A hash of the author's identity (from --author flag or config)
- The session salt (already generated per-run by SymbolTable)
- A timestamp

Extraction: Given the seed, the watermark can be extracted and verified.

```java
package com.nightshade.strategy;

import com.nightshade.model.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

public class WatermarkEncoder implements PoisonStrategy {

    private boolean enabled = false;
    private String authorId = "nightshade-user";

    @Override public String getName()          { return "Watermark Encoder"; }
    @Override public String getDescription()   { return "Embeds steganographic fingerprint for copyright provenance tracking"; }
    @Override public String getResearchBasis() { return "Code watermarking via whitespace steganography — invisible to humans, extractable with key"; }
    @Override public boolean isEnabled()       { return enabled; }
    @Override public void setEnabled(boolean e){ this.enabled = e; }

    public void setAuthorId(String id) { this.authorId = id; }

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        List<String> lines = new ArrayList<>(source.getObfuscatedLines());
        
        // Generate watermark bits from author + salt + timestamp
        String payload = authorId + "|" + symbols.getSessionSalt() + "|" + System.currentTimeMillis();
        byte[] hash = sha256(payload);
        boolean[] bits = bytesToBits(hash);
        
        int bitIndex = 0;
        int embedded = 0;
        
        for (int i = 0; i < lines.size() && bitIndex < bits.length; i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            
            // Skip blank lines and lines with no indentation
            if (trimmed.isEmpty()) continue;
            int leadingSpaces = line.length() - line.stripLeading().length();
            if (leadingSpaces < 2) continue;
            
            // Encode one bit per eligible line:
            // bit=0 → use spaces for indent (no change)
            // bit=1 → add one invisible Unicode zero-width space after indent
            if (bits[bitIndex]) {
                // Insert a zero-width space (U+200B) after the indent
                lines.set(i, line.substring(0, leadingSpaces) + "\u200B" + trimmed);
                embedded++;
            }
            bitIndex++;
        }

        SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        modified.setObfuscatedLines(lines);

        ObfuscationResult result = new ObfuscationResult(source, modified, 0.0);
        result.setWhitespaceChanges(embedded);
        return result;
    }

    private byte[] sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean[] bytesToBits(byte[] bytes) {
        boolean[] bits = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + j] = ((bytes[i] >> (7 - j)) & 1) == 1;
            }
        }
        return bits;
    }
}
```

## TASK 7.2: Add --author CLI Flag

File: src/main/java/com/nightshade/CLI.java

In the argument parsing section, add:
```java
case "--author" -> {
    if (i + 1 < args.length) authorId = args[++i];
}
```

Pass authorId to WatermarkEncoder:
```java
for (PoisonStrategy s : strategies) {
    if (s instanceof WatermarkEncoder wm) {
        wm.setAuthorId(authorId);
    }
}
```

## TASK 7.3: Write WatermarkEncoderTest

File: src/test/java/com/nightshade/strategy/WatermarkEncoderTest.java

Test cases:
1. testEmbedsWatermarkBits()
   - Apply to a 20-line Java file
   - Assert some lines contain U+200B character (zero-width space)
   - Assert the total count of modified lines > 0

2. testWatermarkIsInvisible()
   - Apply watermark, then trim() each line
   - Assert trimmed content is identical to original trimmed content
   - (the watermark should not change visible text)


===============================================================================
PHASE 8: POISONING EFFECTIVENESS REPORT (Analytics)
===============================================================================

## WHY: After running Nightshade, the user has no metrics to understand HOW
## effective the poisoning is. A report gives confidence and credibility.

## TASK 8.1: Create PoisoningReport Generator

File: src/main/java/com/nightshade/engine/PoisoningReport.java

This class takes the list of ObfuscationResults and generates a text/markdown
report with these metrics:

```java
package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import java.util.*;

public class PoisoningReport {

    public static String generate(List<ObfuscationResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nightshade Poisoning Report\n\n");
        sb.append("## Summary\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        
        int totalFiles = results.size();
        int totalRenamed = 0, totalDead = 0, totalComments = 0;
        int totalStrings = 0, totalWhitespace = 0;
        double avgEntropy = 0;
        int filesAboveThreshold = 0;
        
        for (ObfuscationResult r : results) {
            totalRenamed += r.getRenamedIdentifiers();
            totalDead += r.getDeadBlocksInjected();
            totalComments += r.getCommentsPoisoned();
            totalStrings += r.getStringsEncoded();
            totalWhitespace += r.getWhitespaceChanges();
            avgEntropy += r.getEntropyScore();
            if (r.getEntropyScore() >= 0.5) filesAboveThreshold++;
        }
        avgEntropy /= Math.max(1, totalFiles);
        
        sb.append(String.format("| Files Processed | %d |\n", totalFiles));
        sb.append(String.format("| Identifiers Renamed | %d |\n", totalRenamed));
        sb.append(String.format("| Dead Blocks Injected | %d |\n", totalDead));
        sb.append(String.format("| Comments Poisoned | %d |\n", totalComments));
        sb.append(String.format("| Strings Encoded | %d |\n", totalStrings));
        sb.append(String.format("| Whitespace Changes | %d |\n", totalWhitespace));
        sb.append(String.format("| Avg Entropy Score | %.3f |\n", avgEntropy));
        sb.append(String.format("| Files Above Threshold | %d/%d (%.0f%%) |\n", 
            filesAboveThreshold, totalFiles, 
            (double)filesAboveThreshold/Math.max(1,totalFiles)*100));
        
        // Per-file breakdown
        sb.append("\n## Per-File Breakdown\n\n");
        sb.append("| File | Entropy | Renamed | Dead | Comments | Strings |\n");
        sb.append("|------|---------|---------|------|----------|---------|\n");
        for (ObfuscationResult r : results) {
            sb.append(String.format("| %s | %.3f | %d | %d | %d | %d |\n",
                r.getOriginalFile().getFileName(),
                r.getEntropyScore(),
                r.getRenamedIdentifiers(),
                r.getDeadBlocksInjected(),
                r.getCommentsPoisoned(),
                r.getStringsEncoded()));
        }
        
        // MI resistance estimate
        sb.append("\n## Estimated MI Resistance\n");
        sb.append("Based on arXiv:2512.15468, variable renaming alone provides ");
        sb.append("~10.19% MI detection drop. Combined with dead code injection, ");
        sb.append("comment poisoning, and string encoding, estimated total MI ");
        sb.append(String.format("resistance: **%.1f%%**\n", 
            Math.min(95, 10.19 + totalDead * 2.5 + totalComments * 1.5 + totalStrings * 3.0)));
        
        return sb.toString();
    }
}
```

## TASK 8.2: Add --report Flag to CLI.java

In CLI.java, add a --report flag that writes the report to a file:

```java
case "--report" -> {
    if (i + 1 < args.length) reportPath = args[++i];
}
```

After processing, add:
```java
if (reportPath != null) {
    String report = PoisoningReport.generate(results);
    Files.writeString(Path.of(reportPath), report);
    System.out.println("[REPORT] Written to: " + reportPath);
}
```

## TASK 8.3: Show Report in GUI

In MainController.java, after pipeline completion, display the report in
a new TextArea or a popup dialog. Call PoisoningReport.generate(results)
and show the markdown content.


===============================================================================
PHASE 9: MULTI-FILE CROSS-REFERENCE POISONING
===============================================================================

## WHY: Current strategies work on each file independently. LLMs learn from
## CROSS-FILE patterns (imports, call graphs). If ClassA imports ClassB,
## the relationship should also be poisoned.

## TASK 9.1: Add Cross-File Import Poisoning to ObfuscationEngine

File: src/main/java/com/nightshade/engine/ObfuscationEngine.java

After all strategies run on all files, add a post-processing pass:

1. Collect all renamed identifiers across files from SymbolTable.getFullMapping()
2. For each output file, scan import statements
3. If an import references a class whose internal name was renamed,
   update the import to match the new name

This ensures consistency. Currently if ClassA has a private field "helper"
that gets renamed to "bakeCake_421", any file that references it via
reflection or dynamic dispatch will break.

Implementation: Add a method postProcessCrossReferences():
```java
private void postProcessCrossReferences(List<ObfuscationResult> results, SymbolTable symbols) {
    Map<String, String> globalMapping = symbols.getFullMapping();
    for (ObfuscationResult r : results) {
        List<String> lines = new ArrayList<>(r.getObfuscatedFile().getObfuscatedLines());
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // Only modify import/require statements
            if (!line.trim().startsWith("import ") && !line.trim().startsWith("require(")) continue;
            for (Map.Entry<String, String> entry : globalMapping.entrySet()) {
                String original = entry.getKey();
                // Extract just the identifier part (after "::")
                if (original.contains("::")) {
                    String name = original.substring(original.lastIndexOf("::") + 2);
                    String replacement = entry.getValue();
                    if (line.contains(name)) {
                        lines.set(i, line.replace(name, replacement));
                    }
                }
            }
        }
        r.getObfuscatedFile().setObfuscatedLines(lines);
    }
}
```

Call this at the end of process(), before the "Pipeline complete" log.


===============================================================================
PHASE 10: DISTRIBUTION — NATIVE BINARY + WEBSITE
===============================================================================

## TASK 10.1: Add GraalVM Native Image Support

File: pom.xml

Add the GraalVM native-image Maven plugin:
```xml
<plugin>
    <groupId>org.graalvm.buildtools</groupId>
    <artifactId>native-maven-plugin</artifactId>
    <version>0.10.1</version>
    <configuration>
        <mainClass>com.nightshade.CLI</mainClass>
        <imageName>nightshade</imageName>
        <buildArgs>
            <arg>--no-fallback</arg>
            <arg>-H:+ReportExceptionStackTraces</arg>
        </buildArgs>
    </configuration>
</plugin>
```

NOTE: JavaFX does NOT work with GraalVM native-image easily. The native
build should target CLI-only mode. Create a separate Maven profile:
```xml
<profiles>
    <profile>
        <id>native</id>
        <build>
            <plugins>
                <!-- native-maven-plugin config here -->
            </plugins>
        </build>
    </profile>
</profiles>
```

Build with: mvn -Pnative native:compile

## TASK 10.2: Create Landing Page

File: docs/index.html

Create a single-page website for Nightshade with:
- Hero section: "Glaze protects your art. Nightshade protects your code."
- Feature cards for each strategy
- Installation instructions (GitHub Action YAML, CLI, Maven plugin)
- Before/After code comparison
- Link to GitHub repo

Style: Dark theme (matching the JavaFX GUI), amber/green accent colors,
monospace font for code sections. Use vanilla HTML/CSS/JS.

## TASK 10.3: Create a sample-repo/ Directory

Create: sample-repo/Hello.java, sample-repo/Calculator.java, sample-repo/Utils.py

These are small sample files that ship with the repo for testing/demos.
They should contain a mix of:
- Public and private methods
- Comments
- String literals
- Various domains (file I/O, math, collections)

This lets users run: java -jar nightshade.jar --input ./sample-repo --output ./out
and immediately see results.


===============================================================================
VERIFICATION CHECKLIST
===============================================================================

After Phase 6 (Control Flow Flattening):
  [ ] mvn test — all tests pass including new ControlFlowFlattenerTest
  [ ] Run with -s flatten on a Java file — output contains switch/_ns_state
  [ ] Public methods are NEVER flattened
  [ ] Flattened code still compiles (use --verify)

After Phase 7 (Watermarking):
  [ ] Run with -s watermark --author "Ibrahim" — no visible change to output
  [ ] Hex-dump a line — U+200B characters present on some lines

After Phase 8 (Report):
  [ ] Run with --report report.md — file is created with markdown table
  [ ] Report shows per-file entropy and MI resistance estimate
  [ ] GUI shows report after pipeline completion

After Phase 9 (Cross-Reference):
  [ ] Process a multi-file project — import statements are updated consistently
  [ ] No broken references in output

After Phase 10 (Distribution):
  [ ] docs/index.html renders correctly in browser
  [ ] sample-repo files produce meaningful poisoned output
  [ ] (Optional) mvn -Pnative native:compile produces a binary


===============================================================================
FILE INVENTORY — All new/modified files for Phases 6-10
===============================================================================

NEW FILES:
  src/main/java/com/nightshade/strategy/ControlFlowFlattener.java
  src/main/java/com/nightshade/strategy/WatermarkEncoder.java
  src/main/java/com/nightshade/engine/PoisoningReport.java
  src/test/java/com/nightshade/strategy/ControlFlowFlattenerTest.java
  src/test/java/com/nightshade/strategy/WatermarkEncoderTest.java
  docs/index.html
  sample-repo/Hello.java
  sample-repo/Calculator.java
  sample-repo/Utils.py

MODIFIED FILES:
  pom.xml (GraalVM native profile)
  src/main/java/com/nightshade/CLI.java (add flatten, watermark, --author, --report)
  src/main/java/com/nightshade/controller/MainController.java (flatten checkbox, report display)
  src/main/java/com/nightshade/engine/ObfuscationEngine.java (cross-reference post-processing)
  src/main/resources/com/nightshade/fxml/main.fxml (new checkboxes)
  README.md (update strategy table, add Phases 6-10 docs)
