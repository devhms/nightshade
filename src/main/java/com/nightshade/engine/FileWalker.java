package com.nightshade.engine;

import com.nightshade.model.SourceFile;
import com.nightshade.util.FileUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Recursively walks a directory and returns a sorted list of SourceFile objects.
 *
 * Filters:
 *   - Only .java, .py, .js files
 *   - Skips: .git, node_modules, target, __pycache__, .idea, .vscode, build, dist
 *
 * OOP: Uses a FileFilter (functional interface) to abstract the extension check,
 * demonstrating ABSTRACTION.
 */
public class FileWalker {

    private static final Set<String> ALLOWED_EXTENSIONS =
        Set.of(".java", ".py", ".js");

    private static final Set<String> SKIP_DIRS = Set.of(
        ".git", "node_modules", "target", "__pycache__",
        ".idea", ".vscode", "build", "dist", ".gradle", "out"
    );

    private final FileUtil fileUtil = new FileUtil();

    /**
     * Walks the directory tree and returns all eligible source files.
     * Results are sorted alphabetically by absolute path.
     *
     * @throws IOException if the root directory cannot be read
     */
    public List<SourceFile> walk(File root) throws IOException {
        List<SourceFile> files = new ArrayList<>();
        collectFiles(root, files);
        files.sort(Comparator.comparing(SourceFile::getAbsolutePath));
        return files;
    }

    private void collectFiles(File dir, List<SourceFile> acc) throws IOException {
        if (dir == null || !dir.exists()) return;

        File[] entries = dir.listFiles();
        if (entries == null) return;

        for (File entry : entries) {
            if (entry.isDirectory()) {
                if (!SKIP_DIRS.contains(entry.getName())) {
                    collectFiles(entry, acc);
                }
            } else if (entry.isFile() && isAllowedExtension(entry.getName())) {
                try {
                    acc.add(fileUtil.read(entry));
                } catch (IOException e) {
                    // Non-fatal: log and continue to next file
                    System.err.println("[WARN] Could not read: " + entry.getAbsolutePath() + " — " + e.getMessage());
                }
            }
        }
    }

    private boolean isAllowedExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0) return false;
        return ALLOWED_EXTENSIONS.contains(filename.substring(dot));
    }

    public Set<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }
}
