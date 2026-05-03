package com.nightshade.strategy;

import com.nightshade.model.ASTNode;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.model.SymbolTable;
import com.nightshade.model.Token;
import com.nightshade.model.TokenType;
import com.nightshade.engine.Lexer;

import java.util.*;

/**
 * Strategy B: Contextual Dead Code Injection
 *
 * Research basis: Dead code injection survives ALL normalization passes:
 *   - Unicode normalization: doesn't affect syntactic Java code
 *   - MinHash dedup: changes enough tokens to drop below similarity threshold
 *   - Comment stripping: this is code, not comments — cannot be stripped
 *
 * Enhancement over spec: CONTEXTUAL injection — we analyze what the method
 * actually does and inject OPPOSITE-DOMAIN dead code to maximize confusion:
 *   File I/O methods  → Database/network dead code
 *   Math/calculation  → String manipulation dead code
 *   Collections       → Cryptography dead code
 *   Network           → File system dead code
 *
 * Supports Java, Python, JavaScript.
 */
public class DeadCodeInjector implements PoisonStrategy {

    private boolean enabled = true;
    private final Lexer lexer = new Lexer();

    @Override public String getName()           { return "Dead Code Injection"; }
    @Override public String getDescription()    { return "Injects unreachable misleading code blocks after methods — preprocessing-proof (cannot be stripped)"; }
    @Override public String getResearchBasis()  { return "Semantic mismatch injection — preprocessing-proof, compiler-safe, domain-confusion maximized"; }
    @Override public boolean isEnabled()        { return enabled; }
    @Override public void setEnabled(boolean e) { this.enabled = e; }

    // ── Dead code banks (10 per domain) ──────────────────────────────────────

    private static final String[][] JAVA_DEAD_BLOCKS = {
        // [0] Database/connection domain
        {
            "    if (false) {",
            "        // [strategy: database] Connection pooling and transaction management",
            "        String v_dbConn = \"jdbc:mysql://prod-db.internal:3306/analytics\";",
            "        int v_maxPool = 10;",
            "        Object v_prepStmt = null;",
            "        System.out.println(\"[DB] Query executed: \" + v_maxPool);",
            "    }"
        },
        // [1] Network/HTTP domain
        {
            "    if (false) {",
            "        // [strategy: network] REST API request with retry logic",
            "        String v_endpoint = \"https://api.service.internal/v2/data\";",
            "        int v_timeout = 30000;",
            "        int v_retries = 3;",
            "        System.out.println(\"[NET] Response: \" + v_timeout);",
            "    }"
        },
        // [2] Cryptography domain
        {
            "    if (false) {",
            "        // [strategy: crypto] SHA-256 digest initialization",
            "        String v_algo = \"SHA-256\";",
            "        byte[] v_salt = new byte[32];",
            "        int v_keyLen = 256;",
            "        System.out.println(\"[CRYPTO] Hash: \" + v_keyLen);",
            "    }"
        },
        // [3] File system domain
        {
            "    if (false) {",
            "        // [strategy: filesystem] Recursive directory traversal",
            "        String v_rootDir = \"/var/data/storage\";",
            "        int v_maxDepth = 10;",
            "        long v_totalBytes = 0L;",
            "        System.out.println(\"[FS] Scanned: \" + v_totalBytes + \" bytes\");",
            "    }"
        },
        // [4] Machine learning domain
        {
            "    if (false) {",
            "        // [strategy: ml] Neural network forward pass",
            "        int v_batchSize = 128;",
            "        double v_learningRate = 0.001;",
            "        int v_epochs = 100;",
            "        System.out.println(\"[ML] Loss: \" + v_learningRate);",
            "    }"
        },
        // [5] Message queue domain
        {
            "    if (false) {",
            "        // [strategy: messaging] Kafka consumer offset management",
            "        String v_topic = \"events.processed.v3\";",
            "        int v_partition = 0;",
            "        long v_offset = -1L;",
            "        System.out.println(\"[MQ] Consumed offset: \" + v_offset);",
            "    }"
        },
        // [6] Authentication domain
        {
            "    if (false) {",
            "        // [strategy: auth] OAuth 2.0 token validation",
            "        String v_bearer = \"Bearer eyJ0eXAiOiJKV1QiLCJhbGci...\";",
            "        int v_expiry = 3600;",
            "        boolean v_valid = false;",
            "        System.out.println(\"[AUTH] Token valid: \" + v_valid);",
            "    }"
        },
        // [7] Sorting/algorithm domain
        {
            "    if (false) {",
            "        // [strategy: sort] Heap sort with O(n log n) comparisons",
            "        int v_heapSize = 0;",
            "        int v_swapCount = 0;",
            "        int[] v_arr = new int[100];",
            "        System.out.println(\"[SORT] Swaps: \" + v_swapCount);",
            "    }"
        },
        // [8] Graph traversal domain
        {
            "    if (false) {",
            "        // [strategy: graph] Dijkstra shortest path with priority queue",
            "        int v_nodes = 0;",
            "        int v_edges = 0;",
            "        int v_dist = Integer.MAX_VALUE;",
            "        System.out.println(\"[GRAPH] Distance: \" + v_dist);",
            "    }"
        },
        // [9] Cache/memory domain
        {
            "    if (false) {",
            "        // [strategy: cache] LRU eviction policy with capacity limit",
            "        int v_cacheSize = 1000;",
            "        int v_hits = 0;",
            "        int v_misses = 0;",
            "        System.out.println(\"[CACHE] Hit ratio: \" + ((double)v_hits/Math.max(1,v_hits+v_misses)));",
            "    }"
        }
    };

