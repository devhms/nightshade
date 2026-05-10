package com.example;

public class HelloWorld {
    // This is a sample comment for testing.
    public static void main(String[] args) {
        String greeting = "Hello, World!";
        System.out.println(greeting);
        int result = calculate(5, 10);
        System.out.println("Result: " + result);
    }
    
    // Another comment
    private static int calculate(int a, int b) {
        if (a < 0) {
            return -1;
        }
        return a + b;
    }
}
