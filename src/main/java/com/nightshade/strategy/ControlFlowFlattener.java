package com.nightshade.strategy;

import com.nightshade.model.*;
import java.util.*;
import java.util.regex.*;

public class ControlFlowFlattener implements PoisonStrategy {

    private boolean enabled = false; // disabled by default — aggressive
    
    @Override public String getName()          { return "Control Flow Flattening"; }
    @Override public String getDescription()   { return "Rewrites method bodies into switch-dispatch loops — changes code structure, not just names"; }
    @Override public String getResearchBasis() { return "Structure-level obfuscation — survives variable normalization and reformatting"; }
    @Override public boolean isEnabled()       { return enabled; }
    @Override public void setEnabled(boolean e){ this.enabled = e; }

    // Detects private method declarations
    private static final Pattern PRIVATE_METHOD = Pattern.compile(
        "^(\\s*)(private\\s+\\w+\\s+(\\w+)\\s*\\([^)]*\\))\\s*\\{\\s*$");

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        List<String> lines = new ArrayList<>(source.getObfuscatedLines());
        int flattenedCount = 0;
        int totalMethods = 0;

        // Find private methods and flatten them
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = PRIVATE_METHOD.matcher(lines.get(i));
            if (!m.matches()) continue;
            totalMethods++;

            String indent = m.group(1);
            // Find the closing brace of this method
            int braceDepth = 1;
            int bodyStart = i + 1;
            int bodyEnd = -1;
            for (int j = bodyStart; j < lines.size(); j++) {
                for (char c : lines.get(j).toCharArray()) {
                    if (c == '{') braceDepth++;
                    if (c == '}') braceDepth--;
                }
                if (braceDepth == 0) { bodyEnd = j; break; }
            }
            if (bodyEnd == -1 || bodyEnd - bodyStart < 3) continue;

            // Extract body statements (skip blank lines)
            List<String> bodyStatements = new ArrayList<>();
            String returnStatement = null;
            for (int j = bodyStart; j < bodyEnd; j++) {
                String trimmed = lines.get(j).trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("return ")) {
                    returnStatement = trimmed;
                } else {
                    bodyStatements.add(trimmed);
                }
            }
            
            if (bodyStatements.size() < 2) continue; // not worth flattening

            String stateVar = "_ns_state";
            
            // Build the flattened version with proper scoping
            // FIX: Wrap switch in scope block so local variables are visible across cases
            List<String> flattened = new ArrayList<>();
            flattened.add(indent + "    int " + stateVar + " = 0;");
            flattened.add(indent + "    { // scope block for local variable visibility");
            flattened.add(indent + "        while (" + stateVar + " != -1) {");
            flattened.add(indent + "            switch (" + stateVar + ") {");
            for (int s = 0; s < bodyStatements.size(); s++) {
                flattened.add(indent + "            case " + s + ": " 
                    + bodyStatements.get(s) + " " + stateVar + " = " + (s+1) + "; break;");
            }
            flattened.add(indent + "            case " + bodyStatements.size() 
                + ": " + stateVar + " = -1; break;");
            flattened.add(indent + "        }");
            flattened.add(indent + "    }");
            if (returnStatement != null) {
                flattened.add(indent + "    " + returnStatement);
            }

            // Replace original body with flattened version
            List<String> before = new ArrayList<>(lines.subList(0, bodyStart));
            List<String> after = new ArrayList<>(lines.subList(bodyEnd, lines.size()));
            List<String> newLines = new ArrayList<>(before);
            newLines.addAll(flattened);
            newLines.addAll(after);
            // Adjust loop index: skip past the flattened block we just inserted
            i = bodyStart + flattened.size() - 1;
            lines = newLines;
            flattenedCount++;
        }

        SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        modified.setObfuscatedLines(lines);

        ObfuscationResult result = new ObfuscationResult(source, modified, 0.0);
        result.setTotalMethods(Math.max(1, totalMethods));
        return result;
    }
}