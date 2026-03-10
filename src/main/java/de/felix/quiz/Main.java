package de.felix.quiz;

import de.felix.quiz.controller.AppFiles;
import de.felix.quiz.controller.QuizBuilder;
import de.felix.quiz.controller.QuizController;
import de.felix.quiz.interfaces.QuizListRefresh;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

public class Main extends Application {
    private boolean darkModeFromPreviousSession = false;
    private boolean alreadyInSession = false;

    @Override
    public void start(Stage stage) {
        int width = 1280;
        int height = 720;

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double centerX = screenBounds.getMinX() + (screenBounds.getWidth() - width) / 2;
        double centerY = screenBounds.getMinY() + (screenBounds.getHeight() - height) / 2;

        start(stage, false, width, height, centerX, centerY);
    }

    public void start(Stage stage, boolean darkMode, int width, int height, double x, double y) {
        darkModeFromPreviousSession = darkMode;

        // ensure packaged defaults copied to app data (only if index.txt exists and app dir empty)
//        AppFiles.ensurePackagedQuizzesCopied(Main.class);

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(20));
        layout.setAlignment(Pos.CENTER);

        Label label = new Label("Wähle ein Quiz:");
        ComboBox<String> quizSelector = new ComboBox<>();

        // Quizzes aus AppData auflisten
        Path appQuizzes = AppFiles.getQuizzesDir();
        try (Stream<Path> s = Files.list(appQuizzes)) {
            s.filter(p -> p.getFileName().toString().endsWith(".json"))
                    .forEach(p -> quizSelector.getItems().add(p.getFileName().toString().replace(".json", "")));
        } catch (IOException ignored) {}

        quizSelector.setPromptText("Bitte auswählen...");

        ComboBox<String> sizeSelector = new ComboBox<>();
        sizeSelector.getItems().addAll(
                "800x600",
                "1024x768",
                "1280x720",
                "1920x1080",
                "2560x1440",
                "3840x2160",
                "Vollbild"
        );
        if (!alreadyInSession)
            sizeSelector.setValue(width + "x" + height);

        CheckBox darkModeToggle = new CheckBox("🌙 Dark Mode aktivieren");
        darkModeToggle.setSelected(darkModeFromPreviousSession);
        CheckBox examModeToggle = new CheckBox("Prüfungsmodus aktivieren");

        Button startButton = new Button("Starten");
        Button quizBuilderButton = new Button("QuizBuilder");
        Button exitButton = new Button("Beenden");

        Button clearAppDataButton = new Button("AppData\nlöschen");
        clearAppDataButton.getStyleClass().add("danger-button");
        Button createAppDataButton = new Button("AppData\nerstellen");
        createAppDataButton.getStyleClass().add("create-button");

        BorderPane rootPane = new BorderPane();
        rootPane.setCenter(layout);

        HBox bottomBox = new HBox();
        bottomBox.setPadding(new Insets(10));
        bottomBox.setAlignment(Pos.BOTTOM_RIGHT);
        bottomBox.getChildren().add(createAppDataButton);
        bottomBox.getChildren().add(clearAppDataButton);
        rootPane.setBottom(bottomBox);

        layout.getChildren().addAll(
                label,
                quizSelector,
                new Label("Fenstergröße:"),
                sizeSelector,
                darkModeToggle,
                examModeToggle,
                startButton,
                quizBuilderButton,
                exitButton
        );

        Scene menuScene = new Scene(rootPane);
        menuScene.getStylesheets().add(getClass().getResource(
                darkModeFromPreviousSession ? "/styles/dark.css" : "/styles/light.css"
        ).toExternalForm());

        darkModeToggle.setOnAction(e -> {
            menuScene.getStylesheets().clear();
            String stylesheet = darkModeToggle.isSelected() ? "/styles/dark.css" : "/styles/light.css";
            menuScene.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
        });

