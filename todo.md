# 🌙 Nightshade — Stability & User-Friendliness Hardening Plan

> **Goal:** Make the current v3.5.0 work **100% reliably** with zero surprises for any user — before adding any new features.
> **Audience:** This is an actionable checklist for an AI agent. Every item has exact file paths, line numbers, and verification commands.
> **Methodology:** Every item below was found by a line-by-line audit of all 30 source files, 8 test files, CLI runtime, and documentation.

---

## 🔴 TIER 1: BUG FIXES (Things that are currently broken)

> These issues will cause errors, crashes, or incorrect behavior for real users today.

---

### 1.1 Empty Directory Produces Silent "Success" Instead of Warning
- **File:** `CLI.java` lines 129-133
- **Bug:** When a user points `--input` at a directory with no `.java/.py/.js` files, the tool prints `[INFO] Discovered 0 source files.`, runs the full pipeline producing zero results, and displays the success box with `Files processed: 0`. The user gets no hint that something is wrong.
- **Fix:** After `walker.walk()` returns, check `if (files.isEmpty())`. Print a clear warning: `[WARN] No supported source files found (.java, .py, .js) in: <path>`. If `--verbose`, also print the list of skipped directories.
- **User impact:** A user who misspells their path gets no feedback at all.
- **Verify:** `java -jar nightshade.jar -i empty-src/` should print a clear warning and exit cleanly (exit code 0 but with warning).

---

### 1.2 Invalid Strategy Names Are Silently Ignored
- **File:** `CLI.java` `buildStrategies()` lines 183-210
- **Bug:** If a user types `-s entropy,bogus`, the `buildStrategies()` switch has no `default` case. The string `"bogus"` is silently ignored and only `entropy` is loaded. The user has no idea their requested strategy wasn't found.
- **Fix:** Add a `default` case inside the per-strategy switch that prints: `[WARN] Unknown strategy 'bogus' — skipping. Valid options: entropy, deadcode, comments, strings, whitespace, semantic, controlflow, watermark`
- **User impact:** User thinks they're running 3 strategies but only 1 is active.
- **Verify:** `java -jar nightshade.jar -i src/ -s entropy,bogus` should print a warning about `bogus`.

---

### 1.3 Error Message Lists Incomplete Strategy Names
- **File:** `CLI.java` line 110
- **Bug:** The error message reads: `"Options: all, entropy, deadcode, comments, strings, whitespace"` — it's missing `semantic`, `controlflow`, and `watermark`.
- **Fix:** Update to: `"Options: all, entropy, deadcode, comments, strings, whitespace, semantic, controlflow, watermark"`
- **Verify:** Run `java -jar nightshade.jar -i src/ -s ""` and confirm the error message lists all 8 strategy names.

---

### 1.4 `--verify` on Non-Java Files Always Returns "SUCCESS"
- **File:** `CompilationVerifier.java` lines 27-29
- **Bug:** If the user processes Python or JavaScript files with `--verify`, the verifier finds zero `.java` files and returns `true` — printing `[VERIFY] SUCCESS` even though no verification actually occurred. This is misleading.
- **Fix:** When `javaFiles.isEmpty()` and the output directory contains non-Java files, print: `[VERIFY] SKIPPED: No Java files found. Compilation verification only applies to .java files.` This is honest feedback.
- **Verify:** `java -jar nightshade.jar -i python-src/ --verify` should say SKIPPED, not SUCCESS.

---

### 1.5 Single-File Input Creates Wrong Output Path
- **File:** `CLI.java` lines 94-97
- **Bug:** When `--input` is a single file like `src/HelloWorld.java` and `--output` is omitted, the default output path is computed as `parent + "/_nightshade_output"`. But if the file is in the current directory (e.g., `-i HelloWorld.java`), `getParentFile()` can return `null`, causing the output to be placed at `HelloWorld.java/_nightshade_output` (nonsensical).
- **Fix:** When parent is null, use the current working directory: `new File(".").getAbsolutePath() + "/_nightshade_output"`.
- **Verify:** `java -jar nightshade.jar -i ./sample-src/HelloWorld.java` without `-o` should create `./sample-src/_nightshade_output/HelloWorld.java`.

---

