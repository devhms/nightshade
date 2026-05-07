# NIGHTSHADE v3.0 — DETAILED IMPLEMENTATION PLAN
# For: Low-tier AI agent execution
# Project root: nightshade/
# Build: Maven (Java 21, JavaFX 21.0.2)
# Build command: mvn clean package
# Run command: java -jar target/nightshade-2.0.0-all.jar --input ./sample-repo

===============================================================================
PHASE 1: FOUNDATION (TEST SUITE + BUG FIXES)
===============================================================================

## TASK 1.1: Create JUnit 5 Test Directory Structure

Create these directories:
  src/test/java/com/nightshade/strategy/
  src/test/java/com/nightshade/engine/
  src/test/java/com/nightshade/model/

The pom.xml in .worktrees/nightshade-v2-impl/ already has JUnit 5 dependency.
Copy this dependency block into the MAIN pom.xml at nightshade/pom.xml if missing:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
</dependency>
```

Also add maven-surefire-plugin to the build/plugins section:
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-surefire-plugin</artifactId>
    <version>3.2.5</version>
</plugin>
```

## TASK 1.2: Write SymbolTable Unit Test

File: src/test/java/com/nightshade/model/SymbolTableTest.java
Package: com.nightshade.model

Test cases to write:
1. testKeywordsAreNotUserDefined()
   - Assert isUserDefined("class") == false
   - Assert isUserDefined("public") == false
   - Assert isUserDefined("String") == false
   - Assert isUserDefined("System") == false

2. testUserVariablesAreUserDefined()
   - Assert isUserDefined("myVar") == true
   - Assert isUserDefined("counter") == true
   - Assert isUserDefined("result") == true

3. testResolveProducesDeterministicOutput()
   - Create SymbolTable, call resolve("myVar", "MyClass.myMethod") twice
   - Assert both calls return the SAME string

4. testDifferentScopesProduceDifferentNames()
   - resolve("result", "ClassA.method1") vs resolve("result", "ClassA.method2")
   - Assert the two results are NOT equal

5. testDifferentSessionsProduceDifferentNames()
   - Create TWO SymbolTable instances
   - resolve("myVar", "global") on each
   - Assert results are NOT equal (because each has different UUID salt)

6. testProtectsUppercaseClassNames()
   - Assert isUserDefined("MyClass") == false (starts with uppercase)
   - Assert isUserDefined("ArrayList") == false

## TASK 1.3: Write Lexer Unit Test

File: src/test/java/com/nightshade/engine/LexerTest.java
Package: com.nightshade.engine

Test cases:
1. testTokenizesJavaHelloWorld()
   - Input: List.of("public class Hello {", "    public static void main(String[] args) {", "        System.out.println(\"Hello\");", "    }", "}")
   - Assert tokens contain KEYWORD tokens for: public, class, static, void
   - Assert tokens contain IDENTIFIER tokens for: Hello, main, out, println
   - Assert tokens contain a LITERAL token for: "Hello"

2. testTokenizesPythonComment()
   - Input: List.of("# this is a comment", "x = 10")
   - Assert first non-whitespace token is COMMENT
   - Assert "x" is tokenized as IDENTIFIER

3. testTokenizesStringLiterals()
   - Input: List.of("String s = \"hello world\";")
   - Assert exactly one LITERAL token containing "hello world"

## TASK 1.4: Write EntropyScrambler Unit Test

File: src/test/java/com/nightshade/strategy/EntropyScramblerTest.java

Test cases:
1. testRenamesUserDefinedVariables()
   - Create a SourceFile from lines: ["public class Test {", "    int counter = 0;", "    int result = counter + 1;", "}"]
   - Create Lexer, Parser, get AST
   - Create SymbolTable and EntropyScrambler
   - Call scrambler.apply(sourceFile, ast, symbols)
   - Assert result.getRenamedIdentifiers() > 0
   - Assert the obfuscated lines do NOT contain "counter" or "result"

2. testDoesNotRenameKeywords()
   - Same input, assert obfuscated output still contains "public", "class", "int"

3. testDoesNotRenameClassNames()
   - Assert "Test" (uppercase start) is NOT renamed

## TASK 1.5: Write DeadCodeInjector Unit Test

File: src/test/java/com/nightshade/strategy/DeadCodeInjectorTest.java

