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
            String returnExpr = null;
            for (int j = bodyStart; j < bodyEnd; j++) {
                String trimmed = lines.get(j).trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("return ")) {
                    String expr = trimmed.substring("return ".length()).replaceFirst("\\s*;\\s*$", "");
                    returnExpr = expr;
                } else {
                    bodyStatements.add(trimmed);
                }
            }
            
            if (bodyStatements.size() < 2) continue; // not worth flattening

            String stateVar = "_ns_state";
            
            // Detect return type from method signature
            String returnType = "int";
            Matcher sigMatcher = Pattern.compile("private\\s+(\\w+)\\s+" + Pattern.quote(m.group(3)) + "\\s*\\(").matcher(lines.get(i));
            if (sigMatcher.find()) {
                returnType = sigMatcher.group(1);
            }
            
            // Build the flattened version
            // Declare all local variables + return value at the top
            // so they're visible across all switch cases
            Set<String> declaredVars = new HashSet<>();
            declaredVars.add(returnType + " _ns_ret");
            for (String stmt : bodyStatements) {
                extractLocalVarDeclarations(stmt, declaredVars);
            }

            List<String> flattened = new ArrayList<>();
            flattened.add(indent + "    int " + stateVar + " = 0;");
            flattened.add(indent + "    { // scope block for local variable visibility");
            for (String decl : declaredVars) {
                String[] parts = decl.split(" ", 2);
                String type = parts[0];
                String name = parts[1];
                String init = getDefaultValue(type);
                flattened.add(indent + "        " + type + " " + name + " = " + init + ";");
            }
            if (returnExpr != null) {
                flattened.add(indent + "        _ns_ret = " + returnExpr + ";");
            }
            flattened.add(indent + "        while (" + stateVar + " != -1) {");
            flattened.add(indent + "            switch (" + stateVar + ") {");
            for (int s = 0; s < bodyStatements.size(); s++) {
                String stmt = stripDeclaration(bodyStatements.get(s), declaredVars);
                flattened.add(indent + "            case " + s + ": "
                    + stmt + " " + stateVar + " = " + (s+1) + "; break;");
            }
            if (returnExpr != null) {
                flattened.add(indent + "            case " + bodyStatements.size() + ": " + stateVar + " = -1; break;");
            }
            flattened.add(indent + "            default: _ns_state = -1; break;");
            flattened.add(indent + "            }"); // close switch
            flattened.add(indent + "        }");     // close while
            if (returnExpr != null) {
                flattened.add(indent + "        return _ns_ret;");
            }
            flattened.add(indent + "    }");         // close scope block
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

    private void extractLocalVarDeclarations(String statement, Set<String> declaredVars) {
        String stripped = statement.replaceFirst("^\\s*case \\d+:\\s*", "").trim();
        if (stripped.isEmpty()) return;
        Matcher m = Pattern.compile("^\\s*(int|double|float|boolean|char|byte|short|long|String)\\s+(\\w+)")
            .matcher(stripped);
        if (m.find()) {
            declaredVars.add(m.group(1) + " " + m.group(2));
        }
    }

    private String stripDeclaration(String statement, Set<String> declaredVars) {
        for (String decl : declaredVars) {
            String varName = decl.substring(decl.indexOf(' ') + 1);
            String type = decl.substring(0, decl.indexOf(' '));
            Pattern p = Pattern.compile("^\\s*" + Pattern.quote(type) + "\\s+" + Pattern.quote(varName) + "\\s*=");
            if (p.matcher(statement).find()) {
                return varName + " =" + statement.split("=", 2)[1];
            }
        }
        return statement;
    }

    private String getDefaultValue(String type) {
        return switch (type) {
            case "int", "double", "float", "byte", "short", "long" -> "0";
            case "boolean" -> "false";
            case "char" -> "'\\0'";
            case "String" -> "null";
            default -> "null";
        };
    }
}