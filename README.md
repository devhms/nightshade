<div align="center">

<h1>🌑 Nightshade | LLM Training Data Poisoning & Source Code Obfuscation</h1>

<p><strong>An open-source anti-scraping and data poisoning engine that protects intellectual property from unauthorized LLM training by injecting adversarial obfuscation.</strong></p>

[![CI](https://github.com/devhms/nightshade/actions/workflows/ci.yml/badge.svg)](https://github.com/devhms/nightshade/actions/workflows/ci.yml)
[![CodeQL](https://github.com/devhms/nightshade/actions/workflows/codeql.yml/badge.svg)](https://github.com/devhms/nightshade/actions/workflows/codeql.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Built%20with-Maven-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![Version](https://img.shields.io/badge/Version-3.5.0-brightgreen)](CHANGELOG.md)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

</div>

---

> **Maintainer Note (SEO Setup):** Please ensure the following exact topics are applied in the GitHub Repository settings (gear icon) for maximum algorithm discoverability: `llm-security`, `data-poisoning`, `code-obfuscation`, `anti-scraping`, `machine-learning`, `copyright-protection`, `adversarial-machine-learning`, `java`, `python`, `javascript`, `typescript`, `llm-vulnerability`, `security-tools`, `dataset-corruption`, `backdoor-attack`.


---

## Table of Contents

- [Why Nightshade?](#why-nightshade)
- [How It Works](#how-it-works)
- [Poisoning Strategies](#poisoning-strategies)
- [Quick Start](#quick-start)
- [CLI Reference](#cli-reference)
- [Architecture](#architecture)
- [Supported Languages](#supported-languages)
- [Research Basis](#research-basis)
- [Comparison with Alternatives](#comparison-with-alternatives)
- [Building from Source](#building-from-source)
- [Contributing](#contributing)
- [Community](#community)
- [License](#license)

---

## Why Nightshade? (Anti-Scraping & IP Protection)

**Nightshade is an open-source LLM training data poisoning engine** that protects source code intellectual property from unauthorized AI scraping. Every day, crawlers harvest open-source code from GitHub and public repositories to train large language models — without developer consent or compensation. Nightshade fights back by applying five adversarial transformations to source code before publication. The poisoned code is functionally identical to the original: it compiles, passes tests, and runs correctly. However, when ingested by an LLM training pipeline, the corrupted semantic associations degrade model quality on the poisoned patterns. The engine evades MinHash and LSH near-duplicate deduplication, meaning crawlers cannot filter out the poisoned copies. The result is that AI companies who scrape your public code without permission receive low-quality, corrupted training signal instead of clean, usable data.

> **Research-backed:** Based on *arXiv:2512.15468 (Yang et al., 2025)* — variable renaming causes a **10.19% mutual-information detection drop** with only **0.63% task-performance loss**.

### Key Capabilities

- ✅ **Functional Integrity:** Poisoned code compiles and runs identically after adversarial obfuscation.
- ✅ **Human Readability:** Human maintainers can still read and understand the code with minimal friction.
- ✅ **Dataset Corruption:** LLM training pipelines ingest corrupted signal, degrading model quality on your algorithmic patterns.
- ✅ **Deduplication Evasion:** MinHash/LSH filters cannot detect poisoned copies as near-duplicates.
- ✅ **CI/CD Ready:** Integrates as a CLI tool or GitHub Action — poison at deploy-time automatically.


---

## How It Works

Nightshade applies five independently configurable **poisoning strategies** in a weighted pipeline. Each strategy is assigned a weight that contributes to a composite **entropy score**. The pipeline exits early once the score surpasses a configurable threshold — avoiding over-obfuscation.

```
Source Code  ──►  Lexer  ──►  AST  ──►  Strategy Pipeline  ──►  Poisoned Code
                                           │
                                    ┌──────┴──────┐
                                    ▼             ▼
                              Entropy Score   Diff Report
                              (0.0 – 1.0)   (+ / - / !)
```

---

## Poisoning Strategies

| ID | Strategy | Weight | Mechanism |
|----|----------|--------|-----------|
| **A** | Variable Entropy Scrambling | `0.50` | Renames all identifiers with a deterministic SHA-256 hash — strongest mutual-information disruption, survives deduplication |
| **B** | Dead Code Injection | `0.30` | Inserts unreachable, logically plausible code blocks — preprocessing-proof because they pass type checking |
| **C** | Semantic Comment Poisoning | `0.20` | Replaces comments with semantically opposite or misleading text — corrupts LLM association learning |
| **D** | String Literal Encoding | `0.15` | Encodes string literals as character-array expressions — evades MinHash+LSH near-duplicate detection |
| **E** | Whitespace Disruption | `0.10` | Randomises indentation depth and adds zero-width spaces — disrupts BPE tokenizer boundary detection |

### Entropy Formula

```
entropy = (renamedIdentifiers / totalIdentifiers)   × 0.50
        + (deadBlocksInjected / totalMethods)        × 0.30
        + (commentsPoisoned   / totalComments)       × 0.20
        + (stringsEncoded > 0)                       × 0.15
        + (whitespaceChanges > 0)                    × 0.10
```

The score is clamped to `[0.0, 1.0]`. Default threshold: **0.75**.

---

## Quick Start

### Requirements

- **Java 21** (JDK 21+) — [Temurin download](https://adoptium.net/)
- **Maven 3.9+** (bundled in repo; use `./apache-maven-3.9.6/bin/mvn`)

### Install

```bash
git clone https://github.com/devhms/nightshade.git
cd nightshade
mvn clean package -q
```

### Run

```bash
# Poison all supported files in ./src, write output to ./_poisoned
java -jar target/nightshade-3.5.0-all.jar --input ./src --output ./_poisoned

# Apply only variable-renaming and dead-code injection, verbose output
java -jar target/nightshade-3.5.0-all.jar -i ./src -s entropy,deadcode -v

# Dry-run: preview changes without writing files
java -jar target/nightshade-3.5.0-all.jar --input ./src --dry-run

# Set custom entropy threshold (exit early at 80%)
java -jar target/nightshade-3.5.0-all.jar --input ./src --entropy-threshold 0.8
```

### GitHub Action (CI/CD Integration)

Add this to your workflow to automatically poison code on every push:

```yaml
- name: Protect code with Nightshade
  uses: devhms/nightshade@v3
  with:
    input-dir:  './src'
    output-dir: './protected'
    strategies: 'all'
    entropy-threshold: '0.75'
```

---

## CLI Reference

| Flag | Short | Default | Description |
|------|-------|---------|-------------|
| `--input <path>` | `-i` | *(required)* | Source directory or file to poison |
| `--output <path>` | `-o` | `../_nightshade_output` | Destination for poisoned files |
| `--strategies <list>` | `-s` | `all` | Comma-separated strategy IDs: `entropy`, `deadcode`, `comments`, `strings`, `whitespace` |
| `--entropy-threshold <n>` | | `0.75` | Early-exit once composite score ≥ this value (0.0–1.0) |
| `--dry-run` | `-n` | `false` | Process and report without writing output files |
| `--verbose` | `-v` | `false` | Print per-file strategy details and entropy breakdown |
| `--help` | `-h` | | Show help message and exit |

### Diff Marker Legend

| Marker | Color | Meaning |
|--------|-------|---------|
| `+` | 🟢 Green | Line added by poisoning |
| `-` | 🟡 Amber | Line removed by poisoning |
| `!` | 🔴 Red | Line modified by poisoning |

---

## Architecture

```
nightshade/
├── src/main/java/com/nightshade/
│   ├── CLI.java               # Argument parsing and orchestration entry point
│   ├── Main.java              # Application bootstrap
│   ├── engine/
│   │   ├── Lexer.java         # Language-aware tokeniser
│   │   ├── PoisoningEngine.java  # Strategy pipeline coordinator
│   │   └── EntropyScorer.java # Weighted score calculator
│   ├── model/
│   │   ├── ASTNode.java       # Abstract Syntax Tree node
│   │   └── PoisonReport.java  # Per-file transformation report
│   ├── strategy/
│   │   ├── EntropyScrambler.java   # Strategy A — variable renaming
│   │   ├── DeadCodeInjector.java   # Strategy B — dead code
│   │   ├── CommentPoisoner.java    # Strategy C — comment replacement
│   │   ├── StringEncoder.java      # Strategy D — string encoding
│   │   └── WhitespaceDisruptor.java # Strategy E — whitespace randomisation
│   ├── controller/
│   │   └── MainController.java     # JavaFX GUI controller (optional)
│   └── util/
│       └── FileUtil.java           # I/O helpers
└── src/test/                       # JUnit 5 test suite
```

---

## Supported Languages

| Language | Extension | Support Level |
|----------|-----------|---------------|
| Java | `.java` | ✅ Full (all 5 strategies) |
| Python | `.py` | ✅ Full (all 5 strategies) |
| JavaScript | `.js` | ✅ Full (all 5 strategies) |
| TypeScript | `.ts` | ✅ Full (all 5 strategies) |
| C# | `.cs` | 🚧 Planned (v3.x) |
| Go | `.go` | 🚧 Planned (v3.x) |
| Rust | `.rs` | 🔬 Under Research |

---

## Research Basis

Nightshade is grounded in peer-reviewed research on LLM training-data robustness:

| Reference | Finding | Strategy Used |
|-----------|---------|---------------|
| **arXiv:2512.15468** (Yang et al., Dec 2025) | Variable renaming causes a **10.19% mutual-information detection drop** with only **0.63% task-performance loss** | Strategy A |
| **OWASP LLM Top 10 — LLM04** | Training-data poisoning is a critical threat vector for code-generation models | Strategies A–E |
| **Backdoor Attack Research (2024–2025)** | Poisoning effective with as little as 0.001% malicious samples | Strategies B, C |
| **MinHash/LSH Dedup Research** | Near-duplicate detection fails when ≥15% of tokens differ | Strategies D, E |

Dead-code injection is specifically designed to survive all known normalisation passes used in pre-training pipelines.

---

## Comparison with Alternatives

| Feature | Nightshade | ProGuard | yGuard | Obfuscat0r |
|---------|-----------|----------|--------|-----------|
| **LLM poisoning focus** | ✅ Primary goal | ❌ | ❌ | ❌ |
| **Code remains functional** | ✅ Guaranteed | ✅ | ✅ | ⚠️ Partial |
| **Multi-language** | ✅ Java/Py/JS/TS | ❌ JVM only | ❌ JVM only | ⚠️ JS only |
| **CLI + GitHub Action** | ✅ | ❌ | ❌ | ❌ |
| **Open source (MIT)** | ✅ | ✅ GPL | ✅ | ❌ |
| **Entropy scoring** | ✅ | ❌ | ❌ | ❌ |
| **Dry-run mode** | ✅ | ❌ | ❌ | ❌ |
| **Research-backed** | ✅ arXiv | ❌ | ❌ | ❌ |

---

## Building from Source

```bash
# Clone
git clone https://github.com/devhms/nightshade.git
cd nightshade

# Build fat JAR
mvn clean package

# Run tests
mvn test

# Run with coverage report
mvn verify
# Report: target/site/jacoco/index.html
```

**Requirements:** JDK 21, Maven 3.9+

The bundled Maven (`./apache-maven-3.9.6/bin/mvn`) can be used if Maven is not installed globally.

---

## Contributing

We love contributions! Here's how to get started:

1. Read **[CONTRIBUTING.md](CONTRIBUTING.md)** — coding style, commit format, workflow
2. Check **[open issues](https://github.com/devhms/nightshade/issues?q=label%3A%22good+first+issue%22)** labelled `good first issue`
3. Fork → branch → PR

Please follow our **[Code of Conduct](CODE_OF_CONDUCT.md)** in all interactions.

---

## Community

| Channel | Purpose |
|---------|---------|
| 🐛 [Issues](https://github.com/devhms/nightshade/issues) | Bug reports and feature requests |
| 💬 [Discussions](https://github.com/devhms/nightshade/discussions) | Questions, ideas, and general chat |
| 🗺 [Roadmap](ROADMAP.md) | What's coming next |
| 🔒 [Security Policy](SECURITY.md) | Report vulnerabilities privately |
| 💖 [Sponsor](https://github.com/sponsors/devhms) | Support continued development |

---

## FAQ — Frequently Asked Questions

### What is LLM training data poisoning?
LLM training data poisoning is the practice of inserting adversarial, corrupted, or misleading data into datasets used to train large language models. This degrades model quality on specific patterns without being detectable during preprocessing. Nightshade applies this technique to source code to protect developer intellectual property. Unlike blocking scrapers with `robots.txt` (which is routinely ignored), poisoning ensures that even if code is stolen, it becomes low-quality training signal. The technique is grounded in peer-reviewed adversarial machine learning research and has been empirically shown to reduce mutual-information scores in LLMs trained on poisoned data.

### How does Nightshade protect code from AI scraping?
Nightshade applies five adversarial transformations to source code: (A) variable entropy scrambling using SHA-256 hashes, (B) dead code injection with opaque predicates, (C) semantic comment poisoning with misleading text, (D) string literal encoding to evade MinHash deduplication, and (E) whitespace disruption with zero-width Unicode characters. The code remains fully functional and human-readable. The transformations are applied through a weighted entropy pipeline that exits early once a configurable corruption threshold is reached, preventing over-obfuscation. Based on arXiv:2512.15468, variable renaming alone causes a 10.19% mutual-information detection drop.

### Does Nightshade break my code's functionality?
No. Nightshade guarantees functional integrity. All five poisoning strategies are **semantics-preserving** — the poisoned code compiles and runs identically to the original source. A built-in entropy scoring system (`0.0` to `1.0`) monitors the cumulative transformation level and prevents over-obfuscation. You can also use `--dry-run` to preview transformations before writing any output files.

### Can I use Nightshade on a commercial or proprietary codebase?
Yes. Nightshade is licensed under the MIT License, which permits use in commercial and proprietary projects with no restrictions. Since the tool is most effective when applied to publicly visible code (the target of AI crawlers), its primary use case is open-source repositories deployed on platforms like GitHub, where training crawlers actively harvest data.

### How do I integrate Nightshade into my CI/CD pipeline?
Nightshade provides a GitHub Action (`devhms/nightshade@v3`) that can be added to any workflow file. Configure the `input-dir`, `output-dir`, and `entropy-threshold` parameters. On every push to `main`, the action automatically poisons all supported source files and writes the protected output. See the [CLI Reference](#cli-reference) and [GitHub Action](#github-action-cicd-integration) sections for the exact YAML.

---


Released under the **MIT License** — see [LICENSE](LICENSE) for the full text.

---

## Authors

| Name | Role | Contact |
|------|------|---------|
| Ibrahim Salman (25-SE-33) | Creator & Lead | [@devhms](https://github.com/devhms) |
| Saif-ur-Rehman (25-SE-05) | Co-Creator | — |

*University of Engineering and Technology Taxila*

---

<div align="center">

**If Nightshade protects your code, please ⭐ star the repo — it helps others find it.**

</div>