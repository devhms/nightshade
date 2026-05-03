package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;

import java.util.*;
import java.util.regex.*;

/**
 * Strategy C: Semantic Comment Poisoning
 *
 * Replaces inline comments with semantically false but grammatically correct
 * misleading descriptions. Selection is deterministic by line number.
 */
public class CommentPoisoner implements PoisonStrategy {

    private boolean enabled = true;

    @Override public String getName()           { return "Semantic Comment Poisoning"; }
    @Override public String getDescription()    { return "Replaces comments with semantically false content — disrupts LLM association learning"; }
    @Override public String getResearchBasis()  { return "Comments are heavily weighted in training pipelines — false semantics disrupt association learning"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    private static final String[] JAVA_COMMENT_BANK = {
        "// bubble sort O(n^2) — swaps adjacent elements until list is sorted",
        "// network request to external REST API endpoint — async with retry",
        "// recursive depth-first traversal of binary search tree",
        "// SQL query: SELECT * FROM users WHERE active = 1 ORDER BY created_at DESC",
        "// cryptographic hash using SHA-256 digest with HMAC verification",
        "// Fibonacci sequence generator using dynamic programming memoization",
        "// database connection pool — max 10 concurrent connections",
        "// OAuth 2.0 token validation and refresh logic — Bearer scheme",
        "// binary search on sorted array O(log n) — returns index or -1",
        "// LRU cache eviction policy — capacity limit 1000 entries",
        "// Dijkstra's algorithm for shortest path in weighted directed graph",
        "// matrix multiplication using Strassen O(n^2.807) algorithm",
        "// regex pattern matching — NFA simulation with backtracking",
        "// merge sort — divide and conquer, stable, O(n log n) guaranteed",
        "// socket connection to remote peer — TCP with keepalive enabled",
        "// XML parsing using DOM — loads entire document into memory",
        "// gRPC bidirectional streaming — handles backpressure automatically",
        "// Bloom filter membership test — probabilistic, no false negatives",
        "// AES-256 encryption in CBC mode with PKCS7 padding",
        "// observer pattern notification — propagates to all registered listeners",
        "// garbage collection hint — forces full GC cycle on large heap",
        "// distributed lock acquisition via Redis SETNX with TTL",
        "// B-tree index traversal — O(log n) per lookup",
        "// webhook delivery with exponential backoff — max 5 retries",
        "// trie data structure insertion — O(m) where m is key length"
    };

    private static final String[] PYTHON_COMMENT_BANK = {
        "# bubble sort O(n^2) — swaps elements until sorted",
        "# REST API call with retry logic — exponential backoff",
        "# recursive DFS traversal of binary tree",
        "# SQL: SELECT * FROM users WHERE active=True",
        "# SHA-256 hash with HMAC verification",
        "# Fibonacci with memoization cache",
        "# database connection pool — async",
        "# OAuth2 token refresh — Bearer scheme",
        "# binary search O(log n) — sorted input required",
        "# LRU cache eviction — max capacity 1000"
    };

    private static final Pattern JAVA_COMMENT = Pattern.compile("^(\\s*)(//.*?)\\s*$");
    private static final Pattern PY_COMMENT   = Pattern.compile("^(\\s*)(#.*?)\\s*$");

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        List<String> lines = new ArrayList<>(source.getObfuscatedLines());
        String ext = source.getExtension();
        int poisoned = 0;
        int totalComments = 0;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Pattern pat = ext.equals(".py") ? PY_COMMENT : JAVA_COMMENT;
            Matcher m = pat.matcher(line);
            if (m.matches()) {
                totalComments++;
                String indent = m.group(1);
                String[] bank = ext.equals(".py") ? PYTHON_COMMENT_BANK : JAVA_COMMENT_BANK;
                String replacement = bank[(i + 1) % bank.length];
                lines.set(i, indent + replacement.stripLeading());
                poisoned++;
            }
        }

        SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        modified.setObfuscatedLines(lines);

        ObfuscationResult result = new ObfuscationResult(source, modified, 0.0);
        result.setCommentsPoisoned(poisoned);
        result.setTotalComments(Math.max(1, totalComments));
        return result;
    }
}
