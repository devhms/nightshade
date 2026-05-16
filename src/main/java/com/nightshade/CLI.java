package com.nightshade;

import com.nightshade.engine.FileWalker;
import com.nightshade.engine.Lexer;
import com.nightshade.engine.ObfuscationEngine;
import com.nightshade.engine.Parser;
import com.nightshade.engine.Serializer;
import com.nightshade.engine.EntropyCalculator;
import com.nightshade.engine.CompilationVerifier;
import com.nightshade.engine.PoisoningReport;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.strategy.CommentPoisoner;
import com.nightshade.strategy.DeadCodeInjector;
import com.nightshade.strategy.EntropyScrambler;
import com.nightshade.strategy.PoisonStrategy;
import com.nightshade.strategy.StringEncoder;
import com.nightshade.strategy.WhitespaceDisruptor;
import com.nightshade.strategy.SemanticInverter;
import com.nightshade.strategy.ControlFlowFlattener;
import com.nightshade.strategy.WatermarkEncoder;
import com.nightshade.util.FileUtil;
import com.nightshade.util.LogService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CLI mode for Nightshade.
 *
 * Usage:
 *   java -jar nightshade.jar --input ./src --output ./out --strategies all
 *   java -jar nightshade.jar --input ./src --output ./out --strategies entropy,deadcode
 *   java -jar nightshade.jar --help
 */
public class CLI {

    private static final String BANNER =
        "\n" +
        "  ███╗   ██╗██╗ ██████╗ ██╗  ██╗████████╗███████╗██╗  ██╗ █████╗ ██████╗ ███████╗\n" +
        "  ████╗  ██║██║██╔════╝ ██║  ██║╚══██╔══╝██╔════╝██║  ██║██╔══██╗██╔══██╗██╔════╝\n" +
        "  ██╔██╗ ██║██║██║  ███╗███████║   ██║   ███████╗███████║███████║██║  ██║█████╗  \n" +
        "  ██║╚██╗██║██║██║   ██║██╔══██║   ██║   ╚════██║██╔══██║██╔══██║██║  ██║██╔══╝  \n" +
        "  ██║ ╚████║██║╚██████╔╝██║  ██║   ██║   ███████║██║  ██║██║  ██║██████╔╝███████╗\n" +
        "  ╚═╝  ╚═══╝╚═╝ ╚═════╝ ╚═╝  ╚═╝   ╚═╝   ╚══════╝╚═╝  ╚═╝╚═╝  ╚═╝╚═════╝ ╚══════╝\n" +
        "  v3.5.0 | LLM Training Data Poisoning Engine\n" +
        "  https://github.com/devhms/nightshade\n";

    private static final String BANNER_ASCII =
        "\n" +
        "  NIGHTSHADE v3.5.0\n" +
        "  LLM Training Data Poisoning Engine\n" +
        "  https://github.com/devhms/nightshade\n";

    private enum LogLevel { QUIET, NORMAL, VERBOSE }

    public static void main(String[] args) {
        run(args);
    }

