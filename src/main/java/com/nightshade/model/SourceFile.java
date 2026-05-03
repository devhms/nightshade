package com.nightshade.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates a source file — its path, raw content, and the obfuscated lines
 * produced after strategy processing.
 *
 * OOP principle: ENCAPSULATION — rawLines is immutable; obfuscatedLines is
 * set exactly once by the engine pipeline.
 */
public class SourceFile {

    private final String absolutePath;
    private final List<String> rawLines;
    private List<String> obfuscatedLines;   // set by engine after processing

    public SourceFile(String absolutePath, List<String> rawLines) {
        this.absolutePath = absolutePath;
        this.rawLines = Collections.unmodifiableList(new ArrayList<>(rawLines));
        this.obfuscatedLines = new ArrayList<>(rawLines); // default: unchanged
    }

    public String getAbsolutePath()           { return absolutePath; }
    public List<String> getRawLines()         { return rawLines; }

    public List<String> getObfuscatedLines()  { return Collections.unmodifiableList(obfuscatedLines); }

    public void setObfuscatedLines(List<String> lines) {
        this.obfuscatedLines = new ArrayList<>(lines);
    }

    /** Returns the file extension (e.g. ".java", ".py", ".js"). */
    public String getExtension() {
        int dot = absolutePath.lastIndexOf('.');
        return dot >= 0 ? absolutePath.substring(dot) : "";
    }

    /** Short display name — just filename, not full path. */
    public String getFileName() {
        return new java.io.File(absolutePath).getName();
    }

    @Override
    public String toString() {
        return "SourceFile[" + getFileName() + ", " + rawLines.size() + " lines]";
    }
}
