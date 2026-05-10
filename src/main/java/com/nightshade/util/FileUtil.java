package com.nightshade.util;

import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * File I/O helper using only BufferedReader / BufferedWriter (no database).
 *
 * Spec requirement: ALL file access through these standard Java I/O classes.
 */
public class FileUtil {

    private static final DateTimeFormatter LOG_FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Reads a source file into a SourceFile object.
     * Preserves all lines including empty ones (they matter for indentation tracking).
     */
    public SourceFile read(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return new SourceFile(file.getAbsolutePath(), lines);
    }

    /**
     * Writes the obfuscated file to the output directory, preserving
     * the relative directory structure.
     */
    public void write(ObfuscationResult result, File inputRoot, File outputRoot) throws IOException {
        String absolutePath = result.getObfuscatedFile().getAbsolutePath();
        String relativePath = computeRelativePath(absolutePath, inputRoot.getAbsolutePath());

        File outFile = new File(outputRoot, relativePath);
        outFile.getParentFile().mkdirs();

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8))) {
            for (String line : result.getObfuscatedFile().getObfuscatedLines()) {
                writer.write(line);
                writer.newLine();
            }
        }
    }

    /**
     * Appends a timestamped entry to nightshade_run.log in the output directory.
     * Format: [2026-05-03 11:23:45] [LEVEL] message
     */
    public void appendLog(File outputRoot, String level, String message) {
        File logFile = new File(outputRoot, "nightshade_run.log");
        outputRoot.mkdirs();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(logFile, true), StandardCharsets.UTF_8))) {
            writer.write(String.format("[%s] [%s] %s",
                LocalDateTime.now().format(LOG_FMT), level, message));
            writer.newLine();
        } catch (IOException ignored) {
            // Log failure is non-fatal
        }
    }

    /**
     * Writes a full run summary log after processing completes.
     */
    public void writeRunLog(List<ObfuscationResult> results, File outputRoot) throws IOException {
        File logFile = new File(outputRoot, "nightshade_run.log");
        outputRoot.mkdirs();
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(logFile, false), StandardCharsets.UTF_8))) {
            writer.write("Nightshade v3.5.0 — Run Log");
            writer.newLine();
            writer.write("Generated: " + LocalDateTime.now().format(LOG_FMT));
            writer.newLine();
            writer.write("=".repeat(60));
            writer.newLine();
            writer.newLine();

            int totalRenamed = 0, totalDead = 0, totalComments = 0, totalStrings = 0;

            for (ObfuscationResult r : results) {
                writer.write(String.format("[%s] [INFO] %s | entropy=%.3f | renamed=%d dead=%d comments=%d strings=%d",
                    LocalDateTime.now().format(LOG_FMT),
                    r.getOriginalFile().getFileName(),
                    r.getEntropyScore(),
                    r.getRenamedIdentifiers(),
                    r.getDeadBlocksInjected(),
                    r.getCommentsPoisoned(),
                    r.getStringsEncoded()));
                writer.newLine();
                totalRenamed  += r.getRenamedIdentifiers();
                totalDead     += r.getDeadBlocksInjected();
                totalComments += r.getCommentsPoisoned();
                totalStrings  += r.getStringsEncoded();
            }

            writer.newLine();
            writer.write("=".repeat(60));
            writer.newLine();
            writer.write(String.format("TOTAL | files=%d renamed=%d dead=%d comments=%d strings=%d",
                results.size(), totalRenamed, totalDead, totalComments, totalStrings));
            writer.newLine();
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String computeRelativePath(String absoluteFile, String absoluteRoot) {
        if (!absoluteRoot.endsWith(File.separator)) {
            absoluteRoot = absoluteRoot + File.separator;
        }
        if (absoluteFile.startsWith(absoluteRoot)) {
            return absoluteFile.substring(absoluteRoot.length());
        }
        return new File(absoluteFile).getName(); // fallback: just filename
    }
}