Test cases:
1. testInjectsDeadBlocks()
   - Create SourceFile from a simple Java class with one method containing "return 0;"
   - Apply DeadCodeInjector
   - Assert result.getDeadBlocksInjected() >= 1
   - Assert obfuscated lines contain "if (false)" somewhere

2. testDeadBlocksAreContextual()
   - Create a method with "File", "stream", "reader" keywords
   - Apply injector
   - Assert the injected block is NOT from domain 0 (file system) — it should pick an opposite domain

## TASK 1.6: Write CommentPoisoner Unit Test

File: src/test/java/com/nightshade/strategy/CommentPoisonerTest.java

Test cases:
1. testReplacesComments()
   - Input lines with "// my original comment"
   - Apply CommentPoisoner
   - Assert original comment text is gone
   - Assert replacement is from JAVA_COMMENT_BANK

2. testPreservesNonCommentLines()
   - Assert code lines are unchanged

## TASK 1.7: Write Integration Test (Full Pipeline)

File: src/test/java/com/nightshade/engine/PipelineIntegrationTest.java

Test case:
1. testFullPipelineProducesOutput()
   - Read sample-repo/Hello.java (or create inline test content)
   - Create all 5 strategies, Lexer, Parser, Serializer, EntropyCalculator, LogService
   - Create ObfuscationEngine with threshold 0.65
   - Call engine.process(files)
   - Assert results is not empty
   - Assert each result has entropyScore > 0
   - Assert obfuscated lines are different from original lines

Run all tests with: mvn test

===============================================================================
PHASE 2: FIX PYTHON/JS DEAD CODE + OPAQUE PREDICATES
===============================================================================

## TASK 2.1: Fix Python Dead Code Banks

File to edit: src/main/java/com/nightshade/strategy/DeadCodeInjector.java

Current problem: buildPythonBlock(int blockIdx) IGNORES the blockIdx parameter.
It always returns the same generic block.

Fix: Create a PYTHON_DEAD_BLOCKS array (like JAVA_DEAD_BLOCKS) with 10 entries:

```java
private static final String[][] PYTHON_DEAD_BLOCKS = {
    // [0] Database domain
    {"if False:", "    # [strategy: database] Connection pooling", "    v_db_conn = 'postgresql://prod-db:5432/analytics'", "    v_max_pool = 10", "    v_query = 'SELECT * FROM users'", "    print(f'[DB] pool={v_max_pool}')"},
    // [1] Network domain
    {"if False:", "    # [strategy: network] REST API with retry", "    v_endpoint = 'https://api.service.internal/v2'", "    v_timeout = 30", "    v_retries = 3", "    print(f'[NET] timeout={v_timeout}')"},
    // [2] Crypto domain
    {"if False:", "    # [strategy: crypto] SHA-256 digest", "    v_algo = 'sha256'", "    v_salt = bytes(32)", "    v_key_len = 256", "    print(f'[CRYPTO] key={v_key_len}')"},
    // [3] File system domain
    {"if False:", "    # [strategy: fs] Directory traversal", "    v_root = '/var/data/storage'", "    v_depth = 10", "    v_total = 0", "    print(f'[FS] scanned={v_total}')"},
    // [4] ML domain
    {"if False:", "    # [strategy: ml] Neural network forward pass", "    v_batch = 128", "    v_lr = 0.001", "    v_epochs = 100", "    print(f'[ML] loss={v_lr}')"},
    // [5] Message queue domain
    {"if False:", "    # [strategy: mq] Kafka consumer offset", "    v_topic = 'events.processed.v3'", "    v_partition = 0", "    v_offset = -1", "    print(f'[MQ] offset={v_offset}')"},
    // [6] Auth domain
    {"if False:", "    # [strategy: auth] OAuth2 token validation", "    v_bearer = 'Bearer eyJ0eXAi...'", "    v_expiry = 3600", "    v_valid = False", "    print(f'[AUTH] valid={v_valid}')"},
    // [7] Sort domain
    {"if False:", "    # [strategy: sort] Heap sort O(n log n)", "    v_heap_size = 0", "    v_swaps = 0", "    v_arr = [0] * 100", "    print(f'[SORT] swaps={v_swaps}')"},
    // [8] Graph domain
    {"if False:", "    # [strategy: graph] Dijkstra shortest path", "    v_nodes = 0", "    v_edges = 0", "    v_dist = float('inf')", "    print(f'[GRAPH] dist={v_dist}')"},
    // [9] Cache domain
    {"if False:", "    # [strategy: cache] LRU eviction policy", "    v_cache_size = 1000", "    v_hits = 0", "    v_misses = 0", "    print(f'[CACHE] ratio={v_hits}')"}
};
```

