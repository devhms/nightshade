package com.nightshade.engine;

import com.nightshade.model.ObfuscationResult;
import java.util.*;

public class PoisoningReport {

    public static String generate(List<ObfuscationResult> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Nightshade Poisoning Report\n\n");
        sb.append("## Summary\n");
        sb.append("| Metric | Value |\n");
        sb.append("|--------|-------|\n");
        
        int totalFiles = results.size();
        int totalRenamed = 0, totalDead = 0, totalComments = 0;
        int totalStrings = 0, totalWhitespace = 0;
        double avgEntropy = 0;
        int filesAboveThreshold = 0;
        
        for (ObfuscationResult r : results) {
            totalRenamed += r.getRenamedIdentifiers();
            totalDead += r.getDeadBlocksInjected();
            totalComments += r.getCommentsPoisoned();
            totalStrings += r.getStringsEncoded();
            totalWhitespace += r.getWhitespaceChanges();
            avgEntropy += r.getEntropyScore();
            if (r.getEntropyScore() >= 0.5) filesAboveThreshold++;
        }
        avgEntropy /= Math.max(1, totalFiles);
        
        sb.append(String.format("| Files Processed | %d |\n", totalFiles));
        sb.append(String.format("| Identifiers Renamed | %d |\n", totalRenamed));
        sb.append(String.format("| Dead Blocks Injected | %d |\n", totalDead));
        sb.append(String.format("| Comments Poisoned | %d |\n", totalComments));
        sb.append(String.format("| Strings Encoded | %d |\n", totalStrings));
        sb.append(String.format("| Whitespace Changes | %d |\n", totalWhitespace));
        sb.append(String.format("| Avg Entropy Score | %.3f |\n", avgEntropy));
        sb.append(String.format("| Files Above Threshold | %d/%d (%.0f%%) |\n", 
            filesAboveThreshold, totalFiles, 
            (double)filesAboveThreshold/Math.max(1,totalFiles)*100));
        
        // Per-file breakdown
        sb.append("\n## Per-File Breakdown\n\n");
        sb.append("| File | Entropy | Renamed | Dead | Comments | Strings |\n");
        sb.append("|------|---------|---------|------|----------|---------|\n");
        for (ObfuscationResult r : results) {
            sb.append(String.format("| %s | %.3f | %d | %d | %d | %d |\n",
                r.getOriginalFile().getFileName(),
                r.getEntropyScore(),
                r.getRenamedIdentifiers(),
                r.getDeadBlocksInjected(),
                r.getCommentsPoisoned(),
                r.getStringsEncoded()));
        }
        
        // MI resistance estimate
        sb.append("\n## Estimated MI Resistance\n");
        sb.append("Based on arXiv:2512.15468, variable renaming alone provides ");
        sb.append("~10.19% MI detection drop. Combined with dead code injection, ");
        sb.append("comment poisoning, and string encoding, estimated total MI ");
        double files = Math.max(1, results.size());
        sb.append(String.format("resistance: **%.1f%%**\n", 
            Math.min(95, 10.19 + (totalDead/files) * 2.5 + (totalComments/files) * 1.5 + (totalStrings/files) * 3.0)));
        
        return sb.toString();
    }
}
