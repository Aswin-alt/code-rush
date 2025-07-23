#!/bin/bash

echo "🔍 Running ASM Integration Demo"
echo "==============================="

# First compile the project
echo "Compiling project..."
mvn compile -q

if [ $? -ne 0 ]; then
    echo "❌ Compilation failed"
    exit 1
fi

echo "✅ Compilation successful"

# Run the ASM demo
echo "Running ASM analysis demo..."
mvn exec:java -Dexec.mainClass="com.example.jdeps.ASMIntegrationDemo" -Dexec.args="" -q

echo ""
echo "Demo completed! Check the generated reports."