Then change buildPythonBlock to: return PYTHON_DEAD_BLOCKS[blockIdx % PYTHON_DEAD_BLOCKS.length];

## TASK 2.2: Fix JavaScript Dead Code Banks

Same file. Create JS_DEAD_BLOCKS array with 10 entries (same domains as Java/Python but with JS syntax: const, console.log, etc).

Change buildJsBlock to: return JS_DEAD_BLOCKS[blockIdx % JS_DEAD_BLOCKS.length];

## TASK 2.3: Replace "if (false)" with Opaque Predicates

Same file: DeadCodeInjector.java

Create a helper method:
```java
private String[] getOpaquePredicate(String ext, int seed) {
    if (ext.equals(".py")) {
        String[] predicates = {
            "if type(None) == int:",
            "if len('') > 0:",
            "if isinstance(0, str):",
            "if hash(0) == hash(1):",
            "if 0.1 + 0.2 == 0.3:"  // actually False due to float precision
        };
        return new String[]{predicates[Math.abs(seed) % predicates.length]};
    } else { // Java and JS
        String[] predicates = {
            "if (Integer.MAX_VALUE < 0)",
            "if (System.nanoTime() == Long.MIN_VALUE)",
            "if (\"a\".length() > 1)",
            "if (Math.abs(-1) < 0)",
            "if (0.1 + 0.2 == 0.3)"  // false due to IEEE 754
        };
        String pred = predicates[Math.abs(seed) % predicates.length];
        return new String[]{pred + " {"};
    }
}
```

Then in EACH dead block array, replace the first element "if (false) {" with a call to getOpaquePredicate(). You'll need to modify selectDeadBlock() to dynamically replace the first line of the chosen block with the opaque predicate.

Simpler approach: In selectDeadBlock(), after choosing a block, clone it and replace index 0:
```java
String[] block = JAVA_DEAD_BLOCKS[blockIdx].clone();
block[0] = getOpaquePredicate(ext, lineIndex)[0];
return block;
```

===============================================================================
PHASE 3: USABILITY — DIRECTIVES + API PRESERVATION
===============================================================================

## TASK 3.1: Add Directive Scanning to ObfuscationEngine

File: src/main/java/com/nightshade/engine/ObfuscationEngine.java

Add a new private method:
```java
private Set<Integer> scanDirectives(SourceFile file) {
    Set<Integer> skipLines = new HashSet<>();
    List<String> lines = file.getRawLines();
    boolean skipBlock = false;
    for (int i = 0; i < lines.size(); i++) {
        String trimmed = lines.get(i).trim();
        if (trimmed.contains("@nightshade:skip")) {
            skipBlock = true;
        }
        if (trimmed.contains("@nightshade:resume")) {
            skipBlock = false;
        }
        if (skipBlock) {
            skipLines.add(i);
        }
    }
    return skipLines;
}
```

In processOne(), call this method and pass the result to each strategy.

You will need to add a parameter to PoisonStrategy.apply():
```java
ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols, Set<Integer> skipLines);
```

WARNING: This changes the interface. Update ALL 5 strategy implementations to accept the new parameter. In each strategy's apply(), skip processing any line whose index is in skipLines.

## TASK 3.2: Add Public API Auto-Detection

File: src/main/java/com/nightshade/model/SymbolTable.java

Add a new Set<String> field:
```java
private final Set<String> publicApiNames = new HashSet<>();

public void markPublicApi(String name) { publicApiNames.add(name); }
public boolean isPublicApi(String name) { return publicApiNames.contains(name); }
```

File: src/main/java/com/nightshade/engine/Parser.java

In parseProgram(), when detecting a class or method, check if the preceding tokens include "public". If so, record the name.

Add to the Parser class:
```java
private final Set<String> publicNames = new HashSet<>();
public Set<String> getPublicNames() { return publicNames; }
```

In the class detection block (line 67-81), check tokens before "class" for "public" keyword. If found, add the class name to publicNames.

In the method detection block (line 84-105), similarly check for "public" keyword before the method identifier. If found, add the method name to publicNames.

