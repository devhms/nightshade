package com.nightshade.controller;

import com.nightshade.engine.*;
import com.nightshade.model.*;
import com.nightshade.strategy.*;
import com.nightshade.util.FileUtil;
import com.nightshade.util.LogService;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;

import java.awt.Desktop;
import java.io.File;
import java.net.URL;
import java.util.*;

/**
 * Main controller — wires the full UI to the ObfuscationEngine pipeline.
 *
 * Threading contract:
 * - ALL engine work runs in a background Task (never on FX thread).
 * - Platform.runLater() is used only here to update UI from the task.
 * - The progress bar and log list are bound to the LogService's
 * ObservableList which marshals its own updates.
 *
 * OOP: OBSERVER pattern — logView is bound to LogService.getEntries()
 * so any background log() call automatically updates the ListView.
 */
public class MainController implements Initializable {

    // ── FXML Injections ────────────────────────────────────────────────────
    @FXML
    private TextField inputPathField;
    @FXML
    private TextField outputPathField;
    @FXML
    private Button browseInputBtn;
    @FXML
    private Button browseOutputBtn;
    @FXML
    private TreeView<String> fileTreeView;

    @FXML
    private CheckBox cbEntropy;
    @FXML
    private CheckBox cbDeadCode;
    @FXML
    private CheckBox cbComments;
    @FXML
    private CheckBox cbStrings;
    @FXML
    private CheckBox cbWhitespace;
    @FXML
    private CheckBox cbSemantic;
    @FXML
    private CheckBox cbControlFlow;
    @FXML
    private CheckBox cbWatermark;

    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label entropyLabel;
    @FXML
    private Button runBtn;
    @FXML
    private Label statusLabel;

    @FXML
    private TextArea sourceView;
    @FXML
    private TextArea poisonedView;
    @FXML
    private ScrollPane leftScroll;
    @FXML
    private ScrollPane rightScroll;

    @FXML
    private HBox statsBar;
    @FXML
    private Label statFiles;
    @FXML
    private Label statRenamed;
    @FXML
    private Label statDead;
    @FXML
    private Label statComments;
    @FXML
    private Label statStrings;
    @FXML
    private Label statEntropy;
    @FXML
    private Label statTime;
    @FXML
    private Button openOutputBtn;
    @FXML
    private Button aboutBtn;

    @FXML
    private ListView<String> logView;

    // ── Internal state ─────────────────────────────────────────────────────
    private final LogService logService = new LogService();
    private List<ObfuscationResult> lastResults = new ArrayList<>();
    private File lastOutputDir;
    private Timeline progressPulse;
    private Task<Void> activeTask;

    // ── Initialization ─────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Bind log view to observable log entries
        logView.setItems(logService.getEntries());

        // Auto-scroll log to bottom on new entries
        logService.getEntries().addListener((javafx.collections.ListChangeListener<String>) c -> {
            Platform.runLater(() -> {
                if (!logView.getItems().isEmpty()) {
                    logView.scrollTo(logView.getItems().size() - 1);
                }
            });
        });

