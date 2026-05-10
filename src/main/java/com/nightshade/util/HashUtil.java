package com.nightshade.util;

/**
 * Generates deterministic, human-unguessable identifier replacements.
 *
 * The replacement is a prefix + 7-character hash using an ambiguity-free
 * character set (no i, l, o which look like 1, 1, 0).
 *
 * Design: We use Java's built-in hashCode + manual scrambling — no external
 * library needed. The sessionSalt ensures different runs produce different
 * outputs, preventing adaptive attacks.
 */
public final class HashUtil {

    // Ambiguity-free lowercase letters — omit i, l, o which look like digits
    private static final String CHARS = "abcdefghjkmnpqrstuvwxyz";
    private static final int CHARS_LEN = CHARS.length();

    private HashUtil() {} // utility class — no instantiation

    /**
     * Generates a replacement name for the given original identifier.
     *
     * @param original    The original identifier name
     * @param saltedScope sessionSalt + scopePath — ensures uniqueness per run + scope
     * @return Replacement like "v_xkm3ab7" — valid Java identifier, never a keyword
     */
    public static String generateReplacement(String original, String saltedScope) {
        String combined = original + "\u0000" + saltedScope;
        int hash = combined.hashCode() & Integer.MAX_VALUE;

        // Secondary scramble using FNV-1a to reduce clustering
        hash = fnv1a(combined);

        StringBuilder sb = new StringBuilder("v_");
        int h = hash;
        for (int i = 0; i < 7; i++) {
            sb.append(CHARS.charAt((h & Integer.MAX_VALUE) % CHARS_LEN));
            // Advance with mixing to avoid patterns
            h = (h * 1664525 + 1013904223); // LCG constants
        }
        return sb.toString();
    }

    private static int fnv1a(String s) {
        int hash = 0x811c9dc5;
        for (char c : s.toCharArray()) {
            hash ^= c;
            hash *= 0x01000193;
        }
        return hash & Integer.MAX_VALUE;
    }
}
