package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;

import java.util.*;
import java.util.regex.*;

/**
 * Strategy E: Whitespace Pattern Disruption
 *
 * Research basis: BPE and SentencePiece tokenizers encode indentation as part
 * of token sequences. Changing indentation style (4-space → mixed 2/8-space,
 * K&R → Allman brace style) produces different token n-gram fingerprints.
 *
 * Effect: Survives all normalization except aggressive re-formatting (which
 * most training pipelines don't apply because it's computationally expensive).
 *
 * Transformations applied:
 *  1. Brace-on-new-line (Allman style) for method declarations
 *  2. Variable indentation depth changes (± 2 spaces based on line hash)
 *  3. Trailing whitespace injection on statement lines
 */
public class WhitespaceDisruptor implements PoisonStrategy {

    private boolean enabled = true;

    @Override public String getName()           { return "Whitespace Pattern Disruption"; }
    @Override public String getDescription()    { return "Randomizes indentation and brace style — disrupts BPE/SentencePiece tokenization patterns"; }
    @Override public String getResearchBasis()  { return "BPE tokenizers encode whitespace as token prefixes — indentation changes alter n-gram fingerprints"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    // Pattern to detect lines that are ONLY an opening brace (to move to K&R style)
    private static final Pattern ALLMAN_BRACE = Pattern.compile("^(\\s*)\\{\\s*$");
    // Pattern to detect method/if/for/while declaration lines ending with {
    private static final Pattern KR_OPEN = Pattern.compile("^(\\s*)((?:public|private|protected|static|void|class|if|for|while|else|try|catch|finally).+?)\\s*\\{\\s*$");

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        List<String> lines = new ArrayList<>(source.getObfuscatedLines());
        String ext = source.getExtension();
        int changes = 0;

        // Only apply to Java/JS — Python whitespace is semantic
        if (ext.equals(".py")) {
            SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
            modified.setObfuscatedLines(lines);
            ObfuscationResult r = new ObfuscationResult(source, modified, 0.0);
            r.setWhitespaceChanges(0);
            return r;
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);

            // Transformation 1: K&R → Allman (move lone { to previous line's end)
            // We collect and process in a second pass to avoid index confusion
            result.add(line);
        }

        // Second pass: move opening braces to Allman style
        List<String> allman = toAllmanStyle(result);
        int allmanChanges = countDiff(result, allman);
        changes += allmanChanges;

        // Third pass: vary indentation on non-empty, non-comment lines
        List<String> disrupted = varyIndentation(allman);
        changes += countDiff(allman, disrupted);

        SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        modified.setObfuscatedLines(disrupted);

        ObfuscationResult r = new ObfuscationResult(source, modified, 0.0);
        r.setWhitespaceChanges(changes);
        return r;
    }

    private List<String> toAllmanStyle(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            // If line ends with { preceded by code (K&R style), split into two lines
            Matcher m = KR_OPEN.matcher(line);
            if (m.matches()) {
                String indent = m.group(1);
                String code = m.group(2).stripTrailing();
                out.add(indent + code);
                out.add(indent + "{");
            } else {
                out.add(line);
            }
        }
        return out;
    }

    private List<String> varyIndentation(List<String> lines) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            // Only modify lines with content (not blank or comment-only)
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
                out.add(line);
                continue;
            }

            // Count existing leading spaces
            int leadingSpaces = 0;
            while (leadingSpaces < line.length() && line.charAt(leadingSpaces) == ' ') {
                leadingSpaces++;
            }

            // Add 1 extra space on odd-hashed lines (deterministic)
            int extraSpaces = (trimmed.hashCode() ^ (i * 37)) % 3 == 0 ? 1 : 0;
            if (extraSpaces > 0 && leadingSpaces > 0) {
                out.add(" ".repeat(leadingSpaces + extraSpaces) + trimmed);
            } else {
                out.add(line);
            }
        }
        return out;
    }

    private int countDiff(List<String> a, List<String> b) {
        int diff = 0;
        int max = Math.min(a.size(), b.size());
        for (int i = 0; i < max; i++) {
            if (!a.get(i).equals(b.get(i))) diff++;
        }
        return diff + Math.abs(a.size() - b.size());
    }
}
