# Nightshade Product Marketing Context

## Product Overview
- **Name**: Nightshade
- **One-liner**: Open-source LLM training data poisoning engine that protects source code from AI scraping
- **Category**: Developer Security Tool
- **Type**: CLI / GitHub Action
- **Version**: 3.5.0
- **License**: MIT

## Target Audience

### Primary: Open-source Maintainers
- Developers with public repositories
- Maintainers who care about code IP
- Users of GitHub, GitLab, Gitee

### Secondary: Individual Developers
- Freelancers with proprietary code
- Developers building side projects
- Anyone who shares code publicly

### Tertiary: Enterprise DevSecOps
- Security teams at tech companies
- Organizations worried about IP leakage
- Companies with open-source policies

## Key Problems Solved

1. **Code being scraped for LLM training without consent**
   - Every public repo is being crawled
   - No way to opt-out effectively
   - Legal terms ignored by AI companies

2. **No protection options that maintain code functionality**
   - Private repos lose community benefits
   - Obfuscation tools break code
   - No dedicated LLM protection tools existed

3. **Emerging regulatory uncertainty**
   - No clear legal frameworks for AI training
   - Copyright lawsuits in progress
   - Need proactive protection

## Value Proposition

| Benefit | Evidence |
|---------|----------|
| 100% functional integrity | Built-in entropy scoring prevents over-obfuscation |
| Research-backed | Based on arXiv:2512.15468 (10.19% detection drop) |
| Multi-language support | Java (all 8 strategies), Python (5 strategies), JavaScript (5 strategies), TypeScript (via JS) |
| Cross-platform | Runs on any OS with Java 21 (Windows, Linux, macOS) |
| CI/CD integrated | GitHub Action available |
| Zero cost | MIT licensed, open-source |

## Requirements
- **Java 21** (JDK 21+) — [Temurin download](https://adoptium.net/)
- **Maven 3.9+** (for building from source)

## Competitive Differentiation

- **Only open-source tool specifically for LLM protection**
- Zero direct competitors for "LLM data poisoning" category
- MIT licensed - no vendor lock-in
- Research-backed (academic paper)
- GitHub Action for automation

## Positioning Statement

"For developers who share code publicly, Nightshade is the open-source tool that protects source code from LLM training scrapes while maintaining 100% functional integrity—unlike private repos (lose community benefits) or general obfuscation (breaks code)."

## Messaging Framework

### Headline (Choice of 3)
- A: "Poison your code against AI training"
- B: "Stop AI companies from stealing your code"
- C: "Your code, your rules: Defend against LLM theft"

### Subhead
"An open-source engine that injects adversarial obfuscation into your source code—corrupting LLM training datasets while keeping your code 100% functional."

### CTA
- Primary: "View on GitHub"
- Secondary: "Try the Demo"

## Pricing
- Free (open-source, MIT license)
- No paid tiers planned

## Key Metrics to Track
- GitHub stars
- Weekly downloads
- Website traffic (monthly)
- Newsletter subscribers
- Discord members

## Brand Voice
- **Tone**: Technical, confident, slightly edgy
- **Personality**: Security researcher meets developer advocate
- **References**: "Glaze protects your art. Nightshade protects your code."

## Competitive Landscape

| Competitor | Type | Weakness |
|------------|------|----------|
| Private repos | Alternative | Loses open source benefits |
| License changes | Alternative | AI ignores legal terms |
| General obfuscators | Alternative | Breaks code, not LLM-specific |
| ProGuard/R8 | Partial | Java-only, not LLM-focused |

---

*Last updated: May 2026*