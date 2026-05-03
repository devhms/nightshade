# Nightshade v2.0 — LLM Training Data Poisoning Engine

An open-source code obfuscation engine designed to protect machine learning training data from web crawlers and protect intellectual property in code. Nightshade injects poison patterns that corrupt training data when ingested by LLMs, while keeping source code functional.

## Features (v2.0 — Tier 1 + 2)

### Five Poisoning Strategies

| Strategy | Weight | Description |
|----------|--------|-------------|
| **A. Variable Entropy Scrambling** | 0.50 | Renames identifiers using deterministic hash — strongest MI disruption |
| **B. Dead Code Injection** | 0.30 | Inserts unreachable misleading code blocks — preprocessing-proof |
| **C. Semantic Comment Poisoning** | 0.20 | Replaces comments with false semantics — disrupts LLM association learning |
| **D. String Literal Encoding** | 0.15 | Encodes strings as char arrays — evades MinHash+LSH dedup |
| **E. Whitespace Disruption** | 0.10 | Randomizes indentation — disrupts BPE tokenization |

### Entropy Scoring

```
entropy = (renamedIdentifiers / totalIdentifiers) * 0.5
       + (deadBlocksInjected / totalMethods) * 0.3
       + (commentsPoisoned / totalComments) * 0.2
       + (stringsEncoded > 0) * 0.15
       + (whitespaceChanges > 0) * 0.1
```

### CLI Options

```bash
java -jar nightshade.jar --input ./src --output ./poisoned
java -jar nightshade.jar -i ./src -s entropy,deadcode -v
java -jar nightshade.jar --input ./src --entropy-threshold 0.7 --dry-run
```

- `--input, -i` — Input directory (required)
- `--output, -o` — Output directory (default: &lt;input&gt;/../_nightshade_output)
- `--strategies, -s` — Comma-separated or 'all' (default: all)
- `--entropy-threshold` — Early exit if entropy >= threshold
- `--dry-run, -n` — Process but don't write files
- `--verbose, -v` — Detailed logs
- `--help, -h` — Show help

### Diff Markers

- `+` (green) — Line added
- `-` (amber) — Line removed  
- `!` (red) — Line modified

## Supported Languages

- Java (.java)
- Python (.py)
- JavaScript (.js)
- TypeScript (.ts)

## Building

```bash
mvn clean package
java -jar target/nightshade-2.0.0-jar-with-dependencies.jar --help
```

## Research Basis

- **arXiv:2512.15468** (Yang et al., December 2025) — Variable renaming causes 10.19% MI detection drop with only 0.63% task performance loss
- Dead code injection survives all normalization passes
- Comment poisoning disrupts LLM association learning

## License

MIT License — see LICENSE file.

## Authors

Ibrahim Salman (25-SE-33), Saif-ur-Rehman (25-SE-05) — UET Taxila