### 1.6 `FileUtil.write()` Crashes When Input Is a Single File
- **File:** `FileUtil.java` `computeRelativePath()` lines 121-129
- **Bug:** When the `inputRoot` is a single file (not a directory), `computeRelativePath()` compares the obfuscated file's absolute path against the input file's absolute path. Since the paths are the same, `absoluteFile.startsWith(absoluteRoot + separator)` fails, and it falls back to just `getName()`. This works, but if `inputRoot` is `C:\src\HelloWorld.java`, the `File.separator` appending makes the root `C:\src\HelloWorld.java\` which can never match. The fallback saves it, but it's fragile.
- **Fix:** In `CLI.java`, when `inputDir.isFile()`, set the effective input root to `inputDir.getParentFile()` so that `computeRelativePath()` has a proper directory to work with.
- **Verify:** Process a single file and confirm the output is placed correctly in the output directory (not nested weirdly).

---

### 1.7 `--dry-run` Still Shows "Output: ..." in Summary Box
- **File:** `CLI.java` lines 160-174
- **Bug:** In dry-run mode, the tool correctly writes 0 files, but the summary box still shows `Output: <path>` which is confusing — the user wonders "did it write files or not?"
- **Fix:** In dry-run mode, change the box to say: `DRY RUN — no files were written.` instead of showing the output path.
- **Verify:** `java -jar nightshade.jar -i src/ --dry-run` summary should clearly indicate no files were written.

---

### 1.8 Banner Shows Garbled Unicode on Non-UTF8 Terminals (Windows CMD)
- **File:** `CLI.java` lines 38-47
- **Bug:** The ASCII art banner uses Unicode box-drawing characters (`███╗`, `╔══`). On Windows CMD (which defaults to CP437/OEM encoding), these render as `????` garbage characters. This is the first thing any Windows user sees.
- **Fix:** Detect the terminal encoding at startup. If the console doesn't support UTF-8, fall back to a simple ASCII banner:
  ```
  NIGHTSHADE v3.5.0
  LLM Training Data Poisoning Engine
  https://github.com/devhms/nightshade
  ```
  Detection: `System.console() != null && Charset.defaultCharset().name().contains("UTF")` or check `System.getProperty("stdout.encoding")`.
- **Verify:** Run from Windows CMD — banner should be readable, not garbled.

---

### 1.9 Summary Box Columns Are Misaligned for Long Paths
- **File:** `CLI.java` lines 160-174
- **Bug:** The `truncate()` function pads/truncates to 32 chars, but the box border `║` is at position 44. When the output path is exactly 32 chars, the alignment works. But for paths shorter than 32 chars or longer, the right `║` border drifts. Tested: the box is already misaligned in our test runs (the `ms` and `║` don't line up).
- **Fix:** Use `String.format()` with fixed-width fields for ALL rows in the summary box, not just some. The `truncate()` approach is brittle. Replace with:
  ```java
  System.out.printf("║  %-40s║%n", "Output: " + truncate(path, 32));
  ```
- **Verify:** Run on a file with a very short path and a very long path — both should produce aligned boxes.

---

## 🟡 TIER 2: USER EXPERIENCE IMPROVEMENTS (Confusing but not broken)

> These won't crash, but they confuse users or make the tool feel unprofessional.

---

### 2.1 No Progress Indicator for Large Projects
- **File:** `ObfuscationEngine.java` lines 72-92
- **Current behavior:** For a 500-file project, the user sees `[INFO] Discovered 500 source files.` then silence for potentially minutes, then a flood of `[DONE]` messages.
- **Fix:** Add a simple progress counter that prints every N files: `[INFO] Progress: 50/500 files processed (10%)...`. In verbose mode, show per-file progress. In normal mode, show every 10% milestone.
- **User impact:** User thinks the tool has hung on large projects.

---

### 2.2 `--help` Should Be Shown When No Args AND No GUI Available
- **File:** `Main.java` lines 59-65
- **Current behavior:** When run with no arguments, it tries to launch the JavaFX GUI. If JavaFX isn't on the classpath (headless server, Docker, CI), it crashes with a stack trace.
- **Fix:** Wrap the `launch()` call in a try-catch for `UnsupportedOperationException` or `NoClassDefFoundError`. On failure, print the help message + a note: `[INFO] GUI unavailable (headless environment). Use --help for CLI usage.`
- **User impact:** CI/Docker users who just run `java -jar nightshade.jar` get an unhelpful crash.

---

### 2.3 Strategies Marked "Disabled by Default" Are Not Discoverable
- **File:** `SemanticInverter.java` line 23, `ControlFlowFlattener.java` line 9, `WatermarkEncoder.java` line 10
- **Bug:** These 3 strategies have `enabled = false`. When the user runs `-s all`, they ARE added to the list but `isEnabled()` returns false, so the engine skips them silently. The user sees `Strategies enabled: 5/8` but has no idea WHICH 3 are disabled or how to enable them.
- **Fix:** Two changes needed:
  1. In CLI strategy info printout, mark disabled strategies: `• Semantic Inversion (disabled — use -s semantic to enable)`
  2. When a strategy is explicitly listed in `-s`, force-enable it: `strategy.setEnabled(true)` in `buildStrategies()`.
- **Verify:** `java -jar nightshade.jar -i src/ -s semantic` should actually run the Semantic Inversion strategy.

---

### 2.4 Add `--list-strategies` Flag for Discovery
- **File:** `CLI.java`
- **Missing feature:** Users have no way to discover what strategies exist, what they do, or which are enabled without reading the source code.
- **Fix:** Add a `--list-strategies` flag that prints a formatted table:
  ```
  Available Strategies:
    ID            Name                         Status    Description
    entropy       Variable Entropy Scrambling   ON       Renames identifiers...
    deadcode      Dead Code Injection           ON       Injects unreachable...
    semantic      Semantic Inversion            OFF      Replaces variables...
    controlflow   Control Flow Flattening       OFF      Rewrites method bodies...
    watermark     Watermark Encoder             OFF      Embeds steganographic...
  ```
- **User impact:** Currently the only way to know strategy IDs is to read `--help` or the README.

---

### 2.5 Entropy Threshold Not Validated
- **File:** `CLI.java` line 73
- **Bug:** The user can pass `--threshold 5.0` or `--threshold -1.0` with no validation. A threshold of `5.0` means the early-exit condition is never met (all strategies always run). A threshold of `-1.0` means the first strategy always triggers early exit.
- **Fix:** After parsing, validate: `if (entropyThreshold < 0.0 || entropyThreshold > 1.0)` → print error and exit.
- **Verify:** `java -jar nightshade.jar -i src/ --threshold 5.0` should print `[ERROR] Entropy threshold must be between 0.0 and 1.0`.

---

### 2.6 Add Color to CLI Output
- **File:** `CLI.java` and `LogService.java`
- **Missing feature:** All output is monochrome plain text. Professional CLI tools use ANSI colors:
  - `[INFO]` → default/white
  - `[WARN]` → yellow
  - `[ERROR]` → red
  - `[DONE]` → green
  - `[VERIFY] SUCCESS` → green
  - `[VERIFY] FAILED` → red
- **Fix:** Add an ANSI color utility class. Auto-detect whether the terminal supports ANSI (most do in 2026, except Windows CMD without VT mode). Disable colors with `--no-color` flag or `NO_COLOR` environment variable (standard: https://no-color.org/).
- **User impact:** The current monochrome output is functional but looks amateurish compared to tools like cargo, npm, or gradle.

---

### 2.7 Run Summary Should Include Strategy Breakdown
- **File:** `CLI.java` lines 160-174
- **Missing feature:** The summary box only shows aggregate stats. The user can't see which strategy contributed what.
- **Fix:** After the main box, print a per-strategy breakdown:
  ```
  Strategy Breakdown:
    ✓ Entropy Scrambling   — 7 identifiers renamed
    ✓ Dead Code Injection  — 2 blocks injected
    ✓ Comment Poisoning    — 4 comments replaced
    ○ String Encoding      — skipped (early exit)
    ○ Whitespace Disruption — skipped (early exit)
  ```
- **User impact:** Users currently have no visibility into what actually happened unless they use `--verbose`.

---

### 2.8 `--verbose` Floods Output — Add `--quiet` Mode Too
- **File:** `CLI.java`, `LogService.java`
- **Problem:** There are only two modes: normal (some info) and verbose (everything including debug). For CI pipelines, users want QUIET mode — only errors and the final summary.
- **Fix:** Add `--quiet` / `-q` flag that suppresses everything except `[ERROR]` and the final summary box. The LogService already has level-based filtering — extend it with a `QUIET` mode.

---

## 🟢 TIER 3: RELIABILITY & DEFENSIVE CODING (Edge cases that can bite)

> These are not bugs today but will break for specific inputs.

---

### 3.1 DeadCodeInjector Brace Counting Is Fragile
- **File:** `DeadCodeInjector.java` `findReturnStatements()` lines 177-208
- **Problem:** The brace-depth tracker counts `{` and `}` characters in the raw string, including those inside string literals and comments. For code like `String s = "}{}{";`, the depth counter will be completely wrong, causing dead code to be injected in the wrong place.
- **Fix:** Use the Lexer to tokenize the file first, then count braces only in `TokenType.SYMBOL` tokens, not raw characters.
- **Verify:** Create a test file with `String s = "}{";` inside a method and confirm dead code is injected in the correct location.

---

### 3.2 ControlFlowFlattener Produces Broken Code for Methods with Local Variables
- **File:** `ControlFlowFlattener.java` lines 62-77
- **Problem:** The flattener puts each statement into a `case N:` block, but Java scoping rules mean that a variable declared in `case 0:` is not accessible in `case 1:` without a shared scope. The current output appends all statements on the same line as the `case`, which means local variable declarations like `int x = 5;` become visible only within that case. If a later case references `x`, it will fail to compile.
- **Fix:** Either: (a) declare all local variables before the switch statement, or (b) wrap the entire switch body in a single scope `{...}`, or (c) only flatten methods with no local variable declarations.
- **This is critical for `--verify` reliability.** The flattener is currently disabled by default, which masks this bug — but if a user explicitly enables it with `-s controlflow`, their code will fail to compile.
- **Verify:** Enable controlflow, process a method with `int x = 5; int y = x + 1; return y;`, and confirm `--verify` passes.

---

### 3.3 Serializer `applyMapping()` Mutates the Input SourceFile
- **File:** `Serializer.java` line 94
- **Bug:** `applyMapping()` calls `source.setObfuscatedLines(result)` — it MUTATES the input `SourceFile`. This is a side effect that violates the documented contract ("rawLines is immutable; obfuscatedLines is set exactly once by the engine pipeline"). Since `EntropyScrambler` and `SemanticInverter` both call `applyMapping()`, if both are enabled in the same pipeline, the second strategy sees the MUTATED lines from the first, but the SourceFile passed to the second strategy is supposed to be a COPY.
- **Fix:** Remove the side-effect `source.setObfuscatedLines(result)` from `applyMapping()`. The caller (strategy) already creates a new SourceFile and sets the obfuscated lines — the serializer should only return the list, not mutate the input.
- **Risk:** This is a correctness bug that only manifests when running EntropyScrambler + SemanticInverter together (rare since SemanticInverter is disabled by default).

---

### 3.4 StringEncoder Encodes Strings Inside Dead Code Blocks
- **File:** `StringEncoder.java`
- **Problem:** The dead code blocks injected by `DeadCodeInjector` contain strings like `"jdbc:mysql://prod-db..."`. When `StringEncoder` runs after `DeadCodeInjector` in the pipeline, it encodes these dead-code strings into `new String(new char[]{...})`, which makes the dead code blocks much harder to read and potentially bloats file size. Dead code strings should be left alone since they're part of the poisoning strategy.
- **Fix:** Skip lines that contain `if (false)` or are inside an `if (false)` block.

---

### 3.5 CommentPoisoner Doesn't Handle Multi-Line Comments
- **File:** `CommentPoisoner.java` lines 68-69
- **Problem:** The regex patterns only match single-line comments (`// ...` or `# ...`). Block comments (`/* ... */`) and Javadoc (`/** ... */`) are completely ignored. This means Javadoc — which is the MOST valuable comment type for LLM training — passes through unpoisoned.
- **Fix:** Add block-comment detection: track `inBlockComment` state, and replace the content of `/* */` blocks with misleading Javadoc.
- **User impact:** The most impactful comments for LLM training are untouched.

