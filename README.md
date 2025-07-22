# 🔍 JDeps Project Analyzer

A comprehensive web-based tool for analyzing Java project dependencies using JDeps. Upload ZIP files containing Java projects and get detailed dependency analysis reports.

## ✨ Features

- **📁 File Upload**: Drag & drop interface for ZIP files (up to 100MB)
- **🔍 Dual Analysis**: Supports both Java source projects and deployed applications with JAR files
- **📊 Interactive Reports**: Beautiful HTML reports with dependency graphs and statistics
- **⚡ Real-time Progress**: Live progress tracking during analysis
- **🌐 Web Interface**: Modern, responsive web UI
- **🔧 Multiple Formats**: Generates HTML, JSON, and text reports

## 🚀 Quick Start

### Prerequisites
- Java 11 or higher
- Maven 3.6+
- Modern web browser

### Installation & Running

1. **Clone the repository:**
   ```bash
   git clone <your-repo-url>
   cd jdeps-test-maven
   ```

2. **Compile the project:**
   ```bash
   mvn clean compile
   ```

3. **Start the web server:**
   ```bash
   ./start-interactive-server.sh
   ```

4. **Open your browser:**
   ```
   http://localhost:8080/app.html
   ```

## 📖 Usage

### For Java Source Projects
1. Create a ZIP file containing your Java source code
2. Upload via the web interface
3. The tool will:
   - Extract the ZIP
   - Compile Java sources
   - Create JAR file
   - Run JDeps analysis
   - Generate interactive report

### For Deployed Applications
1. Upload a ZIP file containing JAR files
2. The tool will:
   - Extract the ZIP
   - Find existing JAR files
   - Run JDeps on each JAR
   - Generate combined analysis report

## 🛠 Project Structure

```
jdeps-test-maven/
├── src/main/java/
│   └── com/example/jdeps/
│       ├── JDepsTestApp.java           # Main demo application
│       ├── DependencyTestUtil.java     # Utility class with dependencies
│       ├── SimpleJDepsTestApp.java     # Simple JDK-only demo
│       ├── SimpleJDepsReportGenerator.java  # Report generator
│       ├── ZipProjectAnalyzer.java     # ZIP analysis engine
│       └── JDepsWebServer.java         # HTTP server
├── web-ui/
│   ├── app.html                        # Interactive upload interface
│   ├── demo.html                       # Demo page
│   └── index.html                      # File upload UI
├── scripts/
│   ├── start-interactive-server.sh     # Server startup script
│   ├── setup-web-ui.sh                # Setup script
│   └── build-and-analyze.sh           # Build and analyze script
└── pom.xml                             # Maven configuration
```

## 🔧 API Endpoints

- `GET /` - Serve static files
- `POST /upload` - Handle file uploads
- `POST /analyze` - Trigger analysis
- `GET /reports/{id}/*` - Serve generated reports

## 📊 Analysis Types

The tool provides three levels of JDeps analysis:

1. **Basic Analysis**: Shows direct dependencies
2. **Verbose Analysis**: Detailed class-level dependencies
3. **Summary Analysis**: High-level dependency summary

## 🎯 Supported File Types

- **ZIP files** containing:
  - Java source code (`.java`)
  - Compiled JAR files (`.jar`)
  - Mixed projects with both source and JARs

## 🔍 Example JDeps Commands

For manual analysis, you can use these JDeps commands:

```bash
# Build the project first
mvn package

# Basic analysis
jdeps target/jdeps-test-1.0-SNAPSHOT.jar

# Verbose analysis showing all dependencies
jdeps -verbose:class target/jdeps-test-1.0-SNAPSHOT.jar

# Show only external dependencies
jdeps -filter:none target/jdeps-test-1.0-SNAPSHOT.jar

# Generate dependency graph
jdeps -dotoutput graphs target/jdeps-test-1.0-SNAPSHOT.jar

# Check for internal API usage
jdeps -jdkinternals target/jdeps-test-1.0-SNAPSHOT.jar

# Summary view
jdeps -s target/jdeps-test-1.0-SNAPSHOT.jar
```

## 🔍 Example Output

The generated reports include:
- **Dependency graphs** showing relationships between modules
- **Statistics** on dependency counts and types
- **Module information** and package details
- **Interactive tables** for exploring dependencies

## 🆘 Troubleshooting

### Common Issues

**"Analysis Failed" Error:**
- Ensure ZIP file contains valid Java code or JAR files
- Check file size (max 100MB)
- Verify Java version compatibility

**Server Won't Start:**
- Check if port 8080 is available
- Ensure Java 11+ is installed
- Verify Maven compilation succeeded

**Upload Issues:**
- Try smaller file sizes
- Check file format (must be ZIP)
- Clear browser cache and retry

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## Dependencies

The project includes the following external dependencies:
- Apache Commons Lang3 (for string utilities)
- Jackson Databind (for JSON processing)
- JUnit (for testing)

## 🔗 Related Tools

- [JDeps Documentation](https://docs.oracle.com/javase/8/docs/technotes/tools/unix/jdeps.html)
- [Maven Documentation](https://maven.apache.org/guides/)

## 📝 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

**Built with ❤️ using Java, Maven, and modern web technologies**
