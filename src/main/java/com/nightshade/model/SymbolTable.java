package com.nightshade.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Maps original identifier strings to their deterministic replacements.
 *
 * Design decisions:
 *  1. A single UUID salt is generated once per run and mixed into every hash.
 *     Same input file processed in two runs produces different output â€”
 *     this prevents adaptive attacks.
 *  2. Scope-aware resolution: the key is (scopePath + "::" + original), so
 *     "result" in method A and "result" in method B get different replacements.
 *  3. isUserDefined() guards against renaming keywords and stdlib types.
 */
public class SymbolTable {

    /** Full set of Java reserved words + common stdlib types we must not rename. */
    private static final Set<String> PROTECTED_IDENTIFIERS = Set.of(
        // Java keywords (all 67)
        "abstract","assert","boolean","break","byte","case","catch","char","class",
        "const","continue","default","do","double","else","enum","extends","final",
        "finally","float","for","goto","if","implements","import","instanceof","int",
        "interface","long","native","new","package","private","protected","public",
        "return","short","static","strictfp","super","switch","synchronized","this",
        "throw","throws","transient","try","var","void","volatile","while","record",
        "sealed","permits","yield","null","true","false",
        // Common stdlib types
        "String","System","Object","Class","Exception","RuntimeException","Error",
        "Throwable","Override","Deprecated","SuppressWarnings","FunctionalInterface",
        "SafeVarargs","Retention","Target","Documented","Inherited",
        // JavaFX types
        "Stage","Scene","Application","Platform","FXMLLoader","FXML","Initializable",
        "Controller","initialize","start","stop","launch",
        // Java collections + common
        "ArrayList","LinkedList","HashMap","HashSet","TreeMap","TreeSet","LinkedHashMap",
        "List","Map","Set","Collection","Iterator","Optional","Stream","Arrays","Collections",
        "Math","Integer","Long","Double","Float","Boolean","Character","Byte","Short",
        "StringBuilder","StringBuffer","CharSequence","Comparable","Iterable",
        "Runnable","Thread","Callable","Future","ExecutorService",
        // I/O
        "File","Path","Files","Paths","BufferedReader","BufferedWriter","FileReader",
        "FileWriter","InputStreamReader","OutputStreamWriter","FileInputStream","FileOutputStream",
        "PrintWriter","Scanner","IOException","FileNotFoundException",
        // Annotations
        "main","args","toString","equals","hashCode","compareTo","clone","finalize",
        "getClass","notify","notifyAll","wait","length","size","get","put","add",
        "remove","contains","isEmpty","clear","iterator","next","hasNext",
        // Java stdlib methods - must not be renamed to preserve compilation
        "abs","min","max","pow","sqrt","random","floor","ceil","round","exp","log",
        "append","insert","delete","deleteCharAt","replace","reverse","setLength","charAt",
        "valueOf","format","split","trim","substring","indexOf","lastIndexOf","startsWith","endsWith",
        "keySet","values","entrySet","containsKey","containsValue",
        "out","in","err","println","print","printf",
        "setTitle","stream","toUpperCase","getItems"
    );

    private final Map<String, String> mapping;   // scoped-key â†’ replacement
    private final String sessionSalt;
    private final Set<String> dynamicProtected = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    public SymbolTable() {
        this.mapping = new HashMap<>();
        this.sessionSalt = UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Returns the replacement for the given identifier in the given scope.
     * Creates a new replacement if one doesn't exist.
     *
     * @param original  The original identifier name
     * @param scopePath The scope path (e.g. "MyClass.myMethod")
     */
    public String resolve(String original, String scopePath) {
        String key = scopePath + "::" + original;
        return mapping.computeIfAbsent(key, k ->
            com.nightshade.util.HashUtil.generateReplacement(original, sessionSalt + scopePath));
    }

    /**
     * Scope-unaware resolve — for backward compatibility and global symbols.
     */
    public String resolve(String original) {
        return resolve(original, "global");
    }

    /**
     * Protects a specific identifier from being renamed (e.g., public APIs).
     */
    public void protect(String identifier) {
        if (identifier != null && !identifier.isEmpty()) {
            dynamicProtected.add(identifier);
        }
    }

    /**
     * Returns true if this token is a user-defined name that may be renamed.
     * False for keywords, stdlib types, and other protected identifiers.
     */
    public boolean isUserDefined(String token) {
        if (token == null || token.isEmpty()) return false;
        // Protected set check
        if (PROTECTED_IDENTIFIERS.contains(token)) return false;
        if (dynamicProtected.contains(token)) return false;
        // Must start with letter or underscore (not a literal that slipped through)
        if (!Character.isLetter(token.charAt(0)) && token.charAt(0) != '_') return false;
        // Uppercase-only names are likely constants or type names â€” protect them
        // (but allow mixed-case like myVar or MY_CONST partially)
        // We protect all-caps identifiers of length > 1 (TRUE, FALSE already in set)
        if (token.length() > 1 && token.equals(token.toUpperCase()) && !token.contains("_")) {
            return false; // e.g. MAX, MIN â€” often constants or enums from stdlib
        }
        // Protect Java convention: Class/Type names start with uppercase
        if (Character.isUpperCase(token.charAt(0))) return false;
        return true;
    }

    public Map<String, String> getFullMapping() {
        return Collections.unmodifiableMap(mapping);
    }

    public String getSessionSalt() { return sessionSalt; }

    public int getMappingSize() { return mapping.size(); }
}