---

### 3.6 WhitespaceDisruptor Should Not Touch Python Files (Confirmed Working But Undocumented)
- **File:** `WhitespaceDisruptor.java` lines 47-54
- **Status:** The code correctly skips `.py` files (Python whitespace is semantic), but this is NOT documented in the `--help` or README.
- **Fix:** Add a note to `--help`: `"Note: whitespace strategy is automatically skipped for Python files (indentation is semantic)."`

---

## 🔵 TIER 4: TEST COVERAGE GAPS (Current: 22 tests, ~56% coverage)

> The existing tests pass (22/22 ✅) but major logic paths are untested.

---

### 4.1 Missing Tests — High Priority
- [ ] **CLI argument parsing tests** (`CLITest.java` — does not exist)
  - Test: `--input` without value → exit code 1
  - Test: no `--input` → exit code 1 with help message
  - Test: `--threshold 5.0` → exit code 1 (after validation fix)
  - Test: `-s bogus` → warning printed
  - Test: `--version` → prints version and returns
  - Test: `--help` → prints help and returns
  - Test: single file input → processes correctly
  - Test: empty directory input → warning printed

- [ ] **FileWalker tests** (`FileWalkerTest.java` — does not exist)
  - Test: directory with mixed extensions → only .java/.py/.js returned
  - Test: single file input → returns exactly 1 file
  - Test: empty directory → returns empty list
  - Test: nested directories with `.git` → `.git` skipped
  - Test: file with no extension → skipped

- [ ] **CompilationVerifier tests** (`CompilationVerifierTest.java` — does not exist)
  - Test: valid Java file → returns true
  - Test: invalid Java syntax → returns false
  - Test: empty directory → returns true
  - Test: non-Java files only → returns true (or SKIPPED after fix)

- [ ] **FileUtil tests** (`FileUtilTest.java` — does not exist)
  - Test: `computeRelativePath()` with matching root → correct relative path
  - Test: `computeRelativePath()` with non-matching root → fallback to filename
  - Test: `writeRunLog()` → produces valid log file

- [ ] **StringEncoder tests** (`StringEncoderTest.java` — does not exist)
  - Test: encodes a simple string literal
  - Test: skips strings longer than 80 chars
  - Test: handles escape characters in strings
  - Test: skips comment-only lines

- [ ] **WhitespaceDisruptor tests** (`WhitespaceDisruptorTest.java` — does not exist)
  - Test: K&R to Allman style conversion
  - Test: Python files are skipped
  - Test: `@nightshade:skip` blocks are preserved

- [ ] **SemanticInverter tests** (`SemanticInverterTest.java` — does not exist)
  - Test: variables are renamed to misleading terms
  - Test: keywords are not renamed

