package com.example.jdeps;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main application class for testing JDeps dependency analysis.
 * This class uses various external libraries and JDK modules to demonstrate
 * dependency relationships that can be analyzed using JDeps.
 */
public class JDepsTestApp {

    private final ObjectMapper objectMapper;
    private final Map<String, Object> dataStore;

    public JDepsTestApp() {
        super();
        this.objectMapper = new ObjectMapper();
        this.dataStore = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        System.out.println("Starting JDeps Test Application...");
        
        JDepsTestApp app = new JDepsTestApp();
        app.demonstrateDependencies();
        
        System.out.println("Application completed successfully!");
    }

    /**
     * Demonstrates various dependencies that can be analyzed by JDeps
     */
    public void demonstrateDependencies() {
        // Test Apache Commons Lang dependency
        testApacheCommonsLang();
        
        // Test Jackson JSON processing dependency
        testJacksonJsonProcessing();
        
        // Test JDK internal dependencies
        testJdkDependencies();
        
        // Test data processing
        testDataProcessing();
    }

    /**
     * Tests Apache Commons Lang functionality
     */
    private void testApacheCommonsLang() {
        System.out.println("\n=== Testing Apache Commons Lang Dependencies ===");
        
        String testString = "  Hello JDeps World  ";
        String result = StringUtils.trim(testString);
        System.out.println("Original: '" + testString + "'");
        System.out.println("Trimmed: '" + result + "'");
        
        boolean isEmpty = StringUtils.isEmpty("");
        boolean isBlank = StringUtils.isBlank("   ");
        System.out.println("Empty string check: " + isEmpty);
        System.out.println("Blank string check: " + isBlank);
        
        String reversed = StringUtils.reverse("JDeps");
        System.out.println("Reversed 'JDeps': " + reversed);
    }

    /**
     * Tests Jackson JSON processing functionality
     */
    private void testJacksonJsonProcessing() {
        System.out.println("\n=== Testing Jackson JSON Dependencies ===");
        
        try {
            // Create a sample object
            Map<String, Object> sampleData = new HashMap<>();
            sampleData.put("name", "JDeps Test");
            sampleData.put("version", "1.0");
            sampleData.put("timestamp", LocalDateTime.now().toString());
            sampleData.put("dependencies", Arrays.asList("commons-lang3", "jackson-databind"));
            
            // Convert to JSON
            String jsonString = objectMapper.writeValueAsString(sampleData);
            System.out.println("JSON Output: " + jsonString);
            
            // Parse JSON back
            JsonNode jsonNode = objectMapper.readTree(jsonString);
            System.out.println("Parsed name: " + jsonNode.get("name").asText());
            
            dataStore.put("sample", sampleData);
            
        } catch (IOException e) {
            System.err.println("Error processing JSON: " + e.getMessage());
        }
    }

    /**
     * Tests JDK internal dependencies
     */
    private void testJdkDependencies() {
        System.out.println("\n=== Testing JDK Dependencies ===");
        
        // java.time package
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Current time: " + now);
        
        // java.util.concurrent package
        ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put("key1", "value1");
        concurrentMap.put("key2", "value2");
        System.out.println("Concurrent map size: " + concurrentMap.size());
        
        // java.util.stream package
        List<String> items = Arrays.asList("apple", "banana", "cherry", "date");
        long count = items.stream()
                .filter(item -> item.length() > 4)
                .count();
        System.out.println("Items with length > 4: " + count);
    }

    /**
     * Tests data processing functionality
     */
    private void testDataProcessing() {
        System.out.println("\n=== Testing Data Processing ===");
        
        dataStore.put("counter", 42);
        dataStore.put("message", "Hello from JDeps test");
        
        System.out.println("Data store contents:");
        dataStore.forEach((key, value) -> 
            System.out.println("  " + key + " -> " + value));
    }

    /**
     * Gets the current data store
     * @return the data store map
     */
    public Map<String, Object> getDataStore() {
        return new HashMap<>(dataStore);
    }

    /**
     * Gets the object mapper instance
     * @return the ObjectMapper instance
     */
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
}
