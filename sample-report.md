# Nightshade Poisoning Report

## Summary
| Metric | Value |
|--------|-------|
| Files Processed | 4 |
| Identifiers Renamed | 72 |
| Dead Blocks Injected | 2 |
| Comments Poisoned | 12 |
| Strings Encoded | 2 |
| Whitespace Changes | 19 |
| Avg Entropy Score | 0.545 |
| Files Above Threshold | 2/4 (50%) |

## Per-File Breakdown

| File | Entropy | Renamed | Dead | Comments | Strings |
|------|---------|---------|------|----------|---------|
| Calculator.java | 0.810 | 13 | 2 | 2 | 0 |
| Hello.java | 0.567 | 8 | 0 | 1 | 2 |
| Utils.py | 0.457 | 18 | 0 | 1 | 0 |
| algorithms.py | 0.346 | 33 | 0 | 8 | 0 |

## Estimated MI Resistance
Based on arXiv:2512.15468, variable renaming alone provides ~10.19% MI detection drop. Combined with dead code injection, comment poisoning, and string encoding, estimated total MI resistance: **39.2%**
