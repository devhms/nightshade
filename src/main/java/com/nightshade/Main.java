package com.nightshade;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

/**
 * Nightshade v3.5.0 — LLM Training Data Poisoning Engine
 *
 * Entry point. If CLI args are present, delegates to CLI mode.
 * Otherwise launches the JavaFX GUI.
 *
 * 
 * https://github.com/devhms/nightshade
 */
public class Main extends Application {

    public static final String APP_TITLE = "Nightshade v3.5.0 | Code Obfuscation Engine";
    public static final String APP_VERSION = "3.5.0";

    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/nightshade/fxml/main.fxml"));
        Scene scene = new Scene(loader.load(), 1280, 800);

        // Apply dark terminal theme
        scene.getStylesheets().add(
            Objects.requireNonNull(
                getClass().getResource("/com/nightshade/css/nightshade.css")
            ).toExternalForm()
        );

        stage.setTitle(APP_TITLE);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        stage.setScene(scene);

        // App icon (amber N on dark background — generated at build)
        try {
            Image icon = new Image(
                Objects.requireNonNull(
                    getClass().getResourceAsStream("/com/nightshade/assets/app-icon.png")
                )
            );
            stage.getIcons().add(icon);
        } catch (Exception ignored) {
            // Icon optional — app works fine without it
        }

        stage.show();
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            CLI.run(args);
        } else {
            try {
                launch(args);
            } catch (UnsupportedOperationException | NoClassDefFoundError e) {
                System.out.println("[INFO] GUI unavailable (headless environment). Showing CLI help:");
                System.out.println();
                CLI.run(new String[]{"--help"});
            }
        }
    }
}
