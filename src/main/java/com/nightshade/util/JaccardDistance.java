package com.nightshade.util;

import java.util.*;
import java.util.stream.Collectors;

public class JaccardDistance {

    private final int n;

    public JaccardDistance() {
        this(3);
    }

    public JaccardDistance(int n) {
        this.n = n;
    }

    public double calculate(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Input strings cannot be null");
        }
        if (a.isEmpty() && b.isEmpty()) {
            return 0.0;
        }
        if (a.isEmpty() || b.isEmpty()) {
            return 1.0;
        }

        Set<String> setA = getNGrams(a);
        Set<String> setB = getNGrams(b);

        Set<String> intersection = new HashSet<>(setA);
        intersection.retainAll(setB);

        Set<String> union = new HashSet<>(setA);
        union.addAll(setB);

        if (union.isEmpty()) {
            return 0.0;
        }

        double similarity = (double) intersection.size() / union.size();
        return 1.0 - similarity;
    }

    private Set<String> getNGrams(String text) {
        Set<String> ngrams = new HashSet<>();
        if (text.length() < n) {
            ngrams.add(text);
            return ngrams;
        }

        for (int i = 0; i <= text.length() - n; i++) {
            ngrams.add(text.substring(i, i + n));
        }
        return ngrams;
    }

    public double calculateForFiles(List<String> originalLines, List<String> obfuscatedLines) {
        String original = String.join("\n", originalLines);
        String obfuscated = String.join("\n", obfuscatedLines);
        return calculate(original, obfuscated);
    }

    public static void main(String[] args) {
        JaccardDistance jaccard = new JaccardDistance(3);

        String s1 = "hello world";
        String s2 = "hello world";
        String s3 = "hello there";

        System.out.println("Identical strings: " + jaccard.calculate(s1, s2));
        System.out.println("Different strings: " + jaccard.calculate(s1, s3));
        System.out.println("Empty vs non-empty: " + jaccard.calculate("", s1));
    }
}