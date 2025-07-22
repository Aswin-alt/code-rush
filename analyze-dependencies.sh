#!/bin/bash

# JDeps Analysis Script for Maven Project
# This script builds the project and runs various JDeps analyses

echo "=== JDeps Analysis Script ==="
echo ""

# Check if Maven is available
if ! command -v mvn &> /dev/null; then
    echo "Error: Maven is not installed or not in PATH"
    exit 1
fi

# Check if JDeps is available
if ! command -v jdeps &> /dev/null; then
    echo "Error: JDeps is not available. Make sure you're using JDK 8 or later"
    exit 1
fi

# Build the project
echo "1. Building the Maven project..."
mvn clean package -q

if [ $? -ne 0 ]; then
    echo "Error: Maven build failed"
    exit 1
fi

echo "✓ Build successful"
echo ""

# Create output directory for results
mkdir -p jdeps-output
cd jdeps-output

echo "2. Running JDeps analyses..."
echo ""

# Basic dependency analysis
echo "--- Basic Dependency Analysis ---"
jdeps ../target/jdeps-test-1.0-SNAPSHOT.jar > basic-analysis.txt
cat basic-analysis.txt
echo ""

# Verbose analysis
echo "--- Verbose Analysis ---"
jdeps -verbose:class ../target/jdeps-test-1.0-SNAPSHOT.jar > verbose-analysis.txt
echo "Verbose analysis saved to jdeps-output/verbose-analysis.txt"
echo ""

# Summary analysis
echo "--- Summary Analysis ---"
jdeps -s ../target/jdeps-test-1.0-SNAPSHOT.jar > summary-analysis.txt
cat summary-analysis.txt
echo ""

# Check for JDK internal API usage
echo "--- JDK Internal API Usage Check ---"
jdeps -jdkinternals ../target/jdeps-test-1.0-SNAPSHOT.jar > internal-api-check.txt
if [ -s internal-api-check.txt ]; then
    cat internal-api-check.txt
else
    echo "No JDK internal API usage detected"
fi
echo ""

# Generate DOT files for visualization
echo "--- Generating Dependency Graphs ---"
jdeps -dotoutput . ../target/jdeps-test-1.0-SNAPSHOT.jar
if [ -f "jdeps-test-1.0-SNAPSHOT.jar.dot" ]; then
    echo "✓ DOT file generated: jdeps-test-1.0-SNAPSHOT.jar.dot"
    echo "  Use Graphviz to visualize: dot -Tpng jdeps-test-1.0-SNAPSHOT.jar.dot -o dependency-graph.png"
else
    echo "No DOT file generated"
fi
echo ""

# Module analysis (Java 9+)
echo "--- Module Analysis ---"
jdeps -s ../target/classes > module-analysis.txt
cat module-analysis.txt
echo ""

# Filter analysis
echo "--- Filter Analysis (External Dependencies Only) ---"
jdeps -filter:none ../target/jdeps-test-1.0-SNAPSHOT.jar > filter-analysis.txt
cat filter-analysis.txt
echo ""

echo "=== Analysis Complete ==="
echo "All output files are saved in the 'jdeps-output' directory:"
ls -la
echo ""
echo "To run the application:"
echo "  java -jar ../target/jdeps-test-1.0-SNAPSHOT-shaded.jar"
