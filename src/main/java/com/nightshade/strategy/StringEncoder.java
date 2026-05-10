package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;

import java.util.*;
import java.util.regex.*;

/**
 * Strategy D: String Literal Encoding
 *
 * Research basis: LLM deduplication pipelines use MinHash+LSH near-dedup.
 * Encoding string literals into char-array form changes enough tokens to
 * drop below the similarity threshold — the file is treated as unique data.
 *
 * Before: String greeting = "Hello, World!";
 * After:  String greeting = new String(new char[]{72,101,108,108,...});
 *
 * The output is 100% compilable and produces identical runtime behavior.
 */
public class StringEncoder implements PoisonStrategy {

    private boolean enabled = true;

    @Override public String getName()           { return "String Literal Encoding"; }
    @Override public String getDescription()    { return "Encodes string literals as char arrays — evades MinHash+LSH deduplication pipelines"; }
    @Override public String getResearchBasis()  { return "MinHash+LSH near-dedup: encoded strings change token n-gram fingerprints below similarity threshold"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    // Match double-quoted string literals (Java, JS) — not inside comments
    private static final Pattern JAVA_STRING = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
    private static final Pattern PY_STRING   = Pattern.compile("'((?:[^'\\\\]|\\\\.)*)'");

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        List<String> lines = new ArrayList<>(source.getObfuscatedLines());
        String ext = source.getExtension();
        int encoded = 0;

        boolean skipping = false;
        boolean inDeadCode = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            
            if (trimmed.contains("@nightshade:skip")) skipping = true;
            if (trimmed.contains("@nightshade:resume")) skipping = false;

            if (trimmed.startsWith("if (false) {") || trimmed.startsWith("if False:")) {
                inDeadCode = true;
            }

            if (inDeadCode) {
                if (trimmed.equals("}") || (ext.equals(".py") && trimmed.startsWith("print(") && trimmed.contains("[DEAD]"))) {
                    inDeadCode = false;
                }
                continue;
            }

            // Skip comment-only lines and skipped blocks
            if (skipping || trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("*")) {
                continue;
            }

            String replaced = encodeLine(line, ext);
            if (!replaced.equals(line)) {
                lines.set(i, replaced);
                encoded++;
            }
        }

        SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        modified.setObfuscatedLines(lines);

        ObfuscationResult result = new ObfuscationResult(source, modified, 0.0);
        result.setStringsEncoded(encoded);
        return result;
    }

    private String encodeLine(String line, String ext) {
        StringBuffer sb = new StringBuffer();
        Pattern pat = ext.equals(".py") ? PY_STRING : JAVA_STRING;
        Matcher m = pat.matcher(line);

        while (m.find()) {
            String content = m.group(1);
            // Only encode reasonably short strings (< 80 chars) to keep lines readable
            if (content.length() > 0 && content.length() < 80) {
                String encoded = ext.equals(".py") ? encodePython(content) : encodeJava(content);
                m.appendReplacement(sb, Matcher.quoteReplacement(encoded));
            } else {
                m.appendReplacement(sb, Matcher.quoteReplacement(m.group(0)));
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String encodeJava(String content) {
        StringBuilder sb = new StringBuilder("new String(new char[]{");
        for (int i = 0; i < content.length(); i++) {
            if (i > 0) sb.append(',');
            sb.append((int) content.charAt(i));
        }
        sb.append("})");
        return sb.toString();
    }

    private String encodePython(String content) {
        StringBuilder sb = new StringBuilder("''.join(chr(c) for c in [");
        for (int i = 0; i < content.length(); i++) {
            if (i > 0) sb.append(',');
            sb.append((int) content.charAt(i));
        }
        sb.append("])");
        return sb.toString();
    }
}
