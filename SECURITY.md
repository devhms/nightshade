# Security Policy

Nightshade takes security seriously. We appreciate responsible disclosure and will work with you to resolve vulnerabilities quickly and transparently.

---

## Supported Versions

| Version | Supported | Notes |
|---------|-----------|-------|
| `3.5.x` (latest) | ✅ Active | Current stable release |
| `3.x` | ✅ Active | Security patches only |
| `2.x` | ⚠️ Limited | Critical fixes only for 90 days post-v3 GA |
| `< 2.0` | ❌ End-of-life | No further patches |

---

## Reporting a Vulnerability

**Please do NOT open a public GitHub issue for security vulnerabilities.** Doing so exposes all users to risk before a patch is available.

### Preferred: GitHub Private Vulnerability Reporting

The fastest path to resolution is via GitHub's built-in Private Vulnerability Reporting (PVR):

➡️ **[Report a vulnerability](https://github.com/devhms/nightshade/security/advisories/new)**

This creates a private, encrypted thread between you and the maintainers. You can attach proof-of-concept code, patches, and screenshots securely.

### Fallback: Encrypted Email

If you cannot use GitHub PVR, email the maintainer team at:

**security [at] nightshade-project [dot] dev**

Encrypt sensitive content using our PGP key (fingerprint to be published at project launch):

```
Fingerprint: (to be published with first stable release)
Key ID: (to be published with first stable release)
```

---

## What to Include

A high-quality vulnerability report helps us triage and fix faster. Please include:

- [ ] **Vulnerability type** (e.g., path traversal, arbitrary code execution, information disclosure)
- [ ] **Affected component** (e.g., `CLI.java`, `FileUtil.java`, a specific strategy)
- [ ] **Affected versions** (e.g., `3.5.0`, `all versions prior to 3.x`)
- [ ] **Reproduction steps** — the minimum set of commands/inputs to trigger the issue
- [ ] **Potential impact** — what an attacker could achieve
- [ ] **Proof-of-concept** — code, logs, or screenshots (optional but very helpful)
- [ ] **Suggested fix** — if you have one (optional)

---

## Scope

### In Scope

Security issues we consider valid vulnerabilities:

- **Path traversal / directory escape** via `--input` or `--output` flags allowing reads/writes outside the intended directory
- **Arbitrary code execution** triggered by processing a maliciously crafted source file
- **Denial of service** caused by a specially crafted input that causes infinite loops or OOM errors
- **Information disclosure** of sensitive system paths or environment variables in output
- **Dependency vulnerabilities** in third-party libraries that have a realistic exploit path against Nightshade users

### Out of Scope

We will not accept the following as valid security reports:

- Issues requiring physical access to the user's machine
- Social engineering attacks against maintainers or users
- Vulnerabilities in operating systems or JDK versions that are themselves unsupported
- Reports lacking a clear exploit path against a realistic usage scenario
- "Scanner found this" reports with no demonstrated impact

---

## Response Targets

| Milestone | Target |
|-----------|--------|
| Initial acknowledgement | Within **48 hours** of receiving the report |
| Severity assessment | Within **5 business days** |
| Fix or mitigation plan | Within **14 days** for Critical/High severity |
| Patch release | Within **30 days** for Critical; **90 days** for Medium/Low |
| Public disclosure | Coordinated with the reporter after patch release |

We follow a **90-day coordinated disclosure** policy. If we are unable to ship a fix within 90 days we will notify you and agree on an extension or proceed with a temporary mitigation advisory.

---

## Severity Classification

We use the [CVSS v3.1](https://www.first.org/cvss/v3.1/specification-document) scoring system for severity classification:

| CVSS Score | Severity | Response SLA |
|-----------|----------|-------------|
| 9.0 – 10.0 | Critical | 14 days to patch |
| 7.0 – 8.9 | High | 30 days to patch |
| 4.0 – 6.9 | Medium | 90 days to patch |
| 0.1 – 3.9 | Low | Next scheduled release |

---

## CVE Assignment

For confirmed vulnerabilities we will:

1. Request a CVE number from the GitHub Security Advisory system
2. Credit the reporter in the advisory (unless you request anonymity)
3. Publish the advisory simultaneously with the patch release

---

## Hall of Fame

We thank the following researchers for responsible disclosure:

*(This section will be updated as reports are received and resolved.)*

---

## Acknowledgements

This policy is inspired by:

- [OWASP Vulnerability Disclosure Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Vulnerability_Disclosure_Cheat_Sheet.html)
- [Google Project Zero disclosure policy](https://googleprojectzero.blogspot.com/p/vulnerability-disclosure-faq.html)
- [Coordinated Vulnerability Disclosure guidelines — NCSC](https://www.ncsc.gov.uk/information/vulnerability-reporting)