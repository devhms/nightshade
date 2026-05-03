package com.nightshade.model;

/**
 * Classifies each token produced by the Lexer.
 *
 * Used by strategies to decide which tokens to transform:
 *  - IDENTIFIER → eligible for renaming (EntropyScrambler)
 *  - COMMENT    → eligible for poisoning (CommentPoisoner)
 *  - LITERAL    → eligible for encoding (StringEncoder)
 *  - KEYWORD    → must NEVER be renamed
 *  - SYMBOL, WHITESPACE → structural; modified by WhitespaceDisruptor
 */
public enum TokenType {
    KEYWORD,
    IDENTIFIER,
    LITERAL,
    SYMBOL,
    COMMENT,
    WHITESPACE
}