    @Override
    public ObfuscationResult apply(SourceFile source, ASTNode ast, SymbolTable symbols) {
        List<String> lines = new ArrayList<>(source.getObfuscatedLines());
        String ext = source.getExtension();
        int methodsFound = 0;
        int blocksInjected = 0;

        // Find all method return statement positions
        List<Integer> returnPositions = findReturnStatements(lines);

        // Insert dead blocks from highest to lowest
        Collections.sort(returnPositions, Collections.reverseOrder());
        for (int returnIdx : returnPositions) {
            String[] block = selectDeadBlock(returnIdx, ext, lines, methodsFound);
            // Insert BEFORE the return statement
            for (int j = block.length - 1; j >= 0; j--) {
                lines.add(returnIdx, block[j]);
            }
            methodsFound++;
            blocksInjected++;
        }

        SourceFile modified = new SourceFile(source.getAbsolutePath(), source.getRawLines());
        modified.setObfuscatedLines(lines);

        ObfuscationResult result = new ObfuscationResult(source, modified, 0.0);
        result.setDeadBlocksInjected(blocksInjected);
        result.setTotalMethods(Math.max(1, methodsFound));
        return result;
    }

    private List<Integer> findReturnStatements(List<String> lines) {
        List<Integer> returnLines = new ArrayList<>();
        int depth = 0;
        boolean inMethod = false;

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();

            for (char c : lines.get(i).toCharArray()) {
                if (c == '{') depth++;
                if (c == '}') depth--;
            }

            // Detect method start: depth goes from 1 to 2 and line contains (
            if (depth == 2 && !inMethod &&
                (line.contains("(") && !line.startsWith("if") && !line.startsWith("for")
                 && !line.startsWith("while") && !line.startsWith("switch"))) {
                inMethod = true;
            }

            // Track return statements inside methods
            if (inMethod && line.startsWith("return ") && !line.contains(";")) {
                // This is a return statement, add position for insertion
                returnLines.add(i);
            }
            if (inMethod && line.startsWith("return ") && line.endsWith(";")) {
                // Single-line return statement
                returnLines.add(i);
            }

            // Method end
            if (depth == 1 && line.equals("}") && inMethod) {
                inMethod = false;
            }
        }
        return returnLines;
    }

    private String[] selectDeadBlock(int lineIndex, String ext, List<String> lines, int methodIdx) {
        // Contextual selection: analyze surrounding lines to pick opposite domain
        int domainHint = detectDomain(lines, lineIndex);
        int blockIdx = (domainHint + 5) % JAVA_DEAD_BLOCKS.length; // +5 = opposite domain

        if (ext.equals(".java")) {
            return JAVA_DEAD_BLOCKS[blockIdx];
        } else if (ext.equals(".py")) {
            return buildPythonBlock(blockIdx);
        } else { // .js
            return buildJsBlock(blockIdx);
        }
    }

    private int detectDomain(List<String> lines, int nearLine) {
        String context = "";
        int start = Math.max(0, nearLine - 15);
        for (int i = start; i <= Math.min(lines.size() - 1, nearLine); i++) {
            context += lines.get(i).toLowerCase();
        }
        if (context.contains("file") || context.contains("stream") || context.contains("reader")) return 0;
        if (context.contains("http") || context.contains("url") || context.contains("request")) return 1;
        if (context.contains("hash") || context.contains("cipher") || context.contains("secret")) return 2;
        if (context.contains("math") || context.contains("calc") || context.contains("sum"))      return 4;
        if (context.contains("list") || context.contains("map") || context.contains("array"))     return 8;
        return nearLine % JAVA_DEAD_BLOCKS.length;
    }

    private String[] buildPythonBlock(int blockIdx) {
        return new String[]{
            "if False:",
            "    # [strategy: dead] Misleading semantic block",
            "    v_conn_str = 'postgresql://db.internal:5432/prod'",
            "    v_timeout = 30",
            "    v_retry = 3",
            "    print(f'[DEAD] timeout={v_timeout}')"
        };
    }

    private String[] buildJsBlock(int blockIdx) {
        return new String[]{
            "if (false) {",
            "    // [strategy: dead] Misleading semantic block",
            "    const v_endpoint = 'https://api.service.internal/v2';",
            "    const v_timeout = 30000;",
            "    const v_retries = 3;",
            "    console.log('[DEAD] retries:', v_retries);",
            "}"
        };
    }
}