### 4.2 Missing Tests — Medium Priority
- [ ] **PoisoningReport tests** (`PoisoningReportTest.java` — does not exist)
  - Test: generates valid markdown table
  - Test: handles zero results without division by zero

- [ ] **EntropyCalculator tests** (`EntropyCalculatorTest.java` — does not exist)
  - Test: `safeDivide(0, 0)` returns 0.0
  - Test: score is clamped to [0.0, 1.0]
  - Test: weights sum correctly

---

## 🟣 TIER 5: DOCUMENTATION ACCURACY (Docs ↔ Code Alignment)

> Every claim in the README and docs must be verifiable against the actual code.

---

### 5.1 README Claims "Five Adversarial Transformations" — There Are Eight
- **File:** `README.md` line 49
- **Quote:** "applying five adversarial transformations"
- **Reality:** There are 8 strategies (entropy, deadcode, comments, strings, whitespace, semantic, controlflow, watermark).
- **Fix:** Update to "eight adversarial transformation strategies (five enabled by default)."

---

### 5.2 README "Diff Marker Legend" Is Not Implemented
- **File:** `README.md` lines 184-191
- **Reality:** The CLI does not produce any diff output with `+`, `-`, `!` markers. This section describes a feature that doesn't exist.
- **Fix:** Either implement diff output (as a `--diff` flag) or remove this section from the README.

---

### 5.3 `action.yml` Has No `entropy-threshold` Input
- **File:** `action.yml`
- **Bug:** The README shows the GitHub Action accepting `entropy-threshold: '0.75'`, but `action.yml` only has: `input-dir`, `output-dir`, `strategies`, `verify`, `version`. The `entropy-threshold` input is missing.
- **Fix:** Add `entropy-threshold` as an optional input in `action.yml` and wire it to the `--threshold` flag in the run step.

---

### 5.4 CONTRIBUTING.md References `evaluate.sh` That May Not Exist
- **File:** `README.md` line 259 references `scripts/evaluate.sh`
- **Check:** Verify this file exists. If not, either create it or remove the reference.
- **Fix:** List the actual contents of `scripts/` and update the architecture section accordingly.

---

### 5.5 Pre-commit Hook May Not Work
- **File:** `README.md` lines 155-170 and `.pre-commit-hooks.yaml`
- **Problem:** The pre-commit hook configuration references `rev: v3.5.0`, but there are no Git tags or releases. Users following these instructions will get an error.
- **Fix:** Add a note: "Requires a tagged release. See Releases for available versions." And ensure the pre-commit hooks yaml is valid.

---

## ⚪ TIER 6: POLISH & PROFESSIONALISM (Nice-to-have for 100% quality)

> These separate a good project from a great one.

---

### 6.1 Add Exit Codes for Different Failure Modes
- **Current:** The tool uses `System.exit(1)` for ALL errors — missing input, invalid args, compile failure, runtime exception.
- **Fix:** Define semantic exit codes:
  - `0` — Success
  - `1` — Argument parsing error
  - `2` — No files found (warning exit)
  - `3` — Processing error (strategy failed)
  - `4` — Verification failed (compile error in output)
  - This enables CI pipelines to distinguish between "wrong args" and "obfuscation broke something".

---

### 6.2 Add a `--report` Flag to Generate Markdown Report
- **File:** `PoisoningReport.java` exists but is NEVER called from CLI
- **Bug:** The `PoisoningReport.generate()` method produces a beautiful markdown report with per-file stats and MI resistance estimates — but it's completely unreachable from the CLI. No flag invokes it.
- **Fix:** Add `--report` / `-r` flag to CLI. When set, write `nightshade_report.md` to the output directory.
- **User impact:** A professional feature that's implemented but invisible.

---

### 6.3 Add a Timing Breakdown in Verbose Mode
- **Problem:** The summary shows total elapsed time but doesn't break down where time was spent.
- **Fix:** In verbose mode, print: `Lexer: 45ms | Parser: 12ms | Strategies: 890ms | Verification: 230ms | I/O: 56ms`

---

### 6.4 Ensure Idempotency Warning
- **Problem:** If a user accidentally runs Nightshade on an ALREADY-poisoned output directory, strategies will be applied on top of existing obfuscation, potentially breaking the code.
- **Fix:** Add a sentinel comment to every output file: `// @nightshade:processed v3.5.0`. Before processing, check for this comment. If found, print: `[WARN] File appears to already be processed by Nightshade. Use --force to re-process.`

---

### 6.5 Dockerfile Should Have a Healthcheck and ENTRYPOINT
- **File:** `Dockerfile`
- **Current:** Uses `CMD` but no `ENTRYPOINT`. No healthcheck.
- **Fix:**
  ```dockerfile
  ENTRYPOINT ["java", "-jar", "/app/nightshade.jar"]
  CMD ["--help"]
  HEALTHCHECK CMD ["java", "-jar", "/app/nightshade.jar", "--version"]
  ```
  This allows: `docker run nightshade -i /data/src -o /data/out` without repeating `java -jar`.

---

### 6.6 Add `.editorconfig` for Consistent Formatting
- **Missing:** No `.editorconfig` file. Contributors may use different tab widths, line endings, etc.
- **Fix:** Add `.editorconfig` with:
  ```ini
  root = true
  [*]
  indent_style = space
  indent_size = 4
  end_of_line = lf
  charset = utf-8
  trim_trailing_whitespace = true
  insert_final_newline = true
  ```

---

## 📋 EXECUTION ORDER (Recommended)

> **Work through tiers in order.** Each tier builds on the previous one.

| Order | Tier | Items | Estimated Effort |
|-------|------|-------|-----------------|
| 1st   | 🔴 TIER 1 | Bug fixes (1.1–1.9) | ~2 hours |
| 2nd   | 🟢 TIER 3 | Defensive coding (3.1–3.6) | ~2 hours |
| 3rd   | 🟡 TIER 2 | UX improvements (2.1–2.8) | ~3 hours |
| 4th   | 🔵 TIER 4 | Test coverage (4.1–4.2) | ~3 hours |
| 5th   | 🟣 TIER 5 | Doc accuracy (5.1–5.5) | ~1 hour |
| 6th   | ⚪ TIER 6 | Polish (6.1–6.6) | ~2 hours |

---

## ✅ VERIFICATION CHECKLIST (Run after all tiers complete)