    public static void run(String[] args) {
        printBanner();

        if (args.length == 0) {
            printHelp();
            return;
        }

        String inputPath = null;
        String outputPath = null;
        String strategiesArg = "all";
        LogLevel logLevel = LogLevel.NORMAL;
        double entropyThreshold = 0.65;

        boolean dryRun = false;
        boolean verify = false;
        boolean generateReport = false;
        boolean listStrategies = false;
        boolean libraryMode = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            try {
                switch (arg) {
                    case "--input", "-i"       -> inputPath = args[++i];
                    case "--output", "-o"      -> outputPath = args[++i];
                    case "--strategies", "-s"  -> strategiesArg = args[++i];
                    case "--threshold", "--entropy-threshold", "-t" -> entropyThreshold = Double.parseDouble(args[++i]);
                    case "--verbose", "-v"     -> logLevel = LogLevel.VERBOSE;
                    case "--quiet", "-q"       -> logLevel = LogLevel.QUIET;
                    case "--dry-run"           -> dryRun = true;
                    case "--verify"            -> verify = true;
                    case "--library-mode"     -> libraryMode = true;
                    case "--report", "-r"      -> generateReport = true;
                    case "--version"           -> { System.out.println("Nightshade v3.5.0"); return; }
                    case "--help", "-h"        -> { printHelp(); return; }
                    case "--list-strategies"   -> { printStrategyList(); return; }
                    default -> logError("[WARN] Unknown argument: " + arg, logLevel);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                logError("[ERROR] Missing value for argument: " + arg, logLevel);
                System.exit(1);
            } catch (NumberFormatException e) {
                logError("[ERROR] Invalid number format for argument: " + arg, logLevel);
                System.exit(1);
            }
        }

        if (entropyThreshold < 0.0 || entropyThreshold > 1.0) {
            logError("[ERROR] Entropy threshold must be between 0.0 and 1.0", logLevel);
            System.exit(1);
        }

        if (inputPath == null) {
            logError("[ERROR] --input is required. Use --help for usage.", logLevel);
            System.exit(1);
        }

        File inputDir = new File(inputPath);
        if (!inputDir.exists()) {
            logError("[ERROR] Input path does not exist: " + inputPath, logLevel);
            System.exit(1);
        }

        File outputDir;
        if (outputPath != null) {
            outputDir = new File(outputPath);
        } else {
            File parent = inputDir.isFile() ? inputDir.getParentFile() : inputDir;
            String basePath = parent != null ? parent.getAbsolutePath() : new File(".").getAbsolutePath();
            outputDir = new File(basePath + "/_nightshade_output");
        }

        File effectiveInputDir = inputDir.isFile() 
            ? Optional.ofNullable(inputDir.getParentFile()).orElse(new File("."))
            : inputDir;

        List<PoisonStrategy> strategies = buildStrategies(strategiesArg);
        if (strategies.isEmpty()) {
            logError("[ERROR] No valid strategies specified. Options: all, entropy, deadcode, comments, strings, whitespace, semantic, controlflow, watermark", logLevel);
            System.exit(1);
        }

        logInfo("Input:  " + inputDir.getAbsolutePath(), logLevel);
        logInfo("Output: " + outputDir.getAbsolutePath(), logLevel);
        logInfo("Active strategies:", logLevel);
        
        int enabledCount = 0;
        int disabledCount = 0;
        for (PoisonStrategy s : strategies) {
            String status = s.isEnabled() ? "" : " (disabled)";
            logInfo("         • " + s.getName() + status, logLevel);
            if (s.isEnabled()) enabledCount++; else disabledCount++;
        }
        logInfo("Strategies enabled: " + enabledCount + "/" + strategies.size(), logLevel);
        logInfo("Entropy threshold: " + entropyThreshold, logLevel);
        if (libraryMode) {
            logInfo("Library mode: ENABLED (preserving public APIs)", logLevel);
        }
        logInfo("", logLevel);

        LogService logService = new LogService(logLevel == LogLevel.VERBOSE);
        Lexer lexer = new Lexer();
        Parser parser = new Parser();
        Serializer serializer = new Serializer();
        EntropyCalculator entropyCalc = new EntropyCalculator();
        ObfuscationEngine engine = new ObfuscationEngine(strategies, lexer, parser, serializer, entropyCalc, logService, entropyThreshold);
        FileUtil fileUtil = new FileUtil();

        try {
            long start = System.currentTimeMillis();
            FileWalker walker = new FileWalker();
            List<SourceFile> files = walker.walk(inputDir);
            
            if (files.isEmpty()) {
                logError("[WARN] No supported source files found (.java, .py, .js) in: " + inputPath, logLevel);
                System.exit(2);
            }
            
            int totalFiles = files.size();
            logInfo("Discovered " + totalFiles + " source files.", logLevel);

            List<ObfuscationResult> results = engine.process(files);

            int written = 0;
            for (int idx = 0; idx < results.size(); idx++) {
                ObfuscationResult result = results.get(idx);
                
                if (logLevel == LogLevel.VERBOSE) {
                    logInfo(String.format("Progress: %d/%d files processed (%d%%)...", 
                        idx + 1, totalFiles, ((idx + 1) * 100 / totalFiles)), logLevel);
                } else if (logLevel == LogLevel.NORMAL && totalFiles > 10) {
                    int percent = (idx + 1) * 100 / totalFiles;
                    if (percent % 25 == 0 && ((idx + 1) / (totalFiles / 4)) > ((idx) / (totalFiles / 4))) {
                        logInfo(String.format("Progress: %d/%d (%d%%)...", idx + 1, totalFiles, percent), logLevel);
                    }
                }
                
                if (!dryRun) {
                    fileUtil.write(result, effectiveInputDir, outputDir);
                    written++;
                }
            }

            if (!dryRun) fileUtil.writeRunLog(results, outputDir);
            
            if (verify && !dryRun) {
                CompilationVerifier verifier = new CompilationVerifier();
                if (!verifier.hasJavaFiles(outputDir)) {
                    logInfo("[VERIFY] SKIPPED: No Java files found. Compilation verification only applies to .java files.", logLevel);
                } else {
                    logInfo("[INFO] Verification requested. Running javac on obfuscated files...", logLevel);
                    boolean verified = verifier.verify(outputDir);
                    if (verified) {
                        logInfo("[VERIFY] SUCCESS: All files compiled successfully.", logLevel);
                    } else {
                        logError("[VERIFY] FAILED: Obfuscated code contains syntax errors.", logLevel);
                        System.exit(1);
                    }
                }
            }
            long elapsed = System.currentTimeMillis() - start;

            if (generateReport && !dryRun && !results.isEmpty()) {
                try {
                    String report = PoisoningReport.generate(results);
                    File reportFile = new File(outputDir, "nightshade_report.md");
                    java.nio.file.Files.writeString(reportFile.toPath(), report);
                    logInfo("[INFO] Report written to: " + reportFile.getName(), logLevel);
                } catch (Exception e) {
                    logInfo("[WARN] Failed to generate report: " + e.getMessage(), logLevel);
                }
            }

            if (logLevel != LogLevel.QUIET) {
                printSummary(results, written, outputDir, elapsed, dryRun, logLevel);
            } else {
                System.out.println("Done. " + written + " files processed in " + elapsed + "ms");
            }

        } catch (Exception e) {
            logError("[ERROR] " + e.getMessage(), logLevel);
            if (logLevel == LogLevel.VERBOSE) e.printStackTrace();
            System.exit(1);
        }
    }

