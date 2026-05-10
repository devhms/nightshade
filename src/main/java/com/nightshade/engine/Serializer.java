package com.nightshade.engine;

import com.nightshade.model.SourceFile;
import com.nightshade.model.Token;
import com.nightshade.model.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Converts a modified token stream back into source lines.
 *
 * The Serializer reconstructs lines by walking the token list and
 * rebuilding text. For strategies that modify tokens in-place (EntropyScrambler,
 * CommentPoisoner), this reconstructs the file from the modified token values.
 *
 * For strategies that add lines (DeadCodeInjector, WhitespaceDisruptor),
 * those strategies work directly on the SourceFile's line list and bypass
 * the Serializer's token reconstruction.
 */
public class Serializer {

    /**
     * Rebuilds source lines from a token list.
     * Tokens already carry their line numbers — we reconstruct line by line.
     */
    public List<String> serialize(List<Token> tokens) {
        if (tokens.isEmpty()) return new ArrayList<>();

        // Find max line number
        int maxLine = tokens.stream()
            .mapToInt(Token::getLineNumber)
            .max()
            .orElse(1);

        // Group tokens by line
        List<StringBuilder> lineBuilders = new ArrayList<>();
        for (int i = 0; i <= maxLine; i++) {
            lineBuilders.add(new StringBuilder());
        }

        for (Token t : tokens) {
            int lineIdx = Math.min(t.getLineNumber(), maxLine);
            lineBuilders.get(lineIdx).append(t.getValue());
        }

        // Convert to list (skip index 0 since lines are 1-based)
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= maxLine; i++) {
            result.add(lineBuilders.get(i).toString());
        }
        return result;
    }

    /**
     * Applies a token-value mapping to a SourceFile's lines.
     * Used by EntropyScrambler to do direct string replacement
     * when token position tracking is sufficient.
     *
     * @param source  Original source file
     * @param mapping Map of original identifier → replacement
     * @return New line list with replacements applied
     */
    public List<String> applyMapping(SourceFile source, Map<String, String> mapping) {
        List<String> result = new ArrayList<>();
        boolean skipping = false;
        for (String line : source.getObfuscatedLines()) {
            String modified = line;
            String trimmed = line.trim();
            
            if (trimmed.contains("@nightshade:skip")) skipping = true;
            if (trimmed.contains("@nightshade:resume")) skipping = false;

            // Skip renaming on package, import, or skipped lines
            if (skipping || trimmed.startsWith("package ") || trimmed.startsWith("import ")) {
                result.add(line);
                continue;
            }

            // Apply replacements — longer names first to avoid partial matches
                List<String> keys = new ArrayList<>(mapping.keySet());
                keys.sort((a, b) -> b.length() - a.length());
                for (String original : keys) {
                    String replacement = mapping.get(original);
                    // Use word-boundary replacement to avoid partial matches
                    modified = modified.replaceAll(
                        "(?<![a-zA-Z0-9_$])" + java.util.regex.Pattern.quote(original) + "(?![a-zA-Z0-9_$])",
                        java.util.regex.Matcher.quoteReplacement(replacement)
                    );
                }
            result.add(modified);
        }
        return result;
    }
}
