package com.example.jdeps;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Advanced bytecode analyzer using ASM framework.
 * Provides deep analysis of Java classes including method calls, field access, and complexity metrics.
 */
public class ASMBytecodeAnalyzer {

    public ASMBytecodeAnalyzer() {
        super();
    }
    
    public static class ClassAnalysisResult {
        private String className;
        private String superClass;
        private Set<String> interfaces = new HashSet<>();
        private Set<String> methodCalls = new HashSet<>();
        private Set<String> fieldAccess = new HashSet<>();
        private Set<String> annotations = new HashSet<>();
        private Map<String, Integer> methodComplexity = new HashMap<>();
        private int totalMethods = 0;
        private int totalFields = 0;
        private boolean isAbstract = false;
        private boolean isInterface = false;
        private boolean isFinal = false;

        public ClassAnalysisResult() {
            super();
        }
        
        // Getters and setters
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public String getSuperClass() { return superClass; }
        public void setSuperClass(String superClass) { this.superClass = superClass; }
        
        public Set<String> getInterfaces() { return interfaces; }
        public Set<String> getMethodCalls() { return methodCalls; }
        public Set<String> getFieldAccess() { return fieldAccess; }
        public Set<String> getAnnotations() { return annotations; }
        public Map<String, Integer> getMethodComplexity() { return methodComplexity; }
        
        public int getTotalMethods() { return totalMethods; }
        public void setTotalMethods(int totalMethods) { this.totalMethods = totalMethods; }
        
        public int getTotalFields() { return totalFields; }
        public void setTotalFields(int totalFields) { this.totalFields = totalFields; }
        
        public boolean isAbstract() { return isAbstract; }
        public void setAbstract(boolean isAbstract) { this.isAbstract = isAbstract; }
        
        public boolean isInterface() { return isInterface; }
        public void setInterface(boolean isInterface) { this.isInterface = isInterface; }
        
        public boolean isFinal() { return isFinal; }
        public void setFinal(boolean isFinal) { this.isFinal = isFinal; }
        
        public int getTotalComplexity() {
            return methodComplexity.values().stream().mapToInt(Integer::intValue).sum();
        }
        
        public double getAverageMethodComplexity() {
            return totalMethods > 0 ? (double) getTotalComplexity() / totalMethods : 0;
        }
    }
    
    /**
     * Analyzes a single class file using ASM
     */
    public static ClassAnalysisResult analyzeClass(InputStream classStream) throws IOException {
        ClassReader classReader = new ClassReader(classStream);
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);
        
        ClassAnalysisResult result = new ClassAnalysisResult();
        result.setClassName(classNode.name);
        result.setSuperClass(classNode.superName);
        result.setAbstract((classNode.access & Opcodes.ACC_ABSTRACT) != 0);
        result.setInterface((classNode.access & Opcodes.ACC_INTERFACE) != 0);
        result.setFinal((classNode.access & Opcodes.ACC_FINAL) != 0);
        
        // Analyze interfaces
        if (classNode.interfaces != null) {
            result.getInterfaces().addAll(classNode.interfaces);
        }
        
        // Analyze annotations
        if (classNode.visibleAnnotations != null) {
            for (AnnotationNode annotation : classNode.visibleAnnotations) {
                result.getAnnotations().add(annotation.desc);
            }
        }
        
        // Analyze fields
        if (classNode.fields != null) {
            result.setTotalFields(classNode.fields.size());
            for (FieldNode field : classNode.fields) {
                if (field.visibleAnnotations != null) {
                    for (AnnotationNode annotation : field.visibleAnnotations) {
                        result.getAnnotations().add(annotation.desc);
                    }
                }
            }
        }
        
        // Analyze methods
        if (classNode.methods != null) {
            result.setTotalMethods(classNode.methods.size());
            for (MethodNode method : classNode.methods) {
                analyzeMethod(method, result);
            }
        }
        
