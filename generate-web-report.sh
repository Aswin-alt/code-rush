#!/bin/bash

# Generate JDeps Web Report Script
echo "=== JDeps Web Report Generator ==="
echo ""

# Build the project first
echo "1. Building the project..."
mkdir -p build/classes
mkdir -p web-report

# Download Jackson dependencies if not present
echo "2. Downloading dependencies..."
mkdir -p lib

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

# Compile classes
echo "3. Compiling classes..."
CLASSPATH="lib/*"

javac -cp "$CLASSPATH" -d build/classes \
    src/main/java/com/example/jdeps/*.java

if [ $? -ne 0 ]; then
    echo "✗ Compilation failed"
    exit 1
fi

# Create JAR
echo "4. Creating JAR..."
cd build/classes
jar cf ../jdeps-test.jar com/
cd ../..

echo "✓ Build successful"

# Run JDeps analysis and generate web report
echo "5. Generating web report..."

# Copy web files to report directory
cp -r src/main/resources/web/* web-report/ 2>/dev/null || echo "Web files will be created by generator"

# Run the web report generator
java -cp "lib/*:build/classes" com.example.jdeps.JDepsWebReportGenerator \
    "$(pwd)" "build/jdeps-test.jar"

if [ $? -eq 0 ]; then
    echo ""
    echo "=== Report Generated Successfully! ==="
    echo ""
    echo "📊 Web Report Location: $(pwd)/web-report/"
    echo ""
    echo "🌐 To view the report:"
    echo "   Option 1: Open web-report/index.html in your browser"
    echo "   Option 2: Start HTTP server:"
    echo "            cd web-report && python3 -m http.server 8080"
    echo "            Then visit: http://localhost:8080"
    echo ""
    echo "📁 Files created:"
    echo "   - web-report/index.html (Main report page)"
    echo "   - web-report/styles.css (Styling)"
    echo "   - web-report/script.js (Interactive features)"
    echo "   - web-report/analysis-data.json (Analysis data)"
    echo ""
    
    # Try to open the report automatically
    if command -v open &> /dev/null; then
        echo "🚀 Opening report in default browser..."
        open "$(pwd)/web-report/index.html"
    elif command -v xdg-open &> /dev/null; then
        echo "🚀 Opening report in default browser..."
        xdg-open "$(pwd)/web-report/index.html"
    else
        echo "💡 Please open web-report/index.html manually in your browser"
    fi
    
else
    echo "✗ Report generation failed"
    exit 1
fi
