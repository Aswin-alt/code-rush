#!/bin/bash

# Simple JDK-only build and test script
echo "=== Simple JDK-only Build Script ==="
echo ""

# Create build directory
mkdir -p build/classes

# Compile the simple application (JDK dependencies only)
echo "Compiling SimpleJDepsTestApp..."
javac -d build/classes src/main/java/com/example/jdeps/SimpleJDepsTestApp.java

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

# Run the application
echo "Running the application..."
java -cp build/classes com.example.jdeps.SimpleJDepsTestApp
echo ""

# Run JDeps analysis
echo "=== JDeps Analysis ==="
echo ""

echo "1. Basic analysis:"
jdeps build/simple-jdeps-test.jar
echo ""

echo "2. Verbose analysis:"
jdeps -verbose:class build/simple-jdeps-test.jar
echo ""

echo "3. Summary analysis:"
jdeps -s build/simple-jdeps-test.jar
echo ""

echo "4. Module analysis:"
jdeps -s build/classes
echo ""

echo "5. Generate DOT file for visualization:"
jdeps -dotoutput build build/simple-jdeps-test.jar
if [ -f "build/simple-jdeps-test.jar.dot" ]; then
    echo "✓ DOT file generated: build/simple-jdeps-test.jar.dot"
    echo "  To visualize: dot -Tpng build/simple-jdeps-test.jar.dot -o build/dependency-graph.png"
fi
echo ""

echo "Build and analysis complete!"
echo "Files created:"
echo "  - build/classes/ (compiled classes)"
echo "  - build/simple-jdeps-test.jar (JAR file)"
echo "  - build/*.dot (dependency graph files)"