        return result;
    }
    
    /**
     * Analyzes a single method for complexity and dependencies
     */
    private static void analyzeMethod(MethodNode method, ClassAnalysisResult result) {
        int complexity = calculateCyclomaticComplexity(method);
        result.getMethodComplexity().put(method.name, complexity);
        
        // Analyze method annotations
        if (method.visibleAnnotations != null) {
            for (AnnotationNode annotation : method.visibleAnnotations) {
                result.getAnnotations().add(annotation.desc);
            }
        }
        
        // Analyze method instructions for dependencies
        if (method.instructions != null) {
            for (AbstractInsnNode instruction : method.instructions) {
                analyzeInstruction(instruction, result);
            }
        }
    }
    
    /**
     * Calculates cyclomatic complexity of a method
     */
    private static int calculateCyclomaticComplexity(MethodNode method) {
        int complexity = 1; // Base complexity
        
        if (method.instructions != null) {
            for (AbstractInsnNode instruction : method.instructions) {
                switch (instruction.getOpcode()) {
                    case Opcodes.IFEQ:
                    case Opcodes.IFNE:
                    case Opcodes.IFLT:
                    case Opcodes.IFGE:
                    case Opcodes.IFGT:
                    case Opcodes.IFLE:
                    case Opcodes.IF_ICMPEQ:
                    case Opcodes.IF_ICMPNE:
                    case Opcodes.IF_ICMPLT:
                    case Opcodes.IF_ICMPGE:
                    case Opcodes.IF_ICMPGT:
                    case Opcodes.IF_ICMPLE:
                    case Opcodes.IF_ACMPEQ:
                    case Opcodes.IF_ACMPNE:
                    case Opcodes.IFNULL:
                    case Opcodes.IFNONNULL:
                        complexity++;
                        break;
                    case Opcodes.TABLESWITCH:
                    case Opcodes.LOOKUPSWITCH:
                        // For switch statements, add the number of cases
                        if (instruction instanceof TableSwitchInsnNode) {
                            TableSwitchInsnNode tableSwitch = (TableSwitchInsnNode) instruction;
                            complexity += tableSwitch.labels.size();
                        } else if (instruction instanceof LookupSwitchInsnNode) {
                            LookupSwitchInsnNode lookupSwitch = (LookupSwitchInsnNode) instruction;
                            complexity += lookupSwitch.labels.size();
                        }
                        break;
                }
            }
        }
        
        return complexity;
    }
    
    /**
     * Analyzes individual bytecode instructions for dependencies
     */
    private static void analyzeInstruction(AbstractInsnNode instruction, ClassAnalysisResult result) {
        switch (instruction.getType()) {
            case AbstractInsnNode.METHOD_INSN:
                MethodInsnNode methodInsn = (MethodInsnNode) instruction;
                result.getMethodCalls().add(methodInsn.owner + "." + methodInsn.name + methodInsn.desc);
                break;
            case AbstractInsnNode.FIELD_INSN:
                FieldInsnNode fieldInsn = (FieldInsnNode) instruction;
                result.getFieldAccess().add(fieldInsn.owner + "." + fieldInsn.name);
                break;
            case AbstractInsnNode.TYPE_INSN:
                TypeInsnNode typeInsn = (TypeInsnNode) instruction;
                result.getMethodCalls().add("TYPE:" + typeInsn.desc);
                break;
        }
    }
    
    /**
     * Analyzes all classes in a JAR file
     */
    public static Map<String, ClassAnalysisResult> analyzeJarFile(String jarPath) throws IOException {
        Map<String, ClassAnalysisResult> results = new HashMap<>();
        
        try (JarFile jarFile = new JarFile(jarPath)) {
            Enumeration<JarEntry> entries = jarFile.entries();
            
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                
                if (entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
                    try (InputStream inputStream = jarFile.getInputStream(entry)) {
                        ClassAnalysisResult result = analyzeClass(inputStream);
                        results.put(result.getClassName(), result);
                    } catch (Exception e) {
                        System.err.println("Error analyzing class " + entry.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
        
        return results;
    }
    
    /**
     * Analyzes all classes in a directory
     */
    public static Map<String, ClassAnalysisResult> analyzeDirectory(File directory) throws IOException {
        Map<String, ClassAnalysisResult> results = new HashMap<>();
        analyzeDirectoryRecursive(directory, results);
        return results;
    }
    
    private static void analyzeDirectoryRecursive(File directory, Map<String, ClassAnalysisResult> results) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    analyzeDirectoryRecursive(file, results);
                } else if (file.getName().endsWith(".class")) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        ClassAnalysisResult result = analyzeClass(fis);
                        results.put(result.getClassName(), result);
                    } catch (Exception e) {
                        System.err.println("Error analyzing class " + file.getName() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
    
    /**
     * Generates a comprehensive HTML report from analysis results
     */
    public static String generateHTMLReport(Map<String, ClassAnalysisResult> analysisResults) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n");
        html.append("<title>ASM Bytecode Analysis Report</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 20px; }\n");
        html.append("table { border-collapse: collapse; width: 100%; margin: 20px 0; }\n");
        html.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        html.append("th { background-color: #f2f2f2; }\n");
        html.append(".metric { background-color: #e7f3ff; }\n");
        html.append(".high-complexity { background-color: #ffebee; }\n");
        html.append(".medium-complexity { background-color: #fff3e0; }\n");
        html.append(".low-complexity { background-color: #e8f5e8; }\n");
        html.append("</style>\n</head>\n<body>\n");
        
        html.append("<h1>🔍 ASM Bytecode Analysis Report</h1>\n");
        html.append("<p>Generated on: ").append(new Date()).append("</p>\n");
        
        // Summary statistics
        html.append("<h2>📊 Summary Statistics</h2>\n");
        int totalClasses = analysisResults.size();
        int totalMethods = analysisResults.values().stream().mapToInt(ClassAnalysisResult::getTotalMethods).sum();
        int totalFields = analysisResults.values().stream().mapToInt(ClassAnalysisResult::getTotalFields).sum();
        double avgComplexity = analysisResults.values().stream()
                .mapToDouble(ClassAnalysisResult::getAverageMethodComplexity)
                .average().orElse(0.0);
        
        html.append("<table>\n");
        html.append("<tr><th>Metric</th><th>Value</th></tr>\n");
        html.append("<tr class='metric'><td>Total Classes</td><td>").append(totalClasses).append("</td></tr>\n");
        html.append("<tr class='metric'><td>Total Methods</td><td>").append(totalMethods).append("</td></tr>\n");
        html.append("<tr class='metric'><td>Total Fields</td><td>").append(totalFields).append("</td></tr>\n");
        html.append("<tr class='metric'><td>Average Method Complexity</td><td>").append(String.format("%.2f", avgComplexity)).append("</td></tr>\n");
        html.append("</table>\n");
        
        // Detailed class analysis
        html.append("<h2>🏗️ Class Analysis Details</h2>\n");
        html.append("<table>\n");
        html.append("<tr><th>Class Name</th><th>Type</th><th>Methods</th><th>Fields</th><th>Avg Complexity</th><th>Dependencies</th></tr>\n");
        
        for (ClassAnalysisResult result : analysisResults.values()) {
            String complexityClass = getComplexityClass(result.getAverageMethodComplexity());
            html.append("<tr class='").append(complexityClass).append("'>\n");
            html.append("<td>").append(result.getClassName()).append("</td>\n");
            html.append("<td>");
            if (result.isInterface()) html.append("Interface");
            else if (result.isAbstract()) html.append("Abstract");
            else html.append("Class");
            html.append("</td>\n");
            html.append("<td>").append(result.getTotalMethods()).append("</td>\n");
            html.append("<td>").append(result.getTotalFields()).append("</td>\n");
            html.append("<td>").append(String.format("%.2f", result.getAverageMethodComplexity())).append("</td>\n");
            html.append("<td>").append(result.getMethodCalls().size() + result.getFieldAccess().size()).append("</td>\n");
            html.append("</tr>\n");
        }
        
        html.append("</table>\n");
        html.append("</body>\n</html>");
        
        return html.toString();
    }
    
    private static String getComplexityClass(double complexity) {
        if (complexity > 10) return "high-complexity";
        else if (complexity > 5) return "medium-complexity";
        else return "low-complexity";
    }
    
    /**
     * Example usage and testing method
     */
    public static void main(String[] args) {
        try {
            // Example: Analyze current project's compiled classes
            File classesDir = new File("target/classes");
            if (classesDir.exists()) {
                System.out.println("Analyzing compiled classes...");
                Map<String, ClassAnalysisResult> results = analyzeDirectory(classesDir);
                
                System.out.println("\n=== ASM Analysis Results ===");
                for (ClassAnalysisResult result : results.values()) {
                    System.out.println("Class: " + result.getClassName());
                    System.out.println("  Methods: " + result.getTotalMethods());
                    System.out.println("  Fields: " + result.getTotalFields());
                    System.out.println("  Average Complexity: " + String.format("%.2f", result.getAverageMethodComplexity()));
                    System.out.println("  Dependencies: " + (result.getMethodCalls().size() + result.getFieldAccess().size()));
                    System.out.println();
                }
                
                // Generate HTML report
                String htmlReport = generateHTMLReport(results);
                try (FileWriter writer = new FileWriter("asm-analysis-report.html")) {
                    writer.write(htmlReport);
                    System.out.println("HTML report generated: asm-analysis-report.html");
                }
            } else {
                System.out.println("Please compile the project first: mvn compile");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Comprehensive project insights combining all analysis results
     */
    public static class ProjectInsights {
        private Map<String, ClassAnalysisResult> classResults;
        private PackageInsights packageInsights;
        private SecurityInsights securityInsights;
        private QualityInsights qualityInsights;
        private ImpactAnalysis impactAnalysis;
        
        public ProjectInsights(Map<String, ClassAnalysisResult> classResults) {
            this.classResults = classResults;
            this.packageInsights = analyzePackages(classResults);
            this.securityInsights = analyzeSecurityIssues(classResults);
            this.qualityInsights = analyzeCodeQuality(classResults);
            this.impactAnalysis = analyzeRemovalImpact(classResults);
        }
        
        // Getters
        public Map<String, ClassAnalysisResult> getClassResults() { return classResults; }
        public PackageInsights getPackageInsights() { return packageInsights; }
        public SecurityInsights getSecurityInsights() { return securityInsights; }
        public QualityInsights getQualityInsights() { return qualityInsights; }
        public ImpactAnalysis getImpactAnalysis() { return impactAnalysis; }
    }
    
    public static class PackageInsights {
        private Map<String, Integer> packageClassCount = new HashMap<>();
        private Map<String, Double> packageComplexity = new HashMap<>();
        private Map<String, Set<String>> packageDependencies = new HashMap<>();
        private List<String> mostConnectedPackages = new ArrayList<>();
        
        // Getters
        public Map<String, Integer> getPackageClassCount() { return packageClassCount; }
        public Map<String, Double> getPackageComplexity() { return packageComplexity; }
        public Map<String, Set<String>> getPackageDependencies() { return packageDependencies; }
        public List<String> getMostConnectedPackages() { return mostConnectedPackages; }
    }
    
    public static class SecurityInsights {
        private List<String> reflectionUsage = new ArrayList<>();
        private List<String> serializationClasses = new ArrayList<>();
        private List<String> nativeMethodUsage = new ArrayList<>();
        private List<String> deprecatedApiUsage = new ArrayList<>();
        private int securityScore = 100;
        
        // Getters
        public List<String> getReflectionUsage() { return reflectionUsage; }
        public List<String> getSerializationClasses() { return serializationClasses; }
        public List<String> getNativeMethodUsage() { return nativeMethodUsage; }
        public List<String> getDeprecatedApiUsage() { return deprecatedApiUsage; }
        public int getSecurityScore() { return securityScore; }
    }
    
    public static class QualityInsights {
        private double overallComplexity;
        private List<String> highComplexityClasses = new ArrayList<>();
        private List<String> potentialRefactoringCandidates = new ArrayList<>();
        private int maintainabilityScore = 100;
        private Map<String, Integer> designPatterns = new HashMap<>();
        
        // Getters
        public double getOverallComplexity() { return overallComplexity; }
        public List<String> getHighComplexityClasses() { return highComplexityClasses; }
        public List<String> getPotentialRefactoringCandidates() { return potentialRefactoringCandidates; }
        public int getMaintainabilityScore() { return maintainabilityScore; }
        public Map<String, Integer> getDesignPatterns() { return designPatterns; }
    }
    
    public static class ImpactAnalysis {
        private Map<String, Set<String>> classImpacts = new HashMap<>();
        private Map<String, Integer> removalRiskScores = new HashMap<>();
        private List<String> criticalClasses = new ArrayList<>();
        private Map<String, List<String>> affectedClasses = new HashMap<>();
        
        // Getters
        public Map<String, Set<String>> getClassImpacts() { return classImpacts; }
        public Map<String, Integer> getRemovalRiskScores() { return removalRiskScores; }
        public List<String> getCriticalClasses() { return criticalClasses; }
        public Map<String, List<String>> getAffectedClasses() { return affectedClasses; }
    }
    
    /**
     * Analyze packages for better organization insights
     */
    private static PackageInsights analyzePackages(Map<String, ClassAnalysisResult> classResults) {
        PackageInsights insights = new PackageInsights();
        
        for (ClassAnalysisResult result : classResults.values()) {
            String packageName = getPackageName(result.getClassName());
            
            // Count classes per package
            insights.packageClassCount.merge(packageName, 1, Integer::sum);
            
            // Calculate package complexity
            double complexity = result.getAverageMethodComplexity();
            insights.packageComplexity.merge(packageName, complexity, (old, new_val) -> (old + new_val) / 2);
            
            // Track package dependencies
            Set<String> deps = new HashSet<>();
            for (String call : result.getMethodCalls()) {
                String depPackage = getPackageName(call);
                if (!depPackage.equals(packageName) && !depPackage.startsWith("java.")) {
                    deps.add(depPackage);
                }
            }
            insights.packageDependencies.put(packageName, deps);
        }
        
        // Find most connected packages
        insights.mostConnectedPackages = insights.packageDependencies.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toList());
        
        return insights;
    }
    
    /**
     * Analyze security-related issues
     */
    private static SecurityInsights analyzeSecurityIssues(Map<String, ClassAnalysisResult> classResults) {
        SecurityInsights insights = new SecurityInsights();
        
        for (ClassAnalysisResult result : classResults.values()) {
            String className = result.getClassName();
            
            // Check for reflection usage
            for (String call : result.getMethodCalls()) {
                if (call.contains("java/lang/reflect/") || call.contains("Class.forName")) {
                    insights.reflectionUsage.add(className + " -> " + call);
                    insights.securityScore -= 5;
                }
            }
            
            // Check for serialization
            if (result.getInterfaces().contains("java/io/Serializable")) {
                insights.serializationClasses.add(className);
                insights.securityScore -= 3;
            }
            
            // Check for deprecated APIs (simple heuristic)
            for (String call : result.getMethodCalls()) {
                if (call.contains("deprecated") || call.contains("Date(") || call.contains("Thread.stop")) {
                    insights.deprecatedApiUsage.add(className + " -> " + call);
                    insights.securityScore -= 2;
                }
            }
        }
        
        insights.securityScore = Math.max(0, insights.securityScore);
        return insights;
    }
    
    /**
     * Analyze code quality metrics
     */
    private static QualityInsights analyzeCodeQuality(Map<String, ClassAnalysisResult> classResults) {
        QualityInsights insights = new QualityInsights();
        
        double totalComplexity = 0;
        int classCount = 0;
        
        for (ClassAnalysisResult result : classResults.values()) {
            String className = result.getClassName();
            double complexity = result.getAverageMethodComplexity();
            totalComplexity += complexity;
            classCount++;
            
            // High complexity classes
            if (complexity > 10) {
                insights.highComplexityClasses.add(className + " (complexity: " + String.format("%.2f", complexity) + ")");
                insights.maintainabilityScore -= 10;
            }
            
            // Potential refactoring candidates (large classes with high complexity)
            if (result.getTotalMethods() > 20 && complexity > 8) {
                insights.potentialRefactoringCandidates.add(className);
                insights.maintainabilityScore -= 5;
            }
            
            // Detect potential design patterns
            detectDesignPatterns(result, insights);
        }
        
        insights.overallComplexity = classCount > 0 ? totalComplexity / classCount : 0;
        insights.maintainabilityScore = Math.max(0, insights.maintainabilityScore);
        
        return insights;
    }
    
    /**
     * Analyze impact of removing classes/JARs
     */
    private static ImpactAnalysis analyzeRemovalImpact(Map<String, ClassAnalysisResult> classResults) {
        ImpactAnalysis analysis = new ImpactAnalysis();
        
        // Build dependency graph
        Map<String, Set<String>> dependents = new HashMap<>();
        
        for (ClassAnalysisResult result : classResults.values()) {
            String className = result.getClassName();
            
            for (String call : result.getMethodCalls()) {
                String targetClass = extractClassName(call);
                if (classResults.containsKey(targetClass)) {
                    dependents.computeIfAbsent(targetClass, k -> new HashSet<>()).add(className);
                }
            }
            
            for (String field : result.getFieldAccess()) {
                String targetClass = extractClassName(field);
                if (classResults.containsKey(targetClass)) {
                    dependents.computeIfAbsent(targetClass, k -> new HashSet<>()).add(className);
                }
            }
        }
        
        // Calculate impact scores
        for (String className : classResults.keySet()) {
            Set<String> impacts = dependents.getOrDefault(className, new HashSet<>());
            analysis.classImpacts.put(className, impacts);
            
            // Risk score based on number of dependents and their complexity
            int riskScore = impacts.size() * 10;
            for (String dependent : impacts) {
                ClassAnalysisResult depResult = classResults.get(dependent);
                if (depResult != null) {
                    riskScore += (int) depResult.getAverageMethodComplexity();
                }
            }
            
            analysis.removalRiskScores.put(className, riskScore);
            
            // Critical classes (high impact)
            if (riskScore > 50) {
                analysis.criticalClasses.add(className);
            }
            
            // Build affected classes list
            analysis.affectedClasses.put(className, new ArrayList<>(impacts));
        }
        
        return analysis;
    }
    
    /**
     * Simple design pattern detection
     */
    private static void detectDesignPatterns(ClassAnalysisResult result, QualityInsights insights) {
        String className = result.getClassName().toLowerCase();
        
        if (className.contains("factory")) {
            insights.designPatterns.merge("Factory", 1, Integer::sum);
        }
        if (className.contains("singleton")) {
            insights.designPatterns.merge("Singleton", 1, Integer::sum);
        }
        if (className.contains("observer") || className.contains("listener")) {
            insights.designPatterns.merge("Observer", 1, Integer::sum);
        }
        if (className.contains("adapter")) {
            insights.designPatterns.merge("Adapter", 1, Integer::sum);
        }
        if (className.contains("strategy")) {
            insights.designPatterns.merge("Strategy", 1, Integer::sum);
        }
    }
    
    /**
     * Extract package name from class name
     */
    private static String getPackageName(String className) {
        if (className == null || !className.contains("/")) {
            return "default";
        }
        int lastSlash = className.lastIndexOf('/');
        return className.substring(0, lastSlash).replace('/', '.');
    }
    
    /**
     * Extract class name from method/field reference
     */
    private static String extractClassName(String reference) {
        if (reference == null) return "";
        
        // Handle method calls like "com/example/MyClass.method"
        if (reference.contains(".")) {
            return reference.substring(0, reference.lastIndexOf('.'));
        }
        
        // Handle field access like "com/example/MyClass/field"
        if (reference.contains("/")) {
            String[] parts = reference.split("/");
            if (parts.length > 1) {
                return String.join("/", Arrays.copyOf(parts, parts.length - 1));
            }
        }
        
        return reference;
    }
    
    /**
     * Generate comprehensive project insights
     */
    public static ProjectInsights generateProjectInsights(Map<String, ClassAnalysisResult> classResults) {
        return new ProjectInsights(classResults);
    }
}
