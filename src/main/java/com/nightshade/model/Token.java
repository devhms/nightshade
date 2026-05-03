package com.nightshade.model;

/**
 * Immutable token produced by the Lexer.
 *
 * All fields are final — no setters. Tokens are value objects that represent
 * a single lexical unit in a source file.
 *
 * OOP principle demonstrated: ENCAPSULATION — all state is private + final,
 * exposed only through getters.
 */
public final class Token {

    private final TokenType type;
    private final String value;
    private final int lineNumber;
    private final int columnStart;

    public Token(TokenType type, String value, int lineNumber, int columnStart) {
        this.type = type;
        this.value = value;
        this.lineNumber = lineNumber;
        this.columnStart = columnStart;
    }

    public TokenType getType()      { return type; }
    public String getValue()        { return value; }
    public int getLineNumber()      { return lineNumber; }
    public int getColumnStart()     { return columnStart; }

    /** Creates a new Token with a replaced value (preserves position metadata). */
    public Token withValue(String newValue) {
        return new Token(type, newValue, lineNumber, columnStart);
    }

    @Override
    public String toString() {
        return String.format("Token[%s, \"%s\", L%d:C%d]",
            type, value.replace("\n", "\\n"), lineNumber, columnStart);
    }
}