    private static void printBanner() {
        String encoding = System.getProperty("stdout.encoding");
        boolean isUtf8 = encoding != null && encoding.toLowerCase().contains("utf");
        if (!isUtf8 && System.console() != null) {
            String charset = java.nio.charset.Charset.defaultCharset().name().toLowerCase();
            isUtf8 = charset.contains("utf");
        }
        System.out.println(isUtf8 ? BANNER : BANNER_ASCII);
    }

    private static void logInfo(String msg, LogLevel level) {
        if (level != LogLevel.QUIET) System.out.println(msg);
    }

    private static void logError(String msg, LogLevel level) {
        if (level == LogLevel.QUIET) System.out.println(msg);
        else System.err.println(msg);
    }

    private static void printSummary(List<ObfuscationResult> results, int written, File outputDir, long elapsed, boolean dryRun, LogLevel level) {
        System.out.println();
        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  NIGHTSHADE COMPLETE                     ║");
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.printf("║  %-38s ║%n", "Files processed: " + results.size());
        System.out.printf("║  %-38s ║%n", "Files written:   " + written);

        double avgEntropy = results.stream()
            .mapToDouble(ObfuscationResult::getEntropyScore)
            .average().orElse(0.0);
        System.out.printf("║  %-38s ║%n", String.format("Avg entropy:     %.3f", avgEntropy));
        System.out.printf("║  %-38s ║%n", "Time elapsed:    " + elapsed + "ms");
        
        System.out.println("╠══════════════════════════════════════════╣");
        System.out.println("║  Strategy Breakdown:                      ║");
        
        int totalRenamed = results.stream().mapToInt(ObfuscationResult::getRenamedIdentifiers).sum();
        int totalDead = results.stream().mapToInt(ObfuscationResult::getDeadBlocksInjected).sum();
        int totalComments = results.stream().mapToInt(ObfuscationResult::getCommentsPoisoned).sum();
        int totalStrings = results.stream().mapToInt(ObfuscationResult::getStringsEncoded).sum();
        
        String renamedStr = totalRenamed > 0 ? String.format("✓ Entropy Scrambling  — %d identifiers renamed", totalRenamed) : "○ Entropy Scrambling  — skipped";
        String deadStr = totalDead > 0 ? String.format("✓ Dead Code Injection — %d blocks injected", totalDead) : "○ Dead Code Injection — skipped";
        String commentStr = totalComments > 0 ? String.format("✓ Comment Poisoning   — %d comments replaced", totalComments) : "○ Comment Poisoning   — skipped";
        String stringStr = totalStrings > 0 ? String.format("✓ String Encoding     — %d strings encoded", totalStrings) : "○ String Encoding     — skipped";
        
        System.out.printf("║  %-40s║%n", renamedStr);
        System.out.printf("║  %-40s║%n", deadStr);
        System.out.printf("║  %-40s║%n", commentStr);
        System.out.printf("║  %-40s║%n", stringStr);
        
        System.out.println("╠══════════════════════════════════════════╣");
        if (dryRun) {
            System.out.printf("║  %-38s ║%n", "DRY RUN — no files were written.");
        } else {
            String outStr = "Output: " + truncatePath(outputDir.getAbsolutePath(), 36);
            System.out.printf("║  %-38s ║%n", outStr);
        }
        System.out.println("╚══════════════════════════════════════════╝");
    }

