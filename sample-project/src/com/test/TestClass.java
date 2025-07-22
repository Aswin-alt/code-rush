package com.test;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

public class TestClass {
    public TestClass() {
        super();
    }

    public static void main(String[] args) {
        List<String> items = Arrays.asList("apple", "banana", "cherry");
        
        List<String> filtered = items.stream()
            .filter(s -> s.length() > 5)
            .collect(Collectors.toList());
        
        System.out.println("Filtered items: " + filtered);
        System.out.println("Current time: " + LocalDateTime.now());
    }
}
