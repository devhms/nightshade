package com.nightshade.strategy;

import com.nightshade.model.*;
import java.util.*;
import java.security.*;
import java.nio.charset.StandardCharsets;

public class WatermarkEncoder implements PoisonStrategy {

    private boolean enabled = false;
    private String authorId = "nightshade-user";

    @Override public String getName()          { return "Watermark Encoder"; }
    @Override public String getDescription()   { return "Embeds steganographic fingerprint for copyright provenance tracking"; }
    @Override public String getResearchBasis() { return "Code watermarking via whitespace steganography — invisible to humans, extractable with key"; }
    @Override public boolean isEnabled()       { return enabled; }
    @Override public void setEnabled(boolean e){ this.enabled = e; }

    public void setAuthorId(String id) { this.authorId = id; }

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        List<String> lines = new ArrayList<>(source.getObfuscatedLines());
        
        // Generate watermark bits from author + salt + timestamp
        String payload = authorId + "|" + symbols.getSessionSalt() + "|" + System.currentTimeMillis();
        byte[] hash = sha256(payload);
        boolean[] bits = bytesToBits(hash);
        
        int bitIndex = 0;
        int embedded = 0;
        
        for (int i = 0; i < lines.size() && bitIndex < bits.length; i++) {
            String line = lines.get(i);
            String trimmed = line.trim();
            
            // Skip blank lines and lines with no indentation
            if (trimmed.isEmpty()) continue;
            int leadingSpaces = line.length() - line.stripLeading().length();
            if (leadingSpaces < 2) continue;
            
            // Encode one bit per eligible line:
            // bit=0 → use normal indent (2 spaces per indentation unit)
            // bit=1 → use tab character for the first indent unit (invisible but compilable)
            if (bits[bitIndex]) {
                // Use tab for first indent unit instead of zero-width space
                // Tab is invisible in most editors and doesn't break compilation
                if (leadingSpaces >= 1) {
                    lines.set(i, "\t" + line.substring(leadingSpaces));
                    embedded++;
                }
            }
            bitIndex++;
        }

        SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        modified.setObfuscatedLines(lines);

        ObfuscationResult result = new ObfuscationResult(source, modified, 0.0);
        result.setWhitespaceChanges(embedded);
        return result;
    }

    private byte[] sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean[] bytesToBits(byte[] bytes) {
        boolean[] bits = new boolean[bytes.length * 8];
        for (int i = 0; i < bytes.length; i++) {
            for (int j = 0; j < 8; j++) {
                bits[i * 8 + j] = ((bytes[i] >> (7 - j)) & 1) == 1;
            }
        }
        return bits;
    }
}
