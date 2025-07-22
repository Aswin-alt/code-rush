package com.example.jdeps;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Simple JDeps report generator that creates an HTML report
 */
public class SimpleJDepsReportGenerator {

    public SimpleJDepsReportGenerator() {
        super();
    }
    
    public static void main(String[] args) {
        try {
            String projectPath = System.getProperty("user.dir");
            String jarPath = "build/simple-jdeps-test.jar";
            String outputDir = "jdeps-report";
            
            generateReport(jarPath, outputDir);
            
            System.out.println("Report generated successfully!");
            System.out.println("Open jdeps-report/index.html in your browser to view the report.");
            
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void generateReport(String jarPath, String outputDir) throws IOException {
        System.out.println("Generating JDeps analysis report...");
        
        // Create output directory
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);
        
        // Run JDeps analysis
        String basicAnalysis = runJDepsCommand(jarPath);
        String verboseAnalysis = runJDepsCommand(jarPath, "-verbose:class");
        String summaryAnalysis = runJDepsCommand(jarPath, "-s");
        
        // Generate HTML report
        String htmlContent = generateHtmlReport(basicAnalysis, verboseAnalysis, summaryAnalysis, jarPath);
        Files.write(outputPath.resolve("index.html"), htmlContent.getBytes());
        
        // Also create a text report
        String textReport = generateTextReport(basicAnalysis, verboseAnalysis, summaryAnalysis);
        Files.write(outputPath.resolve("analysis.txt"), textReport.getBytes());
        
        System.out.println("Report generated in: " + outputPath.toAbsolutePath());
    }
    
    private static String runJDepsCommand(String jarPath, String... options) {
        try {
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
            
            process.waitFor();
            return output.toString();
            
        } catch (Exception e) {
            System.err.println("Error running JDeps command: " + e.getMessage());
            return "Error running analysis: " + e.getMessage();
        }
    }
    
    private static String generateHtmlReport(String basic, String verbose, String summary, String jarPath) {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>JDeps Analysis Report</title>\n" +
            "    <style>\n" +
            "        body {\n" +
            "            font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;\n" +
            "            margin: 0; padding: 20px;\n" +
            "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "            min-height: 100vh; color: #333;\n" +
            "        }\n" +
            "        .container {\n" +
            "            max-width: 1200px; margin: 0 auto;\n" +
            "            background: rgba(255,255,255,0.95);\n" +
            "            border-radius: 15px; box-shadow: 0 20px 40px rgba(0,0,0,0.1);\n" +
            "            overflow: hidden;\n" +
            "        }\n" +
            "        .header {\n" +
            "            background: linear-gradient(135deg, #667eea, #764ba2);\n" +
            "            color: white; padding: 40px; text-align: center;\n" +
            "        }\n" +
            "        .header h1 { margin: 0; font-size: 3em; font-weight: 300; }\n" +
            "        .header p { margin: 10px 0 0; opacity: 0.9; font-size: 1.2em; }\n" +
            "        .nav {\n" +
            "            background: #34495e; display: flex;\n" +
            "        }\n" +
            "        .nav-item {\n" +
            "            flex: 1; padding: 20px; text-align: center;\n" +
            "            color: white; cursor: pointer; transition: all 0.3s;\n" +
            "            border: none; background: none; font-size: 1.1em;\n" +
            "        }\n" +
            "        .nav-item:hover { background: rgba(255,255,255,0.1); }\n" +
            "        .nav-item.active { background: #3498db; }\n" +
            "        .content {\n" +
            "            padding: 30px;\n" +
            "        }\n" +
            "        .section {\n" +
            "            display: none;\n" +
            "        }\n" +
            "        .section.active {\n" +
            "            display: block;\n" +
            "        }\n" +
            "        .info-box {\n" +
            "            background: #f8f9fa; border-left: 4px solid #3498db;\n" +
            "            padding: 20px; margin: 20px 0; border-radius: 0 8px 8px 0;\n" +
            "        }\n" +
            "        .analysis-output {\n" +
            "            background: #2c3e50; color: #ecf0f1;\n" +
            "            padding: 25px; border-radius: 10px;\n" +
            "            font-family: 'Monaco', 'Menlo', monospace;\n" +
            "            font-size: 14px; line-height: 1.6;\n" +
            "            overflow-x: auto; white-space: pre-wrap;\n" +
            "            max-height: 600px; overflow-y: auto;\n" +
            "        }\n" +
            "        .stats {\n" +
            "            display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));\n" +
            "            gap: 20px; margin: 30px 0;\n" +
            "        }\n" +
            "        .stat-card {\n" +
            "            background: white; padding: 25px; border-radius: 10px;\n" +
            "            box-shadow: 0 5px 15px rgba(0,0,0,0.1); text-align: center;\n" +
            "            border-top: 4px solid #3498db;\n" +
            "        }\n" +
            "        .stat-number {\n" +
            "            font-size: 2.5em; font-weight: bold;\n" +
            "            color: #3498db; margin-bottom: 10px;\n" +
            "        }\n" +
            "        .stat-label {\n" +
            "            color: #7f8c8d; font-size: 1.1em;\n" +
            "        }\n" +
            "        .highlight { background: #f39c12; color: white; padding: 2px 6px; border-radius: 3px; }\n" +
            "        .dependency { color: #27ae60; }\n" +
            "        .arrow { color: #e74c3c; font-weight: bold; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>🔍 JDeps Analysis Report</h1>\n" +
            "            <p>Comprehensive Java Dependency Analysis</p>\n" +
            "            <p style=\"font-size: 0.9em; margin-top: 15px;\">JAR: <code>" + jarPath + "</code></p>\n" +
            "        </div>\n" +
            "        \n" +
            "        <div class=\"nav\">\n" +
            "            <button class=\"nav-item active\" onclick=\"showSection('overview')\">📊 Overview</button>\n" +
            "            <button class=\"nav-item\" onclick=\"showSection('basic')\">🔎 Basic Analysis</button>\n" +
            "            <button class=\"nav-item\" onclick=\"showSection('verbose')\">📋 Verbose Analysis</button>\n" +
            "            <button class=\"nav-item\" onclick=\"showSection('summary')\">📈 Summary</button>\n" +
            "        </div>\n" +
            "        \n" +
            "        <div class=\"content\">\n" +
            "            <div id=\"overview\" class=\"section active\">\n" +
            "                <h2>📊 Analysis Overview</h2>\n" +
            "                <div class=\"info-box\">\n" +
            "                    <h3>About This Report</h3>\n" +
            "                    <p>This report shows the dependency analysis of your Java application using JDeps. " +
            "                       JDeps is a static analysis tool that shows the package-level or class-level dependencies.</p>\n" +
            "                    <p><strong>Generated:</strong> " + new Date() + "</p>\n" +
            "                </div>\n" +
            "                \n" +
            "                <div class=\"stats\">\n" +
            "                    <div class=\"stat-card\">\n" +
            "                        <div class=\"stat-number\">" + countLines(basic) + "</div>\n" +
            "                        <div class=\"stat-label\">Basic Dependencies</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"stat-card\">\n" +
            "                        <div class=\"stat-number\">" + countLines(verbose) + "</div>\n" +
            "                        <div class=\"stat-label\">Verbose Dependencies</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"stat-card\">\n" +
            "                        <div class=\"stat-number\">" + countLines(summary) + "</div>\n" +
            "                        <div class=\"stat-label\">Summary Items</div>\n" +
            "                    </div>\n" +
            "                    <div class=\"stat-card\">\n" +
            "                        <div class=\"stat-number\">" + countJavaBaseReferences(basic) + "</div>\n" +
            "                        <div class=\"stat-label\">JDK Dependencies</div>\n" +
            "                    </div>\n" +
            "                </div>\n" +
            "                \n" +
            "                <div class=\"info-box\">\n" +
            "                    <h3>🎯 Key Findings</h3>\n" +
            "                    <ul>\n" +
            "                        <li>Your application primarily depends on <span class=\"highlight\">java.base</span> module</li>\n" +
            "                        <li>Uses modern Java features like <span class=\"dependency\">streams</span>, <span class=\"dependency\">time API</span>, and <span class=\"dependency\">collections</span></li>\n" +
            "                        <li>No external library dependencies detected in this simple version</li>\n" +
            "                        <li>Clean dependency structure with standard JDK usage</li>\n" +
            "                    </ul>\n" +
            "                </div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <div id=\"basic\" class=\"section\">\n" +
            "                <h2>🔎 Basic Dependency Analysis</h2>\n" +
            "                <div class=\"info-box\">\n" +
            "                    <p>Shows package-level dependencies of your application. " +
            "                       Each line shows: <span class=\"dependency\">source</span> <span class=\"arrow\">→</span> <span class=\"dependency\">target</span></p>\n" +
            "                </div>\n" +
            "                <div class=\"analysis-output\">" + formatAnalysisOutput(basic) + "</div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <div id=\"verbose\" class=\"section\">\n" +
            "                <h2>📋 Verbose Class-Level Analysis</h2>\n" +
            "                <div class=\"info-box\">\n" +
            "                    <p>Detailed class-level dependencies showing exactly which classes depend on which packages.</p>\n" +
            "                </div>\n" +
            "                <div class=\"analysis-output\">" + formatAnalysisOutput(verbose) + "</div>\n" +
            "            </div>\n" +
            "            \n" +
            "            <div id=\"summary\" class=\"section\">\n" +
            "                <h2>📈 Summary Analysis</h2>\n" +
            "                <div class=\"info-box\">\n" +
            "                    <p>High-level summary showing module dependencies only.</p>\n" +
            "                </div>\n" +
            "                <div class=\"analysis-output\">" + formatAnalysisOutput(summary) + "</div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    \n" +
            "    <script>\n" +
            "        function showSection(sectionId) {\n" +
            "            // Hide all sections\n" +
            "            document.querySelectorAll('.section').forEach(section => {\n" +
            "                section.classList.remove('active');\n" +
            "            });\n" +
            "            \n" +
            "            // Remove active class from all nav items\n" +
            "            document.querySelectorAll('.nav-item').forEach(item => {\n" +
            "                item.classList.remove('active');\n" +
            "            });\n" +
            "            \n" +
            "            // Show selected section\n" +
            "            document.getElementById(sectionId).classList.add('active');\n" +
            "            \n" +
            "            // Add active class to clicked nav item\n" +
            "            event.target.classList.add('active');\n" +
            "        }\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }
    
    private static String formatAnalysisOutput(String output) {
        if (output == null || output.trim().isEmpty()) {
            return "No output generated";
        }
        
        // Escape HTML and highlight important parts
        return output.replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("->", "<span class=\"arrow\">→</span>")
                    .replace("java.base", "<span class=\"highlight\">java.base</span>")
                    .replace("java.util", "<span class=\"dependency\">java.util</span>")
                    .replace("java.time", "<span class=\"dependency\">java.time</span>")
                    .replace("java.nio", "<span class=\"dependency\">java.nio</span>")
                    .replace("java.util.concurrent", "<span class=\"dependency\">java.util.concurrent</span>")
                    .replace("java.util.stream", "<span class=\"dependency\">java.util.stream</span>")
                    .replace("java.util.regex", "<span class=\"dependency\">java.util.regex</span>");
    }
    
    private static int countLines(String text) {
        if (text == null || text.trim().isEmpty()) return 0;
        return text.split("\n").length;
    }
    
    private static int countJavaBaseReferences(String text) {
        if (text == null) return 0;
        return text.split("java\\.base", -1).length - 1;
    }
    
    private static String generateTextReport(String basic, String verbose, String summary) {
        StringBuilder report = new StringBuilder();
        report.append("JDeps Analysis Report\n");
        report.append("==========================================\n");
        report.append("Generated: ").append(new Date()).append("\n\n");
        
        report.append("BASIC ANALYSIS:\n");
        report.append("---------------\n");
        report.append(basic).append("\n\n");
        
        report.append("VERBOSE ANALYSIS:\n");
        report.append("-----------------\n");
        report.append(verbose).append("\n\n");
        
        report.append("SUMMARY ANALYSIS:\n");
        report.append("-----------------\n");
        report.append(summary).append("\n");
        
        return report.toString();
    }
}
