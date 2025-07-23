package com.example.jdeps;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

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
}
