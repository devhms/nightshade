package com.nightshade.model;

/**
 * Holds the before/after result of processing one SourceFile.
 *
 * Also accumulates per-run statistics used by the UI dashboard:
 *   - renamedIdentifiers, deadBlocksInjected, commentsPoisoned,
 *     stringsEncoded for the entropy score formula.
 */
public class ObfuscationResult {

    private final SourceFile originalFile;
    private final SourceFile obfuscatedFile;
    private final double entropyScore;

    // Statistics for dashboard
    private int renamedIdentifiers;
    private int deadBlocksInjected;
    private int commentsPoisoned;
    private int stringsEncoded;
    private int whitespaceChanges;
    private int totalIdentifiers;
    private int totalMethods;
    private int totalComments;

    public ObfuscationResult(SourceFile originalFile, SourceFile obfuscatedFile, double entropyScore) {
        this.originalFile = originalFile;
        this.obfuscatedFile = obfuscatedFile;
        this.entropyScore = entropyScore;
    }

    public SourceFile getOriginalFile()    { return originalFile; }
    public SourceFile getObfuscatedFile()  { return obfuscatedFile; }
    public double getEntropyScore()        { return entropyScore; }

    // Stats getters/setters
    public int getRenamedIdentifiers()     { return renamedIdentifiers; }
    public int getDeadBlocksInjected()     { return deadBlocksInjected; }
    public int getCommentsPoisoned()       { return commentsPoisoned; }
    public int getStringsEncoded()         { return stringsEncoded; }
    public int getWhitespaceChanges()      { return whitespaceChanges; }
    public int getTotalIdentifiers()       { return totalIdentifiers; }
    public int getTotalMethods()           { return totalMethods; }
    public int getTotalComments()          { return totalComments; }

    public void setRenamedIdentifiers(int n)  { this.renamedIdentifiers = n; }
    public void setDeadBlocksInjected(int n)  { this.deadBlocksInjected = n; }
    public void setCommentsPoisoned(int n)    { this.commentsPoisoned = n; }
    public void setStringsEncoded(int n)      { this.stringsEncoded = n; }
    public void setWhitespaceChanges(int n)   { this.whitespaceChanges = n; }
    public void setTotalIdentifiers(int n)    { this.totalIdentifiers = n; }
    public void setTotalMethods(int n)        { this.totalMethods = n; }
    public void setTotalComments(int n)       { this.totalComments = n; }

    @Override
    public String toString() {
        return String.format("ObfuscationResult[%s, entropy=%.3f]",
            originalFile.getFileName(), entropyScore);
    }
}