        layout.prefWidthProperty().bind(stage.widthProperty());
        layout.prefHeightProperty().bind(stage.heightProperty());

        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            double fontSize = Math.max(16, newVal.doubleValue() / 40.0);
            layout.setStyle("-fx-font-size: " + fontSize + "px;");
        });

        sizeSelector.setOnAction(e -> {
            String selectedSize = sizeSelector.getValue();
            int newWidth = width;
            int newHeight = height;
            boolean fullscreen = false;

            switch (selectedSize) {
                case "1024x768" -> { newWidth = 1024; newHeight = 768; }
                case "1280x720" -> { newWidth = 1280; newHeight = 720; }
                case "1920x1080" -> { newWidth = 1920; newHeight = 1080; }
                case "2560x1440" -> { newWidth = 2560; newHeight = 1440; }
                case "3840x2160" -> { newWidth = 3840; newHeight = 2160; }
                case "Vollbild" -> fullscreen = true;
            }

            if (fullscreen) {
                stage.setFullScreen(true);
            } else {
                stage.setFullScreen(false);
                double centerX2 = stage.getX() + stage.getWidth() / 2;
                double centerY2 = stage.getY() + stage.getHeight() / 2;
                stage.setWidth(newWidth);
                stage.setHeight(newHeight);
                stage.setX(centerX2 - newWidth / 2);
                stage.setY(centerY2 - newHeight / 2);
            }
        });

        // Quiz starten
        startButton.setOnAction(e -> {
            String selectedQuiz = quizSelector.getValue();
            if (selectedQuiz == null || selectedQuiz.isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Bitte ein Quiz auswählen.").showAndWait();
                return;
            }
            String selectedSize = sizeSelector.getValue();
            // Datei aus AppData verwenden
            String file = AppFiles.getQuizzesDir().resolve(selectedQuiz + ".json").toUri().toString();

            int newWidth = 800, newHeight = 600;
            boolean fullscreen = false;
            switch (selectedSize) {
                case "1024x768" -> { newWidth = 1024; newHeight = 768; }
                case "1280x720" -> { newWidth = 1280; newHeight = 720; }
                case "1920x1080" -> { newWidth = 1920; newHeight = 1080; }
                case "2560x1440" -> { newWidth = 2560; newHeight = 1440; }
                case "3840x2160" -> { newWidth = 3840; newHeight = 2160; }
                case "Vollbild" -> fullscreen = true;
            }

            boolean darkModeSelected = darkModeToggle.isSelected();
            boolean examMode = examModeToggle.isSelected();

            // QuizController erwartet Stage, Scene(menu), quizFile (als String), width,height,fullscreen,dark,exam
            QuizController controller = new QuizController(stage, menuScene, file, newWidth, newHeight, fullscreen, darkModeSelected, examMode);
            Scene quizScene = controller.buildScene();
            stage.setScene(quizScene);
            stage.setTitle("Quiz");
        });

        // QuizBuilder öffnen
        quizBuilderButton.setOnAction(e -> {
            QuizBuilder builder = new QuizBuilder(menuScene, stage, () -> {
                // refresh: neu aus AppData laden
                quizSelector.getItems().clear();
                try (Stream<Path> s = Files.list(AppFiles.getQuizzesDir())) {
                    s.filter(p -> p.getFileName().toString().endsWith(".json"))
                            .forEach(p -> quizSelector.getItems().add(p.getFileName().toString().replace(".json", "")));
                } catch (IOException ignored) {}
            });
            Scene builderScene = builder.buildScene(darkModeToggle.isSelected());
            stage.setScene(builderScene);
            stage.setTitle("QuizBuilder");
        });

        exitButton.setOnAction(e -> Platform.exit());

        clearAppDataButton.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("AppData löschen");
            confirm.setHeaderText("Alle gespeicherten Daten löschen?");
            confirm.setContentText("Möchtest du wirklich alle gespeicherten Quizzes und Bilder in deinem AppData-Ordner löschen? Diese Aktion kann nicht rückgängig gemacht werden.");
            var result = confirm.showAndWait();
            if (result.isPresent() && result.get() == ButtonType.OK) {
                try {
                    Path appDir = AppFiles.getAppDataDir();
                    // rekursiv löschen
                    if (Files.exists(appDir)) {
                        Files.walk(appDir)
                                .sorted(Comparator.reverseOrder())
                                .forEach(p -> {
                                    try { Files.deleteIfExists(p); } catch (Exception ex) { /* log */ }
                                });
                    }
                    // neu anlegen, damit App weiter funktioniert
                    Files.createDirectories(AppFiles.getQuizzesDir());
                    Files.createDirectories(AppFiles.getImagesDir());
                    // UI-Refresh: falls ComboBox existiert, leere sie
                    // (hier quizSelector ist final nicht deklariert; wir aktualisieren manuell)
                    // Hinweis-Dialog
                    new Alert(Alert.AlertType.INFORMATION, "AppData wurde gelöscht. Die Anwendung wurde zurückgesetzt.").showAndWait();
                    quizSelector.getItems().clear();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Fehler beim Löschen: " + ex.getMessage()).showAndWait();
                }
            }
        });

        createAppDataButton.setOnAction(e -> {
            // ensure packaged defaults copied to app data (only if index.txt exists and app dir empty)
            AppFiles.ensurePackagedQuizzesCopied(Main.class);
            try (Stream<Path> s = Files.list(appQuizzes)) {
                s.filter(p -> p.getFileName().toString().endsWith(".json"))
                        .forEach(p -> quizSelector.getItems().add(p.getFileName().toString().replace(".json", "")));
            } catch (IOException ignored) {}
        });

        stage.setScene(menuScene);
        stage.setTitle("Quiz Auswahl");
        stage.setX(x);
        stage.setY(y);
        stage.setWidth(width);
        stage.setHeight(height);
        stage.show();

        sizeSelector.getOnAction().handle(null);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