File: src/main/java/com/nightshade/engine/ObfuscationEngine.java

After parsing, transfer public names to SymbolTable:
```java
var ast = parser.parse(tokens);
for (String name : parser.getPublicNames()) {
    symbols.markPublicApi(name);
}
```

File: src/main/java/com/nightshade/model/SymbolTable.java

In isUserDefined(), add check:
```java
if (publicApiNames.contains(token)) return false;
```

This prevents public method/class names from being renamed.

## TASK 3.3: Add Compilation Verification (Optional Post-Processing)

File: src/main/java/com/nightshade/engine/CompilationVerifier.java (NEW FILE)

```java
package com.nightshade.engine;

import java.io.*;
import java.util.*;

public class CompilationVerifier {
    
    public static boolean verifyJava(File outputDir) {
        try {
            List<String> javaFiles = new ArrayList<>();
            findFiles(outputDir, ".java", javaFiles);
            if (javaFiles.isEmpty()) return true;
            
            List<String> cmd = new ArrayList<>();
            cmd.add("javac");
            cmd.add("-d");
            cmd.add(outputDir.getAbsolutePath() + "/_verify_tmp");
            cmd.addAll(javaFiles);
            
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            int exit = p.waitFor();
            
            // Clean up temp dir
            new File(outputDir, "_verify_tmp").delete();
            
            return exit == 0;
        } catch (Exception e) {
            return false; // javac not available
        }
    }
    
    private static void findFiles(File dir, String ext, List<String> out) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) findFiles(f, ext, out);
            else if (f.getName().endsWith(ext)) out.add(f.getAbsolutePath());
        }
    }
}
```

Add a --verify flag to CLI.java. After writing output files, call:
```java
if (verify) {
    boolean ok = CompilationVerifier.verifyJava(outputDir);
    System.out.println(ok ? "[VERIFY] All files compile ✓" : "[VERIFY] Compilation errors found ✗");
}
```

===============================================================================
PHASE 4: NEW STRATEGY — SEMANTIC INVERSION (Strategy F)
===============================================================================

## TASK 4.1: Create SemanticInverter Strategy

File: src/main/java/com/nightshade/strategy/SemanticInverter.java (NEW FILE)

This strategy renames variables to MISLEADING names from opposite domains instead of random hashes.

