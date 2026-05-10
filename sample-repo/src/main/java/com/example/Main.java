package com.example;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello, World!");
        String test = helper("Nightshade");
        System.out.println(test);
    }

    private static String helper(String input) {
        return "Processed " + input;
    }
}
