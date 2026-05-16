#!/bin/bash

# Nightshade GitHub Marketing Setup Script
# Run this after: gh auth login

echo "========================================"
echo "Nightshade GitHub Marketing Setup"
echo "========================================"

# Check if authenticated
gh auth status || { echo "Please run: gh auth login"; exit 1; }

REPO="devhms/nightshade"

# ============================================
# 7.1 Apply GitHub Repository Topics
# ============================================
echo ""
echo "[1/3] Setting GitHub Topics..."
gh repo edit $REPO \
  --add-topic llm-security \
  --add-topic data-poisoning \
  --add-topic code-obfuscation \
  --add-topic anti-scraping \
  --add-topic adversarial-machine-learning \
  --add-topic copyright-protection \
  --add-topic java \
  --add-topic python \
  --add-topic javascript \
  --add-topic security

echo "✅ Topics applied: llm-security, data-poisoning, code-obfuscation, anti-scraping, adversarial-machine-learning, copyright-protection, java, python, javascript, security"

# ============================================
# 7.2 Set Repository Description and Website
# ============================================
echo ""
echo "[2/3] Setting Repository Description..."
gh repo edit $REPO \
  --description "Open-source code obfuscation engine that poisons LLM training data — protects Java, Python & JavaScript source code from AI scraping" \
  --homepage "https://devhms.github.io/nightshade/"

echo "✅ Description and homepage set"

# ============================================
# 7.3 Create GitHub Release v3.5.0
# ============================================
echo ""
echo "[3/3] Creating GitHub Release v3.5.0..."

# First, build the JAR if it doesn't exist
if [ ! -f "target/nightshade-3.5.0-all.jar" ]; then
    echo "Building JAR..."
    mvn clean package -DskipTests -q
fi

# Check if release tag already exists
if gh release view v3.5.0 --repo $REPO &> /dev/null; then
    echo "⚠️ Release v3.5.0 already exists. Skipping creation."
else
    gh release create v3.5.0 \
        --title "Nightshade v3.5.0 — LLM Data Poisoning Engine" \
        --notes "Release v3.5.0 with full poisoning strategies support.

## What's New
- 8 poisoning strategies (5 enabled by default)
- Java, Python, JavaScript support
- GitHub Action integration
- Entropy scoring system

## Installation
\`\`\`bash
java -jar nightshade-3.5.0-all.jar --input ./src
\`\`\`

## GitHub Action
\`\`\`yaml
- uses: devhms/nightshade@v3.5.0
\`\`\`" \
        target/nightshade-3.5.0-all.jar

    echo "✅ Release v3.5.0 created"
fi

echo ""
echo "========================================"
echo "✅ GitHub Marketing Setup Complete!"
echo "========================================"
echo ""
echo "Summary:"
echo "  - Repository Topics: Applied"
echo "  - Description: Set"
echo "  - Homepage: Set"
echo "  - Release: Created (or already exists)"
echo ""
echo "Next steps:"
echo "  1. Star the repo: gh repo star $REPO"
echo "  2. Promote on social media"
echo "  3. Submit to Product Hunt"