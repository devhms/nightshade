<div align="center">

<h1>Nightshade: LLM Anti-Scraping & Code Obfuscation Engine</h1>

> **Note:** This is Nightshade for **source code** protection — not the [UChicago Nightshade](https://github.com/Shawn-Shan/nightshade-release) image poisoning tool. This tool defends Java, Python, and JavaScript source code from being scraped for LLM training data.

<p><strong>An open-source anti-scraping and data poisoning engine that protects intellectual property from unauthorized LLM training by injecting adversarial obfuscation.</strong></p>

[![CI](https://github.com/devhms/nightshade/actions/workflows/ci.yml/badge.svg)](https://github.com/devhms/nightshade/actions/workflows/ci.yml)
[![CodeQL](https://github.com/devhms/nightshade/actions/workflows/codeql.yml/badge.svg)](https://github.com/devhms/nightshade/actions/workflows/codeql.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/devhms/nightshade/badge)](https://securityscorecards.dev/viewer/?uri=github.com/devhms/nightshade)
[![SLSA 3](https://slsa.dev/images/gh-badge-level3.svg)](https://slsa.dev)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java](https://img.shields.io/badge/Java-21-blue?logo=openjdk)](https://adoptium.net/)
[![Maven](https://img.shields.io/badge/Built%20with-Maven-C71A36?logo=apachemaven)](https://maven.apache.org/)
[![Version](https://img.shields.io/badge/Version-3.5.0-brightgreen)](CHANGELOG.md)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](CONTRIBUTING.md)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-2.1-4baaaa.svg)](CODE_OF_CONDUCT.md)

</div>

---

---

## Table of Contents

- [How Nightshade Protects Against LLM Training](#how-nightshade-protects-against-llm-training)
- [How It Works](#how-it-works)
- [Poisoning Strategies](#poisoning-strategies)
- [Installation](#installation)
- [Pre-commit Hook](#pre-commit-hook)
- [CLI Reference](#cli-reference)
- [Supply Chain Security](#supply-chain-security)
- [Adversarial Obfuscation Architecture](#adversarial-obfuscation-architecture)
- [Supported Languages](#supported-languages)
- [Research Basis](#research-basis)
- [Comparison with Alternatives](#comparison-with-alternatives)
- [Installation](#installation)
- [Contributing](#contributing)
- [Community](#community)
- [License](#license)

---

## How Nightshade Protects Against LLM Training

**Nightshade is an open-source LLM training data poisoning engine** that protects source code intellectual property from unauthorized AI scraping. Every day, crawlers harvest open-source code from GitHub and public repositories to train large language models — without developer consent or compensation. Nightshade fights back by applying eight adversarial transformation strategies (five enabled by default) to source code before publication. The poisoned code is functionally identical to the original: it compiles, passes tests, and runs correctly. However, when ingested by an LLM training pipeline, the corrupted semantic associations degrade model quality on the poisoned patterns. The engine evades MinHash and LSH near-duplicate deduplication, meaning crawlers cannot filter out the poisoned copies. The result is that AI companies who scrape your public code without permission receive low-quality, corrupted training signal instead of clean, usable data.

> **Research-backed:** Based on *arXiv:2512.15468 (Yang et al., 2025)* — variable renaming causes a **10.19% mutual-information detection drop** with only **0.63% task-performance loss**.

### Key Capabilities

- ✅ **Functional Integrity:** Poisoned code compiles and runs identically after adversarial obfuscation.
- ✅ **Human Readability:** Human maintainers can still read and understand the code with minimal friction.
- ✅ **Dataset Corruption:** LLM training pipelines ingest corrupted signal, degrading model quality on your algorithmic patterns.
- ✅ **Deduplication Evasion:** MinHash/LSH filters cannot detect poisoned copies as near-duplicates.
- ✅ **CI/CD Ready:** Integrates as a CLI tool or GitHub Action — poison at deploy-time automatically.


---

## How It Works

Nightshade applies eight independently configurable **poisoning strategies** in a weighted pipeline. Each strategy is assigned a weight that contributes to a composite **entropy score**. The pipeline exits early once the score surpasses a configurable threshold — avoiding over-obfuscation.

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
| **F** | Semantic Inversion | — | Replaces variable names with misleading domain terms (culinary, automotive, biology) — degrades LLM semantic comprehension |
| **G** | Control Flow Flattening | — | Rewrites method bodies into switch-dispatch loops — changes code structure, not just names |
| **H** | Watermark Encoder | — | Embeds steganographic fingerprint via zero-width Unicode characters for copyright provenance tracking |

> **Note:** Strategies F, G, and H are disabled by default and enabled when using `--strategies all` or by name (e.g., `--strategies semantic,controlflow,watermark`).

### Entropy Formula

```
entropy = (renamedIdentifiers / totalIdentifiers)   × 0.50
        + (deadBlocksInjected / totalMethods)        × 0.30
        + (commentsPoisoned   / totalComments)       × 0.20
        + (stringsEncoded > 0)                       × 0.05
        + (whitespaceChanges > 0)                    × 0.05
```

The score is clamped to `[0.0, 1.0]`. Default threshold: **0.65**.

---

## Installation

### Requirements

- **Java 21** (JDK 21+) — [Temurin download](https://adoptium.net/)
- **Maven 3.9+** — [Maven download](https://maven.apache.org/download.cgi)

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

# Poison a single file instead of a full directory
java -jar target/nightshade-3.5.0-all.jar -i src/HelloWorld.java -o output_dir

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
  uses: devhms/nightshade@v3.5.0
  with:
    input-dir: './src'
    output-dir: './obfuscated-src'
    strategies: 'all'
    entropy-threshold: '0.65'
```

### Pre-commit Hook

Nightshade can be integrated into your developer workflow using the [pre-commit](https://pre-commit.com/) framework. Add the following to your `.pre-commit-config.yaml` file to run Nightshade locally before commits. Note that `java` is required on your machine.

```yaml
repos:
  - repo: https://github.com/devhms/nightshade
    rev: v3.5.0
    hooks:
      - id: nightshade
```

---

## CLI Reference

| Flag | Short | Default | Description |
|------|-------|---------|-------------|
| `--input <path>` | `-i` | *(required)* | Source directory or file to poison |
| `--output <path>` | `-o` | `../_nightshade_output` | Destination for poisoned files |
| `--strategies <list>` | `-s` | `all` | Comma-separated strategy IDs: `entropy`, `deadcode`, `comments`, `strings`, `whitespace`, `semantic`, `controlflow`, `watermark` |
| `--entropy-threshold <n>` | `-t` | `0.65` | Early-exit once composite score ≥ this value (0.0–1.0) |
| `--dry-run` | | `false` | Process and report without writing output files |
| `--verify` | | `false` | Run post-obfuscation Java compilation verification |
| `--verbose` | `-v` | `false` | Print per-file strategy details and entropy breakdown |
| `--version` | | | Print version string and exit |
| `--help` | `-h` | | Show help message and exit |

### Diff Marker Legend

| Marker | Color | Meaning |
|--------|-------|---------|
| `+` | 🟢 Green | Line added by poisoning |
| `-` | 🟡 Amber | Line removed by poisoning |
| `!` | 🔴 Red | Line modified by poisoning |

---

## Supply Chain Security

Nightshade implements strict supply chain security measures to ensure that the engine itself is safe to download and use in your environments.

- **SLSA Level 3 Provenance**: Every release artifact is built via GitHub Actions with cryptographic provenance attached, verifying the build origin and preventing tampering.
- **Sigstore Cosign Signatures**: The release JARs are signed using keyless OIDC signatures.
- **CycloneDX SBOMs**: A complete Software Bill of Materials (SBOM) is attached to every release, listing all dependencies and their versions.

**To verify a release locally:**
```bash
# 1. Verify the JAR signature
cosign verify-blob \
  --certificate-identity "https://github.com/devhms/nightshade/.github/workflows/release.yml@refs/tags/v3.5.0" \
  --certificate-oidc-issuer "https://token.actions.githubusercontent.com" \
  --bundle nightshade.sig \
  nightshade-3.5.0-all.jar
  
# 2. Verify SLSA Provenance
slsa-verifier verify-artifact nightshade-3.5.0-all.jar \
  --provenance-path multiple.intoto.jsonl \
  --source-uri github.com/devhms/nightshade
```

---

## Adversarial Obfuscation Architecture

```
nightshade/
├── src/main/java/com/nightshade/
│   ├── CLI.java                    # Argument parsing and orchestration entry point
│   ├── Launcher.java               # Fat JAR entry point (JavaFX module bypass)
│   ├── Main.java                   # Application bootstrap (CLI/GUI router)
│   ├── engine/
│   │   ├── Lexer.java              # Language-aware tokeniser
│   │   ├── Parser.java             # Simplified AST builder for strategy consumption
│   │   ├── Serializer.java         # Token-to-source reconstruction
│   │   ├── ObfuscationEngine.java  # Strategy pipeline coordinator
│   │   ├── EntropyCalculator.java  # Weighted entropy score calculator
│   │   ├── FileWalker.java         # Recursive directory scanner
│   │   ├── CompilationVerifier.java # Post-obfuscation compilation check
│   │   └── PoisoningReport.java    # Markdown report generator
│   ├── model/
│   │   ├── ASTNode.java            # Composite-pattern AST node
│   │   ├── SourceFile.java         # Encapsulated source file with raw + obfuscated lines
│   │   ├── SymbolTable.java        # Scope-aware identifier mapping registry
│   │   ├── ObfuscationResult.java  # Per-file transformation result + stats
│   │   ├── Token.java              # Immutable lexical token
│   │   └── TokenType.java          # Token classification enum
│   ├── strategy/
│   │   ├── PoisonStrategy.java     # Strategy interface (plugin contract)
│   │   ├── EntropyScrambler.java   # Strategy A — variable renaming
│   │   ├── DeadCodeInjector.java   # Strategy B — contextual dead code
│   │   ├── CommentPoisoner.java    # Strategy C — comment replacement
│   │   ├── StringEncoder.java      # Strategy D — string encoding
│   │   ├── WhitespaceDisruptor.java # Strategy E — whitespace randomisation
│   │   ├── SemanticInverter.java   # Strategy F — domain-mismatch renaming
│   │   ├── ControlFlowFlattener.java # Strategy G — switch-dispatch flattening
│   │   └── WatermarkEncoder.java   # Strategy H — steganographic fingerprint
│   ├── controller/
│   │   └── MainController.java     # JavaFX GUI controller (optional)
│   └── util/
│       ├── FileUtil.java           # I/O helpers and run-log writer
│       ├── HashUtil.java           # FNV-1a based identifier hashing
│       └── LogService.java         # Observable log stream (FX-thread safe)
├── scripts/evaluate.sh             # Reproducible evaluation harness
└── src/test/                       # JUnit 5 test suite
```

---

## Supported Languages

| Language | Extension | Support Level |
|----------|-----------|---------------|
| Java | `.java` | ✅ Full (all 8 strategies) |
| Python | `.py` | ✅ Full (strategies A–E) |
| JavaScript | `.js` | ✅ Full (strategies A–E) |
| TypeScript | `.ts` | 🔗 Via `.js` processing |
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

## Installation

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
Nightshade applies eight adversarial transformation strategies to source code: (A) variable entropy scrambling using SHA-256 hashes, (B) dead code injection with opaque predicates, (C) semantic comment poisoning with misleading text, (D) string literal encoding to evade MinHash deduplication, (E) whitespace disruption, (F) semantic inversion to misleading names, (G) control flow flattening, and (H) watermark embedding. The code remains fully functional and human-readable. The transformations are applied through a weighted entropy pipeline that exits early once a configurable corruption threshold is reached, preventing over-obfuscation. Based on arXiv:2512.15468, variable renaming alone causes a 10.19% mutual-information detection drop.

### Does Nightshade break my code's functionality?
No. Nightshade guarantees functional integrity. All eight poisoning strategies are **semantics-preserving** — the poisoned code compiles and runs identically to the original source. A built-in entropy scoring system (`0.0` to `1.0`) monitors the cumulative transformation level and prevents over-obfuscation. You can also use `--dry-run` to preview transformations before writing any output files.

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
| Ibrahim Salman | Creator & Lead | [@devhms](https://github.com/devhms) |
| Saif-ur-Rehman | Co-Creator | — |

*University of Engineering and Technology Taxila*

---

<div align="center">

**If Nightshade protects your code, please ⭐ star the repo — it helps others find it.**

</div>