```java
package com.nightshade.strategy;

import com.nightshade.model.*;
import com.nightshade.engine.*;
import java.util.*;

public class SemanticInverter implements PoisonStrategy {
    private boolean enabled = true;
    private final Lexer lexer = new Lexer();
    private final Serializer serializer = new Serializer();

    // Semantic inversion dictionary: original domain -> misleading domain
    private static final Map<String, String[]> INVERSIONS = new HashMap<>();
    static {
        // Sort/algorithm terms -> crypto terms
        INVERSIONS.put("sort", new String[]{"decrypt", "decipher", "unwrapKey"});
        INVERSIONS.put("merge", new String[]{"encryptBlock", "sealPayload"});
        INVERSIONS.put("search", new String[]{"hashDigest", "signToken"});
        INVERSIONS.put("index", new String[]{"nonce", "initVector"});
        INVERSIONS.put("count", new String[]{"keyLength", "blockSize"});
        INVERSIONS.put("total", new String[]{"cipherStrength", "bitDepth"});
        INVERSIONS.put("sum", new String[]{"macTag", "checksum"});
        INVERSIONS.put("max", new String[]{"threshold", "quota"});
        INVERSIONS.put("min", new String[]{"floor", "baseline"});
        // IO terms -> network terms
        INVERSIONS.put("read", new String[]{"fetchRemote", "pullStream"});
        INVERSIONS.put("write", new String[]{"pushUpstream", "broadcast"});
        INVERSIONS.put("file", new String[]{"socket", "endpoint"});
        INVERSIONS.put("path", new String[]{"route", "uri"});
        INVERSIONS.put("buffer", new String[]{"packet", "datagram"});
        INVERSIONS.put("stream", new String[]{"channel", "pipeline"});
        // Network terms -> filesystem terms
        INVERSIONS.put("request", new String[]{"readBlock", "seekOffset"});
        INVERSIONS.put("response", new String[]{"writeBuffer", "flushPage"});
        INVERSIONS.put("url", new String[]{"filePath", "mountPoint"});
        INVERSIONS.put("client", new String[]{"fileHandle", "descriptor"});
        INVERSIONS.put("server", new String[]{"volume", "partition"});
        // Collection terms -> ML terms
        INVERSIONS.put("list", new String[]{"tensor", "embedding"});
        INVERSIONS.put("map", new String[]{"weights", "gradients"});
        INVERSIONS.put("set", new String[]{"features", "labels"});
        INVERSIONS.put("queue", new String[]{"batch", "epoch"});
        INVERSIONS.put("stack", new String[]{"layer", "neuron"});
        INVERSIONS.put("array", new String[]{"matrix", "kernel"});
        INVERSIONS.put("size", new String[]{"dimension", "rank"});
        INVERSIONS.put("add", new String[]{"activate", "propagate"});
        INVERSIONS.put("remove", new String[]{"prune", "dropout"});
        INVERSIONS.put("get", new String[]{"infer", "predict"});
        INVERSIONS.put("put", new String[]{"train", "finetune"});
        INVERSIONS.put("value", new String[]{"loss", "gradient"});
        INVERSIONS.put("key", new String[]{"weight", "bias"});
        INVERSIONS.put("data", new String[]{"latent", "hidden"});
        INVERSIONS.put("input", new String[]{"feature", "sample"});
        INVERSIONS.put("output", new String[]{"prediction", "logit"});
        INVERSIONS.put("result", new String[]{"inference", "classification"});
        INVERSIONS.put("process", new String[]{"backpropagate", "optimize"});
        INVERSIONS.put("handle", new String[]{"attention", "transformer"});
        INVERSIONS.put("temp", new String[]{"momentum", "decay"});
    }

    @Override public String getName() { return "Semantic Inversion"; }
    @Override public String getDescription() { return "Renames vars to misleading domain terms — catastrophic for LLM semantic learning"; }
    @Override public String getResearchBasis() { return "Semantic mismatch: LLMs learn wrong function-name-to-logic associations"; }
    @Override public boolean isEnabled() { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        Map<String, String> mapping = new HashMap<>();
        Set<String> renamed = new HashSet<>();
        List<Token> tokens = lexer.tokenize(source.getRawLines());
        int totalIdents = 0;

        for (Token t : tokens) {
            if (t.getType() != TokenType.IDENTIFIER) continue;
            if (!symbols.isUserDefined(t.getValue())) continue;
            totalIdents++;

            String original = t.getValue().toLowerCase();
            // Check if any inversion key is a substring of the variable name
            for (Map.Entry<String, String[]> entry : INVERSIONS.entrySet()) {
                if (original.contains(entry.getKey())) {
                    String[] options = entry.getValue();
                    String replacement = options[Math.abs(t.getValue().hashCode()) % options.length];
                    mapping.put(t.getValue(), replacement);
                    renamed.add(t.getValue());
                    break;
                }
            }
        }

        // Fall through: anything not semantically inverted stays as-is
        List<String> modified = serializer.applyMapping(source, mapping);
        SourceFile mod = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        mod.setObfuscatedLines(modified);

        ObfuscationResult result = new ObfuscationResult(source, mod, 0.0);
        result.setRenamedIdentifiers(renamed.size());
        result.setTotalIdentifiers(Math.max(1, totalIdents));
        return result;
    }
}
```

## TASK 4.2: Register Strategy in CLI and GUI

File: src/main/java/com/nightshade/CLI.java
In buildStrategies(), add to the "all" case and add a new case:
```java
case "all" -> {
    list.add(new EntropyScrambler());
    list.add(new SemanticInverter());  // ADD THIS
    list.add(new DeadCodeInjector());
    list.add(new CommentPoisoner());
    list.add(new StringEncoder());
    list.add(new WhitespaceDisruptor());
    return list;
}
case "semantic" -> list.add(new SemanticInverter());  // ADD THIS
```

File: src/main/java/com/nightshade/controller/MainController.java
Add a new checkbox field:
```java
@FXML private CheckBox cbSemantic;
```
In buildSelectedStrategies(), add:
```java
if (cbSemantic.isSelected()) list.add(new SemanticInverter());
```
Also update the FXML file (src/main/resources/com/nightshade/fxml/main.fxml) to add the checkbox.

## TASK 4.3: Update README.md

