package com.example.jdeps;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.regex.Pattern;

/**
 * Backend service for processing uploaded ZIP files and generating JDeps reports
 */
public class ZipProjectAnalyzer {

    public ZipProjectAnalyzer() {
        super();
    }
    
    private static final String UPLOAD_DIR = "uploads";
    private static final String REPORTS_DIR = "web-reports";
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB
    
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java ZipProjectAnalyzer <zip-file-path>");
            System.exit(1);
        }
        
        try {
            ZipProjectAnalyzer analyzer = new ZipProjectAnalyzer();
            String reportPath = analyzer.analyzeZipProject(args[0]);
            System.out.println("Analysis complete! Report generated at: " + reportPath);
        } catch (Exception e) {
            System.err.println("Analysis failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public String analyzeZipProject(String zipFilePath) throws IOException {
        System.out.println("Starting analysis of: " + zipFilePath);
        
        // Validate input file
        Path zipPath = Paths.get(zipFilePath);
        if (!Files.exists(zipPath)) {
            throw new IOException("ZIP file not found: " + zipFilePath);
        }
        
        if (Files.size(zipPath) > MAX_FILE_SIZE) {
            throw new IOException("File size exceeds limit of " + (MAX_FILE_SIZE / 1024 / 1024) + "MB");
        }
        
        // Create working directories
        Path uploadDir = createWorkingDirectory(UPLOAD_DIR);
        Path reportsDir = createWorkingDirectory(REPORTS_DIR);
        
        // Extract ZIP file
        String projectName = extractProjectName(zipPath);
        Path extractedDir = uploadDir.resolve(projectName);
        extractZipFile(zipPath, extractedDir);
        
        // Analyze the project
        ProjectAnalysis analysis = analyzeExtractedProject(extractedDir);
        
        // Generate report
        String reportPath = generateWebReport(analysis, reportsDir, projectName);
        
        // Cleanup extracted files
        cleanupDirectory(extractedDir);
        
        return reportPath;
    }
    
    private Path createWorkingDirectory(String dirName) throws IOException {
        Path dir = Paths.get(dirName);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir;
    }
    
    private String extractProjectName(Path zipPath) {
        String fileName = zipPath.getFileName().toString();
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
    }
    
    private void extractZipFile(Path zipPath, Path extractDir) throws IOException {
        System.out.println("Extracting ZIP file to: " + extractDir);
        
        if (Files.exists(extractDir)) {
            cleanupDirectory(extractDir);
        }
        Files.createDirectories(extractDir);
        
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path targetPath = extractDir.resolve(entry.getName());
                
                // Security check: prevent path traversal
                if (!targetPath.normalize().startsWith(extractDir.normalize())) {
                    throw new IOException("Invalid ZIP entry: " + entry.getName());
                }
                
                if (entry.isDirectory()) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createDirectories(targetPath.getParent());
                    Files.copy(zis, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
                zis.closeEntry();
            }
        }
    }
    
    private ProjectAnalysis analyzeExtractedProject(Path projectDir) throws IOException {
        System.out.println("Analyzing extracted project: " + projectDir);
        
        ProjectAnalysis analysis = new ProjectAnalysis();
        analysis.projectPath = projectDir.toString();
        analysis.projectName = projectDir.getFileName().toString();
        
        // Find Java source files
        analysis.javaFiles = findJavaFiles(projectDir);
        System.out.println("Found " + analysis.javaFiles.size() + " Java files");
        
        // Find JAR files if no Java source files found
        if (analysis.javaFiles.isEmpty()) {
            analysis.jarFiles = findJarFiles(projectDir);
            System.out.println("Found " + analysis.jarFiles.size() + " JAR files");
        }
        
        // Detect project structure
        analysis.projectType = detectProjectType(projectDir);
        System.out.println("Detected project type: " + analysis.projectType);
        
        // Compile the project if we have Java files
        if (!analysis.javaFiles.isEmpty()) {
            Path compiledDir = compileProject(projectDir, analysis);
            if (compiledDir != null) {
                analysis.compiledClassesDir = compiledDir.toString();
                
                // Create JAR file
                Path jarFile = createJarFile(compiledDir, projectDir.resolve(analysis.projectName + ".jar"));
                if (jarFile != null) {
                    analysis.jarFile = jarFile.toString();
                    
                    // Run JDeps analysis
                    analysis.jdepsResults = runJDepsAnalysis(jarFile);
                    
                    // Run ASM bytecode analysis for enhanced metrics
                    analysis.asmResults = runASMAnalysis(jarFile);
                    
                    // Generate comprehensive insights
                    if (analysis.asmResults != null && !analysis.asmResults.isEmpty()) {
                        analysis.projectInsights = ASMBytecodeAnalyzer.generateProjectInsights(analysis.asmResults);
                    }
                }
            }
        } else if (analysis.jarFiles != null && !analysis.jarFiles.isEmpty()) {
            // Analyze existing JAR files
            System.out.println("Analyzing existing JAR files...");
            
            // For multiple JARs, we'll analyze the first few and create a combined result
            int maxJarsToAnalyze = Math.min(5, analysis.jarFiles.size()); // Limit to first 5 JARs
            JDepsResults combinedResults = new JDepsResults();
            List<String> basicResults = new ArrayList<>();
            List<String> verboseResults = new ArrayList<>();
            List<String> summaryResults = new ArrayList<>();
            
            for (int i = 0; i < maxJarsToAnalyze; i++) {
                String jarPath = analysis.jarFiles.get(i);
                Path jarFile = Paths.get(jarPath);
                if (Files.exists(jarFile)) {
                    System.out.println("Running JDeps on: " + jarFile.getFileName());
                    JDepsResults result = runJDepsAnalysis(jarFile);
                    if (result != null) {
                        if (result.basicAnalysis != null) {
                            basicResults.add("=== " + jarFile.getFileName() + " ===\n" + result.basicAnalysis);
                        }
                        if (result.verboseAnalysis != null) {
                            verboseResults.add("=== " + jarFile.getFileName() + " ===\n" + result.verboseAnalysis);
                        }
                        if (result.summaryAnalysis != null) {
                            summaryResults.add("=== " + jarFile.getFileName() + " ===\n" + result.summaryAnalysis);
                        }
                    }
                }
            }
            
            combinedResults.basicAnalysis = String.join("\n\n", basicResults);
            combinedResults.verboseAnalysis = String.join("\n\n", verboseResults);
            combinedResults.summaryAnalysis = String.join("\n\n", summaryResults);
            
            analysis.jdepsResults = combinedResults;
            
            // Run ASM analysis on existing JAR files
            if (!analysis.jarFiles.isEmpty()) {
                System.out.println("Running ASM analysis on existing JAR files...");
                Map<String, ASMBytecodeAnalyzer.ClassAnalysisResult> combinedAsmResults = new HashMap<>();
                
                for (int i = 0; i < maxJarsToAnalyze; i++) {
                    String jarPath = analysis.jarFiles.get(i);
                    Path jarFile = Paths.get(jarPath);
                    if (Files.exists(jarFile)) {
                        try {
                            Map<String, ASMBytecodeAnalyzer.ClassAnalysisResult> jarAsmResults = 
                                runASMAnalysis(jarFile);
                            combinedAsmResults.putAll(jarAsmResults);
                        } catch (Exception e) {
                            System.err.println("ASM analysis failed for " + jarFile.getFileName() + ": " + e.getMessage());
                        }
                    }
                }
                
                analysis.asmResults = combinedAsmResults;
                System.out.println("Combined ASM analysis completed for " + combinedAsmResults.size() + " classes");
                
                // Generate comprehensive insights
                if (!combinedAsmResults.isEmpty()) {
                    analysis.projectInsights = ASMBytecodeAnalyzer.generateProjectInsights(combinedAsmResults);
                }
            }
        }
        
        return analysis;
    }
    
    private List<String> findJavaFiles(Path projectDir) throws IOException {
        List<String> javaFiles = new ArrayList<>();
        Pattern javaPattern = Pattern.compile(".*\\.java$");
        
        Files.walk(projectDir)
            .filter(Files::isRegularFile)
            .filter(path -> javaPattern.matcher(path.toString()).matches())
            .forEach(path -> javaFiles.add(path.toAbsolutePath().toString()));
        
        return javaFiles;
    }
    
    private List<String> findJarFiles(Path projectDir) throws IOException {
        List<String> jarFiles = new ArrayList<>();
        Pattern jarPattern = Pattern.compile(".*\\.jar$");
        
        Files.walk(projectDir)
            .filter(Files::isRegularFile)
            .filter(path -> jarPattern.matcher(path.toString()).matches())
            .forEach(path -> jarFiles.add(path.toAbsolutePath().toString()));
        
        return jarFiles;
    }
    
    private String detectProjectType(Path projectDir) throws IOException {
        if (Files.exists(projectDir.resolve("pom.xml"))) {
            return "Maven";
        } else if (Files.exists(projectDir.resolve("build.gradle")) || Files.exists(projectDir.resolve("build.gradle.kts"))) {
            return "Gradle";
        } else if (Files.exists(projectDir.resolve("src"))) {
            return "Standard Java";
        } else {
            return "Unknown";
        }
    }
    
    private Path compileProject(Path projectDir, ProjectAnalysis analysis) {
        try {
            System.out.println("Compiling project...");
            
            Path classesDir = projectDir.resolve("compiled-classes");
            Files.createDirectories(classesDir);
            
            // Simple compilation for standard Java projects
            if (analysis.javaFiles.isEmpty()) {
                System.out.println("No Java files to compile");
                return null;
            }
            
            List<String> command = new ArrayList<>();
            command.add("javac");
            command.add("-d");
            command.add(classesDir.toAbsolutePath().toString());
            
            // Add all Java files with absolute paths
            for (String javaFile : analysis.javaFiles) {
                // Files are already absolute paths from findJavaFiles
                command.add(javaFile);
            }
            
            System.out.println("Compilation command: " + String.join(" ", command));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(System.getProperty("user.dir"))); // Use current working directory
            pb.redirectErrorStream(true); // Combine stdout and stderr
            
            Process process = pb.start();
            
            // Capture output for debugging
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("Compilation output: " + line);
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Compilation successful");
                return classesDir;
            } else {
                System.err.println("Compilation failed with exit code: " + exitCode);
                // Read error output
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println(line);
                    }
                }
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("Compilation error: " + e.getMessage());
            return null;
        }
    }
    
    private Path createJarFile(Path classesDir, Path jarPath) {
        try {
            System.out.println("Creating JAR file: " + jarPath);
            
            List<String> command = Arrays.asList(
                "jar", "cf", jarPath.toString(), "-C", classesDir.toString(), "."
            );
            
            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("JAR creation successful");
                return jarPath;
            } else {
                System.err.println("JAR creation failed with exit code: " + exitCode);
                return null;
            }
            
        } catch (Exception e) {
            System.err.println("JAR creation error: " + e.getMessage());
            return null;
        }
    }
    
    private JDepsResults runJDepsAnalysis(Path jarFile) {
        System.out.println("Running JDeps analysis on: " + jarFile);
        
        JDepsResults results = new JDepsResults();
        
        try {
            // Basic analysis
            results.basicAnalysis = runJDepsCommand(jarFile.toString());
            
            // Verbose analysis
            results.verboseAnalysis = runJDepsCommand(jarFile.toString(), "-verbose:class");
            
            // Summary analysis
            results.summaryAnalysis = runJDepsCommand(jarFile.toString(), "-s");
            
            System.out.println("JDeps analysis completed");
            
        } catch (Exception e) {
            System.err.println("JDeps analysis error: " + e.getMessage());
        }
        
        return results;
    }
    
    private String runJDepsCommand(String jarPath, String... options) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("jdeps");
        command.addAll(Arrays.asList(options));
        command.add(jarPath);
        
        ProcessBuilder pb = new ProcessBuilder(command);
        Process process = pb.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new IOException("JDeps command failed with exit code: " + exitCode);
        }
        
        return output.toString();
    }
    
    /**
     * Run ASM bytecode analysis for enhanced metrics and insights
     */
    private Map<String, ASMBytecodeAnalyzer.ClassAnalysisResult> runASMAnalysis(Path jarFile) {
        System.out.println("Running ASM bytecode analysis on: " + jarFile);
        
        try {
            Map<String, ASMBytecodeAnalyzer.ClassAnalysisResult> results = 
                ASMBytecodeAnalyzer.analyzeJarFile(jarFile.toString());
            
            System.out.println("ASM analysis completed. Analyzed " + results.size() + " classes");
            return results;
            
        } catch (Exception e) {
            System.err.println("ASM analysis error: " + e.getMessage());
            e.printStackTrace();
            return new HashMap<>();
        }
    }
    
    private String generateWebReport(ProjectAnalysis analysis, Path reportsDir, String projectName) throws IOException {
        System.out.println("Generating web report...");
        
        Path reportDir = reportsDir.resolve(projectName + "-" + System.currentTimeMillis());
        Files.createDirectories(reportDir);
        
        // Generate HTML report
        String htmlContent = generateAdvancedHtmlReport(analysis);
        Path htmlFile = reportDir.resolve("index.html");
        Files.write(htmlFile, htmlContent.getBytes());
        
        // Generate JSON data file
        String jsonData = generateJsonReport(analysis);
        Path jsonFile = reportDir.resolve("data.json");
        Files.write(jsonFile, jsonData.getBytes());
        
        System.out.println("Report generated at: " + reportDir.toAbsolutePath());
        return reportDir.toAbsolutePath().toString();
    }
    
    private String generateAdvancedHtmlReport(ProjectAnalysis analysis) {
        StringBuilder html = new StringBuilder();
        
        html.append("<!DOCTYPE html>\n")
            .append("<html lang=\"en\">\n")
            .append("<head>\n")
            .append("    <meta charset=\"UTF-8\">\n")
            .append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n")
            .append("    <title>JDeps Analysis - ").append(analysis.projectName).append("</title>\n")
            .append("    <style>\n")
            .append(getReportStyles())
            .append("    </style>\n")
            .append("</head>\n")
            .append("<body>\n")
            .append("    <div class=\"container\">\n")
            .append("        <div class=\"header\">\n")
            .append("            <h1>📊 ").append(analysis.projectName).append(" - Dependency Analysis</h1>\n")
            .append("            <p>Generated on ").append(new Date()).append("</p>\n")
            .append("        </div>\n")
            .append("        \n")
            .append("        <div class=\"project-info\">\n")
            .append("            <h2>Project Information</h2>\n")
            .append("            <div class=\"info-grid\">\n")
            .append("                <div class=\"info-item\">\n")
            .append("                    <strong>Project Type:</strong> ").append(analysis.projectType).append("\n")
            .append("                </div>\n")
            .append("                <div class=\"info-item\">\n")
            .append("                    <strong>Java Files:</strong> ").append(analysis.javaFiles.size()).append("\n")
            .append("                </div>\n")
            .append("                <div class=\"info-item\">\n")
            .append("                    <strong>JAR File:</strong> ").append(analysis.jarFile != null ? "✅ Created" : "❌ Failed").append("\n")
            .append("                </div>\n")
            .append("                <div class=\"info-item\">\n")
            .append("                    <strong>Analysis Status:</strong> ").append(analysis.jdepsResults != null ? "✅ Complete" : "❌ Failed").append("\n")
            .append("                </div>\n")
            .append("            </div>\n")
            .append("        </div>\n");
        
        if (analysis.jdepsResults != null) {
            html.append("        <div class=\"analysis-section\">\n")
                .append("            <div class=\"tab-container\">\n")
                .append("                <div class=\"tab-buttons\">\n")
                .append("                    <button class=\"tab-button active\" onclick=\"showTab('overview')\">📊 Overview</button>\n")
                .append("                    <button class=\"tab-button\" onclick=\"showTab('dependencies')\">🔗 Dependencies</button>\n")
                .append("                    <button class=\"tab-button\" onclick=\"showTab('quality')\">⭐ Quality</button>\n")
                .append("                    <button class=\"tab-button\" onclick=\"showTab('security')\">🔒 Security</button>\n")
                .append("                    <button class=\"tab-button\" onclick=\"showTab('impact')\">💥 Impact Analysis</button>\n")
                .append("                    <button class=\"tab-button\" onclick=\"showTab('asm')\">🔍 Technical Details</button>\n")
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"overview\" class=\"tab-content active\">\n")
                .append(generateOverviewSection(analysis))
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"dependencies\" class=\"tab-content\">\n")
                .append(generateDependenciesSection(analysis))
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"quality\" class=\"tab-content\">\n")
                .append(generateQualitySection(analysis))
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"security\" class=\"tab-content\">\n")
                .append(generateSecuritySection(analysis))
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"impact\" class=\"tab-content\">\n")
                .append(generateImpactSection(analysis))
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"asm\" class=\"tab-content\">\n")
                .append("                    <h3>🔍 Technical Bytecode Analysis</h3>\n")
                .append(generateASMAnalysisSection(analysis.asmResults))
                .append("                </div>\n")
                .append("            </div>\n")
                .append("        </div>\n");
        } else {
            html.append("        <div class=\"error-section\">\n")
                .append("            <h3>⚠️ Analysis Failed</h3>\n")
                .append("            <p>The JDeps analysis could not be completed. This might be due to:</p>\n")
                .append("            <ul>\n")
                .append("                <li>Compilation errors in the Java code</li>\n")
                .append("                <li>Missing dependencies</li>\n")
                .append("                <li>Unsupported project structure</li>\n")
                .append("            </ul>\n")
                .append("        </div>\n");
        }
        
        html.append("    </div>\n")
            .append("    \n")
            .append("    <script>\n")
            .append("        function showTab(tabName) {\n")
            .append("            document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));\n")
            .append("            document.querySelectorAll('.tab-button').forEach(button => button.classList.remove('active'));\n")
            .append("            document.getElementById(tabName).classList.add('active');\n")
            .append("            event.target.classList.add('active');\n")
            .append("        }\n")
            .append("        \n")
            .append("        function showMiniTab(tabName) {\n")
            .append("            document.querySelectorAll('.tab-content.mini').forEach(tab => tab.classList.remove('active'));\n")
            .append("            document.querySelectorAll('.tab-button.mini').forEach(button => button.classList.remove('active'));\n")
            .append("            document.getElementById(tabName + '-mini').classList.add('active');\n")
            .append("            event.target.classList.add('active');\n")
            .append("        }\n")
            .append("        \n")
            .append("        function filterImpactTable() {\n")
            .append("            const searchValue = document.getElementById('impactSearch').value.toLowerCase();\n")
            .append("            const riskFilter = document.getElementById('riskFilter').value;\n")
            .append("            const table = document.getElementById('impactTable');\n")
            .append("            const rows = table.getElementsByTagName('tr');\n")
            .append("            \n")
            .append("            for (let i = 1; i < rows.length; i++) {\n")
            .append("                const row = rows[i];\n")
            .append("                const className = row.cells[0].textContent.toLowerCase();\n")
            .append("                const riskScore = parseInt(row.cells[1].textContent);\n")
            .append("                \n")
            .append("                let showRow = className.includes(searchValue);\n")
            .append("                \n")
            .append("                if (riskFilter !== 'all') {\n")
            .append("                    if (riskFilter === 'critical' && riskScore < 50) showRow = false;\n")
            .append("                    if (riskFilter === 'moderate' && (riskScore < 20 || riskScore >= 50)) showRow = false;\n")
            .append("                    if (riskFilter === 'low' && riskScore >= 20) showRow = false;\n")
            .append("                }\n")
            .append("                \n")
            .append("                row.style.display = showRow ? '' : 'none';\n")
            .append("            }\n")
            .append("        }\n")
            .append("        \n")
            .append("        // Initialize tooltips and interactive elements\n")
            .append("        document.addEventListener('DOMContentLoaded', function() {\n")
            .append("            // Add click handlers for expandable sections\n")
            .append("            document.querySelectorAll('.critical-class-card').forEach(card => {\n")
            .append("                card.addEventListener('click', function() {\n")
            .append("                    const details = this.querySelector('.impact-details');\n")
            .append("                    details.style.display = details.style.display === 'none' ? 'block' : 'none';\n")
            .append("                });\n")
            .append("            });\n")
            .append("        });\n")
            .append("    </script>\n")
            .append("</body>\n")
            .append("</html>");
        
        return html.toString();
    }
    
    private String getReportStyles() {
        return "body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; }\n" +
               ".container { max-width: 1400px; margin: 0 auto; background: rgba(255,255,255,0.95); border-radius: 15px; padding: 30px; }\n" +
               ".header { text-align: center; margin-bottom: 40px; color: #2c3e50; }\n" +
               ".header h1 { font-size: 2.5em; margin-bottom: 10px; }\n" +
               ".project-info { background: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 30px; }\n" +
               ".info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }\n" +
               ".info-item { padding: 10px; background: white; border-radius: 5px; }\n" +
               ".tab-container { background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }\n" +
               ".tab-buttons { display: flex; background: #34495e; }\n" +
               ".tab-button { flex: 1; padding: 15px; background: none; border: none; color: white; cursor: pointer; font-size: 14px; }\n" +
               ".tab-button:hover { background: rgba(255,255,255,0.1); }\n" +
               ".tab-button.active { background: #3498db; }\n" +
               ".tab-content { display: none; padding: 30px; min-height: 400px; }\n" +
               ".tab-content.active { display: block; }\n" +
               
               // Executive Summary Styles
               ".exec-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin: 20px 0; }\n" +
               ".summary-card { background: linear-gradient(135deg, #f8f9fa, #e9ecef); padding: 20px; border-radius: 12px; text-align: center; box-shadow: 0 4px 6px rgba(0,0,0,0.1); }\n" +
               ".card-header { font-size: 1.1em; font-weight: bold; margin-bottom: 15px; color: #2c3e50; }\n" +
               ".score-circle { width: 80px; height: 80px; border-radius: 50%; margin: 10px auto; display: flex; align-items: center; justify-content: center; }\n" +
               ".score-circle.excellent { background: linear-gradient(135deg, #2ecc71, #27ae60); }\n" +
               ".score-circle.good { background: linear-gradient(135deg, #f39c12, #e67e22); }\n" +
               ".score-circle.fair { background: linear-gradient(135deg, #f39c12, #d35400); }\n" +
               ".score-circle.poor { background: linear-gradient(135deg, #e74c3c, #c0392b); }\n" +
               ".score { color: white; font-size: 1.2em; font-weight: bold; }\n" +
               ".card-details { font-size: 0.9em; color: #666; }\n" +
               ".metric-value { font-size: 2em; font-weight: bold; color: #3498db; }\n" +
               ".metric-label { font-size: 0.9em; color: #666; margin-top: 5px; }\n" +
               
               // Insights Styles
               ".insights-section { margin: 30px 0; }\n" +
               ".insights-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(350px, 1fr)); gap: 20px; }\n" +
               ".insight-item { display: flex; padding: 15px; border-radius: 8px; border-left: 4px solid; }\n" +
               ".insight-item.warning { background: #fff3cd; border-color: #f0ad4e; }\n" +
               ".insight-item.security { background: #f8d7da; border-color: #dc3545; }\n" +
               ".insight-item.info { background: #d1ecf1; border-color: #17a2b8; }\n" +
               ".insight-item.success { background: #d4edda; border-color: #28a745; }\n" +
               ".insight-icon { font-size: 1.5em; margin-right: 15px; }\n" +
               ".insight-content h5 { margin: 0 0 5px 0; color: #2c3e50; }\n" +
               ".insight-content p { margin: 0; color: #666; }\n" +
               
               // Dependencies Styles
               ".dependencies-overview { margin: 20px 0; }\n" +
               ".package-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; }\n" +
               ".package-card { background: #f8f9fa; padding: 15px; border-radius: 8px; border-left: 4px solid #3498db; }\n" +
               ".package-card h5 { margin: 0 0 10px 0; color: #2c3e50; }\n" +
               ".package-stats { display: flex; gap: 15px; margin-bottom: 10px; }\n" +
               ".stat { background: #e9ecef; padding: 4px 8px; border-radius: 4px; font-size: 0.85em; }\n" +
               ".dependencies-list { max-height: 100px; overflow-y: auto; }\n" +
               ".dep-item { padding: 2px 0; font-size: 0.85em; color: #666; }\n" +
               ".dep-item.more { font-style: italic; color: #999; }\n" +
               ".dep-item.none { color: #28a745; }\n" +
               
               // Quality Styles
               ".quality-dashboard { margin: 20px 0; }\n" +
               ".quality-metrics { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }\n" +
               ".metric-card { background: white; padding: 20px; border-radius: 8px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
               ".metric-number { font-size: 2em; font-weight: bold; color: #3498db; }\n" +
               ".metric-bar { height: 6px; background: #e9ecef; border-radius: 3px; margin-top: 10px; overflow: hidden; }\n" +
               ".metric-fill { height: 100%; background: #f39c12; transition: width 0.3s ease; }\n" +
               ".metric-fill.success { background: #28a745; }\n" +
               ".metric-indicator { font-size: 1.5em; margin-top: 10px; }\n" +
               ".metric-indicator.good { color: #28a745; }\n" +
               ".complexity-section, .refactoring-section, .patterns-section { margin: 30px 0; }\n" +
               ".complexity-list, .refactoring-list { display: flex; flex-direction: column; gap: 10px; }\n" +
               ".complexity-item, .refactoring-item { background: #fff3cd; padding: 15px; border-radius: 8px; border-left: 4px solid #f0ad4e; }\n" +
               ".patterns-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }\n" +
               ".pattern-card { background: #d4edda; padding: 15px; border-radius: 8px; text-align: center; }\n" +
               ".pattern-name { font-weight: bold; color: #155724; }\n" +
               ".pattern-count { color: #666; font-size: 0.9em; }\n" +
               
               // Security Styles
               ".security-score-section { display: flex; align-items: center; gap: 30px; margin: 20px 0; }\n" +
               ".security-score { width: 120px; height: 120px; border-radius: 50%; display: flex; flex-direction: column; align-items: center; justify-content: center; color: white; }\n" +
               ".score-value { font-size: 2em; font-weight: bold; }\n" +
               ".score-label { font-size: 0.9em; }\n" +
               ".security-breakdown { flex: 1; }\n" +
               ".breakdown-item { padding: 8px 0; border-bottom: 1px solid #eee; }\n" +
               ".security-issues { margin: 30px 0; }\n" +
               ".issue-list { display: flex; flex-direction: column; gap: 15px; }\n" +
               ".issue-item { display: flex; padding: 15px; background: #fff3cd; border-radius: 8px; border-left: 4px solid #f0ad4e; }\n" +
               ".issue-icon { font-size: 1.5em; margin-right: 15px; }\n" +
               ".issue-details { flex: 1; }\n" +
               ".issue-description { font-weight: bold; margin-bottom: 5px; }\n" +
               ".issue-recommendation { color: #666; font-size: 0.9em; }\n" +
               ".security-recommendations { margin: 30px 0; }\n" +
               ".recommendations-list { display: flex; flex-direction: column; gap: 10px; }\n" +
               ".recommendation-item { display: flex; align-items: center; padding: 15px; background: #f8f9fa; border-radius: 8px; }\n" +
               ".priority-badge { padding: 4px 8px; border-radius: 4px; font-size: 0.8em; font-weight: bold; margin-right: 15px; }\n" +
               ".priority-badge.high { background: #dc3545; color: white; }\n" +
               ".priority-badge.medium { background: #ffc107; color: black; }\n" +
               ".priority-badge.low { background: #28a745; color: white; }\n" +
               
               // Impact Analysis Styles
               ".impact-overview { margin: 20px 0; }\n" +
               ".impact-summary { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 20px; }\n" +
               ".impact-metric { background: white; padding: 20px; border-radius: 8px; text-align: center; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
               ".impact-metric.critical { border-left: 4px solid #dc3545; }\n" +
               ".impact-metric.moderate { border-left: 4px solid #ffc107; }\n" +
               ".impact-metric.low { border-left: 4px solid #28a745; }\n" +
               ".metric-desc { font-size: 0.8em; color: #666; margin-top: 5px; }\n" +
               ".critical-classes-section { margin: 30px 0; }\n" +
               ".critical-classes-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 20px; }\n" +
               ".critical-class-card { background: #f8d7da; padding: 20px; border-radius: 8px; border-left: 4px solid #dc3545; }\n" +
               ".class-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }\n" +
               ".class-name { font-weight: bold; color: #2c3e50; word-break: break-all; }\n" +
               ".risk-score.critical { background: #dc3545; color: white; padding: 4px 8px; border-radius: 4px; font-size: 0.8em; }\n" +
               ".affected-count { font-weight: bold; margin-bottom: 10px; }\n" +
               ".affected-preview { margin-bottom: 10px; }\n" +
               ".affected-item { font-size: 0.9em; color: #666; padding: 2px 0; }\n" +
               ".affected-more { font-style: italic; color: #999; }\n" +
               ".removal-warning { background: #721c24; color: white; padding: 8px; border-radius: 4px; font-size: 0.9em; }\n" +
               ".impact-matrix-section { margin: 30px 0; }\n" +
               ".matrix-controls { display: flex; gap: 15px; margin-bottom: 20px; }\n" +
               ".matrix-controls input, .matrix-controls select { padding: 8px; border: 1px solid #ddd; border-radius: 4px; }\n" +
               ".impact-table { width: 100%; border-collapse: collapse; min-width: 800px; }\n" +
               ".impact-table th, .impact-table td { border: 1px solid #ddd; padding: 12px 8px; text-align: left; }\n" +
               ".impact-table th { background: #f8f9fa; font-weight: bold; position: sticky; top: 0; }\n" +
               ".impact-table tr.risk-critical { background: #f8d7da; }\n" +
               ".impact-table tr.risk-moderate { background: #fff3cd; }\n" +
               ".impact-table tr.risk-low { background: #d4edda; }\n" +
               ".risk-badge { padding: 4px 8px; border-radius: 4px; font-size: 0.8em; font-weight: bold; }\n" +
               ".risk-badge.critical { background: #dc3545; color: white; }\n" +
               ".risk-badge.moderate { background: #ffc107; color: black; }\n" +
               ".risk-badge.low { background: #28a745; color: white; }\n" +
               ".jar-simulation-section { margin: 30px 0; background: #e3f2fd; padding: 20px; border-radius: 8px; }\n" +
               ".simulation-explanation { color: #1565c0; }\n" +
               
               // Mini tabs for JDeps output
               ".tab-container.mini { margin: 20px 0; }\n" +
               ".tab-buttons.mini { background: #6c757d; }\n" +
               ".tab-button.mini { padding: 10px; font-size: 12px; }\n" +
               ".tab-content.mini { padding: 15px; }\n" +
               ".analysis-output.mini { max-height: 200px; font-size: 0.85em; }\n" +
               
               // Common styles
               ".analysis-output { background: #2c3e50; color: #ecf0f1; padding: 20px; border-radius: 5px; font-family: monospace; white-space: pre-wrap; max-height: 400px; overflow-y: auto; }\n" +
               ".error-section { background: #fee; border: 1px solid #fcc; padding: 20px; border-radius: 10px; color: #d00; }\n" +
               ".no-insights, .no-data { text-align: center; padding: 40px; color: #666; font-style: italic; }\n" +
               ".table-container { overflow-x: auto; margin: 20px 0; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }\n" +
               ".class-table { width: 100%; border-collapse: collapse; min-width: 800px; background: white; }\n" +
               ".class-table th, .class-table td { border: 1px solid #ddd; padding: 12px 8px; text-align: left; white-space: nowrap; }\n" +
               ".class-table th { background: #f8f9fa; font-weight: bold; position: sticky; top: 0; z-index: 10; }\n" +
               ".class-table td.class-name { max-width: 300px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }\n" +
               ".complexity-low { background: #d4edda; }\n" +
               ".complexity-medium { background: #fff3cd; }\n" +
               ".complexity-high { background: #f8d7da; }\n" +
               "details { margin: 20px 0; }\n" +
               "summary { cursor: pointer; padding: 10px; background: #f8f9fa; border-radius: 4px; }\n" +
               "@media (max-width: 768px) { .container { padding: 15px; margin: 10px; } .exec-summary, .quality-metrics, .package-grid { grid-template-columns: 1fr; } .critical-classes-grid { grid-template-columns: 1fr; } .matrix-controls { flex-direction: column; } }";
    }
    
    /**
     * Generate ASM analysis section for the HTML report
     */
    private String generateASMAnalysisSection(Map<String, ASMBytecodeAnalyzer.ClassAnalysisResult> asmResults) {
        if (asmResults == null || asmResults.isEmpty()) {
            return "<p>ASM analysis not available or failed.</p>";
        }
        
        StringBuilder section = new StringBuilder();
        
        // Calculate summary statistics
        int totalClasses = asmResults.size();
        int totalMethods = asmResults.values().stream().mapToInt(ASMBytecodeAnalyzer.ClassAnalysisResult::getTotalMethods).sum();
        int totalFields = asmResults.values().stream().mapToInt(ASMBytecodeAnalyzer.ClassAnalysisResult::getTotalFields).sum();
        double avgComplexity = asmResults.values().stream()
                .mapToDouble(ASMBytecodeAnalyzer.ClassAnalysisResult::getAverageMethodComplexity)
                .average().orElse(0.0);
        int totalDependencies = asmResults.values().stream()
                .mapToInt(r -> r.getMethodCalls().size() + r.getFieldAccess().size()).sum();
        
        // Summary statistics
        section.append("<div class=\"asm-stats\">\n")
               .append("  <div class=\"stat-card\">\n")
               .append("    <span class=\"stat-number\">").append(totalClasses).append("</span>\n")
               .append("    <span class=\"stat-label\">Classes Analyzed</span>\n")
               .append("  </div>\n")
               .append("  <div class=\"stat-card\">\n")
               .append("    <span class=\"stat-number\">").append(totalMethods).append("</span>\n")
               .append("    <span class=\"stat-label\">Total Methods</span>\n")
               .append("  </div>\n")
               .append("  <div class=\"stat-card\">\n")
               .append("    <span class=\"stat-number\">").append(totalFields).append("</span>\n")
               .append("    <span class=\"stat-label\">Total Fields</span>\n")
               .append("  </div>\n")
               .append("  <div class=\"stat-card\">\n")
               .append("    <span class=\"stat-number\">").append(String.format("%.1f", avgComplexity)).append("</span>\n")
               .append("    <span class=\"stat-label\">Avg Complexity</span>\n")
               .append("  </div>\n")
               .append("  <div class=\"stat-card\">\n")
               .append("    <span class=\"stat-number\">").append(totalDependencies).append("</span>\n")
               .append("    <span class=\"stat-label\">Dependencies</span>\n")
               .append("  </div>\n")
               .append("</div>\n");
        
        // Detailed class table
        section.append("<h4>📋 Class Details</h4>\n")
               .append("<div class=\"table-container\">\n")
               .append("<table class=\"class-table\">\n")
               .append("  <thead>\n")
               .append("    <tr>\n")
               .append("      <th>Class Name</th>\n")
               .append("      <th>Type</th>\n")
               .append("      <th>Methods</th>\n")
               .append("      <th>Fields</th>\n")
               .append("      <th>Avg Complexity</th>\n")
               .append("      <th>Dependencies</th>\n")
               .append("    </tr>\n")
               .append("  </thead>\n")
               .append("  <tbody>\n");
        
        for (ASMBytecodeAnalyzer.ClassAnalysisResult result : asmResults.values()) {
            String complexityClass = getComplexityClass(result.getAverageMethodComplexity());
            String classType = result.isInterface() ? "Interface" : 
                              result.isAbstract() ? "Abstract" : "Class";
            
            // Truncate long class names for better display
            String displayClassName = result.getClassName().replace("/", ".");
            if (displayClassName.length() > 50) {
                displayClassName = "..." + displayClassName.substring(displayClassName.length() - 47);
            }
            
            section.append("    <tr class=\"").append(complexityClass).append("\">\n")
                   .append("      <td class=\"class-name\" title=\"").append(result.getClassName().replace("/", ".")).append("\">").append(displayClassName).append("</td>\n")
                   .append("      <td>").append(classType).append("</td>\n")
                   .append("      <td>").append(result.getTotalMethods()).append("</td>\n")
                   .append("      <td>").append(result.getTotalFields()).append("</td>\n")
                   .append("      <td>").append(String.format("%.2f", result.getAverageMethodComplexity())).append("</td>\n")
                   .append("      <td>").append(result.getMethodCalls().size() + result.getFieldAccess().size()).append("</td>\n")
                   .append("    </tr>\n");
        }
        
        section.append("  </tbody>\n")
               .append("</table>\n")
               .append("</div>\n");
        
        return section.toString();
    }
    
    /**
     * Generate comprehensive overview section
     */
    private String generateOverviewSection(ProjectAnalysis analysis) {
        StringBuilder section = new StringBuilder();
        
        section.append("<h3>📊 Project Overview & Executive Summary</h3>\n");
        
        if (analysis.projectInsights != null) {
            ASMBytecodeAnalyzer.ProjectInsights insights = analysis.projectInsights;
            ASMBytecodeAnalyzer.QualityInsights quality = insights.getQualityInsights();
            ASMBytecodeAnalyzer.SecurityInsights security = insights.getSecurityInsights();
            ASMBytecodeAnalyzer.PackageInsights packages = insights.getPackageInsights();
            
            // Executive summary cards
            section.append("<div class=\"exec-summary\">\n")
                   .append("  <div class=\"summary-card quality-card\">\n")
                   .append("    <div class=\"card-header\">Code Quality</div>\n")
                   .append("    <div class=\"score-circle " + getScoreClass(quality.getMaintainabilityScore()) + "\">\n")
                   .append("      <span class=\"score\">").append(quality.getMaintainabilityScore()).append("%</span>\n")
                   .append("    </div>\n")
                   .append("    <div class=\"card-details\">\n")
                   .append("      <p>Complexity: ").append(String.format("%.2f", quality.getOverallComplexity())).append("</p>\n")
                   .append("      <p>High complexity classes: ").append(quality.getHighComplexityClasses().size()).append("</p>\n")
                   .append("    </div>\n")
                   .append("  </div>\n")
                   .append("  \n")
                   .append("  <div class=\"summary-card security-card\">\n")
                   .append("    <div class=\"card-header\">Security Score</div>\n")
                   .append("    <div class=\"score-circle " + getScoreClass(security.getSecurityScore()) + "\">\n")
                   .append("      <span class=\"score\">").append(security.getSecurityScore()).append("%</span>\n")
                   .append("    </div>\n")
                   .append("    <div class=\"card-details\">\n")
                   .append("      <p>Security issues: ").append(getTotalSecurityIssues(security)).append("</p>\n")
                   .append("      <p>Reflection usage: ").append(security.getReflectionUsage().size()).append("</p>\n")
                   .append("    </div>\n")
                   .append("  </div>\n")
                   .append("  \n")
                   .append("  <div class=\"summary-card architecture-card\">\n")
                   .append("    <div class=\"card-header\">Architecture</div>\n")
                   .append("    <div class=\"metric-value\">").append(packages.getPackageClassCount().size()).append("</div>\n")
                   .append("    <div class=\"metric-label\">Packages</div>\n")
                   .append("    <div class=\"card-details\">\n")
                   .append("      <p>Classes: ").append(analysis.asmResults.size()).append("</p>\n")
                   .append("      <p>Design patterns: ").append(quality.getDesignPatterns().size()).append("</p>\n")
                   .append("    </div>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
            
            // Key insights
            section.append("<div class=\"insights-section\">\n")
                   .append("  <h4>🎯 Key Insights & Recommendations</h4>\n")
                   .append("  <div class=\"insights-grid\">\n");
            
            // Quality insights
            if (!quality.getHighComplexityClasses().isEmpty()) {
                section.append("    <div class=\"insight-item warning\">\n")
                       .append("      <div class=\"insight-icon\">⚠️</div>\n")
                       .append("      <div class=\"insight-content\">\n")
                       .append("        <h5>High Complexity Detected</h5>\n")
                       .append("        <p>").append(quality.getHighComplexityClasses().size())
                       .append(" classes have high complexity. Consider refactoring for better maintainability.</p>\n")
                       .append("      </div>\n")
                       .append("    </div>\n");
            }
            
            // Security insights
            if (!security.getReflectionUsage().isEmpty()) {
                section.append("    <div class=\"insight-item security\">\n")
                       .append("      <div class=\"insight-icon\">🔒</div>\n")
                       .append("      <div class=\"insight-content\">\n")
                       .append("        <h5>Reflection Usage Found</h5>\n")
                       .append("        <p>").append(security.getReflectionUsage().size())
                       .append(" instances of reflection usage detected. Review for security implications.</p>\n")
                       .append("      </div>\n")
                       .append("    </div>\n");
            }
            
            // Architecture insights
            if (!packages.getMostConnectedPackages().isEmpty()) {
                section.append("    <div class=\"insight-item info\">\n")
                       .append("      <div class=\"insight-icon\">🏗️</div>\n")
                       .append("      <div class=\"insight-content\">\n")
                       .append("        <h5>Highly Connected Packages</h5>\n")
                       .append("        <p>Package '").append(packages.getMostConnectedPackages().get(0))
                       .append("' has the most dependencies. Consider reviewing its responsibilities.</p>\n")
                       .append("      </div>\n")
                       .append("    </div>\n");
            }
            
            // Design patterns insights
            if (!quality.getDesignPatterns().isEmpty()) {
                section.append("    <div class=\"insight-item success\">\n")
                       .append("      <div class=\"insight-icon\">✅</div>\n")
                       .append("      <div class=\"insight-content\">\n")
                       .append("        <h5>Design Patterns Identified</h5>\n")
                       .append("        <p>Found evidence of ")
                       .append(String.join(", ", quality.getDesignPatterns().keySet()))
                       .append(" patterns in your codebase.</p>\n")
                       .append("      </div>\n")
                       .append("    </div>\n");
            }
            
            section.append("  </div>\n")
                   .append("</div>\n");
        } else {
            section.append("<p class=\"no-insights\">📈 Enhanced insights will be available once ASM analysis completes successfully.</p>\n");
        }
        
        return section.toString();
    }
    
    /**
     * Generate dependencies analysis section
     */
    private String generateDependenciesSection(ProjectAnalysis analysis) {
        StringBuilder section = new StringBuilder();
        
        section.append("<h3>🔗 Dependency Analysis</h3>\n");
        
        if (analysis.projectInsights != null) {
            ASMBytecodeAnalyzer.PackageInsights packages = analysis.projectInsights.getPackageInsights();
            
            // Package dependency visualization
            section.append("<div class=\"dependencies-overview\">\n")
                   .append("  <h4>📦 Package Dependencies</h4>\n")
                   .append("  <div class=\"package-grid\">\n");
            
            for (Map.Entry<String, Set<String>> entry : packages.getPackageDependencies().entrySet()) {
                String packageName = entry.getKey();
                Set<String> deps = entry.getValue();
                int classCount = packages.getPackageClassCount().getOrDefault(packageName, 0);
                
                section.append("    <div class=\"package-card\">\n")
                       .append("      <h5>").append(packageName).append("</h5>\n")
                       .append("      <div class=\"package-stats\">\n")
                       .append("        <span class=\"stat\">").append(classCount).append(" classes</span>\n")
                       .append("        <span class=\"stat\">").append(deps.size()).append(" dependencies</span>\n")
                       .append("      </div>\n")
                       .append("      <div class=\"dependencies-list\">\n");
                
                if (!deps.isEmpty()) {
                    for (String dep : deps.stream().limit(3).toArray(String[]::new)) {
                        section.append("        <div class=\"dep-item\">→ ").append(dep).append("</div>\n");
                    }
                    if (deps.size() > 3) {
                        section.append("        <div class=\"dep-item more\">... and ").append(deps.size() - 3).append(" more</div>\n");
                    }
                } else {
                    section.append("        <div class=\"dep-item none\">No external dependencies</div>\n");
                }
                
                section.append("      </div>\n")
                       .append("    </div>\n");
            }
            
            section.append("  </div>\n")
                   .append("</div>\n");
        }
        
        // JDeps raw output (collapsed by default)
        if (analysis.jdepsResults != null) {
            section.append("<div class=\"jdeps-section\">\n")
                   .append("  <details>\n")
                   .append("    <summary><h4>🔧 Raw JDeps Analysis Output</h4></summary>\n")
                   .append("    <div class=\"tab-container mini\">\n")
                   .append("      <div class=\"tab-buttons mini\">\n")
                   .append("        <button class=\"tab-button mini active\" onclick=\"showMiniTab('basic')\">Basic</button>\n")
                   .append("        <button class=\"tab-button mini\" onclick=\"showMiniTab('verbose')\">Verbose</button>\n")
                   .append("        <button class=\"tab-button mini\" onclick=\"showMiniTab('summary')\">Summary</button>\n")
                   .append("      </div>\n")
                   .append("      <div id=\"basic-mini\" class=\"tab-content mini active\">\n")
                   .append("        <div class=\"analysis-output mini\">\n")
                   .append(formatAnalysisOutput(analysis.jdepsResults.basicAnalysis))
                   .append("        </div>\n")
                   .append("      </div>\n")
                   .append("      <div id=\"verbose-mini\" class=\"tab-content mini\">\n")
                   .append("        <div class=\"analysis-output mini\">\n")
                   .append(formatAnalysisOutput(analysis.jdepsResults.verboseAnalysis))
                   .append("        </div>\n")
                   .append("      </div>\n")
                   .append("      <div id=\"summary-mini\" class=\"tab-content mini\">\n")
                   .append("        <div class=\"analysis-output mini\">\n")
                   .append(formatAnalysisOutput(analysis.jdepsResults.summaryAnalysis))
                   .append("        </div>\n")
                   .append("      </div>\n")
                   .append("    </div>\n")
                   .append("  </details>\n")
                   .append("</div>\n");
        }
        
        return section.toString();
    }
    
    /**
     * Generate quality analysis section
     */
    private String generateQualitySection(ProjectAnalysis analysis) {
        StringBuilder section = new StringBuilder();
        
        section.append("<h3>⭐ Code Quality Analysis</h3>\n");
        
        if (analysis.projectInsights != null) {
            ASMBytecodeAnalyzer.QualityInsights quality = analysis.projectInsights.getQualityInsights();
            
            // Quality metrics dashboard
            section.append("<div class=\"quality-dashboard\">\n")
                   .append("  <div class=\"quality-metrics\">\n")
                   .append("    <div class=\"metric-card\">\n")
                   .append("      <div class=\"metric-number\">").append(String.format("%.2f", quality.getOverallComplexity())).append("</div>\n")
                   .append("      <div class=\"metric-label\">Overall Complexity</div>\n")
                   .append("      <div class=\"metric-bar\"><div class=\"metric-fill\" style=\"width: ").append(Math.min(100, quality.getOverallComplexity() * 5)).append("%\"></div></div>\n")
                   .append("    </div>\n")
                   .append("    <div class=\"metric-card\">\n")
                   .append("      <div class=\"metric-number\">").append(quality.getMaintainabilityScore()).append("%</div>\n")
                   .append("      <div class=\"metric-label\">Maintainability</div>\n")
                   .append("      <div class=\"metric-bar\"><div class=\"metric-fill success\" style=\"width: ").append(quality.getMaintainabilityScore()).append("%\"></div></div>\n")
                   .append("    </div>\n")
                   .append("    <div class=\"metric-card\">\n")
                   .append("      <div class=\"metric-number\">").append(quality.getDesignPatterns().size()).append("</div>\n")
                   .append("      <div class=\"metric-label\">Design Patterns</div>\n")
                   .append("      <div class=\"metric-indicator good\">✓</div>\n")
                   .append("    </div>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
            
            // High complexity classes
            if (!quality.getHighComplexityClasses().isEmpty()) {
                section.append("<div class=\"complexity-section\">\n")
                       .append("  <h4>⚠️ High Complexity Classes</h4>\n")
                       .append("  <div class=\"complexity-list\">\n");
                
                for (String complexClass : quality.getHighComplexityClasses()) {
                    section.append("    <div class=\"complexity-item high\">\n")
                           .append("      <div class=\"class-name\">").append(complexClass).append("</div>\n")
                           .append("      <div class=\"recommendation\">Consider breaking into smaller classes or methods</div>\n")
                           .append("    </div>\n");
                }
                
                section.append("  </div>\n")
                       .append("</div>\n");
            }
            
            // Refactoring candidates
            if (!quality.getPotentialRefactoringCandidates().isEmpty()) {
                section.append("<div class=\"refactoring-section\">\n")
                       .append("  <h4>🔧 Refactoring Candidates</h4>\n")
                       .append("  <div class=\"refactoring-list\">\n");
                
                for (String candidate : quality.getPotentialRefactoringCandidates()) {
                    section.append("    <div class=\"refactoring-item\">\n")
                           .append("      <div class=\"class-name\">").append(candidate).append("</div>\n")
                           .append("      <div class=\"reason\">Large class with high complexity</div>\n")
                           .append("    </div>\n");
                }
                
                section.append("  </div>\n")
                       .append("</div>\n");
            }
            
            // Design patterns
            if (!quality.getDesignPatterns().isEmpty()) {
                section.append("<div class=\"patterns-section\">\n")
                       .append("  <h4>🎨 Design Patterns Detected</h4>\n")
                       .append("  <div class=\"patterns-grid\">\n");
                
                for (Map.Entry<String, Integer> pattern : quality.getDesignPatterns().entrySet()) {
                    section.append("    <div class=\"pattern-card\">\n")
                           .append("      <div class=\"pattern-name\">").append(pattern.getKey()).append("</div>\n")
                           .append("      <div class=\"pattern-count\">").append(pattern.getValue()).append(" instance(s)</div>\n")
                           .append("    </div>\n");
                }
                
                section.append("  </div>\n")
                       .append("</div>\n");
            }
        } else {
            section.append("<p class=\"no-data\">Quality analysis requires ASM bytecode analysis to complete.</p>\n");
        }
        
        return section.toString();
    }
    
    /**
     * Generate security analysis section
     */
    private String generateSecuritySection(ProjectAnalysis analysis) {
        StringBuilder section = new StringBuilder();
        
        section.append("<h3>🔒 Security Analysis</h3>\n");
        
        if (analysis.projectInsights != null) {
            ASMBytecodeAnalyzer.SecurityInsights security = analysis.projectInsights.getSecurityInsights();
            
            // Security score
            section.append("<div class=\"security-score-section\">\n")
                   .append("  <div class=\"security-score " + getScoreClass(security.getSecurityScore()) + "\">\n")
                   .append("    <div class=\"score-value\">").append(security.getSecurityScore()).append("%</div>\n")
                   .append("    <div class=\"score-label\">Security Score</div>\n")
                   .append("  </div>\n")
                   .append("  <div class=\"security-breakdown\">\n")
                   .append("    <div class=\"breakdown-item\">Reflection usage: ").append(security.getReflectionUsage().size()).append("</div>\n")
                   .append("    <div class=\"breakdown-item\">Serializable classes: ").append(security.getSerializationClasses().size()).append("</div>\n")
                   .append("    <div class=\"breakdown-item\">Deprecated API usage: ").append(security.getDeprecatedApiUsage().size()).append("</div>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
            
            // Security issues
            if (!security.getReflectionUsage().isEmpty()) {
                section.append("<div class=\"security-issues\">\n")
                       .append("  <h4>⚠️ Reflection Usage</h4>\n")
                       .append("  <div class=\"issue-list\">\n");
                
                for (String reflection : security.getReflectionUsage()) {
                    section.append("    <div class=\"issue-item reflection\">\n")
                           .append("      <div class=\"issue-icon\">🔍</div>\n")
                           .append("      <div class=\"issue-details\">\n")
                           .append("        <div class=\"issue-description\">").append(reflection).append("</div>\n")
                           .append("        <div class=\"issue-recommendation\">Review for security implications and consider alternatives</div>\n")
                           .append("      </div>\n")
                           .append("    </div>\n");
                }
                
                section.append("  </div>\n")
                       .append("</div>\n");
            }
            
            if (!security.getSerializationClasses().isEmpty()) {
                section.append("<div class=\"security-issues\">\n")
                       .append("  <h4>📦 Serialization Classes</h4>\n")
                       .append("  <div class=\"issue-list\">\n");
                
                for (String serClass : security.getSerializationClasses()) {
                    section.append("    <div class=\"issue-item serialization\">\n")
                           .append("      <div class=\"issue-icon\">📦</div>\n")
                           .append("      <div class=\"issue-details\">\n")
                           .append("        <div class=\"issue-description\">").append(serClass).append("</div>\n")
                           .append("        <div class=\"issue-recommendation\">Ensure proper serialization security measures</div>\n")
                           .append("      </div>\n")
                           .append("    </div>\n");
                }
                
                section.append("  </div>\n")
                       .append("</div>\n");
            }
            
            if (!security.getDeprecatedApiUsage().isEmpty()) {
                section.append("<div class=\"security-issues\">\n")
                       .append("  <h4>⚠️ Deprecated API Usage</h4>\n")
                       .append("  <div class=\"issue-list\">\n");
                
                for (String deprecated : security.getDeprecatedApiUsage()) {
                    section.append("    <div class=\"issue-item deprecated\">\n")
                           .append("      <div class=\"issue-icon\">⚠️</div>\n")
                           .append("      <div class=\"issue-details\">\n")
                           .append("        <div class=\"issue-description\">").append(deprecated).append("</div>\n")
                           .append("        <div class=\"issue-recommendation\">Update to use modern API alternatives</div>\n")
                           .append("      </div>\n")
                           .append("    </div>\n");
                }
                
                section.append("  </div>\n")
                       .append("</div>\n");
            }
            
            // Security recommendations
            section.append("<div class=\"security-recommendations\">\n")
                   .append("  <h4>🛡️ Security Recommendations</h4>\n")
                   .append("  <div class=\"recommendations-list\">\n");
            
            if (security.getSecurityScore() < 80) {
                section.append("    <div class=\"recommendation-item high-priority\">\n")
                       .append("      <div class=\"priority-badge high\">HIGH</div>\n")
                       .append("      <div class=\"recommendation-text\">Address reflection and serialization usage to improve security posture</div>\n")
                       .append("    </div>\n");
            }
            
            section.append("    <div class=\"recommendation-item\">\n")
                   .append("      <div class=\"priority-badge medium\">MEDIUM</div>\n")
                   .append("      <div class=\"recommendation-text\">Implement input validation for all external data sources</div>\n")
                   .append("    </div>\n")
                   .append("    <div class=\"recommendation-item\">\n")
                   .append("      <div class=\"priority-badge low\">LOW</div>\n")
                   .append("      <div class=\"recommendation-text\">Consider using static analysis tools for deeper security scanning</div>\n")
                   .append("    </div>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
        } else {
            section.append("<p class=\"no-data\">Security analysis requires ASM bytecode analysis to complete.</p>\n");
        }
        
        return section.toString();
    }
    
    /**
     * Generate impact analysis section - the key feature for JAR removal analysis
     */
    private String generateImpactSection(ProjectAnalysis analysis) {
        StringBuilder section = new StringBuilder();
        
        section.append("<h3>💥 Impact Analysis - JAR/Class Removal Effects</h3>\n");
        
        if (analysis.projectInsights != null) {
            ASMBytecodeAnalyzer.ImpactAnalysis impact = analysis.projectInsights.getImpactAnalysis();
            
            // Critical classes overview
            section.append("<div class=\"impact-overview\">\n")
                   .append("  <div class=\"impact-summary\">\n")
                   .append("    <div class=\"impact-metric critical\">\n")
                   .append("      <div class=\"metric-value\">").append(impact.getCriticalClasses().size()).append("</div>\n")
                   .append("      <div class=\"metric-label\">Critical Classes</div>\n")
                   .append("      <div class=\"metric-desc\">High impact if removed</div>\n")
                   .append("    </div>\n")
                   .append("    <div class=\"impact-metric moderate\">\n")
                   .append("      <div class=\"metric-value\">").append(getModerateRiskCount(impact)).append("</div>\n")
                   .append("      <div class=\"metric-label\">Moderate Risk</div>\n")
                   .append("      <div class=\"metric-desc\">Medium impact if removed</div>\n")
                   .append("    </div>\n")
                   .append("    <div class=\"impact-metric low\">\n")
                   .append("      <div class=\"metric-value\">").append(getLowRiskCount(impact)).append("</div>\n")
                   .append("      <div class=\"metric-label\">Low Risk</div>\n")
                   .append("      <div class=\"metric-desc\">Minimal impact if removed</div>\n")
                   .append("    </div>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
            
            // Critical classes detailed view
            if (!impact.getCriticalClasses().isEmpty()) {
                section.append("<div class=\"critical-classes-section\">\n")
                       .append("  <h4>🚨 Critical Classes - Removal Would Break Dependencies</h4>\n")
                       .append("  <div class=\"critical-classes-grid\">\n");
                
                for (String criticalClass : impact.getCriticalClasses()) {
                    int riskScore = impact.getRemovalRiskScores().getOrDefault(criticalClass, 0);
                    List<String> affected = impact.getAffectedClasses().getOrDefault(criticalClass, new ArrayList<>());
                    
                    section.append("    <div class=\"critical-class-card\">\n")
                           .append("      <div class=\"class-header\">\n")
                           .append("        <div class=\"class-name\">").append(criticalClass.replace("/", ".")).append("</div>\n")
                           .append("        <div class=\"risk-score critical\">Risk: ").append(riskScore).append("</div>\n")
                           .append("      </div>\n")
                           .append("      <div class=\"impact-details\">\n")
                           .append("        <div class=\"affected-count\">").append(affected.size()).append(" classes would be affected</div>\n")
                           .append("        <div class=\"affected-preview\">\n");
                    
                    for (String affectedClass : affected.stream().limit(3).toArray(String[]::new)) {
                        section.append("          <div class=\"affected-item\">").append(affectedClass.replace("/", ".")).append("</div>\n");
                    }
                    if (affected.size() > 3) {
                        section.append("          <div class=\"affected-more\">... and ").append(affected.size() - 3).append(" more</div>\n");
                    }
                    
                    section.append("        </div>\n")
                           .append("        <div class=\"removal-warning\">⚠️ Removing this class would likely cause compilation failures</div>\n")
                           .append("      </div>\n")
                           .append("    </div>\n");
                }
                
                section.append("  </div>\n")
                       .append("</div>\n");
            }
            
            // Impact matrix
            section.append("<div class=\"impact-matrix-section\">\n")
                   .append("  <h4>📊 Complete Removal Impact Matrix</h4>\n")
                   .append("  <div class=\"matrix-controls\">\n")
                   .append("    <input type=\"text\" id=\"impactSearch\" placeholder=\"Search classes...\" onkeyup=\"filterImpactTable()\">\n")
                   .append("    <select id=\"riskFilter\" onchange=\"filterImpactTable()\">\n")
                   .append("      <option value=\"all\">All Risk Levels</option>\n")
                   .append("      <option value=\"critical\">Critical (50+)</option>\n")
                   .append("      <option value=\"moderate\">Moderate (20-49)</option>\n")
                   .append("      <option value=\"low\">Low (0-19)</option>\n")
                   .append("    </select>\n")
                   .append("  </div>\n")
                   .append("  <div class=\"table-container\">\n")
                   .append("    <table class=\"impact-table\" id=\"impactTable\">\n")
                   .append("      <thead>\n")
                   .append("        <tr>\n")
                   .append("          <th>Class Name</th>\n")
                   .append("          <th>Risk Score</th>\n")
                   .append("          <th>Dependents</th>\n")
                   .append("          <th>Impact Level</th>\n")
                   .append("          <th>Removal Recommendation</th>\n")
                   .append("        </tr>\n")
                   .append("      </thead>\n")
                   .append("      <tbody>\n");
            
            // Sort by risk score descending
            impact.getRemovalRiskScores().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(entry -> {
                        String className = entry.getKey();
                        int riskScore = entry.getValue();
                        int dependentCount = impact.getAffectedClasses().getOrDefault(className, new ArrayList<>()).size();
                        String riskLevel = getRiskLevel(riskScore);
                        String recommendation = getRemovalRecommendation(riskScore);
                        
                        section.append("        <tr class=\"risk-" + riskLevel.toLowerCase() + "\">\n")
                               .append("          <td class=\"class-name\">").append(className.replace("/", ".")).append("</td>\n")
                               .append("          <td class=\"risk-score\">").append(riskScore).append("</td>\n")
                               .append("          <td class=\"dependent-count\">").append(dependentCount).append("</td>\n")
                               .append("          <td class=\"impact-level\">\n")
                               .append("            <span class=\"risk-badge " + riskLevel.toLowerCase() + "\">").append(riskLevel).append("</span>\n")
                               .append("          </td>\n")
                               .append("          <td class=\"recommendation\">").append(recommendation).append("</td>\n")
                               .append("        </tr>\n");
                    });
            
            section.append("      </tbody>\n")
                   .append("    </table>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
            
            // JAR removal simulation
            section.append("<div class=\"jar-simulation-section\">\n")
                   .append("  <h4>🧪 JAR Removal Simulation</h4>\n")
                   .append("  <div class=\"simulation-explanation\">\n")
                   .append("    <p>This analysis shows what would happen if you remove specific JARs or classes from your project:</p>\n")
                   .append("    <ul>\n")
                   .append("      <li><strong>Critical (50+):</strong> Removal will likely break compilation</li>\n")
                   .append("      <li><strong>Moderate (20-49):</strong> May cause issues, thorough testing needed</li>\n")
                   .append("      <li><strong>Low (0-19):</strong> Safe to remove with minimal impact</li>\n")
                   .append("    </ul>\n")
                   .append("  </div>\n")
                   .append("</div>\n");
        } else {
            section.append("<p class=\"no-data\">Impact analysis requires ASM bytecode analysis to complete.</p>\n");
        }
        
        return section.toString();
    }
    
    // Helper methods for the new report sections
    private String getScoreClass(int score) {
        if (score >= 80) return "excellent";
        else if (score >= 60) return "good";
        else if (score >= 40) return "fair";
        else return "poor";
    }
    
    private int getTotalSecurityIssues(ASMBytecodeAnalyzer.SecurityInsights security) {
        return security.getReflectionUsage().size() + 
               security.getSerializationClasses().size() + 
               security.getDeprecatedApiUsage().size();
    }
    
    private int getModerateRiskCount(ASMBytecodeAnalyzer.ImpactAnalysis impact) {
        return (int) impact.getRemovalRiskScores().values().stream()
                .filter(score -> score >= 20 && score < 50)
                .count();
    }
    
    private int getLowRiskCount(ASMBytecodeAnalyzer.ImpactAnalysis impact) {
        return (int) impact.getRemovalRiskScores().values().stream()
                .filter(score -> score < 20)
                .count();
    }
    
    private String getRiskLevel(int riskScore) {
        if (riskScore >= 50) return "Critical";
        else if (riskScore >= 20) return "Moderate";
        else return "Low";
    }
    
    private String getRemovalRecommendation(int riskScore) {
        if (riskScore >= 50) return "❌ Do not remove - critical dependencies";
        else if (riskScore >= 20) return "⚠️ Test thoroughly before removal";
        else return "✅ Safe to remove";
    }
    
    private String getComplexityClass(double complexity) {
        if (complexity > 10) return "complexity-high";
        else if (complexity > 5) return "complexity-medium";
        else return "complexity-low";
    }
    
    private String formatAnalysisOutput(String output) {
        if (output == null || output.trim().isEmpty()) {
            return "No output generated";
        }
        return output.replace("<", "&lt;").replace(">", "&gt;");
    }
    
    private String generateJsonReport(ProjectAnalysis analysis) {
        StringBuilder json = new StringBuilder();
        json.append("{\n")
            .append("  \"projectName\": \"").append(analysis.projectName).append("\",\n")
            .append("  \"projectType\": \"").append(analysis.projectType).append("\",\n")
            .append("  \"javaFileCount\": ").append(analysis.javaFiles.size()).append(",\n")
            .append("  \"analysisDate\": \"").append(new Date()).append("\",\n")
            .append("  \"successful\": ").append(analysis.jdepsResults != null).append("\n")
            .append("}");
        return json.toString();
    }
    
    private void cleanupDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            Files.walk(dir)
                .sorted((a, b) -> b.compareTo(a)) // Reverse order to delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
        }
    }
    
    // Inner classes for data structures
    public static class ProjectAnalysis {
        public String projectPath;
        public String projectName;
        public String projectType;
        public List<String> javaFiles = new ArrayList<>();
        public List<String> jarFiles = new ArrayList<>();
        public String compiledClassesDir;
        public String jarFile;
        public JDepsResults jdepsResults;
        public Map<String, ASMBytecodeAnalyzer.ClassAnalysisResult> asmResults;
        public ASMBytecodeAnalyzer.ProjectInsights projectInsights;

        public ProjectAnalysis() {
            super();
        }
    }
    
    public static class JDepsResults {
        public String basicAnalysis;
        public String verboseAnalysis;
        public String summaryAnalysis;

        public JDepsResults() {
            super();
        }
    }
}