```bash
# 1. Build must succeed with zero warnings
mvn clean package -q

# 2. All tests must pass
mvn test

# 3. Single file processing
java -jar target/nightshade-3.5.0-all.jar -i sample-src/HelloWorld.java -o test-out --verify -v

# 4. Directory processing
java -jar target/nightshade-3.5.0-all.jar -i sample-src -o test-out --verify

# 5. Empty directory warning
java -jar target/nightshade-3.5.0-all.jar -i empty-src -o test-out

# 6. Invalid args
java -jar target/nightshade-3.5.0-all.jar -i nonexistent
java -jar target/nightshade-3.5.0-all.jar -s bogus
java -jar target/nightshade-3.5.0-all.jar --threshold 5.0

# 7. Dry-run mode
java -jar target/nightshade-3.5.0-all.jar -i sample-src --dry-run

# 8. Help and version
java -jar target/nightshade-3.5.0-all.jar --help
java -jar target/nightshade-3.5.0-all.jar --version

# 9. Disabled strategy explicit enable
java -jar target/nightshade-3.5.0-all.jar -i sample-src -s semantic -v

# 10. Strategy listing
java -jar target/nightshade-3.5.0-all.jar --list-strategies

# 11. Docker build (if Docker available)
docker build -t nightshade .
docker run nightshade --help
```

---

# 🌙 Nightshade — Master Execution Playbook (Future Roadmap)

> **Mission:** Transform Nightshade into the #1 adversarial source code defense tool.
> **Research basis:** 3 Gemini Deep Research reports (pipeline evasion, growth playbook, competitive analysis) + full codebase audit.
> **Key insight:** NO existing tool targets LLM training data poisoning for source code. This market is uncontested.

---

## 🔴 PHASE 0: CRITICAL BUGS — Ship-Blocking Issues

> These 10 issues make the project non-functional for any user who follows the docs.

### 0.1 Version Chaos (Bug #6)
- [ ] **Sync all versions to 3.5.0:**
  - `pom.xml` line 10: change `2.0.0` → `3.5.0`
  - `CLI.java` lines 34-43: change banner/version `2.0.0` → `3.5.0`
  - `Main.java` lines 23-24: change `2.0.0` → `3.5.0`
  - `action.yml` line 34: change `v3.0` → `v3.5.0`
