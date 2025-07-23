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
                .append("                    <button class=\"tab-button active\" onclick=\"showTab('basic')\">Basic Analysis</button>\n")
                .append("                    <button class=\"tab-button\" onclick=\"showTab('verbose')\">Verbose Analysis</button>\n")
                .append("                    <button class=\"tab-button\" onclick=\"showTab('summary')\">Summary</button>\n")
                .append("                    <button class=\"tab-button\" onclick=\"showTab('asm')\">ASM Bytecode</button>\n")
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"basic\" class=\"tab-content active\">\n")
                .append("                    <h3>Basic Dependency Analysis</h3>\n")
                .append("                    <div class=\"analysis-output\">\n")
                .append(formatAnalysisOutput(analysis.jdepsResults.basicAnalysis))
                .append("                    </div>\n")
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"verbose\" class=\"tab-content\">\n")
                .append("                    <h3>Verbose Class-Level Analysis</h3>\n")
                .append("                    <div class=\"analysis-output\">\n")
                .append(formatAnalysisOutput(analysis.jdepsResults.verboseAnalysis))
                .append("                    </div>\n")
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"summary\" class=\"tab-content\">\n")
                .append("                    <h3>Summary Analysis</h3>\n")
                .append("                    <div class=\"analysis-output\">\n")
                .append(formatAnalysisOutput(analysis.jdepsResults.summaryAnalysis))
                .append("                    </div>\n")
                .append("                </div>\n")
                .append("                \n")
                .append("                <div id=\"asm\" class=\"tab-content\">\n")
                .append("                    <h3>🔍 ASM Bytecode Analysis</h3>\n")
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
            .append("    </script>\n")
            .append("</body>\n")
            .append("</html>");
        
        return html.toString();
    }
    
    private String getReportStyles() {
        return "body { font-family: 'Segoe UI', sans-serif; margin: 0; padding: 20px; background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); min-height: 100vh; }\n" +
               ".container { max-width: 1200px; margin: 0 auto; background: rgba(255,255,255,0.95); border-radius: 15px; padding: 30px; }\n" +
               ".header { text-align: center; margin-bottom: 40px; color: #2c3e50; }\n" +
               ".header h1 { font-size: 2.5em; margin-bottom: 10px; }\n" +
               ".project-info { background: #f8f9fa; padding: 20px; border-radius: 10px; margin-bottom: 30px; }\n" +
               ".info-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; }\n" +
               ".info-item { padding: 10px; background: white; border-radius: 5px; }\n" +
               ".tab-container { background: white; border-radius: 10px; overflow: hidden; box-shadow: 0 5px 15px rgba(0,0,0,0.1); }\n" +
               ".tab-buttons { display: flex; background: #34495e; }\n" +
               ".tab-button { flex: 1; padding: 15px; background: none; border: none; color: white; cursor: pointer; }\n" +
               ".tab-button:hover { background: rgba(255,255,255,0.1); }\n" +
               ".tab-button.active { background: #3498db; }\n" +
               ".tab-content { display: none; padding: 20px; }\n" +
               ".tab-content.active { display: block; }\n" +
               ".analysis-output { background: #2c3e50; color: #ecf0f1; padding: 20px; border-radius: 5px; font-family: monospace; white-space: pre-wrap; max-height: 400px; overflow-y: auto; }\n" +
               ".error-section { background: #fee; border: 1px solid #fcc; padding: 20px; border-radius: 10px; color: #d00; }" +
               ".asm-stats { display: grid; grid-template-columns: repeat(auto-fit, minmax(150px, 1fr)); gap: 15px; margin: 20px 0; }" +
               ".stat-card { background: linear-gradient(135deg, #3498db, #2980b9); color: white; padding: 15px; border-radius: 8px; text-align: center; }" +
               ".stat-number { font-size: 1.8em; font-weight: bold; display: block; }" +
               ".stat-label { font-size: 0.9em; opacity: 0.9; }" +
               ".class-table { width: 100%; border-collapse: collapse; margin: 20px 0; }" +
               ".class-table th, .class-table td { border: 1px solid #ddd; padding: 8px; text-align: left; }" +
               ".class-table th { background: #f2f2f2; }" +
               ".complexity-low { background: #d4edda; }" +
               ".complexity-medium { background: #fff3cd; }" +
               ".complexity-high { background: #f8d7da; }";
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
            
            section.append("    <tr class=\"").append(complexityClass).append("\">\n")
                   .append("      <td>").append(result.getClassName().replace("/", ".")).append("</td>\n")
                   .append("      <td>").append(classType).append("</td>\n")
                   .append("      <td>").append(result.getTotalMethods()).append("</td>\n")
                   .append("      <td>").append(result.getTotalFields()).append("</td>\n")
                   .append("      <td>").append(String.format("%.2f", result.getAverageMethodComplexity())).append("</td>\n")
                   .append("      <td>").append(result.getMethodCalls().size() + result.getFieldAccess().size()).append("</td>\n")
                   .append("    </tr>\n");
        }
        
        section.append("  </tbody>\n")
               .append("</table>\n");
        
        return section.toString();
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
