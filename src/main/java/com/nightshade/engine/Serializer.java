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
        Lexer lexer = new Lexer();
        for (String line : source.getObfuscatedLines()) {
            String trimmed = line.trim();
            
            if (trimmed.contains("@nightshade:skip")) skipping = true;
            if (trimmed.contains("@nightshade:resume")) skipping = false;

            // Skip renaming on package, import, or skipped lines
            if (skipping || trimmed.startsWith("package ") || trimmed.startsWith("import ")) {
                result.add(line);
                continue;
            }

            List<Token> tokens = lexer.tokenize(List.of(line));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                Token prevToken = previousNonWhitespace(tokens, i);
                Token nextToken = nextNonWhitespace(tokens, i);
                boolean isDotCall = prevToken != null
                    && ".".equals(prevToken.getValue())
                    && token.getType() == TokenType.IDENTIFIER;
                boolean isDotReceiver = token.getType() == TokenType.IDENTIFIER
                    && nextToken != null
                    && ".".equals(nextToken.getValue());
                if (token.getType() == TokenType.IDENTIFIER && !isDotCall && !isDotReceiver) {
                    String replacement = mapping.get(token.getValue());
                    if (replacement != null) {
                        sb.append(replacement);
                    } else {
                        sb.append(token.getValue());
                    }
                } else {
                    sb.append(token.getValue());
                }
            }
            result.add(sb.toString());
        }
        return result;
    }

    private Token previousNonWhitespace(List<Token> tokens, int index) {
        for (int i = index - 1; i >= 0; i--) {
            Token token = tokens.get(i);
            if (token.getType() != TokenType.WHITESPACE) {
                return token;
            }
        }
        return null;
    }

    private Token nextNonWhitespace(List<Token> tokens, int index) {
        for (int i = index + 1; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.getType() != TokenType.WHITESPACE) {
                return token;
            }
        }
        return null;
    }
}
