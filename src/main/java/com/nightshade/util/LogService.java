package com.nightshade.util;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Observable log stream that the JavaFX UI binds to.
 *
 * All public methods are thread-safe — they marshal to the JavaFX
 * Application Thread via Platform.runLater() so background tasks
 * can safely call log() without causing IllegalStateException.
 *
 * The verbose flag controls whether [DEBUG] entries are included
 * (used by CLI mode to optionally show detailed processing info).
 */
public class LogService {

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_ENTRIES = 5000; // prevent unbounded growth

    private final ObservableList<String> entries = FXCollections.observableArrayList();
    private final boolean verbose;

    public LogService() {
        this(false);
    }

    public LogService(boolean verbose) {
        this.verbose = verbose;
    }

    /** Returns the observable list for binding to a ListView. */
    public ObservableList<String> getEntries() {
        return entries;
    }

    public void log(String message) {
        addEntry("[INFO] " + message);
    }

    public void logError(String message) {
        addEntry("[ERROR] " + message);
    }

    public void logDebug(String message) {
        if (verbose) addEntry("[DEBUG] " + message);
    }

    public void logSuccess(String message) {
        addEntry("[DONE] " + message);
    }

    public void clear() {
        runOnFxThread(entries::clear);
    }

    // ── Internal ─────────────────────────────────────────────────────────────

    private void addEntry(String entry) {
        String timestamped = "[" + LocalTime.now().format(TIME_FMT) + "] " + entry;

        if (Platform.isFxApplicationThread()) {
            appendAndTrim(timestamped);
        } else {
            // Might not have FX running (CLI mode) — try Platform, fallback to stdout
            try {
                Platform.runLater(() -> appendAndTrim(timestamped));
            } catch (IllegalStateException e) {
                // CLI mode: FX toolkit not initialized — print directly
                System.out.println(timestamped);
            }
        }
    }

    private void appendAndTrim(String entry) {
        entries.add(entry);
        if (entries.size() > MAX_ENTRIES) {
            entries.remove(0, entries.size() - MAX_ENTRIES);
        }
    }

    private void runOnFxThread(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            try {
                Platform.runLater(r);
            } catch (IllegalStateException e) {
                r.run(); // CLI fallback
            }
        }
    }
}