        // Custom log cell factory — color by level
        logView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }
                setText(item);
                if (item.contains("[ERROR]"))
                    setStyle("-fx-text-fill: #FF4444;");
                else if (item.contains("[DONE]"))
                    setStyle("-fx-text-fill: #4CAF50;");
                else if (item.contains("[DEBUG]"))
                    setStyle("-fx-text-fill: #555555;");
                else
                    setStyle("-fx-text-fill: #707070;");
            }
        });

        // File tree click → load source view
        fileTreeView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null && newVal.isLeaf()) {
                        onFileSelected(newVal.getValue());
                    }
                });

        // Sync scroll between before/after views
        ChangeListener<Number> syncScroll = (obs, ov, nv) -> {
            // intentionally left empty — TextArea scrollbars sync via
            // scroll position binding setup below
        };

        setupScrollSync();

        logService.log("Nightshade v3.5.0 ready. Select an input directory to begin.");
        logService.log(
                "8 strategies loaded: Entropy, DeadCode, Comments, Strings, Whitespace, Semantic, ControlFlow, Watermark");
    }

    private void setupScrollSync() {
        // Scroll sync: when left scrollPane scrolls, mirror to right
        leftScroll.vvalueProperty().addListener((obs, ov, nv) -> rightScroll.setVvalue(nv.doubleValue()));
        rightScroll.vvalueProperty().addListener((obs, ov, nv) -> leftScroll.setVvalue(nv.doubleValue()));
    }

    // ── Browse buttons ─────────────────────────────────────────────────────

    @FXML
    private void onBrowseInput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Input Directory");
        File dir = chooser.showDialog(runBtn.getScene().getWindow());
        if (dir != null) {
            inputPathField.setText(dir.getAbsolutePath());
            outputPathField.setText(dir.getParent() + File.separator + "_nightshade_output");
            buildFileTree(dir);
            logService.log("Input set: " + dir.getAbsolutePath());
        }
    }

    @FXML
    private void onBrowseOutput() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Directory");
        File dir = chooser.showDialog(runBtn.getScene().getWindow());
        if (dir != null) {
            outputPathField.setText(dir.getAbsolutePath());
        }
    }

    // ── File Tree ──────────────────────────────────────────────────────────

    private void buildFileTree(File root) {
        TreeItem<String> rootItem = new TreeItem<>(root.getName());
        rootItem.setExpanded(true);
        addTreeItems(rootItem, root);
        fileTreeView.setRoot(rootItem);
    }

    private void addTreeItems(TreeItem<String> parent, File dir) {
        File[] files = dir.listFiles();
        if (files == null)
            return;
        Arrays.sort(files, Comparator.comparing(f -> (f.isDirectory() ? "0" : "1") + f.getName()));
        for (File f : files) {
            String name = f.getName();
            if (Set.of(".git", "target", "node_modules", "__pycache__", "build").contains(name))
                continue;
            TreeItem<String> item = new TreeItem<>(f.isDirectory() ? "📁 " + name : "📄 " + name);
            if (f.isDirectory()) {
                addTreeItems(item, f);
            }
            parent.getChildren().add(item);
        }
    }

    private void onFileSelected(String displayName) {
        String cleanName = displayName.replace("📄 ", "").replace("📁 ", "");
        String inputDir = inputPathField.getText();
        if (inputDir.isEmpty())
            return;

        // Find the file in the input directory tree
        findAndLoadFile(new File(inputDir), cleanName);
    }

    private void findAndLoadFile(File dir, String filename) {
        File[] files = dir.listFiles();
        if (files == null)
            return;
        for (File f : files) {
            if (f.isDirectory()) {
                findAndLoadFile(f, filename);
            } else if (f.getName().equals(filename)) {
                try {
                    List<String> lines = new java.util.ArrayList<>();
                    try (java.io.BufferedReader br = new java.io.BufferedReader(
                            new java.io.FileReader(f))) {
                        String line;
                        while ((line = br.readLine()) != null)
                            lines.add(line);
                    }
                    Platform.runLater(() -> {
                        sourceView.setText(String.join("\n", lines));

                        // If we have results, show the poisoned version too
                        for (ObfuscationResult r : lastResults) {
                            if (r.getOriginalFile().getFileName().equals(filename)) {
                                poisonedView.setText(
                                        String.join("\n", r.getObfuscatedFile().getObfuscatedLines()));
                                break;
                            }
                        }
                    });
                } catch (Exception e) {
                    logService.logError("Could not load file: " + e.getMessage());
                }
                return;
            }
        }
    }

    // ── Run ────────────────────────────────────────────────────────────────

    @FXML
    private void onRunClicked() {
        String inputPath = inputPathField.getText().trim();
        String outputPath = outputPathField.getText().trim();

        if (inputPath.isEmpty()) {
            showAlert("Select Input", "Please select an input directory first.");
            return;
        }
        File inputDir = new File(inputPath);
        if (!inputDir.exists() || !inputDir.isDirectory()) {
            showAlert("Invalid Input", "Input path does not exist: " + inputPath);
            return;
        }
        if (outputPath.isEmpty()) {
            outputPath = inputDir.getParent() + File.separator + "_nightshade_output";
            outputPathField.setText(outputPath);
        }
        final File outputDir = new File(outputPath);
        final String finalOutputPath = outputPath;

        // Build strategy list from checkboxes
        List<PoisonStrategy> strategies = buildSelectedStrategies();
        if (strategies.isEmpty()) {
            showAlert("No Strategies", "Please enable at least one strategy.");
            return;
        }

        // Disable UI during run
        setRunning(true);
        logService.clear();
        startProgressPulse();

        final long startTime = System.currentTimeMillis();

        activeTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                FileWalker walker = new FileWalker();
                List<SourceFile> files = walker.walk(inputDir);

                if (files.isEmpty()) {
                    logService.logError("No .java/.py/.js files found in: " + inputPath);
                    return null;
                }

                Lexer lexer = new Lexer();
                Parser parser = new Parser();
                Serializer serializer = new Serializer();
                EntropyCalculator calc = new EntropyCalculator();
                double defaultEntropyThreshold = 0.65;
                ObfuscationEngine engine = new ObfuscationEngine(
                        strategies, lexer, parser, serializer, calc, logService, defaultEntropyThreshold);

                List<ObfuscationResult> results = engine.process(files);

                // Write output files
                FileUtil fileUtil = new FileUtil();
                for (ObfuscationResult r : results) {
                    fileUtil.write(r, inputDir, outputDir);
                }
                fileUtil.writeRunLog(results, outputDir);

                long elapsed = System.currentTimeMillis() - startTime;
                lastResults = results;
                lastOutputDir = outputDir;

                // Update UI on FX thread
                Platform.runLater(() -> updateStats(results, elapsed));

                return null;
            }
        };

        activeTask.setOnSucceeded(e -> {
            stopProgressPulse();
            setRunning(false);
            statusLabel.setText("Complete ✓");
            statusLabel.setStyle("-fx-text-fill: #4CAF50;");
            progressBar.setProgress(1.0);
        });

        activeTask.setOnFailed(e -> {
            stopProgressPulse();
            setRunning(false);
            statusLabel.setText("Error ✗");
            statusLabel.setStyle("-fx-text-fill: #FF4444;");
            progressBar.setProgress(0);
            Throwable ex = activeTask.getException();
            logService.logError("Task failed: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread thread = new Thread(activeTask);
        thread.setDaemon(true);
        thread.start();
    }

    private List<PoisonStrategy> buildSelectedStrategies() {
        List<PoisonStrategy> list = new ArrayList<>();
        if (cbEntropy.isSelected())
            list.add(new EntropyScrambler());
        if (cbDeadCode.isSelected())
            list.add(new DeadCodeInjector());
        if (cbComments.isSelected())
            list.add(new CommentPoisoner());
        if (cbStrings.isSelected())
            list.add(new StringEncoder());
        if (cbWhitespace.isSelected())
            list.add(new WhitespaceDisruptor());
        if (cbSemantic != null && cbSemantic.isSelected())
            list.add(new SemanticInverter());
        if (cbControlFlow != null && cbControlFlow.isSelected())
            list.add(new ControlFlowFlattener());
        if (cbWatermark != null && cbWatermark.isSelected())
            list.add(new WatermarkEncoder());
        return list;
    }

    private void updateStats(List<ObfuscationResult> results, long elapsed) {
        int totalRenamed = results.stream().mapToInt(ObfuscationResult::getRenamedIdentifiers).sum();
        int totalDead = results.stream().mapToInt(ObfuscationResult::getDeadBlocksInjected).sum();
        int totalComments = results.stream().mapToInt(ObfuscationResult::getCommentsPoisoned).sum();
        int totalStrings = results.stream().mapToInt(ObfuscationResult::getStringsEncoded).sum();
        double avgEntropy = results.stream().mapToDouble(ObfuscationResult::getEntropyScore).average().orElse(0.0);

        statFiles.setText(String.valueOf(results.size()));
        statRenamed.setText(String.valueOf(totalRenamed));
        statDead.setText(String.valueOf(totalDead));
        statComments.setText(String.valueOf(totalComments));
        statStrings.setText(String.valueOf(totalStrings));
        statEntropy.setText(String.format("%.3f", avgEntropy));
        statTime.setText(elapsed + "ms");

        entropyLabel.setText(String.format("Entropy: %.3f", avgEntropy));
        progressBar.setProgress(avgEntropy);

        statsBar.setVisible(true);
        statsBar.setManaged(true);

        // Animate stats bar fade in
        FadeTransition ft = new FadeTransition(Duration.millis(400), statsBar);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();
    }

    // ── Progress Pulse Animation ───────────────────────────────────────────

    private void startProgressPulse() {
        progressBar.setProgress(-1); // indeterminate
        progressPulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(progressBar.opacityProperty(), 1.0)),
                new KeyFrame(Duration.millis(600), new KeyValue(progressBar.opacityProperty(), 0.4)),
                new KeyFrame(Duration.millis(1200), new KeyValue(progressBar.opacityProperty(), 1.0)));
        progressPulse.setCycleCount(Animation.INDEFINITE);
        progressPulse.play();
    }

    private void stopProgressPulse() {
        if (progressPulse != null) {
            progressPulse.stop();
            progressBar.setOpacity(1.0);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private void setRunning(boolean running) {
        runBtn.setDisable(running);
        browseInputBtn.setDisable(running);
        browseOutputBtn.setDisable(running);
        cbEntropy.setDisable(running);
        cbDeadCode.setDisable(running);
        cbComments.setDisable(running);
        cbStrings.setDisable(running);
        cbWhitespace.setDisable(running);
        if (running) {
            statusLabel.setText("Running...");
            statusLabel.setStyle("-fx-text-fill: #FFA500;");
        }
    }

    @FXML
    private void onClearLog() {
        logService.clear();
    }

    @FXML
    private void onOpenOutput() {
        if (lastOutputDir != null && lastOutputDir.exists()) {
            try {
                Desktop.getDesktop().open(lastOutputDir);
            } catch (Exception e) {
                logService.logError("Could not open output dir: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onAboutClicked() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("About Nightshade v3.5.0");
        alert.setHeaderText("Nightshade — LLM Training Data Poisoning Engine");
        alert.setContentText(
                "Version: 3.5.0\n\n" +
                        "Authors:\n" +
                        "  Ibrahim Salman (25-SE-33)\n" +
                        "  Saif-ur-Rehman (25-SE-05)\n\n" +
                        "Course: OOP Lab — UET Taxila\n\n" +
                        "Research:\n" +
                        "  • arXiv:2512.15468 — Variable renaming MI disruption\n" +
                        "  • MinHash+LSH near-dedup bypass (String Encoding)\n" +
                        "  • BPE tokenizer fingerprint disruption (Whitespace)\n\n" +
                        "Inspired by Nightshade & Glaze (UChicago) —\n" +
                        "first open-source CODE poisoning tool.\n\n" +
                        "MIT License — https://github.com/ibrahim-nightshade/nightshade");
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
