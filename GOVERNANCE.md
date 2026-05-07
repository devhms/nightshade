# Governance

## Roles
- **Maintainers:** Own the roadmap, review PRs, and publish releases.
- **Reviewers:** Help review PRs and triage issues.
- **Contributors:** Submit issues, PRs, documentation, and feedback.

## Decision Making
- Routine changes use lazy consensus (approve unless objections are raised).
- Breaking changes and roadmap shifts require a maintainer vote.
- Security fixes may be merged by a maintainer after review.

## Becoming a Maintainer
- Consistent contributions over at least three months
- Demonstrated review quality and responsiveness
- Approval by existing maintainers

## Releases
- Follow Semantic Versioning (MAJOR.MINOR.PATCH).
- Release checklist:
  - Update CHANGELOG.md
  - Tag a release in GitHub
  - Publish release notes and artifacts

## Repository Settings (GitHub UI)
The following settings are recommended for the default branch:
- Require pull request reviews (minimum 1)
- Require status checks to pass before merging
- Require CODEOWNERS review
- Require linear history and block force pushes
- Enable private vulnerability reporting

## Code of Conduct
The community follows CODE_OF_CONDUCT.md. Report violations to the enforcement contact listed there.

## Security
Security issues are handled per SECURITY.md.