    private static List<PoisonStrategy> buildStrategies(String arg) {
        List<PoisonStrategy> list = new ArrayList<>();
        String[] parts = arg.toLowerCase().split(",");
        
        for (String part : parts) {
            String trimmed = part.trim();
            PoisonStrategy strategy = createStrategy(trimmed);
            
            if (strategy != null) {
                list.add(strategy);
                if (!trimmed.equals("all")) {
                    strategy.setEnabled(true);
                }
            }
        }
        
        if (arg.contains("all")) {
            list.clear();
            list.add(new EntropyScrambler());
            list.add(new DeadCodeInjector());
            list.add(new CommentPoisoner());
            list.add(new StringEncoder());
            list.add(new WhitespaceDisruptor());
            list.add(new SemanticInverter());
            list.add(new ControlFlowFlattener());
            list.add(new WatermarkEncoder());
        }
        
        return list;
    }

    private static PoisonStrategy createStrategy(String name) {
        return switch (name) {
            case "entropy"    -> new EntropyScrambler();
            case "deadcode"   -> new DeadCodeInjector();
            case "comments"   -> new CommentPoisoner();
            case "strings"    -> new StringEncoder();
            case "whitespace" -> new WhitespaceDisruptor();
            case "semantic"   -> { yield new SemanticInverter(); }
            case "controlflow"-> { yield new ControlFlowFlattener(); }
            case "watermark"  -> new WatermarkEncoder();
            case "all"        -> null;
            default -> {
                System.err.println("[WARN] Unknown strategy '" + name + "' — skipping. Valid options: entropy, deadcode, comments, strings, whitespace, semantic, controlflow, watermark");
                yield null;
            }
        };
    }

    private static void printStrategyList() {
        System.out.println("Available Strategies:");
        System.out.println("  ID            Name                         Status    Description");
        System.out.println("  ─────────────────────────────────────────────────────────────────");
        System.out.printf("  %-12s %-26s %-8s %s%n", "entropy", "Variable Entropy Scrambling", "ON", "Renames identifiers to high-entropy names");
        System.out.printf("  %-12s %-26s %-8s %s%n", "deadcode", "Dead Code Injection", "ON", "Injects unreachable code blocks");
        System.out.printf("  %-12s %-26s %-8s %s%n", "comments", "Comment Poisoning", "ON", "Replaces comments with misleading content");
        System.out.printf("  %-12s %-26s %-8s %s%n", "strings", "String Literal Encoding", "ON", "Encodes string literals");
        System.out.printf("  %-12s %-26s %-8s %s%n", "whitespace", "Whitespace Disruption", "ON", "Modifies whitespace patterns");
        System.out.printf("  %-12s %-26s %-8s %s%n", "semantic", "Semantic Inversion", "OFF", "Renames to semantically misleading names");
        System.out.printf("  %-12s %-26s %-8s %s%n", "controlflow", "Control Flow Flattening", "OFF", "Rewrites method bodies with switch");
        System.out.printf("  %-12s %-26s %-8s %s%n", "watermark", "Watermark Encoder", "OFF", "Embeds steganographic watermarks");
        System.out.println();
        System.out.println("Use -s <id> to enable a specific strategy, or -s all for all strategies.");
        System.out.println("Strategies marked OFF are experimental and disabled by default.");
    }

    private static String truncatePath(String path, int maxLen) {
        if (path.length() <= maxLen) return path;
        return "..." + path.substring(path.length() - (maxLen - 3));
    }

    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  java -jar nightshade.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --input,  -i <path>     Input file or directory (required)");
        System.out.println("  --output, -o <path>     Output directory (default: <input>/../_nightshade_output)");
        System.out.println("  --strategies, -s <list> Comma-separated strategies or 'all' (default: all)");
        System.out.println("                          Options: entropy, deadcode, comments, strings, whitespace,");
        System.out.println("                                   semantic, controlflow, watermark");
        System.out.println("  --threshold, -t <num>   Early-exit entropy threshold [0.0 - 1.0] (default: 0.65)");
        System.out.println("  --dry-run               Process and report without writing output files");
        System.out.println("  --verify                Run post-obfuscation compilation verification");
        System.out.println("  --library-mode          Preserve public APIs while obfuscating internals");
        System.out.println("  --report, -r            Generate markdown report (nightshade_report.md)");
        System.out.println("  --verbose, -v           Show detailed processing logs");
        System.out.println("  --quiet, -q             Only show errors and final summary");
        System.out.println("  --list-strategies       Show all available strategies");
        System.out.println("  --version               Print version and exit");
        System.out.println("  --help, -h              Show this help message");
        System.out.println();
        System.out.println("Note: Whitespace strategy is automatically skipped for Python files");
        System.out.println("      (Python indentation is semantic and must be preserved).");
        System.out.println("Examples:");
        System.out.println("  java -jar nightshade.jar --input ./src --output ./poisoned");
        System.out.println("  java -jar nightshade.jar -i ./src -s entropy,deadcode -v");
        System.out.println("  java -jar nightshade.jar -i ./src -s all --verify");
        System.out.println("  java -jar nightshade.jar --list-strategies");
    }
}