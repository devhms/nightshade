package com.nightshade;

import com.nightshade.engine.FileWalker;
import com.nightshade.engine.Lexer;
import com.nightshade.engine.ObfuscationEngine;
import com.nightshade.engine.Parser;
import com.nightshade.engine.Serializer;
import com.nightshade.engine.EntropyCalculator;
import com.nightshade.model.ObfuscationResult;
import com.nightshade.model.SourceFile;
import com.nightshade.strategy.CommentPoisoner;
import com.nightshade.strategy.DeadCodeInjector;
import com.nightshade.strategy.EntropyScrambler;
import com.nightshade.strategy.PoisonStrategy;
import com.nightshade.strategy.StringEncoder;
import com.nightshade.strategy.WhitespaceDisruptor;
import com.nightshade.util.FileUtil;
import com.nightshade.util.LogService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
        "  v2.0.0 | LLM Training Data Poisoning Engine\n" +
        "  Authors: Ibrahim Salman (25-SE-33), Saif-ur-Rehman (25-SE-05) | UET Taxila\n";

    public static void main(String[] args) {
        run(args);
    }

    public static void run(String[] args) {
        System.out.println(BANNER);

        String inputPath = null;
        String outputPath = null;
        String strategiesArg = "all";
        boolean verbose = false;
        double entropyThreshold = 0.65; // Default threshold

        // Parse args
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--input", "-i"       -> inputPath = args[++i];
                case "--output", "-o"      -> outputPath = args[++i];
                case "--strategies", "-s"  -> strategiesArg = args[++i];
                case "--threshold", "-t"   -> entropyThreshold = Double.parseDouble(args[++i]);
                case "--verbose", "-v"     -> verbose = true;
                case "--help", "-h"        -> { printHelp(); return; }
                default -> System.err.println("[WARN] Unknown argument: " + args[i]);
            }
        }

        if (inputPath == null) {
            System.err.println("[ERROR] --input is required. Use --help for usage.");
            System.exit(1);
        }
        if (outputPath == null) {
            outputPath = new File(inputPath).getParent() + "/_nightshade_output";
        }

        File inputDir = new File(inputPath);
        File outputDir = new File(outputPath);

        if (!inputDir.exists() || !inputDir.isDirectory()) {
            System.err.println("[ERROR] Input directory does not exist: " + inputPath);
            System.exit(1);
        }

        // Build strategy list
        List<PoisonStrategy> strategies = buildStrategies(strategiesArg);
        if (strategies.isEmpty()) {
            System.err.println("[ERROR] No valid strategies specified. Options: all, entropy, deadcode, comments, strings, whitespace");
            System.exit(1);
        }

        System.out.println("[INFO] Input:  " + inputDir.getAbsolutePath());
        System.out.println("[INFO] Output: " + outputDir.getAbsolutePath());
        System.out.println("[INFO] Active strategies:");
        strategies.forEach(s -> System.out.println("         • " + s.getName()));
        System.out.println("[INFO] Entropy threshold: " + entropyThreshold);
        System.out.println();

        LogService logService = new LogService(verbose);
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
            System.out.println("[INFO] Discovered " + files.size() + " source files.");

            List<ObfuscationResult> results = engine.process(files);

            int written = 0;
            for (ObfuscationResult result : results) {
                fileUtil.write(result, inputDir, outputDir);
                written++;
            }

            fileUtil.writeRunLog(results, outputDir);
            long elapsed = System.currentTimeMillis() - start;

            System.out.println();
            System.out.println("╔══════════════════════════════════════════╗");
            System.out.println("║  NIGHTSHADE COMPLETE                     ║");
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.printf( "║  Files processed: %-23d║%n", results.size());
            System.out.printf( "║  Files written:   %-23d║%n", written);

            double avgEntropy = results.stream()
                .mapToDouble(ObfuscationResult::getEntropyScore)
                .average().orElse(0.0);
            System.out.printf( "║  Avg entropy:     %-22.3f ║%n", avgEntropy);
            System.out.printf( "║  Time elapsed:    %-19dms ║%n", elapsed);
            System.out.println("╠══════════════════════════════════════════╣");
            System.out.println("║  Output: " + truncate(outputDir.getAbsolutePath(), 32) + " ║");
            System.out.println("╚══════════════════════════════════════════╝");

        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            if (verbose) e.printStackTrace();
            System.exit(1);
        }
    }

    private static List<PoisonStrategy> buildStrategies(String arg) {
        List<PoisonStrategy> list = new ArrayList<>();
        String[] parts = arg.toLowerCase().split(",");
        for (String part : parts) {
            switch (part.trim()) {
                case "all" -> {
                    list.add(new EntropyScrambler());
                    list.add(new DeadCodeInjector());
                    list.add(new CommentPoisoner());
                    list.add(new StringEncoder());
                    list.add(new WhitespaceDisruptor());
                    return list;
                }
                case "entropy"    -> list.add(new EntropyScrambler());
                case "deadcode"   -> list.add(new DeadCodeInjector());
                case "comments"   -> list.add(new CommentPoisoner());
                case "strings"    -> list.add(new StringEncoder());
                case "whitespace" -> list.add(new WhitespaceDisruptor());
            }
        }
        return list;
    }

    private static String truncate(String s, int maxLen) {
        if (s.length() <= maxLen) {
            return s + " ".repeat(maxLen - s.length());
        }
        return "..." + s.substring(s.length() - (maxLen - 3));
    }

    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  java -jar nightshade.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --input,  -i <path>     Input directory (required)");
        System.out.println("  --output, -o <path>     Output directory (default: <input>/../_nightshade_output)");
        System.out.println("  --strategies, -s <list> Comma-separated strategies or 'all' (default: all)");
        System.out.println("                          Options: entropy, deadcode, comments, strings, whitespace");
        System.out.println("  --threshold, -t <num>   Early-exit entropy threshold [0.0 - 1.0] (default: 0.65)");
        System.out.println("  --verbose, -v           Show detailed processing logs");
        System.out.println("  --help, -h              Show this help message");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar nightshade.jar --input ./src --output ./poisoned");
        System.out.println("  java -jar nightshade.jar -i ./src -s entropy,deadcode -v");
        System.out.println();
        System.out.println("No arguments = launches the GUI application.");
    }
}
