# Governance

Nightshade is an open-source project driven by the community and maintained by a core team. This document outlines how decisions are made, how roles are defined, and how the project operates.

---

## Roles

### 1. Contributors
Anyone who interacts with the project is a contributor. This includes submitting issues, creating pull requests, writing documentation, or answering questions in Discussions.

### 2. Reviewers
Reviewers are active contributors who have shown a deep understanding of the codebase and project goals. They are trusted to review pull requests, triage issues, and guide new contributors.
* **How to become a reviewer:** Consistently provide high-quality reviews on open PRs and help triage issues for at least two months.

### 3. Maintainers
Maintainers hold write access to the repository. They are responsible for merging PRs, cutting releases, steering the roadmap, and enforcing the Code of Conduct.
* **Current Maintainers:** See the [CODEOWNERS](.github/CODEOWNERS) file.
* **How to become a maintainer:**
  - Consistent, high-impact contributions over at least three months.
  - Demonstrated review quality and responsiveness.
  - Nominated and approved by a simple majority of existing maintainers.

### 4. Emeritus Maintainers
Maintainers who have stepped away from active duty. We thank them for their service. They retain their title but lose write access for security purposes. They can be reinstated upon request and majority approval.

---

## Decision Making

### Routine Changes (Lazy Consensus)
Most daily decisions (bug fixes, minor features, documentation updates) use **lazy consensus**. 
* A PR is opened.
* If it receives an approval from a maintainer and no objections are raised within 72 hours (excluding weekends), it is merged.
* Silence is consent.

### Major Changes (RFC Process)
For significant architectural changes, new language support, or shifts in the project roadmap, we use the **Request for Comments (RFC)** process.
1. Open an issue with the label `RFC`.
2. Detail the problem, proposed solution, alternatives, and success criteria.
3. The community discusses the RFC.
4. A maintainer calls for a vote after sufficient discussion (usually 1-2 weeks).

### Voting
When consensus cannot be reached, maintainers will vote.
* **Quorum:** At least 50% of active maintainers must vote.
* **Threshold:** A simple majority wins.
* **Veto:** No single maintainer has veto power, except in cases involving security or Code of Conduct violations.

---

## Releases

Nightshade follows [Semantic Versioning (SemVer)](https://semver.org/) (MAJOR.MINOR.PATCH).

* **MAJOR:** Incompatible CLI changes, removed strategies, or major architectural shifts.
* **MINOR:** New poisoning strategies, new language support, or backwards-compatible features.
* **PATCH:** Bug fixes, performance improvements, and documentation updates.

### Release Checklist
1. Verify all CI checks pass on `main`.
2. Update `CHANGELOG.md` with release notes and the current date.
3. Trigger the `Release` GitHub Action workflow to build the fat JAR and publish the release.

---

## Repository Configuration

To protect the integrity of the codebase, the `main` branch has the following protections enabled:
* Require pull request reviews (minimum 1 approval from a CODEOWNER).
* Require status checks to pass before merging (CI, CodeQL).
* Require linear history (Squash and Merge preferred).
* Block force pushes.

---

## Conflict Resolution

If disagreements arise:
1. Keep the discussion focused on the code/technical merits, not the person.
2. If a thread becomes heated, maintainers may temporarily lock it to allow a cooling-off period.
3. If an agreement cannot be reached, it escalates to a maintainer vote.
4. Behavioural conflicts are handled according to the [Code of Conduct](CODE_OF_CONDUCT.md).

---

## Trademark and Naming Policy

"Nightshade" is the name of this open-source project. If you fork the project and significantly alter its purpose, or distribute a commercial, closed-source derivative, please rename your fork to avoid confusing users about the official source of the Nightshade obfuscation engine.
