#!/bin/bash

# Complete Web UI Setup Script
echo "=== Setting up JDeps Web UI Application ==="
echo ""

# Create necessary directories
mkdir -p build/classes
mkdir -p uploads
mkdir -p web-reports

# Compile all Java classes
echo "1. Compiling Java classes..."
javac -d build/classes src/main/java/com/example/jdeps/*.java

if [ $? -eq 0 ]; then
    echo "✓ Compilation successful"
else
    echo "✗ Compilation failed"
    exit 1
fi

# Create a sample ZIP project for testing
echo ""
echo "2. Creating sample project for testing..."
mkdir -p sample-project/src/com/test
cat > sample-project/src/com/test/TestClass.java << 'EOF'
package com.test;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

public class TestClass {
    public static void main(String[] args) {
        List<String> items = Arrays.asList("apple", "banana", "cherry");
        
        List<String> filtered = items.stream()
            .filter(s -> s.length() > 5)
            .collect(Collectors.toList());
        
        System.out.println("Filtered items: " + filtered);
        System.out.println("Current time: " + LocalDateTime.now());
    }
}
EOF

cd sample-project
zip -r ../sample-project.zip .
cd ..
rm -rf sample-project

echo "✓ Sample project created: sample-project.zip"

# Create launcher script
echo ""
echo "3. Creating launcher script..."
cat > start-server.sh << 'EOF'
#!/bin/bash
echo "Starting JDeps Web Server..."
java -cp build/classes com.example.jdeps.JDepsWebServer
EOF

chmod +x start-server.sh

# Create README for the web UI
echo ""
echo "4. Creating web UI documentation..."
cat > WEB_UI_README.md << 'EOF'
# JDeps Web UI Application

A complete web-based interface for analyzing Java projects with JDeps.

## Features

- 📁 **Drag & Drop Upload**: Upload ZIP files or JAR files directly in the browser
- 🔍 **Automatic Analysis**: Extracts, compiles, and analyzes Java projects automatically
- 📊 **Interactive Reports**: Beautiful web-based reports with multiple analysis views
- ⚡ **Real-time Progress**: Live progress tracking during analysis
- 🎨 **Modern UI**: Responsive design with gradient backgrounds and smooth animations

## How to Use

### 1. Start the Server
```bash
./start-server.sh
```

### 2. Open Your Browser
Navigate to: http://localhost:8080

### 3. Upload Your Project
- Drag and drop a ZIP file containing your Java project
- Or click to browse and select a file
- Supported formats: .zip, .jar (up to 100MB)

### 4. Analyze
- Click the "🚀 Analyze Project" button
- Watch the real-time progress
- View the generated report

## Supported Project Types

- ✅ **Maven Projects**: Projects with pom.xml
- ✅ **Gradle Projects**: Projects with build.gradle
- ✅ **Standard Java**: Projects with src/ directory structure
- ✅ **JAR Files**: Pre-compiled JAR files
- ✅ **Source Code**: Any ZIP containing .java files

## API Endpoints

- `GET /`: Serves the web UI
- `POST /upload`: Handles file uploads
- `POST /analyze`: Processes uploaded projects
- `GET /reports/*`: Serves generated reports

## Technology Stack

- **Backend**: Java 11+ with built-in HTTP server
- **Frontend**: Pure HTML5/CSS3/JavaScript (no frameworks)
- **Analysis**: JDeps (built into JDK)
- **Reports**: Interactive HTML with Chart.js

## Sample Test

A sample project (`sample-project.zip`) has been created for testing.
You can upload this file to test the application functionality.

## Output

The application generates:
- Interactive HTML reports with tabs for different analysis types
- JSON data files with analysis results
- Dependency graphs and statistics
- Error reports for failed analyses

## Security Notes

- All processing happens locally on your machine
- Files are temporarily stored in the `uploads/` directory
- Reports are generated in the `web-reports/` directory
- No data is sent to external servers
EOF

echo "✓ Documentation created: WEB_UI_README.md"

echo ""
echo "=== Setup Complete! ==="
echo ""
echo "🚀 To start the web application:"
echo "   ./start-server.sh"
echo ""
echo "🌐 Then open your browser to:"
echo "   http://localhost:8080"
echo ""
echo "📋 Files created:"
echo "   - build/classes/ (compiled Java classes)"
echo "   - sample-project.zip (test project)"
echo "   - start-server.sh (server launcher)"
echo "   - WEB_UI_README.md (documentation)"
echo "   - web-ui/index.html (web interface)"
echo ""
echo "📁 Directories created:"
echo "   - uploads/ (for uploaded files)"
echo "   - web-reports/ (for generated reports)"
echo ""
echo "✨ Ready to analyze Java projects with the web UI!"
