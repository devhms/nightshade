package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;

/**
 * Core interface for all poisoning strategies.
 *
 * OOP principle demonstrated: ABSTRACTION — ObfuscationEngine calls
 * apply() on each element of List<PoisonStrategy> without knowing the
 * concrete type. POLYMORPHISM in action.
 *
 * The interface also carries metadata (getName, getDescription, getVersion)
 * for the plugin architecture and the UI strategy panel.
 */
public interface PoisonStrategy {

    /** Short display name shown in the UI checkbox and log. */
    String getName();

    /** One-sentence description shown in the UI tooltip. */
    String getDescription();

    /** Research citation for this strategy (shown in About and RESEARCH.md). */
    String getResearchBasis();

    /** Version string — for plugin compatibility checks. */
    default String getVersion() { return "2.0.0"; }

    /** Author — for plugin registry display. */
    default String getAuthor() { return "Nightshade Core"; }

    /**
     * Applies this strategy to the given source file and AST.
     *
     * @param source   The current SourceFile (may already be modified by prior strategies)
     * @param ast      The AST parsed from the ORIGINAL source
     * @param symbols  Shared symbol table for consistent renaming across files
     * @return A new ObfuscationResult containing the modified SourceFile
     */
    ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols);

    boolean isEnabled();
    void setEnabled(boolean enabled);
}
