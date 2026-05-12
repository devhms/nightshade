# Tier 1 Hardening (Serializer/AST) Design

## Goal
Eliminate Tier 1 compilation-breakers by making identifier renaming token-aware, preventing external method renames, expanding protected identifiers, and ensuring AST drift is corrected after line-changing strategies. Add automated tests that verify each fix.

## Scope
In-scope:
- Token-safe renaming in `Serializer.applyMapping` (no replacements inside string literals or comments).
- Skip renaming identifiers that follow `.` (object method/field calls) as a near-term guard.
- Expand `SymbolTable.PROTECTED_IDENTIFIERS` with missing stdlib/UI methods listed in `todo_2.md`.
- Re-parse AST when line count changes; ensure correctness via tests.
- Automated tests covering each change.

Out-of-scope:
- Full token-based serialization across all strategies (Tier 5).
- JavaParser integration/type resolution (Tier 5).
- CI/CD, action.yml, repo hygiene, release tasks.

## Architecture and Data Flow
- The pipeline remains: lex → parse → apply strategies → merge stats → compute entropy.
- Identifier renaming remains line-based in `Serializer.applyMapping`, but will respect token types:
  - Only rename tokens of type `IDENTIFIER`.
  - Never rename when token is preceded by a `.` symbol (object member/method call).
  - Never rename inside `LITERAL` or `COMMENT` tokens (tokenization already labels these).
- `SymbolTable` continues to provide `isUserDefined` filtering, expanded with missing protected methods.
- `ObfuscationEngine.processOne` re-parses the AST when line count changes; this is validated by tests for line drift.

## Error Handling
- Per-file exception handling remains unchanged in `ObfuscationEngine.process`.
- The design relies on test coverage to prevent regressions rather than new runtime error paths.

## Files and Responsibilities
- Modify: `src/main/java/com/nightshade/engine/Serializer.java`
  - Enforce token-aware renaming and dot-call guard.
- Modify: `src/main/java/com/nightshade/model/SymbolTable.java`
  - Add missing protected identifiers and keep `isUserDefined` semantics intact.
- Verify (no behavior change required if already correct): `src/main/java/com/nightshade/engine/ObfuscationEngine.java`
  - Re-parse AST after line-count changes; keep public API protection re-application.
- Tests:
  - Modify: `src/test/java/com/nightshade/strategy/EntropyScramblerTest.java`
  - Add: `src/test/java/com/nightshade/engine/SerializerTest.java`
  - Add: `src/test/java/com/nightshade/model/SymbolTableTest.java`
  - Add: `src/test/java/com/nightshade/engine/AstDriftTest.java`

## Test Plan (JUnit 5)
### 1) Token-safe renaming (strings/comments) + dot-call guard
- Input line set includes:
  - `String s = "count"; int count = 1; // count`
  - `myList.add("x");`
- Assertions:
  - String literal text remains `"count"`.
  - Comment text remains `// count`.
  - `count` variable is renamed.
  - `add` method name is NOT renamed.

### 2) Protected identifiers list
- Representative set: `setTitle`, `stream`, `toUpperCase`, `getItems`.
- `SymbolTable.isUserDefined(name)` must return `false` for each.

### 3) AST drift after line insertion
- Construct pipeline using a line-adding strategy (e.g., `DeadCodeInjector`) followed by `EntropyScrambler`.
- Verify the target identifier in the modified output is renamed at the expected line (by name-based checks).

### 4) Regression coverage on existing tests
- Ensure existing tests still pass and update assertions if needed due to expanded protected identifiers.

## Validation
- Run `mvn test`.
- Confirm all new tests pass and existing tests remain green.

## Risks and Mitigations
- Risk: dot-call guard might miss edge cases involving whitespace/comments between `.` and identifier.
  - Mitigation: token stream already preserves symbol order; dot-guard applies using previous token.
- Risk: expanding protected identifiers reduces renaming coverage.
  - Mitigation: this is necessary for correctness; long-term fix via JavaParser in Tier 5.

## Acceptance Criteria
- Identifiers inside string literals or comments are never renamed.
- Object method calls (identifier following `.`) are not renamed.
- `SymbolTable.isUserDefined` returns false for the expanded protected identifier list.
- AST drift test passes when a line-adding strategy precedes renaming.
- `mvn test` passes.