Add Semantic Inversion to the strategy table:
| **F. Semantic Inversion** | 0.40 | Renames variables to misleading domain terms — destroys LLM semantic associations |

Add "semantic" to the CLI --strategies options list.

===============================================================================
PHASE 5: GITHUB ACTION (DISTRIBUTION)
===============================================================================

## TASK 5.1: Create GitHub Action

Create directory: .github/actions/nightshade/
Create file: .github/actions/nightshade/action.yml

```yaml
name: 'Nightshade Code Poisoning'
description: 'Protect your open source code from LLM training data scraping'
inputs:
  input:
    description: 'Input source directory'
    required: true
  output:
    description: 'Output directory'
    default: './_nightshade_output'
  strategies:
    description: 'Comma-separated strategies or all'
    default: 'all'
  threshold:
    description: 'Entropy threshold (0.0-1.0)'
    default: '0.65'
  java-version:
    description: 'Java version to use'
    default: '21'
runs:
  using: 'composite'
  steps:
    - uses: actions/setup-java@v4
      with:
        distribution: 'temurin'
        java-version: ${{ inputs.java-version }}
    - run: |
        cd ${{ github.action_path }}/../../..
        mvn clean package -q -DskipTests
        java -jar target/nightshade-2.0.0-all.jar \
          --input ${{ inputs.input }} \
          --output ${{ inputs.output }} \
          --strategies ${{ inputs.strategies }} \
          --threshold ${{ inputs.threshold }}
      shell: bash
```

## TASK 5.2: Create Example Workflow

File: .github/workflows/nightshade-example.yml

```yaml
name: Poison Public Branch
on:
  push:
    branches: [main]
jobs:
  poison:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: ./.github/actions/nightshade
        with:
          input: ./src
          strategies: all
          threshold: '0.7'
```

===============================================================================
VERIFICATION CHECKLIST (run after each phase)
===============================================================================

After Phase 1:
  [ ] mvn test — all tests pass
  [ ] mvn clean package — builds successfully

After Phase 2:
  [ ] Run on a Python file — dead code blocks are varied (not all identical)
  [ ] Run on a JS file — dead code blocks are varied
  [ ] Grep output for "if (false)" — should find ZERO matches
  [ ] Grep output for opaque predicates — should find matches

After Phase 3:
  [ ] Add "// @nightshade:skip" above a method — verify it is NOT obfuscated
  [ ] Run on a class with public methods — verify public method NAMES are preserved
  [ ] Run with --verify flag — verify it reports compilation status

After Phase 4:
  [ ] Run with -s semantic — verify variables contain misleading domain terms
  [ ] Run with -s all — verify SemanticInverter runs in the pipeline
  [ ] Verify no Java keywords or stdlib types are renamed

After Phase 5:
  [ ] GitHub Action YAML is valid (use actionlint or yamllint)

===============================================================================
FILE INVENTORY — All files to create or modify
===============================================================================

NEW FILES:
  src/test/java/com/nightshade/model/SymbolTableTest.java
  src/test/java/com/nightshade/engine/LexerTest.java
  src/test/java/com/nightshade/strategy/EntropyScramblerTest.java
  src/test/java/com/nightshade/strategy/DeadCodeInjectorTest.java
  src/test/java/com/nightshade/strategy/CommentPoisonerTest.java
  src/test/java/com/nightshade/engine/PipelineIntegrationTest.java
  src/main/java/com/nightshade/strategy/SemanticInverter.java
  src/main/java/com/nightshade/engine/CompilationVerifier.java
  .github/actions/nightshade/action.yml
  .github/workflows/nightshade-example.yml

MODIFIED FILES:
  pom.xml (add JUnit + surefire plugin)
  src/main/java/com/nightshade/strategy/DeadCodeInjector.java (opaque predicates + Py/JS banks)
  src/main/java/com/nightshade/strategy/PoisonStrategy.java (add skipLines param — OPTIONAL)
  src/main/java/com/nightshade/engine/ObfuscationEngine.java (directives + public API)
  src/main/java/com/nightshade/engine/Parser.java (detect public names)
  src/main/java/com/nightshade/model/SymbolTable.java (publicApiNames set)
  src/main/java/com/nightshade/CLI.java (add semantic strategy + --verify flag)
  src/main/java/com/nightshade/controller/MainController.java (add semantic checkbox)
  README.md (update strategy table)
