# Roadmap

This document outlines the current state and future direction of the Nightshade project. It is a living document and represents our best estimate of priorities, subject to change based on community feedback and emerging LLM training techniques.

---

## Guiding Principles

1. **Do no harm:** Code transformed by Nightshade MUST remain 100% functionally identical to the original when executed.
2. **Stay ahead of the curve:** Continuously research and implement new strategies to counter advancements in LLM scraping and de-duplication (e.g., MinHash, LSH, BPE tokenizer updates).
3. **Frictionless integration:** Ensure Nightshade can drop into any CI/CD pipeline (GitHub Actions, GitLab CI, Jenkins) with minimal configuration.

---

## Milestones

### ✅ v2.0: The Foundation (Completed)
- Initial open-source release.
- Five core strategies: Variable Scrambling, Dead Code, Comment Poisoning, String Encoding, Whitespace Disruption.
- Basic CLI interface and entropy scoring.

### 🚀 v3.0: Robustness & Verification (Current Focus)
*Target: Q3 2026*
- **Public API Detection:** Prevent renaming of `public` classes and methods to ensure library APIs remain usable.
- **Skip Directives:** Support for `// @nightshade:skip` to exclude specific blocks of code.
- **Semantic Inversion Strategy:** Rename variables to misleading domain terms (e.g., changing sort variables to crypto variables) to corrupt LLM association learning.
- **Compilation Verification:** Automated post-processing verification via `javac` to ensure transformations didn't break syntax.
- **Testing & CI:** Comprehensive JUnit 5 test suite and robust GitHub Actions pipeline.

### 🔮 v3.x: Ecosystem Expansion
*Target: Q4 2026*
- **C# Support:** Extend the parser and strategies to support `.cs` files.
- **Go Support:** Extend the parser and strategies to support `.go` files.
- **Config Profiles:** Introduce built-in profiles (e.g., `--profile aggressive`, `--profile fast`, `--profile safe`).
- **Configuration File:** Support for `nightshade.yml` to define strategies and weights instead of long CLI flags.

### 🌌 v4.0: The Next Generation
*Target: 2027*
- **AST-Level Restructuring:** Control flow flattening and opaque predicate injection.
- **Plugin Architecture:** Allow the community to write and load custom poisoning strategies via a standard interface.
- **Rust Support:** Preliminary support for `.rs` files.
- **LLM Evaluation Harness:** A standalone tool to measure exactly how much an LLM's performance degrades on poisoned vs. clean code.

---

## Out of Scope (What we won't do)

To keep the project focused, we explicitly will **NOT** implement:
- **Malware generation:** Nightshade is for poisoning datasets, not for evading antivirus detection or writing malicious payloads.
- **Bytecode/Binary obfuscation:** Nightshade operates purely on source code text to corrupt training data, not on compiled `.class` or `.exe` files. (Use tools like ProGuard for bytecode).

---

## How to Contribute to the Roadmap

We track features and bugs using GitHub Issues.
- Have an idea? Open a **Feature Request** issue.
- Want to track progress? Check our [GitHub Projects Board](https://github.com/orgs/devhms/projects/1) (link will be live soon).