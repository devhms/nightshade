package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import com.nightshade.model.Token;
import com.nightshade.model.TokenType;
import com.nightshade.engine.Lexer;
import com.nightshade.engine.Serializer;

import java.util.*;

/**
 * Strategy F: Semantic Inversion
 *
 * Creates misleading variable names matching an opposite domain to disrupt
 * LLM semantic learning. For example, replacing standard programming variables
 * with culinary or automotive terms.
 */
public class SemanticInverter implements PoisonStrategy {

    private boolean enabled = false; // Disabled by default
    private final Lexer lexer = new Lexer();
    private final Serializer serializer = new Serializer();

    // A dictionary of misleading semantic terms (e.g., culinary, automotive, biology)
    private static final String[] MISLEADING_TERMS = {
        "engineOil", "bakeCake", "mitochondria", "brakePad", "recipeDough",
        "transmission", "photosynthesis", "spiceMix", "sparkPlug", "cellWall",
        "exhaustPipe", "boilingWater", "ribosome", "steeringWheel", "choppedOnion",
        "gearbox", "chloroplast", "sugarGlaze", "clutchPedal", "nucleus"
    };

    @Override public String getName()           { return "Semantic Inversion"; }
    @Override public String getDescription()    { return "Replaces variables with misleading domain terms to disrupt semantic learning"; }
    @Override public String getResearchBasis()  { return "Semantic dissonance: using contextually incorrect vocabulary degrades model comprehension"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        Map<String, String> lineMapping = new HashMap<>(); 
        Set<String> renamedNames = new HashSet<>();

        List<ASTNode> identifierNodes = ast.findAll("STATEMENT");
        for (ASTNode node : identifierNodes) {
            Token t = node.getToken();
            if (t == null || t.getType() != TokenType.IDENTIFIER) continue;
            if (!symbols.isUserDefined(t.getValue())) continue;

            String original = t.getValue();
            // Generate deterministic but misleading replacement
            int hash = Math.abs((source.getAbsolutePath() + "::" + original).hashCode());
            String replacement = MISLEADING_TERMS[hash % MISLEADING_TERMS.length] + "_" + (hash % 1000);

            lineMapping.put(original, replacement);
            renamedNames.add(original);
        }

        // Apply mapping
        List<String> modifiedLines = serializer.applyMapping(source, lineMapping);

        SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        modified.setObfuscatedLines(modifiedLines);

        ObfuscationResult result = new ObfuscationResult(source, modified, 0.0);
        result.setRenamedIdentifiers(renamedNames.size());
        // Add total identifiers
        int totalIdents = 0;
        List<Token> tokens = lexer.tokenize(source.getRawLines());
        for (Token t : tokens) {
            if (t.getType() == TokenType.IDENTIFIER && symbols.isUserDefined(t.getValue())) {
                totalIdents++;
            }
        }
        result.setTotalIdentifiers(Math.max(1, totalIdents));
        
        return result;
    }
}
