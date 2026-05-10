package com.nightshade.engine;

import com.nightshade.model.Token;
import com.nightshade.model.TokenType;

import java.util.*;
import java.util.regex.*;

/**
 * Converts raw source lines into a flat list of Tokens using a single
 * compiled regex Pattern with named capturing groups.
 *
 * Pattern order matters:
 *  1. COMMENT must come before IDENTIFIER so // and /* are not tokenized
 *     as two SYMBOL tokens.
 *  2. STRING must come before IDENTIFIER.
 *  3. KEYWORD classification happens post-match on IDENTIFIER group results.
 *
 * Supports .java, .py, and .js files with language-aware patterns.
 */
public class Lexer {

    // ── Master Pattern (DOTALL for multi-line block comments) ───────────────

    private static final String PATTERN_STRING =
        "(?<COMMENT>//[^\n]*|/\\*.*?\\*/|#[^\n]*)"                           // Java/JS // and /* */, Python #
        + "|(?<STRING>\"(?:[^\"\\\\]|\\\\.)*\"|'(?:[^'\\\\]|\\\\.)*'|`[^`]*`)" // double/single/backtick strings
        + "|(?<NUMBER>\\b\\d+\\.?\\d*(?:[eE][+-]?\\d+)?[lLfFdD]?\\b)"       // numeric literals
        + "|(?<IDENTIFIER>[a-zA-Z_$][a-zA-Z0-9_$]*)"                         // identifiers (classified post-match)
        + "|(?<SYMBOL>[{}()\\[\\];,.<>!=+\\-*/%&|^~?:@])"                    // symbols + @ for annotations
        + "|(?<WHITESPACE>[ \\t]+|\\r?\\n)";                                  // spaces, tabs, newlines

    private static final Pattern MASTER_PATTERN =
        Pattern.compile(PATTERN_STRING, Pattern.DOTALL);

    // ── Java reserved words + common stdlib types (must NOT be renamed) ─────

    private static final Set<String> JAVA_KEYWORDS = new HashSet<>(Arrays.asList(
        "abstract","assert","boolean","break","byte","case","catch","char","class",
        "const","continue","default","do","double","else","enum","extends","final",
        "finally","float","for","goto","if","implements","import","instanceof","int",
        "interface","long","native","new","package","private","protected","public",
        "return","short","static","strictfp","super","switch","synchronized","this",
        "throw","throws","transient","try","var","void","volatile","while","record",
        "sealed","permits","yield","null","true","false",
        // Common stdlib that must not be renamed
        "String","System","Object","Class","Exception","RuntimeException","Error",
        "Throwable","Override","Deprecated","SuppressWarnings","FunctionalInterface",
        // Python keywords
        "and","as","async","await","def","del","elif","except","exec","finally",
        "from","global","in","is","lambda","nonlocal","not","or","pass","print",
        "raise","with","yield",
        // JS keywords
        "arguments","async","await","const","debugger","delete","export","function",
        "in","instanceof","let","of","typeof","undefined","void","yield"
    ));

    // ── Public API ───────────────────────────────────────────────────────────

    /**
     * Tokenizes a list of source lines into a flat Token list.
     * Line numbers are 1-based. Column positions are character offsets within the line.
     */
    public List<Token> tokenize(List<String> lines) {
        List<Token> tokens = new ArrayList<>();
        int lineNum = 1;
        boolean skipping = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.contains("@nightshade:skip")) skipping = true;
            if (trimmed.contains("@nightshade:resume")) skipping = false;
            
            if (skipping) {
                lineNum++;
                continue;
            }

            Matcher m = MASTER_PATTERN.matcher(line);
            while (m.find()) {
                String value = m.group();
                TokenType type = classify(m, value);
                tokens.add(new Token(type, value, lineNum, m.start()));
            }
            lineNum++;
        }
        return tokens;
    }

    // ── Classification ───────────────────────────────────────────────────────

    private TokenType classify(Matcher m, String value) {
        if (m.group("COMMENT")    != null) return TokenType.COMMENT;
        if (m.group("STRING")     != null) return TokenType.LITERAL;
        if (m.group("NUMBER")     != null) return TokenType.LITERAL;
        if (m.group("IDENTIFIER") != null) {
            return JAVA_KEYWORDS.contains(value) ? TokenType.KEYWORD : TokenType.IDENTIFIER;
        }
        if (m.group("SYMBOL")     != null) return TokenType.SYMBOL;
        return TokenType.WHITESPACE;
    }

    public boolean isKeyword(String value) {
        return JAVA_KEYWORDS.contains(value);
    }
}
