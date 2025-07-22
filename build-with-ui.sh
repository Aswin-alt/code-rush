#!/bin/bash

# Enhanced build and analysis script with web UI report generation
echo "=== Enhanced JDeps Analysis with Web UI ==="
echo ""

# Create build directory
mkdir -p build/classes

# Compile all Java classes including the new analyzer
echo "Compiling Java classes..."
javac -d build/classes src/main/java/com/example/jdeps/*.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
else
    echo "✗ Compilation failed"
    exit 1
fi

# Create JAR file
echo "Creating JAR file..."
cd build/classes
jar cf ../simple-jdeps-test.jar com/
cd ../..

echo "✓ JAR created: build/simple-jdeps-test.jar"
echo ""

# Run the simple application first
echo "Running the SimpleJDepsTestApp..."
java -cp build/classes com.example.jdeps.SimpleJDepsTestApp
echo ""

# Generate web UI report
echo "=== Generating Web UI Report ==="
java -cp build/classes com.example.jdeps.JDepsAnalyzer
echo ""

# Run basic JDeps analyses for comparison
echo "=== Traditional JDeps Analysis ==="
echo ""

echo "1. Basic analysis:"
jdeps build/simple-jdeps-test.jar
echo ""

echo "2. Summary analysis:"
jdeps -s build/simple-jdeps-test.jar
echo ""

echo "3. Verbose analysis (first 20 lines):"
jdeps -verbose:class build/simple-jdeps-test.jar | head -20
echo "... (truncated for brevity)"
echo ""

echo "=== Analysis Complete ==="
echo "Files created:"
echo "  - build/classes/ (compiled classes)"
echo "  - build/simple-jdeps-test.jar (JAR file)"
echo "  - jdeps-report/jdeps-report.html (Interactive Web Report)"
echo "  - jdeps-report/jdeps-report.json (JSON Data)"
echo ""
echo "📊 Open jdeps-report/jdeps-report.html in your browser to view the interactive report!"
