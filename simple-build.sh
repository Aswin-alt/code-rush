#!/bin/bash

# Simple build script without Maven
# Downloads dependencies and compiles the project manually

echo "=== Simple Build Script (No Maven Required) ==="
echo ""

# Create directories
mkdir -p build/classes
mkdir -p lib

# Download dependencies if they don't exist
echo "Downloading dependencies..."

if [ ! -f "lib/commons-lang3-3.12.0.jar" ]; then
    echo "Downloading Apache Commons Lang3..."
    curl -L -o lib/commons-lang3-3.12.0.jar \
        "https://repo1.maven.org/maven2/org/apache/commons/commons-lang3/3.12.0/commons-lang3-3.12.0.jar"
fi

if [ ! -f "lib/jackson-databind-2.15.2.jar" ]; then
    echo "Downloading Jackson Databind..."
    curl -L -o lib/jackson-databind-2.15.2.jar \
        "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-databind/2.15.2/jackson-databind-2.15.2.jar"
fi

if [ ! -f "lib/jackson-core-2.15.2.jar" ]; then
    echo "Downloading Jackson Core..."
    curl -L -o lib/jackson-core-2.15.2.jar \
        "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-core/2.15.2/jackson-core-2.15.2.jar"
fi

if [ ! -f "lib/jackson-annotations-2.15.2.jar" ]; then
    echo "Downloading Jackson Annotations..."
    curl -L -o lib/jackson-annotations-2.15.2.jar \
        "https://repo1.maven.org/maven2/com/fasterxml/jackson/core/jackson-annotations/2.15.2/jackson-annotations-2.15.2.jar"
fi

echo "✓ Dependencies downloaded"
echo ""

# Compile the classes
echo "Compiling Java classes..."
CLASSPATH="lib/*"

javac -cp "$CLASSPATH" -d build/classes \
    src/main/java/com/example/jdeps/*.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
else
    echo "✗ Compilation failed"
    exit 1
fi

# Create JAR file
echo "Creating JAR file..."
cd build/classes
jar cf ../jdeps-test.jar com/
cd ../..

echo "✓ JAR created: build/jdeps-test.jar"
echo ""

echo "To run the application:"
echo "  java -cp 'lib/*:build/jdeps-test.jar' com.example.jdeps.JDepsTestApp"
echo ""

echo "To analyze with JDeps:"
echo "  jdeps -cp 'lib/*' build/jdeps-test.jar"
