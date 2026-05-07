# Contributing to Nightshade

Thanks for taking the time to contribute. This project follows the Code of Conduct and values respectful, constructive collaboration.

## Quick Links
- Code of Conduct: CODE_OF_CONDUCT.md
- Security Policy: SECURITY.md
- Support: SUPPORT.md

## Prerequisites
- Java 21 (JDK 21)
- Maven 3.9+
- Git

## Getting Started
1. Fork the repository.
2. Clone your fork and enter the repo:

```bash
git clone https://github.com/devhms/nightshade.git
cd nightshade
```

3. Run tests:

```bash
mvn -q test
```

## Build

```bash
mvn clean package
```

## Project Structure
- `src/main/java` - main application sources
- `src/main/resources` - resources and FXML (if applicable)
- `docs/` - documentation and specs

## Development Workflow
1. Create a branch from `main`:

```bash
git checkout -b feat/short-description
```

2. Make focused changes with clear commit messages.
3. Keep changes scoped; avoid unrelated refactors.
4. Ensure tests pass before opening a PR.

## Pull Requests
- Provide a short summary of the change and why it is needed.
- Link related issues if they exist.
- Include screenshots or logs when relevant.
- Confirm that documentation is updated when behavior changes.

## Reporting Bugs
Use the bug report issue template and include:
- Reproduction steps
- Expected vs actual behavior
- Environment details (OS, JDK, Maven)

## Security Issues
Do not open public issues for security problems. Follow SECURITY.md for private reporting.
