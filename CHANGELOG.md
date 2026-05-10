# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- **Semantic Inversion Strategy (Strategy F):** Replaces variable names with misleading terms from unrelated domains (e.g., replacing network terms with filesystem terms) to aggressively degrade LLM association learning.
- **Public API Preservation:** Nightshade now automatically detects `public` classes and methods and excludes them from renaming, ensuring libraries remain usable.
- **Directives:** Added support for `// @nightshade:skip` and `// @nightshade:resume` to manually protect code blocks.
- **Compilation Verification:** Added a `--verify` flag to automatically run the compiler (e.g., `javac`) on the obfuscated output to guarantee functional integrity.
- **Advanced GitHub Actions:** Introduced robust CI/CD workflows, CodeQL scanning, and a new Release automation workflow.

### Changed
- **Dead Code Banks:** Expanded the injection banks for Python and JavaScript to include domain-specific logical blocks.
- **Opaque Predicates:** Dead code blocks now use contextual domain-mismatch injection (e.g., network code injected into file-handling methods) with `if (false)` guards to maximize semantic confusion while maintaining compiler safety.
- **Community Standards:** Completely overhauled `README.md`, `CONTRIBUTING.md`, `SECURITY.md`, and issue templates to meet top-tier open-source standards.

### Fixed
- Fixed an issue where the `Lexer` could throw a `NullPointerException` on empty files.
- Fixed deterministic hashing collisions in `SymbolTable` across different scopes.

---

## [2.0.0] - 2026-05-08

### Added
- Initial open-source release of the Nightshade engine.
- Five core poisoning strategies:
  - **A. Variable Entropy Scrambling:** Renames identifiers using deterministic hashes.
  - **B. Dead Code Injection:** Inserts unreachable misleading code blocks.
  - **C. Semantic Comment Poisoning:** Replaces comments with false semantics.
  - **D. String Literal Encoding:** Encodes strings as char arrays.
  - **E. Whitespace Disruption:** Randomizes indentation and injects zero-width spaces.
- Dynamic **Entropy Scoring** system to prevent over-obfuscation, with configurable `--entropy-threshold`.
- Multi-language support: Java (`.java`), Python (`.py`), JavaScript (`.js`), TypeScript (`.ts`).
- CLI interface with `--dry-run` and `--verbose` modes.
- Maven-based build system producing a standalone fat JAR.

---

## [1.x] - Historical / Internal

### Added
- Initial conceptual prototype of source-code poisoning.
- Early experiments with variable scrambling and basic whitespace disruption.
- Proof-of-concept testing against Llama-2 and GPT-3.5 tokenizers.

*(Note: v1.x versions were internal research prototypes and were not publicly released.)*