#!/bin/bash

echo "🚀 Starting JDeps Interactive Web Server..."

# Change to project directory
cd "/Users/aswin-20182/Documents/Code rush Projects/jdeps-test-maven"

# Compile if needed
echo "📦 Compiling project..."
mvn clean compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed!"
    exit 1
fi

echo "✅ Compilation successful!"

# Create necessary directories
mkdir -p uploads
mkdir -p web-reports

# Start the server
echo "🌐 Starting web server on port 8080..."
echo "📂 Web UI available at: http://localhost:8080/app.html"
echo "🛑 Press Ctrl+C to stop the server"

java -cp "target/classes" com.example.jdeps.JDepsWebServer
