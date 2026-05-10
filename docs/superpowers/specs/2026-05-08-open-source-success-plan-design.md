# Open-Source Success Plan (Nightshade + Obsidian)

## Context
Nightshade is a Java-based open-source project. The repo currently lacks standard OSS community docs (CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, ROADMAP, etc.) and templates. The user wants a concrete plan and to execute it at maximum potential, plus a reusable general plan saved in the Obsidian vault under `30-Resources`.

## Goals
- Add a full OSS baseline to the Nightshade repo: contributor onboarding, governance, security policy, templates, and automation.
- Create an organized, reusable Open Source Success Plan in Obsidian for long-term reference.
- Keep code behavior unchanged; focus on documentation and repository hygiene.

## Non-Goals
- No application feature changes.
- No deployment or release automation beyond dependency/CodeQL config.
- No external service setup (GitHub settings are documented, not enforced via API).

## Approach (Recommended: Maximum Potential)
### Repo OSS Baseline
Add the following files at repo root (or `.github/` as noted):
- `CONTRIBUTING.md` (setup, workflow, testing, style, triage)
- `CODE_OF_CONDUCT.md` (Contributor Covenant)
- `SECURITY.md` (PVR-first guidance, default security email placeholder)
- `SUPPORT.md` (GitHub Issues + discussion guidance)
- `ROADMAP.md` (near/mid/long-term, release goals, success metrics)
- `GOVERNANCE.md` (roles, decision making, maintainers, release policy)
- `CHANGELOG.md` (Keep a Changelog format; start with current release)
- `LICENSE` already exists; keep as-is

### GitHub Community Scaffolding
Add `.github` files:
- `.github/ISSUE_TEMPLATE/bug_report.md`
- `.github/ISSUE_TEMPLATE/feature_request.md`
- `.github/ISSUE_TEMPLATE/config.yml`
- `.github/PULL_REQUEST_TEMPLATE.md`
- `.github/CODEOWNERS`
- `.github/FUNDING.yml`

### CI/Security Hardening
Add:
- `.github/dependabot.yml` (Maven + GitHub Actions weekly)
- `.github/workflows/codeql.yml` (Java CodeQL scanning)

Document GitHub settings (in `GOVERNANCE.md`):
- Require PR reviews and status checks on default branch
- Enable private vulnerability reporting (PVR)
- Require CODEOWNERS review
- Require linear history / block force pushes (recommended)

### README Alignment
Update `README.md` to:
- Link to CONTRIBUTING, CODE_OF_CONDUCT, SECURITY, ROADMAP, SUPPORT, GOVERNANCE
- Add quick contribution section and badges (optional)

### Obsidian Knowledge Base
Create `30-Resources/open-source/` with:
- `open-source-success-plan.md` (deep actionable guide)
- `open-source-success-moc.md` (table of contents + links)

Link to `10-Projects/nightshade/nightshade.md` as example implementation.

## Structure
- Repo docs focus on Nightshade-specific workflows.
- Obsidian plan is generic + reusable with a Nightshade example section.

## Content Standards
- Clear, concise, ASCII-only text.
- No unnecessary comments.
- Actionable checklists and templates.
- Every change is backed by current best-practice sources (search before implementing).

## Execution Plan (High-level)
1. Create repo docs and `.github` templates.
2. Add Dependabot and CodeQL workflows.
3. Update README with links.
4. Create Obsidian resources and MOC.
5. Verify file structure and YAML validity.

## Risks & Mitigations
- Risk: security email placeholder.
  - Mitigation: mark as TODO in SECURITY.md and document how to update.
- Risk: CodeQL may require GitHub settings to fully enable.
  - Mitigation: document in GOVERNANCE.md; add workflow file anyway.

## References
- GitHub CodeQL docs: https://docs.github.com/code-security/code-scanning/introduction-to-code-scanning/about-code-scanning-with-codeql
- GitHub Dependabot docs: https://docs.github.com/en/code-security/how-tos/secure-your-supply-chain/secure-your-dependencies/configuring-dependabot-version-updates
- OpenSSF SCM best practices: https://best.openssf.org/SCM-BestPractices/
- GitHub coordinated disclosure docs: https://docs.github.com/en/code-security/concepts/vulnerability-reporting-and-management/about-coordinated-disclosure-of-security-vulnerabilities
- Contributor Covenant 2.0: https://www.contributor-covenant.org/version/2/0/code_of_conduct/
- Keep a Changelog: https://keepachangelog.com/en/0.3.0/
- CODEOWNERS docs: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners
- Issue template config.yml docs: https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/configuring-issue-templates-for-your-repository
