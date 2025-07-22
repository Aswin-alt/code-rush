package com.example.jdeps;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * JDeps Analyzer that runs JDeps analysis and generates JSON report data
 */
public class JDepsAnalyzer {
    
    private final ObjectMapper objectMapper;
    private final String projectPath;
    
    public JDepsAnalyzer(String projectPath) {
        super();
        this.objectMapper = new ObjectMapper();
        this.projectPath = projectPath;
    }
    
    /**
     * Runs JDeps analysis and generates a comprehensive JSON report
     */
    public void generateReport(String jarPath, String outputDir) throws IOException {
        System.out.println("Generating JDeps analysis report...");
        
        // Create output directory
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);
        
        // Run various JDeps analyses
        Map<String, Object> report = new HashMap<>();
        report.put("metadata", generateMetadata(jarPath));
        report.put("basicAnalysis", runBasicAnalysis(jarPath));
        report.put("verboseAnalysis", runVerboseAnalysis(jarPath));
        report.put("summaryAnalysis", runSummaryAnalysis(jarPath));
        report.put("moduleAnalysis", runModuleAnalysis(jarPath));
        report.put("dependencyStats", generateDependencyStats(jarPath));
        
        // Write JSON report
        String jsonReport = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(report);
        Files.write(outputPath.resolve("jdeps-report.json"), jsonReport.getBytes());
        
        // Generate HTML report
        generateHtmlReport(report, outputPath);
        
