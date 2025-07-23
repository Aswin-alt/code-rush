package com.example.jdeps;

import java.io.File;
import java.util.Map;

/**
 * Demo application showing ASM integration capabilities
 */
public class ASMIntegrationDemo {

    public ASMIntegrationDemo() {
        super();
    }
    
    public static void main(String[] args) {
        System.out.println("🔍 ASM Integration Demo");
        System.out.println("========================");
        
        try {
            // First, compile the project to have classes to analyze
            System.out.println("1. Checking for compiled classes...");
            File classesDir = new File("target/classes");
            
            if (!classesDir.exists()) {
                System.out.println("❌ No compiled classes found. Please run 'mvn compile' first.");
                return;
            }
            
            // Run ASM analysis on compiled classes
            System.out.println("2. Running ASM bytecode analysis...");
            Map<String, ASMBytecodeAnalyzer.ClassAnalysisResult> results = 
                ASMBytecodeAnalyzer.analyzeDirectory(classesDir);
            
            if (results.isEmpty()) {
                System.out.println("❌ No classes found for analysis.");
                return;
            }
            
            System.out.println("✅ Analyzed " + results.size() + " classes");
            System.out.println();
            
            // Display summary statistics
            displaySummaryStats(results);
            
            // Display top complex classes
            displayTopComplexClasses(results);
            
            // Generate HTML report
            System.out.println("3. Generating enhanced HTML report...");
            String htmlReport = ASMBytecodeAnalyzer.generateHTMLReport(results);
            
            File reportFile = new File("asm-enhanced-report.html");
            try (java.io.FileWriter writer = new java.io.FileWriter(reportFile)) {
                writer.write(htmlReport);
            }
            
            System.out.println("✅ Enhanced report generated: " + reportFile.getAbsolutePath());
            System.out.println();
            System.out.println("🌐 Open the report in your browser to see detailed ASM analysis!");
            
        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void displaySummaryStats(Map<String, ASMBytecodeAnalyzer.ClassAnalysisResult> results) {
        System.out.println("📊 Summary Statistics:");
        System.out.println("---------------------");
        
        int totalClasses = results.size();
        int totalMethods = results.values().stream()
                .mapToInt(ASMBytecodeAnalyzer.ClassAnalysisResult::getTotalMethods).sum();
        int totalFields = results.values().stream()
                .mapToInt(ASMBytecodeAnalyzer.ClassAnalysisResult::getTotalFields).sum();
        double avgComplexity = results.values().stream()
                .mapToDouble(ASMBytecodeAnalyzer.ClassAnalysisResult::getAverageMethodComplexity)
                .average().orElse(0.0);
        
        System.out.println("📦 Total Classes: " + totalClasses);
        System.out.println("🔧 Total Methods: " + totalMethods);
        System.out.println("📋 Total Fields: " + totalFields);
        System.out.println("📈 Average Method Complexity: " + String.format("%.2f", avgComplexity));
        System.out.println();
    }
    
    private static void displayTopComplexClasses(Map<String, ASMBytecodeAnalyzer.ClassAnalysisResult> results) {
        System.out.println("🏆 Top Complex Classes:");
        System.out.println("-----------------------");
        
        results.values().stream()
                .filter(r -> r.getTotalMethods() > 0)
                .sorted((a, b) -> Double.compare(b.getAverageMethodComplexity(), a.getAverageMethodComplexity()))
                .limit(5)
                .forEach(result -> {
                    String className = result.getClassName().replace("/", ".");
                    System.out.printf("🔥 %s (%.2f complexity, %d methods)%n", 
                            className, 
                            result.getAverageMethodComplexity(), 
                            result.getTotalMethods());
                });
        System.out.println();
    }
}
