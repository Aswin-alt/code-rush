package com.example.jdeps;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Utility class demonstrating various dependencies for JDeps analysis
 */
public class DependencyTestUtil {

    public DependencyTestUtil() {
        super();
    }

    /**
     * Tests file system operations using java.nio
     */
    public static void testFileOperations() {
        System.out.println("\n=== Testing File System Dependencies ===");
        
        try {
            Path tempDir = Files.createTempDirectory("jdeps-test");
            System.out.println("Created temp directory: " + tempDir);
            
            Path testFile = tempDir.resolve("test.txt");
            Files.write(testFile, "Hello JDeps!".getBytes());
            
            List<String> lines = Files.readAllLines(testFile);
            System.out.println("File contents: " + lines);
            
            // Clean up
            Files.deleteIfExists(testFile);
            Files.deleteIfExists(tempDir);
            
        } catch (IOException e) {
            System.err.println("File operation error: " + e.getMessage());
        }
    }

    /**
     * Tests Apache Commons utilities
     */
    public static void testCommonsUtilities() {
        System.out.println("\n=== Testing Commons Utilities ===");
        
        // Random string generation
        String randomString = RandomStringUtils.randomAlphabetic(10);
        System.out.println("Random string: " + randomString);
        
        // Number utilities
        boolean isNumber = NumberUtils.isCreatable("123.45");
        System.out.println("Is '123.45' a number? " + isNumber);
        
        int maxValue = NumberUtils.max(10, 20, 5, 15);
        System.out.println("Max value: " + maxValue);
    }

    /**
     * Tests collection operations with streams
     */
    public static void testCollectionOperations() {
        System.out.println("\n=== Testing Collection Dependencies ===");
        
        List<Person> people = Arrays.asList(
            new Person("Alice", 25),
            new Person("Bob", 30),
            new Person("Charlie", 35),
            new Person("Diana", 28)
        );
        
        // Stream operations
        List<String> names = people.stream()
            .filter(p -> p.getAge() > 26)
            .map(Person::getName)
            .sorted()
            .collect(Collectors.toList());
        
        System.out.println("Names of people older than 26: " + names);
        
        // Group by age range
        Map<String, List<Person>> ageGroups = people.stream()
            .collect(Collectors.groupingBy(p -> p.getAge() < 30 ? "Young" : "Mature"));
        
        System.out.println("Age groups: " + ageGroups);
    }

    /**
     * Tests regex operations
     */
    public static void testRegexOperations() {
        System.out.println("\n=== Testing Regex Dependencies ===");
        
        String text = "Contact us at support@example.com or sales@company.org";
        String emailPattern = "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b";
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(emailPattern);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        
        System.out.println("Found emails:");
        while (matcher.find()) {
            System.out.println("  " + matcher.group());
        }
    }

    /**
     * Simple Person class for testing
     */
    public static class Person {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            super();
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public int getAge() { return age; }

        @Override
        public String toString() {
            return name + "(" + age + ")";
        }
    }
}
