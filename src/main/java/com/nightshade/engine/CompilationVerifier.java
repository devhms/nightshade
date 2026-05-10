package com.nightshade.engine;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Ensures that obfuscated code remains compilable.
 * Useful as a safety net against syntax-breaking transformations.
 */
public class CompilationVerifier {

    private static final Set<String> SKIP_DIRS = Set.of(
        "_nightshade_output", "nightshade-output", "target", "build", "out"
    );

    public boolean hasJavaFiles(File sourceDirectory) {
        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(sourceDirectory, javaFiles);
        return !javaFiles.isEmpty();
    }

    /**
     * Attempts to compile all Java files in the given directory.
     * @param sourceDirectory The root directory containing obfuscated Java files.
     * @return true if compilation succeeds, false if it fails.
     */
    public boolean verify(File sourceDirectory) {
        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(sourceDirectory, javaFiles);
        
        if (javaFiles.isEmpty()) {
            if (hasNonJavaFiles(sourceDirectory)) {
                System.out.println("[VERIFY] SKIPPED: No Java files found. Compilation verification only applies to .java files.");
            }
            return true;
        }
        
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            System.err.println("[WARN] JavaCompiler not available (requires JDK, not just JRE). Skipping verification.");
            return true;
        }
        
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
                if (!SKIP_DIRS.contains(child.getName())) {
                    collectJavaFiles(child, files);
                }
            } else if (child.getName().endsWith(".java")) {
                files.add(child);
            }
        }
    }
    
    private boolean hasNonJavaFiles(File dir) {
        File[] children = dir.listFiles();
        if (children == null) return false;
        for (File child : children) {
            if (child.isDirectory()) {
                if (hasNonJavaFiles(child)) return true;
            } else if (!child.getName().endsWith(".java")) {
                return true;
            }
        }
        return false;
    }
}
