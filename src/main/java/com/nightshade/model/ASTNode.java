package com.nightshade.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Composite pattern node representing an element in the simplified AST.
 *
 * Non-leaf nodes (CLASS_DECL, METHOD_DECL, BLOCK) have children but no token.
 * Leaf nodes (STATEMENT, FIELD_DECL, COMMENT_NODE) have a token but no children.
 *
 * Node types used:
 *   CLASS_DECL, METHOD_DECL, BLOCK, STATEMENT, FIELD_DECL, COMMENT_NODE, SCOPE
 *
 * OOP principles demonstrated:
 *   COMPOSITION — each node owns a List<ASTNode> children (Composite pattern)
 *   ENCAPSULATION — fields are private, tree navigation via methods only
 */
public class ASTNode {

    private final String nodeType;
    private final Token token;          // null for non-leaf nodes
    private final List<ASTNode> children;
    private ASTNode parent;             // weak reference — not serialized
    private String scopePath;           // e.g. "MyClass.myMethod" for scope-aware renaming
    private int methodIndex;            // ordinal within parent class (for dead code rotation)

    public ASTNode(String nodeType, Token token) {
        this.nodeType = nodeType;
        this.token = token;
        this.children = new ArrayList<>();
    }

    public ASTNode(String nodeType) {
        this(nodeType, null);
    }

    // ── Tree manipulation ────────────────────────────────────────────────────

    public void addChild(ASTNode child) {
        child.parent = this;
        children.add(child);
    }

    /**
     * Recursively finds all descendant nodes with the given nodeType.
     * Returns an unmodifiable view for safety.
     */
    public List<ASTNode> findAll(String type) {
        List<ASTNode> result = new ArrayList<>();
        collectAll(type, result);
        return Collections.unmodifiableList(result);
    }

    private void collectAll(String type, List<ASTNode> acc) {
        if (nodeType.equals(type)) acc.add(this);
        for (ASTNode child : children) {
            child.collectAll(type, acc);
        }
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public String getNodeType()           { return nodeType; }
    public Token getToken()               { return token; }
    public List<ASTNode> getChildren()    { return Collections.unmodifiableList(children); }
    public ASTNode getParent()            { return parent; }
    public String getScopePath()          { return scopePath != null ? scopePath : ""; }
    public int getMethodIndex()           { return methodIndex; }

    public void setScopePath(String scopePath)  { this.scopePath = scopePath; }
    public void setMethodIndex(int idx)         { this.methodIndex = idx; }

    public boolean isLeaf() { return children.isEmpty(); }

    @Override
    public String toString() {
        return "ASTNode[" + nodeType + (token != null ? ", " + token.getValue() : "") + "]";
    }
}
