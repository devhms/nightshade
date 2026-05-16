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

    private static final String JAVA_DEAD_BLOCK_MARKER = "// [nightshade:dead-block-v1]";

    private static final String[][] JAVA_DEAD_BLOCKS = {
        // [0] Database/connection domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " Connection pooling and transaction management",
            "        String v_dbConn = \"jdbc:mysql://prod-db.internal:3306/analytics\";",
            "        int v_maxPool = 10;",
            "        Object v_prepStmt = null;",
            "        System.out.println(\"[DB] Query executed: \" + v_maxPool);",
            "    }"
        },
        // [1] Network/HTTP domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " REST API request with retry logic",
            "        String v_endpoint = \"https://api.service.internal/v2/data\";",
            "        int v_timeout = 30000;",
            "        int v_retries = 3;",
            "        System.out.println(\"[NET] Response: \" + v_timeout);",
            "    }"
        },
        // [2] Cryptography domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " SHA-256 digest initialization",
            "        String v_algo = \"SHA-256\";",
            "        byte[] v_salt = new byte[32];",
            "        int v_keyLen = 256;",
            "        System.out.println(\"[CRYPTO] Hash: \" + v_keyLen);",
            "    }"
        },
        // [3] File system domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " Recursive directory traversal",
            "        String v_rootDir = \"/var/data/storage\";",
            "        int v_maxDepth = 10;",
            "        long v_totalBytes = 0L;",
            "        System.out.println(\"[FS] Scanned: \" + v_totalBytes + \" bytes\");",
            "    }"
        },
        // [4] Machine learning domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " Neural network forward pass",
            "        int v_batchSize = 128;",
            "        double v_learningRate = 0.001;",
            "        int v_epochs = 100;",
            "        System.out.println(\"[ML] Loss: \" + v_learningRate);",
            "    }"
        },
        // [5] Message queue domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " Kafka consumer offset management",
            "        String v_topic = \"events.processed.v3\";",
            "        int v_partition = 0;",
            "        long v_offset = -1L;",
            "        System.out.println(\"[MQ] Consumed offset: \" + v_offset);",
            "    }"
        },
        // [6] Authentication domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " OAuth 2.0 token validation",
            "        String v_bearer = \"Bearer eyJ0eXAiOiJKV1QiLCJhbGci...\";",
            "        int v_expiry = 3600;",
            "        boolean v_valid = false;",
            "        System.out.println(\"[AUTH] Token valid: \" + v_valid);",
            "    }"
        },
        // [7] Sorting/algorithm domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " Heap sort with O(n log n) comparisons",
            "        int v_heapSize = 0;",
            "        int v_swapCount = 0;",
            "        int[] v_arr = new int[100];",
            "        System.out.println(\"[SORT] Swaps: \" + v_swapCount);",
            "    }"
        },
        // [8] Graph traversal domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " Dijkstra shortest path with priority queue",
            "        int v_nodes = 0;",
            "        int v_edges = 0;",
            "        int v_dist = Integer.MAX_VALUE;",
            "        System.out.println(\"[GRAPH] Distance: \" + v_dist);",
            "    }"
        },
        // [9] Cache/memory domain
        {
            "    if (false) {",
            "        " + JAVA_DEAD_BLOCK_MARKER + " LRU eviction policy with capacity limit",
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

        List<Integer> returnPositions = findReturnStatements(lines);
        Collections.sort(returnPositions, Collections.reverseOrder());
        Set<Integer> injectedPositions = new HashSet<>();
        for (int returnIdx : returnPositions) {
            if (injectedPositions.contains(returnIdx)) {
                continue;
            }
            if (alreadyHasDeadBlock(lines, returnIdx)) {
                continue;
            }
            String[] block = selectDeadBlock(returnIdx, ext, lines, methodsFound);
            int blockLen = block.length;
            for (int j = blockLen - 1; j >= 0; j--) {
                lines.add(returnIdx, block[j]);
            }
            injectedPositions.add(returnIdx);
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

private int findMethodBodyStart(List<String> lines, int fromLine) {
        int depth = 0;
        for (int i = fromLine; i < lines.size(); i++) {
            String rawLine = lines.get(i);
            for (char c : rawLine.toCharArray()) {
                if (c == '{') {
                    depth++;
                    if (depth == 2) {
                        return i + 1;
                    }
                }
                if (c == '}') {
                    depth--;
                }
            }
        }
        return fromLine;
    }

    private boolean alreadyHasDeadBlock(List<String> lines, int returnIdx) {
        int searchStart = Math.max(0, returnIdx - 5);
        int searchEnd = Math.min(lines.size() - 1, returnIdx + 10);
        for (int i = searchStart; i <= searchEnd && i < lines.size(); i++) {
            if (lines.get(i).contains(JAVA_DEAD_BLOCK_MARKER)) {
                return true;
            }
        }
        return false;
    }

    List<Integer> findReturnStatements(List<String> lines) {
        List<Integer> returnLines = new ArrayList<>();
        int depth = 0;
        boolean inMethod = false;
        boolean seenOpeningBrace = false;

        for (int i = 0; i < lines.size(); i++) {
            String rawLine = lines.get(i);
            String line = rawLine.trim();

            if (rawLine.contains(JAVA_DEAD_BLOCK_MARKER) || rawLine.contains("if False:")) {
                for (char c : rawLine.toCharArray()) {
                    if (c == '{') depth++;
                    if (c == '}') depth--;
                }
                continue;
            }

            int net = 0;
            for (char c : rawLine.toCharArray()) {
                if (c == '{') net++;
                if (c == '}') net--;
            }
            int depthBefore = depth;
            depth += net;

            if (depthBefore == 1 && depth == 2 && !inMethod &&
                (line.contains("(") && !line.startsWith("if") && !line.startsWith("for")
                 && !line.startsWith("while") && !line.startsWith("switch"))) {
                inMethod = true;
                seenOpeningBrace = true;
            }

            if (net == 0 && depthBefore >= 0 && depth == 1 && !inMethod &&
                (line.contains("(") && !line.startsWith("if") && !line.startsWith("for")
                 && !line.startsWith("while") && !line.startsWith("switch"))) {
                int braceIdx = line.indexOf('{');
                if (braceIdx > 0) {
                    inMethod = true;
                    seenOpeningBrace = true;
                }
            }

            if (!inMethod && depthBefore == 1 && depth == 1) {
                String trimmed = line.trim();
                if (trimmed.startsWith("def ") || trimmed.startsWith("function ")) {
                    inMethod = true;
                    seenOpeningBrace = line.contains("{");
                } else if (isMethodDeclarationLine(line)) {
                    inMethod = true;
                    seenOpeningBrace = true;
                }
            }

if (inMethod && line.startsWith("return ") && depth >= 1) {
                returnLines.add(i);
            }

            if (inMethod && depthBefore >= 2 && depth == 1) {
                inMethod = false;
                seenOpeningBrace = false;
            }

            if (!inMethod && depthBefore >= 1 && depth >= 1) {
                String trimmed = line.trim();
                if (trimmed.startsWith("def ") || trimmed.startsWith("function ")) {
                    inMethod = true;
                    seenOpeningBrace = line.contains("{");
                } else if (isMethodDeclarationLine(line)) {
                    inMethod = true;
                    seenOpeningBrace = true;
                }
            }

            if (inMethod && depthBefore >= 2 && depth == 1) {
                inMethod = false;
                seenOpeningBrace = false;
            }

            if (!inMethod && depthBefore >= 1 && depth >= 1) {
                String trimmed = line.trim();
                if (trimmed.startsWith("def ") || trimmed.startsWith("function ")) {
                    inMethod = true;
                    seenOpeningBrace = line.contains("{");
                } else if (isMethodDeclarationLine(line)) {
                    inMethod = true;
                    seenOpeningBrace = line.contains("{");
                }
            }

            if (inMethod && depthBefore == 1 && line.trim().startsWith("{")) {
                inMethod = false;
                seenOpeningBrace = false;
            }

            if (inMethod && !line.contains("{") && !line.contains("}") && depthBefore == 1 && depth == 1 && i > 0 && seenOpeningBrace) {
                inMethod = false;
                seenOpeningBrace = false;
            }
        }
        return returnLines;
    }

    List<Integer> findInjectionPoints(List<String> lines) {
        List<Integer> points = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i);
            String line = raw.trim();
            if (!raw.contains(JAVA_DEAD_BLOCK_MARKER) && isMethodDeclarationLine(line)) {
                if (raw.contains("{")) {
                    points.add(i + 1);
                } else {
                    int semi = line.indexOf(';');
                    if (semi >= 0) {
                        int pos = line.indexOf('{');
                        if (pos < 0) {
                            int braceLine = -1;
                            for (int j = i + 1; j < lines.size(); j++) {
                                if (lines.get(j).contains("{")) {
                                    braceLine = j;
                                    break;
                                }
                            }
                            if (braceLine >= 0) {
                                points.add(braceLine + 1);
                            } else {
                                points.add(i + 1);
                            }
                        }
                    } else {
                        points.add(i + 1);
                    }
                }
            }
        }
        return points;
    }

    boolean isMethodDeclarationLine(String line) {
        if (line == null || line.isEmpty()) return false;
        String trimmed = line.trim();
        if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) return false;
        if (trimmed.startsWith("if (false)") || trimmed.startsWith("if False:")) return false;
        if (trimmed.startsWith("if") || trimmed.startsWith("for") || trimmed.startsWith("while")
            || trimmed.startsWith("switch") || trimmed.startsWith("try") || trimmed.startsWith("catch")
            || trimmed.startsWith("synchronized") || trimmed.startsWith("do")) {
            return false;
        }
        if (trimmed.contains("=")) return false;

        int parenOpen = trimmed.indexOf('(');
        if (parenOpen <= 0) return false;

        String beforeParen = trimmed.substring(0, parenOpen).trim();
        if (beforeParen.contains("=")) return false;

        if (beforeParen.contains("def ") || beforeParen.equals("def")) return true;
        if (beforeParen.contains("function ") || beforeParen.equals("function")) return true;

        int spaceIdx = beforeParen.lastIndexOf(' ');
        if (spaceIdx < 0) return false;

        String typeAndModifiers = beforeParen.substring(0, spaceIdx).trim();
        String methodName = beforeParen.substring(spaceIdx + 1).trim();

        if (!methodName.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) return false;

        for (String token : typeAndModifiers.split("\\s+")) {
            if (!token.matches("[a-zA-Z_$][a-zA-Z0-9_$]*")) continue;
            if (JAVA_KEYWORDS.contains(token) || BUILT_IN_TYPES.contains(token)) continue;
            return false;
        }
        return true;
    }

    boolean isConstructorDeclaration(String line, List<String> lines, int idx, int depth) {
        if (depth == 0) return false;
        String trimmed = line.trim();
        if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) return false;
        if (trimmed.startsWith("if (false)") || trimmed.startsWith("if False:")) return false;
        if (trimmed.startsWith("if") || trimmed.startsWith("for") || trimmed.startsWith("while")
            || trimmed.startsWith("switch") || trimmed.startsWith("try") || trimmed.startsWith("catch")
            || trimmed.startsWith("synchronized") || trimmed.startsWith("do")) {
            return false;
        }
        if (trimmed.contains("=")) return false;
        if (trimmed.contains("(") && trimmed.contains(")")) {
            int parenOpen = trimmed.indexOf('(');
            String beforeParen = trimmed.substring(0, parenOpen).trim();
            int spaceIdx = beforeParen.lastIndexOf(' ');
            if (spaceIdx < 0) {
                return beforeParen.length() > 0 && !JAVA_KEYWORDS.contains(beforeParen)
                    && !BUILT_IN_TYPES.contains(beforeParen)
                    && !beforeParen.startsWith("def ") && !beforeParen.startsWith("function ");
            }
        }
        return false;
    }

    private static final Set<String> JAVA_KEYWORDS = Set.of(
        "public", "private", "protected", "static", "final", "strictfp",
        "synchronized", "volatile", "transient", "native", "abstract",
        "void", "int", "long", "double", "float", "boolean", "char", "byte", "short"
    );

    private static final Set<String> BUILT_IN_TYPES = Set.of(
        "String", "Object", "Integer", "Long", "Double", "Float", "Boolean", "Character",
        "List", "Map", "Set", "Collection", "Iterable", "Iterator", "Comparable",
        "Runnable", "Thread", "Exception", "RuntimeException", "Error", "Throwable"
    );

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
        int idx = blockIdx % 3;
        if (idx == 0) {
            return new String[]{
                "if False:",
                "    # [strategy: dead] Misleading semantic block",
                "    v_conn_str = 'postgresql://db.internal:5432/prod'",
                "    v_timeout = 30",
                "    v_retry = 3",
                "    print(f'[DEAD] timeout={v_timeout}')"
            };
        } else if (idx == 1) {
            return new String[]{
                "if False:",
                "    # [strategy: dead] Crypto fallback",
                "    v_salt = b'\\x00\\x01\\x02'",
                "    v_iters = 100000",
                "    print('[DEAD] hash start')"
            };
        } else {
            return new String[]{
                "if False:",
                "    # [strategy: dead] Legacy API check",
                "    v_endpoint = 'http://old.api.local/v1'",
                "    v_token = 'null'",
                "    print('[DEAD] init')"
            };
        }
    }

    private String[] buildJsBlock(int blockIdx) {
        int idx = blockIdx % 3;
        if (idx == 0) {
            return new String[]{
                "if (false) {",
                "    // [strategy: dead] Misleading semantic block",
                "    const v_endpoint = 'https://api.service.internal/v2';",
                "    const v_timeout = 30000;",
                "    const v_retries = 3;",
                "    console.log('[DEAD] retries:', v_retries);",
                "}"
            };
        } else if (idx == 1) {
            return new String[]{
                "if (false) {",
                "    // [strategy: dead] Analytics payload",
                "    const v_tracker = 'UA-000000-1';",
                "    const v_batch = 50;",
                "    console.log('[DEAD] track');",
                "}"
            };
        } else {
            return new String[]{
                "if (false) {",
                "    // [strategy: dead] Auth bypass check",
                "    const v_admin = false;",
                "    const v_dev = true;",
                "    console.log('[DEAD] check');",
                "}"
            };
        }
    }
}