        System.out.println("Report generated in: " + outputPath.toAbsolutePath());
    }
    
    private Map<String, Object> generateMetadata(String jarPath) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("jarPath", jarPath);
        metadata.put("analysisDate", new Date().toString());
        metadata.put("jdepsVersion", getJDepsVersion());
        metadata.put("javaVersion", System.getProperty("java.version"));
        return metadata;
    }
    
    private List<Map<String, String>> runBasicAnalysis(String jarPath) {
        return parseJDepsOutput(runJDepsCommand(jarPath));
    }
    
    private List<Map<String, String>> runVerboseAnalysis(String jarPath) {
        return parseJDepsOutput(runJDepsCommand(jarPath, "-verbose:class"));
    }
    
    private List<Map<String, String>> runSummaryAnalysis(String jarPath) {
        return parseJDepsOutput(runJDepsCommand(jarPath, "-s"));
    }
    
    private List<Map<String, String>> runModuleAnalysis(String jarPath) {
        return parseJDepsOutput(runJDepsCommand(jarPath, "-s"));
    }
    
    private Map<String, Object> generateDependencyStats(String jarPath) {
        List<Map<String, String>> dependencies = runBasicAnalysis(jarPath);
        Map<String, Object> stats = new HashMap<>();
        
        Set<String> uniqueTargets = new HashSet<>();
        Set<String> uniquePackages = new HashSet<>();
        
        for (Map<String, String> dep : dependencies) {
            String target = dep.get("target");
            String targetPackage = dep.get("targetPackage");
            
            if (target != null) uniqueTargets.add(target);
            if (targetPackage != null) uniquePackages.add(targetPackage);
        }
        
        stats.put("totalDependencies", dependencies.size());
        stats.put("uniqueTargets", uniqueTargets.size());
        stats.put("uniquePackages", uniquePackages.size());
        stats.put("targetModules", new ArrayList<>(uniqueTargets));
        stats.put("targetPackages", new ArrayList<>(uniquePackages));
        
        return stats;
    }
    
    private String runJDepsCommand(String jarPath, String... options) {
        try {
            List<String> command = new ArrayList<>();
            command.add("jdeps");
            command.addAll(Arrays.asList(options));
            command.add(jarPath);
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(projectPath));
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
            return "";
        }
    }
    
    private List<Map<String, String>> parseJDepsOutput(String output) {
        List<Map<String, String>> dependencies = new ArrayList<>();
        String[] lines = output.split("\n");
        
        Pattern depPattern = Pattern.compile("\\s*([^\\s]+)\\s*->\\s*([^\\s]+)\\s*([^\\s]*)");
        
        for (String line : lines) {
            if (line.trim().isEmpty() || line.contains("->")) {
                Matcher matcher = depPattern.matcher(line);
                if (matcher.find()) {
                    Map<String, String> dependency = new HashMap<>();
                    dependency.put("source", matcher.group(1));
                    dependency.put("target", matcher.group(2));
                    if (matcher.groupCount() > 2 && matcher.group(3) != null) {
                        dependency.put("targetPackage", matcher.group(3));
                    }
                    dependencies.add(dependency);
                }
            }
        }
        
        return dependencies;
    }
    
    private String getJDepsVersion() {
        try {
            ProcessBuilder pb = new ProcessBuilder("jdeps", "-version");
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                return reader.readLine();
            }
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    private void generateHtmlReport(Map<String, Object> report, Path outputPath) throws IOException {
        String htmlTemplate = generateHtmlTemplate();
        String htmlContent = htmlTemplate.replace("{{REPORT_DATA}}", 
            objectMapper.writeValueAsString(report));
        
        Files.write(outputPath.resolve("jdeps-report.html"), htmlContent.getBytes());
    }
    
    private String generateHtmlTemplate() {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"en\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>JDeps Analysis Report</title>\n" +
            "    <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n" +
            "    <style>\n" +
            "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
            "        body {\n" +
            "            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;\n" +
            "            line-height: 1.6; color: #333;\n" +
            "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "            min-height: 100vh;\n" +
            "        }\n" +
            "        .container { max-width: 1200px; margin: 0 auto; padding: 20px; }\n" +
            "        .header {\n" +
            "            background: rgba(255, 255, 255, 0.95); padding: 30px;\n" +
            "            border-radius: 15px; box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);\n" +
            "            margin-bottom: 30px; text-align: center;\n" +
            "        }\n" +
            "        .header h1 { color: #2c3e50; font-size: 2.5em; margin-bottom: 10px; font-weight: 300; }\n" +
            "        .header .subtitle { color: #7f8c8d; font-size: 1.1em; }\n" +
            "        .stats-grid {\n" +
            "            display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));\n" +
            "            gap: 20px; margin-bottom: 30px;\n" +
            "        }\n" +
            "        .stat-card {\n" +
            "            background: rgba(255, 255, 255, 0.95); padding: 25px;\n" +
            "            border-radius: 15px; box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);\n" +
            "            text-align: center; transition: transform 0.3s ease;\n" +
            "        }\n" +
            "        .stat-card:hover { transform: translateY(-5px); }\n" +
            "        .stat-number { font-size: 2.5em; font-weight: bold; color: #3498db; margin-bottom: 10px; }\n" +
            "        .stat-label { color: #7f8c8d; font-size: 1.1em; }\n" +
            "        .chart-container {\n" +
            "            background: rgba(255, 255, 255, 0.95); border-radius: 15px;\n" +
            "            box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1); padding: 20px; margin-bottom: 30px;\n" +
            "        }\n" +
            "        .chart-title { text-align: center; color: #2c3e50; margin-bottom: 20px; font-size: 1.5em; }\n" +
            "        .tab-container {\n" +
            "            background: rgba(255, 255, 255, 0.95); border-radius: 15px;\n" +
            "            box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1); overflow: hidden; margin-bottom: 30px;\n" +
            "        }\n" +
            "        .tab-buttons { display: flex; background: #34495e; }\n" +
            "        .tab-button {\n" +
            "            flex: 1; padding: 15px; background: none; border: none;\n" +
            "            color: white; cursor: pointer; transition: background 0.3s ease;\n" +
            "        }\n" +
            "        .tab-button:hover { background: rgba(255, 255, 255, 0.1); }\n" +
            "        .tab-button.active { background: #3498db; }\n" +
            "        .tab-content { display: none; padding: 20px; max-height: 500px; overflow-y: auto; }\n" +
            "        .tab-content.active { display: block; }\n" +
            "        .dependency-item {\n" +
            "            display: flex; justify-content: space-between; align-items: center;\n" +
            "            padding: 12px 0; border-bottom: 1px solid #ecf0f1;\n" +
            "        }\n" +
            "        .dependency-item:last-child { border-bottom: none; }\n" +
            "        .dependency-source { font-weight: 500; color: #2c3e50; }\n" +
            "        .dependency-arrow { color: #3498db; font-weight: bold; margin: 0 10px; }\n" +
            "        .dependency-target { color: #27ae60; font-family: monospace; }\n" +
            "        .metadata {\n" +
            "            background: rgba(255, 255, 255, 0.95); border-radius: 15px;\n" +
            "            padding: 20px; box-shadow: 0 5px 20px rgba(0, 0, 0, 0.1);\n" +
            "        }\n" +
            "        .metadata h3 { color: #2c3e50; margin-bottom: 15px; }\n" +
            "        .metadata-item {\n" +
            "            display: flex; justify-content: space-between; padding: 8px 0;\n" +
            "            border-bottom: 1px solid #ecf0f1;\n" +
            "        }\n" +
            "        .metadata-item:last-child { border-bottom: none; }\n" +
            "        .metadata-label { font-weight: 500; color: #34495e; }\n" +
            "        .metadata-value { color: #7f8c8d; font-family: monospace; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"container\">\n" +
            "        <div class=\"header\">\n" +
            "            <h1>JDeps Analysis Report</h1>\n" +
            "            <div class=\"subtitle\">Comprehensive Java Dependency Analysis</div>\n" +
            "        </div>\n" +
            "        <div class=\"stats-grid\" id=\"statsGrid\"></div>\n" +
            "        <div class=\"chart-container\">\n" +
            "            <div class=\"chart-title\">Dependency Distribution</div>\n" +
            "            <canvas id=\"dependencyChart\" width=\"400\" height=\"200\"></canvas>\n" +
            "        </div>\n" +
            "        <div class=\"tab-container\">\n" +
            "            <div class=\"tab-buttons\">\n" +
            "                <button class=\"tab-button active\" onclick=\"showTab('basic')\">Basic Analysis</button>\n" +
            "                <button class=\"tab-button\" onclick=\"showTab('verbose')\">Verbose Analysis</button>\n" +
            "                <button class=\"tab-button\" onclick=\"showTab('summary')\">Summary</button>\n" +
            "                <button class=\"tab-button\" onclick=\"showTab('modules')\">Modules</button>\n" +
            "            </div>\n" +
            "            <div id=\"basic\" class=\"tab-content active\">\n" +
            "                <h3>Basic Dependency Analysis</h3>\n" +
            "                <div id=\"basicAnalysis\"></div>\n" +
            "            </div>\n" +
            "            <div id=\"verbose\" class=\"tab-content\">\n" +
            "                <h3>Verbose Class-level Analysis</h3>\n" +
            "                <div id=\"verboseAnalysis\"></div>\n" +
            "            </div>\n" +
            "            <div id=\"summary\" class=\"tab-content\">\n" +
            "                <h3>Summary Analysis</h3>\n" +
            "                <div id=\"summaryAnalysis\"></div>\n" +
            "            </div>\n" +
            "            <div id=\"modules\" class=\"tab-content\">\n" +
            "                <h3>Module Analysis</h3>\n" +
            "                <div id=\"moduleAnalysis\"></div>\n" +
            "            </div>\n" +
            "        </div>\n" +
            "        <div class=\"metadata\">\n" +
            "            <h3>Analysis Metadata</h3>\n" +
            "            <div id=\"metadata\"></div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <script>\n" +
            "        const reportData = {{REPORT_DATA}};\n" +
            "        function initializeReport() {\n" +
            "            populateStats(); populateAnalysisTabs(); populateMetadata(); createDependencyChart();\n" +
            "        }\n" +
            "        function populateStats() {\n" +
            "            const stats = reportData.dependencyStats;\n" +
            "            const statsGrid = document.getElementById('statsGrid');\n" +
            "            const statCards = [\n" +
            "                { number: stats.totalDependencies, label: 'Total Dependencies' },\n" +
            "                { number: stats.uniqueTargets, label: 'Unique Targets' },\n" +
            "                { number: stats.uniquePackages, label: 'Unique Packages' },\n" +
            "                { number: reportData.basicAnalysis.length, label: 'Analysis Items' }\n" +
            "            ];\n" +
            "            statsGrid.innerHTML = statCards.map(stat => `\n" +
            "                <div class=\"stat-card\">\n" +
            "                    <div class=\"stat-number\">${stat.number}</div>\n" +
            "                    <div class=\"stat-label\">${stat.label}</div>\n" +
            "                </div>\n" +
            "            `).join('');\n" +
            "        }\n" +
            "        function populateAnalysisTabs() {\n" +
            "            populateDependencyList('basicAnalysis', reportData.basicAnalysis);\n" +
            "            populateDependencyList('verboseAnalysis', reportData.verboseAnalysis);\n" +
            "            populateDependencyList('summaryAnalysis', reportData.summaryAnalysis);\n" +
            "            populateDependencyList('moduleAnalysis', reportData.moduleAnalysis);\n" +
            "        }\n" +
            "        function populateDependencyList(containerId, dependencies) {\n" +
            "            const container = document.getElementById(containerId);\n" +
            "            if (!dependencies || dependencies.length === 0) {\n" +
            "                container.innerHTML = '<p>No dependencies found</p>';\n" +
            "                return;\n" +
            "            }\n" +
            "            container.innerHTML = dependencies.map(dep => `\n" +
            "                <div class=\"dependency-item\">\n" +
            "                    <span class=\"dependency-source\">${dep.source || 'Unknown'}</span>\n" +
            "                    <span class=\"dependency-arrow\">→</span>\n" +
            "                    <span class=\"dependency-target\">${dep.target || 'Unknown'}</span>\n" +
            "                </div>\n" +
            "            `).join('');\n" +
            "        }\n" +
            "        function populateMetadata() {\n" +
            "            const metadata = reportData.metadata;\n" +
            "            const metadataContainer = document.getElementById('metadata');\n" +
            "            const items = [\n" +
            "                { label: 'JAR Path', value: metadata.jarPath },\n" +
            "                { label: 'Analysis Date', value: metadata.analysisDate },\n" +
            "                { label: 'JDeps Version', value: metadata.jdepsVersion },\n" +
            "                { label: 'Java Version', value: metadata.javaVersion }\n" +
            "            ];\n" +
            "            metadataContainer.innerHTML = items.map(item => `\n" +
            "                <div class=\"metadata-item\">\n" +
            "                    <span class=\"metadata-label\">${item.label}</span>\n" +
            "                    <span class=\"metadata-value\">${item.value}</span>\n" +
            "                </div>\n" +
            "            `).join('');\n" +
            "        }\n" +
            "        function createDependencyChart() {\n" +
            "            const ctx = document.getElementById('dependencyChart').getContext('2d');\n" +
            "            const stats = reportData.dependencyStats;\n" +
            "            new Chart(ctx, {\n" +
            "                type: 'doughnut',\n" +
            "                data: {\n" +
            "                    labels: ['JDK Modules', 'External Libraries', 'Internal Packages'],\n" +
            "                    datasets: [{\n" +
            "                        data: [stats.uniqueTargets - 2, 2, stats.uniquePackages],\n" +
            "                        backgroundColor: ['#3498db', '#e74c3c', '#2ecc71'],\n" +
            "                        borderWidth: 0\n" +
            "                    }]\n" +
            "                },\n" +
            "                options: { responsive: true, plugins: { legend: { position: 'bottom' } } }\n" +
            "            });\n" +
            "        }\n" +
            "        function showTab(tabName) {\n" +
            "            document.querySelectorAll('.tab-content').forEach(tab => tab.classList.remove('active'));\n" +
            "            document.querySelectorAll('.tab-button').forEach(button => button.classList.remove('active'));\n" +
            "            document.getElementById(tabName).classList.add('active');\n" +
            "            event.target.classList.add('active');\n" +
            "        }\n" +
            "        document.addEventListener('DOMContentLoaded', initializeReport);\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }
    
    public static void main(String[] args) {
        try {
            String projectPath = System.getProperty("user.dir");
            String jarPath = "build/simple-jdeps-test.jar";
            String outputDir = "jdeps-report";
            
            JDepsAnalyzer analyzer = new JDepsAnalyzer(projectPath);
            analyzer.generateReport(jarPath, outputDir);
            
            System.out.println("Report generated successfully!");
            System.out.println("Open jdeps-report/jdeps-report.html in your browser to view the report.");
            
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
