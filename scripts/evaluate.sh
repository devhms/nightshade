#!/usr/bin/env bash
set -e

echo "========================================================="
echo " Nightshade v3.5.0 Reproducible Evaluation Script"
echo "========================================================="

# Build the project first
echo "=> Building Nightshade..."
mvn clean package -DskipTests

JAR_PATH="target/nightshade-3.5.0-all.jar"

if [ ! -f "$JAR_PATH" ]; then
    echo "ERROR: JAR file not found at $JAR_PATH"
    exit 1
fi

echo "=> Running Nightshade on sample-repo..."
java -jar "$JAR_PATH" --input sample-repo/src --output sample-repo/obfuscated --strategies all --verify

echo "=> Obfuscation complete. Checking results..."
# Check if obfuscated file exists
if [ -f "sample-repo/obfuscated/main/java/com/example/Main.java" ]; then
    echo "SUCCESS: Obfuscated files generated."
else
    echo "ERROR: Obfuscated files not found."
    exit 1
fi

# Try compiling the output to verify basic syntax
echo "=> Compiling obfuscated code..."
javac sample-repo/obfuscated/main/java/com/example/Main.java

if [ $? -eq 0 ]; then
    echo "SUCCESS: Obfuscated code compiled successfully."
else
    echo "ERROR: Obfuscated code failed to compile."
    exit 1
fi

echo "=> Evaluation passed successfully!"
