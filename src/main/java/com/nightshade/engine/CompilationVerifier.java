package com.nightshade.engine;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Ensures that obfuscated code remains compilable.
 * Useful as a safety net against syntax-breaking transformations.
 */
public class CompilationVerifier {

    /**
     * Attempts to compile all Java files in the given directory.
     * @param sourceDirectory The root directory containing obfuscated Java files.
     * @return true if compilation succeeds, false if it fails.
     */
    public boolean verify(File sourceDirectory) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("[WARN] JavaCompiler not available (requires JDK, not just JRE). Skipping verification.");
            return true; // Skip if no compiler available
        }
        
        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(sourceDirectory, javaFiles);
        if (javaFiles.isEmpty()) return true; // nothing to verify
        
        List<String> filePaths = new ArrayList<>();
        for (File f : javaFiles) {
            filePaths.add(f.getAbsolutePath());
        }
        
        // redirect output to suppress noisy compile errors on stdout
        System.out.println("  [VERIFY] Compiling " + javaFiles.size() + " Java files...");
        int result = compiler.run(null, null, null, filePaths.toArray(new String[0]));
        return result == 0;
    }
    
    private void collectJavaFiles(File dir, List<File> files) {
        File[] children = dir.listFiles();
        if (children == null) return;
        for (File child : children) {
            if (child.isDirectory()) {
                collectJavaFiles(child, files);
            } else if (child.getName().endsWith(".java")) {
                files.add(child);
            }
        }
    }
}
