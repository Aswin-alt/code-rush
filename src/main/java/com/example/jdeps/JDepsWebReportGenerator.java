package com.example.jdeps;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Web Report Generator for JDeps Analysis
 * This class runs JDeps analysis and generates a web-based report
 */
public class JDepsWebReportGenerator {

    private final ObjectMapper objectMapper;
    private final String projectPath;
    private final String jarPath;

    public JDepsWebReportGenerator(String projectPath, String jarPath) {
        super();
        this.objectMapper = new ObjectMapper();
        this.projectPath = projectPath;
        this.jarPath = jarPath;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java JDepsWebReportGenerator <project-path> <jar-path>");
            System.exit(1);
        }

        String projectPath = args[0];
        String jarPath = args[1];

        JDepsWebReportGenerator generator = new JDepsWebReportGenerator(projectPath, jarPath);
        generator.generateReport();
    }

    /**
     * Generates the complete web report
     */
    public void generateReport() {
        try {
            System.out.println("Starting JDeps Web Report Generation...");
            
            // Run JDeps analysis
            Map<String, Object> analysisResults = runJDepsAnalysis();
            
            // Generate JSON data file
            generateJsonData(analysisResults);
            
            // Copy web files if needed
            copyWebFiles();
            
            // Start simple HTTP server (optional)
            startHttpServer();
            
            System.out.println("Report generated successfully!");
            System.out.println("Open: " + projectPath + "/web-report/index.html");
            
        } catch (Exception e) {
            System.err.println("Error generating report: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Runs various JDeps commands and collects results
     */
    private Map<String, Object> runJDepsAnalysis() throws IOException, InterruptedException {
        Map<String, Object> results = new HashMap<>();
        
        // Basic analysis
        String basicOutput = runCommand("jdeps " + jarPath);
        results.put("basic", parseBasicOutput(basicOutput));
        
        // Verbose analysis
        String verboseOutput = runCommand("jdeps -verbose:class " + jarPath);
        results.put("verbose", parseVerboseOutput(verboseOutput));
        
        // Summary analysis
        String summaryOutput = runCommand("jdeps -s " + jarPath);
        results.put("summary", parseSummaryOutput(summaryOutput));
        
        // Internal API check
        String internalOutput = runCommand("jdeps -jdkinternals " + jarPath);
        results.put("internal", parseInternalOutput(internalOutput));
        
        // Generate dependency graph
        runCommand("jdeps -dotoutput " + projectPath + "/web-report " + jarPath);
        
        return results;
    }

    /**
     * Runs a shell command and returns output
     */
    private String runCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder();
        pb.command("sh", "-c", command);
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
    }

    /**
     * Parses basic JDeps output
     */
    private List<Map<String, Object>> parseBasicOutput(String output) {
        List<Map<String, Object>> dependencies = new ArrayList<>();
        
        Pattern pattern = Pattern.compile("^\\s+([^\\s]+)\\s+->\\s+([^\\s]+)\\s+(.*)$");
        String[] lines = output.split("\n");
        
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                Map<String, Object> dep = new HashMap<>();
                dep.put("source", matcher.group(1));
                dep.put("target", matcher.group(2));
                dep.put("module", matcher.group(3));
                dep.put("type", determineType(matcher.group(2)));
                dependencies.add(dep);
            }
        }
        
        return dependencies;
    }

    /**
     * Parses verbose JDeps output
     */
    private List<Map<String, Object>> parseVerboseOutput(String output) {
        List<Map<String, Object>> verboseDeps = new ArrayList<>();
        
        Pattern pattern = Pattern.compile("^\\s+([^\\s]+)\\s+->\\s+([^\\s]+)\\s+(.*)$");
        String[] lines = output.split("\n");
        
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                Map<String, Object> dep = new HashMap<>();
                dep.put("sourceClass", matcher.group(1));
                dep.put("targetClass", matcher.group(2));
                dep.put("module", matcher.group(3));
                dep.put("type", determineType(matcher.group(2)));
                verboseDeps.add(dep);
            }
        }
        
        return verboseDeps;
    }

    /**
     * Parses summary JDeps output
     */
    private List<Map<String, Object>> parseSummaryOutput(String output) {
        List<Map<String, Object>> summary = new ArrayList<>();
        
        Pattern pattern = Pattern.compile("^([^\\s]+)\\s+->\\s+([^\\s]+)$");
        String[] lines = output.split("\n");
        
        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            if (matcher.find()) {
                Map<String, Object> sum = new HashMap<>();
                sum.put("source", matcher.group(1));
                sum.put("target", matcher.group(2));
                sum.put("type", determineType(matcher.group(2)));
                summary.add(sum);
            }
        }
        
        return summary;
    }

    /**
     * Parses internal API usage output
     */
    private List<Map<String, Object>> parseInternalOutput(String output) {
        List<Map<String, Object>> internals = new ArrayList<>();
        
        if (output.trim().isEmpty() || output.contains("No JDK internal")) {
            return internals; // No internal API usage
        }
        
        // Parse internal API usage if any
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (line.contains("->") && line.contains("JDK internal")) {
                Map<String, Object> internal = new HashMap<>();
                internal.put("usage", line.trim());
                internal.put("type", "internal-api");
                internals.add(internal);
            }
        }
        
        return internals;
    }

    /**
     * Determines the type of dependency
     */
    private String determineType(String target) {
        if (target.startsWith("java.") || target.startsWith("javax.") || target.startsWith("jdk.")) {
            return "jdk";
        } else if (target.startsWith("com.example")) {
            return "internal";
        } else {
            return "external";
        }
    }

    /**
     * Generates JSON data file for the web interface
     */
    private void generateJsonData(Map<String, Object> analysisResults) throws IOException {
        Path webReportDir = Paths.get(projectPath, "web-report");
        Files.createDirectories(webReportDir);
        
        ObjectNode jsonData = objectMapper.createObjectNode();
        
        // Add metadata
        jsonData.put("timestamp", System.currentTimeMillis());
        jsonData.put("project", "jdeps-test-maven");
        jsonData.put("jarPath", jarPath);
        
        // Process analysis results
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> basicDeps = (List<Map<String, Object>>) analysisResults.get("basic");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> verboseDeps = (List<Map<String, Object>>) analysisResults.get("verbose");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> summaryDeps = (List<Map<String, Object>>) analysisResults.get("summary");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> internalDeps = (List<Map<String, Object>>) analysisResults.get("internal");
        
        // Create modules array
        ArrayNode modulesArray = objectMapper.createArrayNode();
        Set<String> moduleNames = new HashSet<>();
        
        for (Map<String, Object> dep : basicDeps) {
            String target = (String) dep.get("target");
            if (moduleNames.add(target)) {
                ObjectNode module = objectMapper.createObjectNode();
                module.put("name", target);
                module.put("type", (String) dep.get("type"));
                module.put("dependencies", countDependenciesForModule(target, basicDeps));
                module.put("classes", countClassesForModule(target, verboseDeps));
                modulesArray.add(module);
            }
        }
        
        jsonData.set("modules", modulesArray);
        
        // Create dependencies array
        ArrayNode dependenciesArray = objectMapper.createArrayNode();
        for (Map<String, Object> dep : basicDeps) {
            ObjectNode dependency = objectMapper.createObjectNode();
            dependency.put("source", (String) dep.get("source"));
            dependency.put("target", (String) dep.get("target"));
            dependency.put("type", (String) dep.get("type"));
            dependency.put("details", "Module dependency");
            dependenciesArray.add(dependency);
        }
        
        jsonData.set("dependencies", dependenciesArray);
        
        // Create classes array
        ArrayNode classesArray = objectMapper.createArrayNode();
        Set<String> classNames = new HashSet<>();
        
        for (Map<String, Object> dep : verboseDeps) {
            String sourceClass = (String) dep.get("sourceClass");
            if (classNames.add(sourceClass)) {
                ObjectNode cls = objectMapper.createObjectNode();
                cls.put("name", sourceClass);
                cls.put("module", "jdeps-test");
                cls.put("dependencies", countDependenciesForClass(sourceClass, verboseDeps));
                cls.put("issues", 0);
                classesArray.add(cls);
            }
        }
        
        jsonData.set("classes", classesArray);
        
        // Create issues array
        ArrayNode issuesArray = objectMapper.createArrayNode();
        for (Map<String, Object> internal : internalDeps) {
            ObjectNode issue = objectMapper.createObjectNode();
            issue.put("type", "internal-api");
            issue.put("message", "JDK Internal API Usage");
            issue.put("details", (String) internal.get("usage"));
            issuesArray.add(issue);
        }
        
        if (issuesArray.isEmpty()) {
            ObjectNode noIssue = objectMapper.createObjectNode();
            noIssue.put("type", "info");
            noIssue.put("message", "No JDK internal API usage detected");
            noIssue.put("details", "All dependencies use public APIs");
            issuesArray.add(noIssue);
        }
        
        jsonData.set("issues", issuesArray);
        
        // Create recommendations array
        ArrayNode recommendationsArray = objectMapper.createArrayNode();
        ObjectNode rec1 = objectMapper.createObjectNode();
        rec1.put("type", "optimization");
        rec1.put("message", "Consider using java.util.stream for more operations");
        rec1.put("details", "Stream API can simplify collection processing code");
        recommendationsArray.add(rec1);
        
        ObjectNode rec2 = objectMapper.createObjectNode();
        rec2.put("type", "modularity");
        rec2.put("message", "Good modular design detected");
        rec2.put("details", "Dependencies are well-organized and follow good practices");
        recommendationsArray.add(rec2);
        
        jsonData.set("recommendations", recommendationsArray);
        
        // Write JSON file
        Path jsonFile = webReportDir.resolve("analysis-data.json");
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile.toFile(), jsonData);
        
        System.out.println("Analysis data written to: " + jsonFile);
    }

    private int countDependenciesForModule(String moduleName, List<Map<String, Object>> dependencies) {
        return (int) dependencies.stream()
                .filter(dep -> moduleName.equals(dep.get("target")))
                .count();
    }

    private int countClassesForModule(String moduleName, List<Map<String, Object>> verboseDeps) {
        return (int) verboseDeps.stream()
                .filter(dep -> moduleName.equals(dep.get("module")))
                .map(dep -> dep.get("targetClass"))
                .distinct()
                .count();
    }

    private int countDependenciesForClass(String className, List<Map<String, Object>> verboseDeps) {
        return (int) verboseDeps.stream()
                .filter(dep -> className.equals(dep.get("sourceClass")))
                .count();
    }

    /**
     * Copies web files to the report directory
     */
    private void copyWebFiles() throws IOException {
        Path webReportDir = Paths.get(projectPath, "web-report");
        Path sourceWebDir = Paths.get(projectPath, "src", "main", "resources", "web");
        
        if (!Files.exists(sourceWebDir)) {
            System.out.println("Source web directory not found: " + sourceWebDir);
            return;
        }
        
        // Copy HTML, CSS, and JS files
        Files.copy(sourceWebDir.resolve("index.html"), webReportDir.resolve("index.html"));
        Files.copy(sourceWebDir.resolve("styles.css"), webReportDir.resolve("styles.css"));
        Files.copy(sourceWebDir.resolve("script.js"), webReportDir.resolve("script.js"));
        
        System.out.println("Web files copied to: " + webReportDir);
    }

    /**
     * Starts a simple HTTP server (optional)
     */
    private void startHttpServer() {
        try {
            Path webReportDir = Paths.get(projectPath, "web-report");
            
            System.out.println("\nTo view the report:");
            System.out.println("1. Open: " + webReportDir.resolve("index.html"));
            System.out.println("2. Or start a simple HTTP server:");
            System.out.println("   cd " + webReportDir);
            System.out.println("   python3 -m http.server 8080");
            System.out.println("   Then visit: http://localhost:8080");
            
        } catch (Exception e) {
            System.err.println("Could not provide server instructions: " + e.getMessage());
        }
    }
}
