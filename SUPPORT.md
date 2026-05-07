# Support

We want to ensure you have a great experience using Nightshade. Here is how you can get help, ask questions, or report issues.

---

## Where to Get Help

| Need Help With? | Where to Go |
|-----------------|-------------|
| **Bug Reports & Regressions** | [GitHub Issues](https://github.com/devhms/nightshade/issues) |
| **Feature Requests** | [GitHub Issues](https://github.com/devhms/nightshade/issues) |
| **Questions & Troubleshooting** | [GitHub Discussions](https://github.com/devhms/nightshade/discussions) |
| **Security Vulnerabilities** | See [SECURITY.md](SECURITY.md) |

---

## Before Opening an Issue

To help us resolve your issue as quickly as possible, please:

1. **Search existing issues and discussions** to see if your problem has already been reported or answered.
2. **Check the FAQ** below.
3. **Verify your version** by running `java -jar nightshade.jar --version`. Ensure you are on the latest stable release.
4. **Prepare a minimal reproducible example.** If Nightshade is corrupting a file unexpectedly, provide the smallest snippet of code that reproduces the issue.

---

## Frequently Asked Questions (FAQ)

### 1. Does Nightshade break my code's functionality?
No. Nightshade is designed to be purely semantic and structural for human and LLM readers, but functionally identical when compiled or interpreted. All transformations preserve the original program logic.

### 2. Can I use Nightshade on a proprietary codebase?
Yes. Nightshade is licensed under the MIT License, which permits use in commercial and proprietary projects. However, it is a tool applied *before* publication. If your code is not public, LLMs cannot scrape it, so Nightshade is most useful for open-source code or public-facing scripts (like frontend JavaScript).

### 3. I got a "NullPointerException" or syntax error during parsing!
Nightshade uses robust tokenizers, but edge cases in newer language features (e.g., Java 21+ pattern matching) might occasionally fail. Please open a Bug Report with a minimal code snippet so we can add it to our test suite and fix the parser.

### 4. How do I prevent specific methods or classes from being obfuscated?
Nightshade v3.0 automatically detects `public` API boundaries to prevent renaming classes and methods intended for external use. You can also manually skip blocks using comments:

```java
// @nightshade:skip
public void myMethod() {
    // This method will not be altered
}
// @nightshade:resume
```

---

## Troubleshooting Checklist

If Nightshade isn't behaving as expected:

- [ ] **Run with `--verbose`:** Add the `-v` or `--verbose` flag to your command to see exactly which strategies are applied to which files and what the entropy scores are.
- [ ] **Check the Entropy Threshold:** If no changes are being made, your file might be very small, or the threshold might be too low. Try `--entropy-threshold 1.0` to force all strategies to run fully.
- [ ] **Validate Input:** Ensure your input directory contains supported files (`.java`, `.py`, `.js`, `.ts`).

---

## Community

If you want to contribute to the project, please read our [Contributing Guide](CONTRIBUTING.md) and join the conversation in our [Discussions](https://github.com/devhms/nightshade/discussions) tab.