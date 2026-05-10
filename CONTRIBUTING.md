# Contributing to Nightshade

Thank you for taking the time to contribute! Nightshade is a community-driven project and every contribution — from a typo fix to a new poisoning strategy — makes a real difference.

> **First time contributing to open source?** Check out [How to Contribute to Open Source](https://opensource.guide/how-to-contribute/) — it's a friendly guide to get you started.

---

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Quick Links](#quick-links)
- [Prerequisites](#prerequisites)
- [Getting Started](#getting-started)
- [Project Architecture](#project-architecture)
- [Development Workflow](#development-workflow)
- [Coding Style](#coding-style)
- [Commit Messages](#commit-messages)
- [Testing Guide](#testing-guide)
- [Pull Requests](#pull-requests)
- [Reporting Bugs](#reporting-bugs)
- [Requesting Features](#requesting-features)
- [Security Issues](#security-issues)
- [First Good Issues](#first-good-issues)
- [Developer Certificate of Origin](#developer-certificate-of-origin)

---

## Code of Conduct

This project is governed by the [Contributor Covenant 2.1](CODE_OF_CONDUCT.md). All participants are expected to uphold this code. Please report unacceptable behaviour to the contacts listed in that document.

---

## Quick Links

| Resource | Link |
|----------|------|
| Code of Conduct | [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) |
| Security Policy | [SECURITY.md](SECURITY.md) |
| Support & FAQ | [SUPPORT.md](SUPPORT.md) |
| Roadmap | [ROADMAP.md](ROADMAP.md) |
| Open Issues | [GitHub Issues](https://github.com/devhms/nightshade/issues) |
| Discussions | [GitHub Discussions](https://github.com/devhms/nightshade/discussions) |

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| **JDK** | 21+ | [Temurin 21](https://adoptium.net/) recommended |
| **Maven** | 3.9+ | Bundled in repo as `./apache-maven-3.9.6/` |
| **Git** | Any recent | With `user.email` and `user.name` configured |

---

## Getting Started

```bash
# 1. Fork the repository on GitHub (click the Fork button)

# 2. Clone your fork
git clone https://github.com/<your-username>/nightshade.git
cd nightshade

# 3. Add the upstream remote
git remote add upstream https://github.com/devhms/nightshade.git

# 4. Build and verify everything passes
mvn clean verify -q

# 5. Create a feature branch
git checkout -b feat/your-feature-name
```

---

## Project Architecture

```
nightshade/
├── CLI.java               # Argument parsing + orchestration
├── Main.java              # Bootstrap
├── engine/
│   ├── Lexer.java         # Language-aware tokeniser
│   ├── Parser.java        # AST parser
│   ├── ObfuscationEngine.java  # Strategy pipeline coordinator
│   ├── EntropyCalculator.java # Weighted entropy calculator
│   └── CompilationVerifier.java # Post-obfuscation compile check
├── model/
│   ├── ASTNode.java       # AST node representation
│   ├── Token.java         # Lexer token
│   ├── SymbolTable.java   # Symbol tracking
│   └── ObfuscationResult.java  # Per-file transformation result
├── strategy/              # One class per poisoning strategy
│   ├── EntropyScrambler.java   # Strategy A - Variable renaming
│   ├── DeadCodeInjector.java   # Strategy B - Dead code injection
│   ├── CommentPoisoner.java    # Strategy C - Comment poisoning
│   ├── StringEncoder.java      # Strategy D - String encoding
│   ├── WhitespaceDisruptor.java # Strategy E - Whitespace variation
│   ├── SemanticInverter.java   # Strategy F - Misleading names
│   ├── ControlFlowFlattener.java # Strategy G - Flow flattening
│   └── WatermarkEncoder.java   # Strategy H - Steganographic watermark
├── util/
│   └── FileUtil.java        # I/O helpers
```

### Adding a New Poisoning Strategy

1. Create `src/main/java/com/nightshade/strategy/MyStrategy.java` implementing the `PoisonStrategy` interface.
2. Register it in `ObfuscationEngine.java` with a short identifier string and a weight.
3. Add a CLI flag in `CLI.java` if the strategy needs configuration.
4. Write unit tests in `src/test/java/com/nightshade/strategy/MyStrategyTest.java`.
5. Document the strategy in `README.md` and `CHANGELOG.md`.

---

## Development Workflow

```bash
# Sync with upstream before starting work
git fetch upstream
git rebase upstream/main

# Make your changes, then run the full test suite
mvn verify

# If you have only changed documentation or non-Java files, run a lighter check
mvn -q test
```

### Branch Naming

| Type | Pattern | Example |
|------|---------|---------|
| New feature | `feat/<short-slug>` | `feat/rust-language-support` |
| Bug fix | `fix/<issue-id>-<slug>` | `fix/42-lexer-null-pointer` |
| Documentation | `docs/<slug>` | `docs/improve-readme` |
| Chore / refactor | `chore/<slug>` | `chore/upgrade-actions-v4` |
| Release prep | `release/<version>` | `release/3.6.0` |

---

## Coding Style

Nightshade follows the **[Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)**.

Key rules:
- **Indentation:** 4 spaces (no tabs)
- **Line length:** 120 characters maximum
- **Naming:** `camelCase` for methods and variables; `PascalCase` for classes; `UPPER_SNAKE_CASE` for constants
- **Javadoc:** All `public` classes and methods must have Javadoc
- **Nullability:** Prefer `Optional<T>` over returning `null`

A Checkstyle configuration is planned — follow existing code style in the interim.

---

## Commit Messages

Nightshade uses **[Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/)**.

```
<type>(<scope>): <short summary>

[optional body]

[optional footer(s)]
```

| Type | When to use |
|------|------------|
| `feat` | New feature or poisoning strategy |
| `fix` | Bug fix |
| `docs` | Documentation only |
| `test` | Adding or fixing tests |
| `refactor` | Code change that neither fixes a bug nor adds a feature |
| `chore` | Build system, CI, dependency updates |
| `perf` | Performance improvement |
| `ci` | CI/CD workflow changes |

**Examples:**

```
feat(strategy): add Rust language support to EntropyScrambler

Implements variable-renaming for Rust's identifier model.
Closes #87.

fix(lexer): handle empty input files without NullPointerException

Fixes #102.
```

> Breaking changes must include a `BREAKING CHANGE:` footer or a `!` after the type:
> `feat!: remove --legacy-mode flag`

---

## Testing Guide

```bash
# Run all unit tests
mvn test

# Run tests for a specific class
mvn test -Dtest=CommentPoisonerTest

# Run full verify cycle (tests + integration)
mvn verify

# Generate coverage report (HTML at target/site/jacoco/index.html)
mvn verify jacoco:report
```

### Test Expectations

- Every new feature must be accompanied by **unit tests** covering at least the happy path, edge cases (empty input, max-length input), and error conditions.
- Tests must pass on **Java 21** on **Ubuntu, macOS, and Windows**.
- Do not commit tests that are tagged `@Disabled` without an accompanying issue number.

---

## Pull Requests

1. **Keep PRs focused.** One logical change per PR. Avoid bundling unrelated refactors.
2. **Link the issue.** Use `Closes #<n>` or `Fixes #<n>` in the PR description to auto-close.
3. **Fill in the PR template** completely.
4. **All CI checks must pass** before review will begin.
5. **One approving review** is required from a maintainer before merge.
6. Maintainers may use **squash merge** to keep history clean.

---

## Reporting Bugs

Use the [Bug Report](https://github.com/devhms/nightshade/issues/new?template=bug_report.md) issue template and include:

- Nightshade version (`java -jar nightshade.jar --version`)
- Operating system and JDK version
- Exact command you ran
- Expected vs. actual behaviour
- Full stack trace or log output (attach as a file if long)

---

## Requesting Features

Use the [Feature Request](https://github.com/devhms/nightshade/issues/new?template=feature_request.md) issue template. Before submitting:

- Search existing issues to avoid duplicates
- Explain *why* the feature is needed, not just *what* it should do
- Include use-cases and acceptance criteria

Large proposals may be converted into an RFC discussion before implementation begins.

---

## Security Issues

**Do not open public issues for security vulnerabilities.**

Follow the [Security Policy](SECURITY.md) to report privately via GitHub Private Vulnerability Reporting.

---

## First Good Issues

New to the codebase? Look for issues labelled [`good first issue`](https://github.com/devhms/nightshade/issues?q=label%3A%22good+first+issue%22+is%3Aopen) — they are scoped to be approachable without deep knowledge of the engine.

Issues labelled [`help wanted`](https://github.com/devhms/nightshade/issues?q=label%3A%22help+wanted%22+is%3Aopen) are also a great way to have higher impact.

---

## Developer Certificate of Origin

By making a contribution to this project, you certify that:

1. The contribution was created in whole or in part by you, and you have the right to submit it under the open-source license indicated in the repository.
2. You understand and agree that your contribution and a record of it are maintained indefinitely and may be redistributed consistent with this project's license.

To accept these terms, sign your commits with `--signoff`:

```bash
git commit --signoff -m "feat(strategy): add Go language support"
```

This adds a `Signed-off-by: Your Name <your@email.com>` trailer to the commit message.

---

*Thank you for helping make Nightshade better. 🌑*