- [ ] **Fix pom.xml URL** (Bug #10): change `ibrahim-nightshade/nightshade` → `devhms/nightshade`

### 0.2 CLI ↔ Docs Mismatch (Bug #3)
- [ ] **Add missing CLI flags to `CLI.java`:**
  - `--entropy-threshold` (currently only `--threshold` exists)
  - `--dry-run` (documented but not implemented)
  - `--verify` (documented but not implemented)
  - `--version` (documented in SUPPORT.md but not implemented)
- [ ] **OR update all docs** to match actual flags — pick one source of truth

### 0.3 Wire Missing Strategies to CLI (Bug #4)
- [ ] **Expand `CLI.java buildStrategies()`** to include SemanticInverter, ControlFlowFlattener, WatermarkEncoder
- [ ] Accept strategy names: `semantic`, `controlflow`, `watermark`, `all`
- [ ] Update action.yml inputs to match

### 0.4 Fix Build Toolchain (Bug #5)
- [ ] **Add JUnit 5 dependency to pom.xml:**
  ```xml
  <dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.10.2</version>
    <scope>test</scope>
  </dependency>
  ```
- [ ] **Add maven-surefire-plugin** for test execution
- [ ] **Add JaCoCo plugin** for coverage reporting
- [ ] Verify `mvn clean verify` produces `target/site/jacoco/index.html`

### 0.5 Fix Release Pipeline (Bugs #1, #7)
- [ ] **Align jar names:** release.yml produces `nightshade-${version}.jar`, README expects `nightshade-3.5.0-all.jar`, action expects `nightshade.jar` — pick ONE name
- [ ] **Recommended:** Use `nightshade-3.5.0-all.jar` everywhere (matches maven-shade output)
- [ ] Tag `v3.5.0`, push, trigger release workflow, verify artifact downloads

### 0.6 Fix GitHub Action (Bug #2)
- [ ] **Action must build the jar** (or download from Releases) — currently it just runs `java -jar nightshade.jar` which doesn't exist
- [ ] Fix jar filename in action.yml to match release artifact
- [ ] Test action in a separate repo to verify it works end-to-end

### 0.7 Fix CHANGELOG Claims (Bug #8)
- [ ] Remove `// @nightshade:skip` — not implemented
- [ ] Remove `--verify` claim — not implemented (or implement it)
- [ ] Remove "Public API Preservation" — no such logic exists
- [ ] Only list features that actually exist in code

### 0.8 Fix FAQ Inconsistency
- [ ] FAQ says "five adversarial transformations" — update to 8
- [ ] FAQ references `0.75` threshold — actual default is `0.65`

---

## 🟠 PHASE 0.5: KILL THE "UNIVERSITY PROJECT" SMELL

### 0.9 Remove Academic Signals from README
- [ ] Remove student enrollment IDs: `(25-SE-33)`, `(25-SE-05)`
- [ ] Remove "University of Engineering and Technology Taxila" from footer
- [ ] Remove the SEO maintainer note (line 20) — internal notes don't belong in README
- [ ] Move university affiliation to `CITATION.cff` where it belongs

### 0.10 Clean Repository Root
- [ ] Delete `apache-maven-3.9.6/` — no pro project vendors build tools
- [ ] Delete `claude-seo/`, `.superpowers/`, `.firecrawl/`, `graphify-out/`, `.worktrees/`, `null/`
- [ ] Delete `_poisoned/`, `_nightshade_output/`, `dependency-reduced-pom.xml`
- [ ] Delete `sample-report.md`, `run.bat`, `llms.txt`
- [ ] Add all above to `.gitignore`, run `git rm -r --cached`
- [ ] Add `.cursorrules`, `.cursorignore` to `.gitignore`

### 0.11 Fix CONTRIBUTING.md Staleness
- [ ] Update architecture tree (lines 81-101): references `PoisoningEngine.java` (wrong → `ObfuscationEngine.java`), `EntropyScorer.java` (wrong → `EntropyCalculator.java`), lists only 5 strategies
- [ ] Fix interface name: `PoisoningStrategy` → `PoisonStrategy`

---

## PHASE 1: DISTRIBUTION (Week 1-2)

### 1.1 GitHub Release
- [ ] Tag v3.5.0, push, verify release workflow
- [ ] Attach SHA-256 checksum
- [ ] Write release notes

### 1.2 Homebrew Tap
- [ ] Create `homebrew-nightshade` repo with Formula
- [ ] Auto-update in release workflow

### 1.3 Docker Image
- [ ] `Dockerfile`: `eclipse-temurin:21-jdk` build → `21-jre-alpine` runtime
- [ ] Publish to GHCR, auto-build in release workflow
- [ ] Add OCI labels: `org.opencontainers.image.source`

### 1.4 npm Wrapper (jDeploy)
- [ ] Package as `nightshade-cli` on npm (jDeploy auto-installs JRE)

### 1.5 Pre-commit Hook Integration
- [ ] Create `.pre-commit-hooks.yaml` at repo root:
  ```yaml
  - id: nightshade
    name: Nightshade Code Poisoning
    entry: java -jar nightshade.jar
    language: system
    files: \.(java|py|js)$
    description: Poison source code to defend against LLM scraping
  ```
- [ ] Document in README: `pre-commit install` + `.pre-commit-config.yaml` example
- [ ] Note: `language: system` is the only option for Java JARs (no native pre-commit Java support)
- [ ] Keep hook fast — only lint/check mode for pre-commit, full poisoning for CI

### 1.6 GraalVM Native (Future)
- [ ] Add native-image Maven profile
- [ ] CI matrix: Linux x86_64, macOS ARM64, Windows x86_64

---

## PHASE 1.5: SUPPLY CHAIN SECURITY & TRUST (Week 2)

> **Why this matters:** Nightshade IS a security tool. The Trivy supply chain compromise (March 2026) proved that security tools are HIGH-VALUE targets. Users will not trust a security tool that doesn't practice what it preaches.

### 1.5.1 SLSA Provenance (Level 3)
- [ ] **Add `slsa-github-generator` to release workflow** — generates cryptographic provenance attestation using GitHub OIDC (no long-lived keys)
- [ ] Attach `.intoto.jsonl` provenance file alongside JAR in every release
- [ ] Add `slsa-verifier` command to README so users can verify artifacts
- [ ] Target: SLSA Level 3 (signing separated from build environment)

### 1.5.2 Sigstore Cosign Signing
- [ ] **Install `cosign` via `cosign-installer` action** in release workflow
- [ ] Sign JAR and Docker image using keyless OIDC (Fulcio CA + Rekor transparency log)
- [ ] Publish signature + certificate alongside release artifacts
- [ ] Add verification instructions: `cosign verify-blob --certificate-identity ... nightshade.jar`

### 1.5.3 SBOM Generation (CycloneDX)
- [ ] **Add CycloneDX Maven plugin to pom.xml:**
  ```xml
  <plugin>
    <groupId>org.cyclonedx</groupId>
    <artifactId>cyclonedx-maven-plugin</artifactId>
    <version>2.9.1</version>
    <executions>
      <execution>
        <phase>package</phase>
        <goals><goal>makeAggregateBom</goal></goals>
      </execution>
    </executions>
    <configuration>
      <schemaVersion>1.6</schemaVersion>
      <outputFormat>all</outputFormat>
    </configuration>
  </plugin>
  ```
- [ ] Upload `target/bom.json` as release artifact
- [ ] Add SBOM badge to README

### 1.5.4 OpenSSF Scorecard & Best Practices Badge
- [ ] **Add OpenSSF Scorecard GitHub Action** to CI — auto-generates security score (0-10)
- [ ] Fix all Scorecard checks:
  - `Branch-Protection`: require PR reviews before merge
  - `Pinned-Dependencies`: pin ALL Actions to commit SHAs (not tags)
  - `SAST`: enable CodeQL scanning
  - `Binary-Artifacts`: ensure no binaries in repo (delete vendored Maven!)
  - `Dangerous-Workflow`: avoid `pull_request_target` trigger
- [ ] Register at [bestpractices.dev](https://www.bestpractices.dev) for OpenSSF Best Practices Badge
- [ ] Target: "Passing" badge initially, "Silver" (80%+ coverage, bus factor 2+) by Q3
- [ ] Add Scorecard badge to README: `[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/...)](https://securityscorecards.dev/...)`

### 1.5.5 Workflow Hardening
- [ ] Pin ALL third-party Actions to full commit SHAs (not `@v4` tags)
- [ ] Set `permissions: read-all` at workflow level, grant `write` only where needed
- [ ] Enable Dependabot for Actions dependencies
- [ ] Never use `pull_request_target` trigger

---

## PHASE 2: GITHUB ACTIONS MARKETPLACE (Week 2-3)

> **Requirements confirmed:** action.yml at repo root, one action per repo, public repo, no workflow files in action repo, unique name, branding required.

### 2.1 Repository Setup
- [ ] Extract action to dedicated public repo: `devhms/nightshade-action`
- [ ] Move action.yml to root (Marketplace REQUIRES root placement)
- [ ] Ensure repo has NO `.github/workflows/` files (separate CI into a different mechanism)
- [ ] Accept GitHub Marketplace Developer Agreement

### 2.2 Marketplace Metadata
- [ ] Add `branding`: `icon: shield`, `color: purple`
- [ ] Set unique `name` field (check Marketplace for conflicts)
- [ ] Write marketplace-optimized README with:
  - Copy-pasteable YAML workflow snippet
  - GIF/screenshot of action running in a real workflow
  - Badges: version, license, build status
  - SEO keywords in headings: "CI/CD", "Security", "LLM Defense", "Code Protection"

### 2.3 Publishing
- [ ] Create release with "Publish this Action to the GitHub Marketplace" checkbox
- [ ] Tag with SemVer: `v1.0.0` + floating major tag `v1`
- [ ] Add outputs: `entropy-score`, `files-processed`, `report-path`
- [ ] Add quality gate: fail if avg entropy < configurable minimum
- [ ] PR comment bot showing poisoning stats

---

## PHASE 3: STRATEGY HARDENING (Research-Backed)

> **Key finding:** Gemini research confirms >20% distributed token mutation needed to evade MinHash/LSH at standard 0.75-0.80 Jaccard thresholds.

### 3.1 Evasion Target Parameters
```
Pipeline          | Shingle | Perms | Bands | Rows | Jaccard Threshold
SlimPajama        | 13-gram | 128   | 9     | 13   | 0.80
The Stack v2      | 5-gram  | 110   | 10    | 11   | 0.75-0.85
DataTrove/FineWeb | 5-gram  | 112   | 14    | 8    | 0.75-0.80
```
- [ ] **Implement mutation rate calculator** — count tokens changed vs total, ensure >20% distributed across entire file
- [ ] **Add `--mutation-rate` flag** — let users set minimum mutation target
- [ ] **Add benchmark test**: compute Jaccard similarity pre/post poisoning, assert < 0.70

### 3.2 Strategy Priority (Ranked by Pipeline Resilience)

**TIER 1 — Highest Resilience (survives everything):**
- [ ] **Enhance Comment Poisoner** — rewrite docstrings to semantically contradict code (not just random text). E.g., describe a sorting function as "deletes all records from database"
- [ ] **Add parametric masked triggers** in docstrings (TrojanPuzzle technique) — e.g., describe secure hashing but link to insecure MD5 terminology
- [ ] **Inject fake @author, @since, @version annotations** — pipelines preserve these for instruction-tuning

**TIER 2 — High Resilience:**
- [ ] **Enhance Control Flow Flattener** — convert `for` → `while`, swap `if/else` branch order with negated condition, inject null-op logging blocks
- [ ] **Add loop inversion** — `for(i=0;i<n;i++)` → `for(i=n-1;i>=0;i--)` where semantics allow
- [ ] **Add expression rewriting** — `a + b` → `a - (-b)`, `x * 2` → `x << 1`

**TIER 3 — Moderate Resilience:**
- [ ] **Enhance Semantic Inverter** — use domain-specific dictionaries (medical, financial, gaming) to generate plausible but wrong names like `databaseConnection` for a file handle
- [ ] **Ensure renamed identifiers pass perplexity filters** — no random strings like `x_9124_p`, only natural-looking names

**TIER 4 — LOW Resilience (FIX or REMOVE):**
- [ ] **⚠️ WhitespaceDisruptor zero-width chars** — Gemini research confirms `ftfy` normalization STRIPS zero-width Unicode during preprocessing. This strategy is nearly useless against LLMs.
  - [ ] Option A: Redesign to use valid, visible whitespace variations only
  - [ ] Option B: Downgrade from strategy to optional pass, document limitation
  - [ ] Option C: Remove and replace with more resilient technique
- [ ] **⚠️ StringEncoder** — if it produces non-alphanumeric ratio > 75%, files will be dropped by quality filters. Add alphanumeric ratio check.

### 3.3 New Strategies (From Research)
- [ ] **Import Poisoning** — add unused but valid stdlib imports to corrupt dependency graph analysis (DeepSeek-Coder uses import graphs for topological sorting)
- [ ] **Opaque Predicates** — replace `if(false)` with:
  - `if (Math.sqrt(2) == 1.5)` — float precision, always false
  - `if ("abc".hashCode() == 0)` — deterministic but non-obvious
  - `if (Integer.MAX_VALUE < 0)` — requires semantic analysis to prove dead
- [ ] **License Header Injection** — ensure all poisoned files have permissive license headers (MIT/Apache). Research confirms pipelines like The Stack v2 filter by license using ScanCode toolkit.

### 3.4 Perplexity Awareness (Critical)
- [ ] **Add line-length guard**: average line must stay <100 chars (DeepSeek drops files exceeding this)
- [ ] **Add alphanumeric ratio guard**: file must maintain >25-40% alphabetic characters
- [ ] **Avoid Base64/hex blocks**: these trigger instant rejection by NeMo Curator ScoreFilter
- [ ] **Add naturalness score** to poisoning report — estimate how "normal" the output looks to a proxy model

---

## PHASE 4: TESTING & QUALITY (Week 3-4)

### 4.1 Fix Build First (from Bug #5)
- [ ] Add JUnit 5, Surefire, JaCoCo to pom.xml
- [ ] Verify `mvn clean verify` passes
- [ ] Verify `target/site/jacoco/index.html` generates

### 4.2 Expand Tests
- [ ] Target: 80+ tests, >80% coverage
- [ ] Every strategy: null input, empty file, single-line, comments-only
- [ ] Integration: multi-file → poison → compile → verify functional
- [ ] Property test: any valid Java → poisoned output must compile
- [ ] **Mutation rate test**: verify >20% token change for default settings
- [ ] **Jaccard similarity test**: compute pre/post similarity, assert < 0.70

### 4.3 Static Analysis
- [ ] Add SpotBugs, Checkstyle (Google Java Style), OWASP dependency-check

---

## PHASE 5: BENCHMARKING & RESEARCH ARTIFACTS (Week 4-5)

> **Bug #9:** Research claims currently have zero reproducible artifacts.

### 5.1 Evaluation Framework
- [ ] Create `benchmarks/` with standardized test corpora
- [ ] Measure 4 Collberg pillars: Potency, Resilience, Stealth, Cost
- [ ] **Potency**: cyclomatic complexity increase, identifier entropy (Shannon)
- [ ] **Resilience**: survival rate through MinHash/LSH, BPE tokenizer disruption
- [ ] **Stealth**: alphanumeric ratio, line length distribution, perplexity score vs natural code
- [ ] **Cost**: compilation time overhead, file size increase

### 5.2 LLM-Specific Metrics
- [ ] Jaccard similarity via `datasketch` Python library (use 5-gram, 128 perms, threshold 0.80)
- [ ] BPE token count change via `tiktoken` (GPT-4 tokenizer)
- [ ] Code embedding distance via CodeBERT (cosine distance)
- [ ] Deduplication survival through NVIDIA NeMo Curator pipeline

### 5.3 Reproducible Artifacts
- [ ] Create `scripts/evaluate.sh` with actual working commands
- [ ] Include sample inputs that produce documented results
- [ ] Publish `BENCHMARKS.md` with methodology and raw data

---

## PHASE 6: DOCUMENTATION SITE (Week 3-4)

- [ ] MkDocs Material site with: Getting Started, Strategy Guides, CI/CD, Research
- [ ] Deploy via GitHub Pages
- [ ] Terminal demo GIF (asciinema, 30 seconds)
- [ ] Pipeline architecture diagram (Mermaid)
- [ ] Before/after code comparison screenshots
- [ ] Project logo (SVG)

---

## PHASE 7: COMMUNITY & LAUNCH (Week 4-8)

### 7.1 GitHub Optimization
- [ ] Set 20 topics: `security`, `devsecops`, `llm`, `adversarial-ml`, `code-protection`, `github-actions`, `cli`, `data-poisoning`, `obfuscation`, `java`, `python`, `javascript`, `machine-learning`, `ai-safety`, `intellectual-property`, `training-data`, `anti-scraping`, `developer-tools`, `open-source`, `source-code-protection`
- [ ] Shields.io badges: version, license, build, tests, Java, stars
- [ ] Enable GitHub Discussions
- [ ] Create 10+ Good First Issues with exact file paths and line numbers

### 7.2 Discord Server
- [ ] Channels: `#announcements`, `#development`, `#help`, `#show-and-tell`
- [ ] GitHub → Discord webhook for releases
- [ ] Research shows Discord has 3.2x higher contributor retention than Slack

### 7.3 Awesome List Submissions
> **Requirements:** Most lists require 50+ stars and 90+ days since first release. Submit AFTER launch waves.
- [ ] Fork each list, add entry in correct alphabetical position, open PR
- [ ] `awesome-security` — category: "Code Protection" or "Obfuscation"
- [ ] `awesome-machine-learning` — category: "Adversarial ML"
- [ ] `awesome-java` — category: "Security" or "Build Tools"
- [ ] `awesome-github-actions` — category: "Security"
- [ ] `awesome-devsecops` — category: "Source Code Protection"
- [ ] One-sentence description format: "Nightshade - Adversarial source code defense that poisons code to protect it from LLM training data scraping"
- [ ] Check list activity first — skip lists with no merges in 6+ months

### 7.4 Launch Waves (Gitleaks achieved 10K stars in 43 days with this approach)

**Week 4 — Show HN:**
- [ ] Post Tuesday/Wednesday 08:00-11:00 UTC (12:00 UTC = 12.2% breakout rate)
- [ ] Title: `Show HN: Open-source CLI and GitHub Action to block LLM code scraping`
- [ ] First comment: authentic backstory, architecture details, no marketing jargon
- [ ] Monitor and reply to every comment for 6+ hours

**Week 5 — Reddit:**
- [ ] r/programming, r/devops, r/MachineLearning — use post-mortem format
- [ ] Title style: "How we prevented Claude and Copilot from learning our proprietary algorithms"
- [ ] r/java, r/Python, r/javascript — language-specific angles

**Week 6 — Newsletters:**
- [ ] TLDR InfoSec, Console.dev, DevOps Weekly — 2-sentence pitches, zero marketing fluff

**Week 7 — Content:**
- [ ] Dev.to + Hashnode: "How LLM Training Pipelines Process Your Code (And How to Stop Them)"
- [ ] YouTube: 5-minute terminal-focused demo, Fireship aesthetic

**Week 8 — Ecosystem:**
- [ ] Submit to: `awesome-security`, `awesome-machine-learning`, `awesome-java`, `awesome-github-actions`
- [ ] Product Hunt launch (Tuesday)

---

## PHASE 8: ACADEMIC CREDIBILITY (Week 5-6)

- [ ] Add `CITATION.cff` with university affiliation (moved from README)
- [ ] Create ORCID profiles, upload to ResearchGate + Google Scholar
- [ ] **Differentiate from UChicago Nightshade** — critical branding risk:
  - Same trademark class (Class 9: Computer Software, Class 42: SaaS/Research)
  - Consider sub-branding: "Nightshade for Code" or rename to avoid UDRP action
  - Document in README: "UChicago Nightshade = images, this tool = source code"
- [ ] Submit CFP to: CAMLIS (Oct 2026), AISec (Nov 2026), SATML

---

## PHASE 9: MONETIZATION (Week 10+)

> Research shows: Semgrep Pro = $30/month/contributor, Nuclei Cloud = enterprise scanning, Snyk = $25K+/year enterprise

### Open-Core Model:
- [ ] **Free (MIT):** Full CLI, all 8 strategies, basic GitHub Action, Docker
- [ ] **Pro ($30/month/contributor):** Cross-file analysis, custom watermark keys, priority strategy updates
- [ ] **Enterprise:** Centralized fleet management across thousands of repos, audit/compliance reports, SSO/RBAC, threat intelligence feed of new crawler user-agents

### Funding:
- [ ] GitHub Sponsors tiers
- [ ] Sovereign Tech Fund grant application (EU-backed, targets critical digital infrastructure)
- [ ] NLnet Foundation micro-grant (internet resilience, privacy)

---

## PHASE 10: LANGUAGE EXPANSION (Week 8-12)

- [ ] **C# (.cs)** — largest enterprise language without code poisoning tools
- [ ] **Go (.go)** — cloud-native/DevOps overlap with target audience
- [ ] **Rust (.rs)** — growing security community
- [ ] **Kotlin (.kt)** — Android + JVM interop

---

## PHASE 11: IDE INTEGRATION (Week 12-16)

- [ ] **VS Code Extension** — right-click → "Poison with Nightshade", status bar, diff view, settings UI
- [ ] Publish to VS Code Marketplace with GIF demo
- [ ] IntelliJ Plugin (future)

---

## STRATEGIC MESSAGING (From Competitive Analysis)

| ❌ Avoid | ✅ Use |
|----------|--------|
| "Hide your code from everyone" | "Adversarial Defense for Source Code" |
| "Poison the models that steal your work" | "Semantic IP Protection" |
| Generic obfuscation language | "Renders unauthorized training data computationally toxic" |

---

## KEY RESEARCH PARAMETERS

| Parameter | Value | Source |
|-----------|-------|--------|
| Min token mutation rate | >20% distributed | Gemini Pipeline Research |
| MinHash/LSH Jaccard threshold | 0.70-0.85 | SlimPajama, Stack v2, DataTrove |
| Shingle sizes used | 5-gram, 13-gram | Industry standard |
| Min alphabetic ratio | >25-40% | NeMo Curator, DeepSeek filters |
| Max avg line length | <100 chars | DeepSeek-Coder pipeline |
| Comment poisoning resilience | HIGHEST | TrojanPuzzle, COVERT research |
| Zero-width char resilience | LOWEST (stripped by ftfy) | Gemini Pipeline Research |
| Poisoning rate needed | 0.09-0.5% of corpus | TrojanPuzzle ASR >80% |
| Gitleaks 10K star timeline | 43 days | Gemini Growth Research |
| HN optimal post time | Tue/Wed 08:00-11:00 UTC | Empirical analysis of 1200 Show HNs |
| Discord vs Slack retention | 3.2x higher WAC | 2024 benchmark, 1200 repos |
| Code quality tools TAM | $2.74B → $8.17B by 2033 | 13.2% CAGR |
| Security automation TAM | $10.45B → $22.92B by 2030 | 14.0% CAGR |
