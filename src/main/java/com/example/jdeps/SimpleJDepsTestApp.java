package com.example.jdeps;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

/**
 * Simple JDeps test application that only uses JDK classes
 * This version doesn't require external dependencies
 */
public class SimpleJDepsTestApp {

    private final Map<String, Object> dataStore;

    public SimpleJDepsTestApp() {
        super();
        this.dataStore = new ConcurrentHashMap<>();
    }

    public static void main(String[] args) {
        System.out.println("Starting Simple JDeps Test Application...");
        
        SimpleJDepsTestApp app = new SimpleJDepsTestApp();
        app.demonstrateJdkDependencies();
        
        System.out.println("Application completed successfully!");
    }

    /**
     * Demonstrates various JDK dependencies that can be analyzed by JDeps
     */
    public void demonstrateJdkDependencies() {
        // Test java.util package
        testCollections();
        
        // Test java.time package
        testDateTime();
        
        // Test java.util.concurrent package
        testConcurrency();
        
        // Test java.util.stream package
        testStreams();
        
        // Test java.nio package
        testNio();
        
        // Test java.util.regex package
        testRegex();
    }

    private void testCollections() {
        System.out.println("\n=== Testing java.util Collections ===");
        
        List<String> list = new ArrayList<>();
        list.add("apple");
        list.add("banana");
        list.add("cherry");
        
        Set<String> set = new HashSet<>(list);
        Map<String, Integer> map = new HashMap<>();
        
        for (String item : list) {
            map.put(item, item.length());
        }
        
        System.out.println("List: " + list);
        System.out.println("Set: " + set);
        System.out.println("Map: " + map);
        
        dataStore.put("collections-test", map);
    }

    private void testDateTime() {
        System.out.println("\n=== Testing java.time Package ===");
        
        LocalDateTime now = LocalDateTime.now();
        System.out.println("Current time: " + now);
        
        LocalDateTime future = now.plusDays(7);
        System.out.println("One week from now: " + future);
        
        dataStore.put("current-time", now.toString());
    }

    private void testConcurrency() {
        System.out.println("\n=== Testing java.util.concurrent Package ===");
        
        ConcurrentHashMap<String, String> concurrentMap = new ConcurrentHashMap<>();
        concurrentMap.put("key1", "value1");
        concurrentMap.put("key2", "value2");
        concurrentMap.put("key3", "value3");
        
        System.out.println("Concurrent map size: " + concurrentMap.size());
        System.out.println("Concurrent map contents: " + concurrentMap);
        
        dataStore.put("concurrent-data", concurrentMap);
    }

    private void testStreams() {
        System.out.println("\n=== Testing java.util.stream Package ===");
        
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        
        List<Integer> evenNumbers = numbers.stream()
            .filter(n -> n % 2 == 0)
            .collect(Collectors.toList());
        
        int sum = numbers.stream()
            .mapToInt(Integer::intValue)
            .sum();
        
        long count = numbers.stream()
            .filter(n -> n > 5)
            .count();
        
        System.out.println("Original numbers: " + numbers);
        System.out.println("Even numbers: " + evenNumbers);
        System.out.println("Sum: " + sum);
        System.out.println("Count > 5: " + count);
        
        dataStore.put("stream-results", evenNumbers);
    }

    private void testNio() {
        System.out.println("\n=== Testing java.nio Package ===");
        
        try {
            Path tempFile = Files.createTempFile("jdeps-test", ".txt");
            String content = "Hello JDeps! Testing NIO operations.";
            
            Files.write(tempFile, content.getBytes());
            
            List<String> lines = Files.readAllLines(tempFile);
            System.out.println("File content: " + lines);
            
            long fileSize = Files.size(tempFile);
            System.out.println("File size: " + fileSize + " bytes");
            
            // Clean up
            Files.deleteIfExists(tempFile);
            
            dataStore.put("nio-test", "success");
            
        } catch (IOException e) {
            System.err.println("NIO operation error: " + e.getMessage());
            dataStore.put("nio-test", "failed");
        }
    }

    private void testRegex() {
        System.out.println("\n=== Testing java.util.regex Package ===");
        
        String text = "The quick brown fox jumps over the lazy dog. Numbers: 123, 456, 789.";
        
        // Find all words
        java.util.regex.Pattern wordPattern = java.util.regex.Pattern.compile("\\b\\w+\\b");
        java.util.regex.Matcher wordMatcher = wordPattern.matcher(text);
        
        List<String> words = new ArrayList<>();
        while (wordMatcher.find()) {
            words.add(wordMatcher.group());
        }
        
        // Find all numbers
        java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile("\\d+");
        java.util.regex.Matcher numberMatcher = numberPattern.matcher(text);
        
        List<String> numbers = new ArrayList<>();
        while (numberMatcher.find()) {
            numbers.add(numberMatcher.group());
        }
        
        System.out.println("Found " + words.size() + " words");
        System.out.println("Found numbers: " + numbers);
        
        dataStore.put("regex-words", words.size());
        dataStore.put("regex-numbers", numbers);
    }

    public Map<String, Object> getDataStore() {
        return new HashMap<>(dataStore);
    }
}
