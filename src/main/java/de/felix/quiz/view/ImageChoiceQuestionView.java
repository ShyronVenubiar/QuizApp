package de.felix.quiz.view;

import de.felix.quiz.controller.AppFiles;
import de.felix.quiz.controller.QuizController;
import de.felix.quiz.model.QuizData;
import de.felix.quiz.model.QuizData.ImageOption;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ImageChoiceQuestionView {
    private final VBox root;
    private final ToggleGroup toggleGroup = new ToggleGroup();

    public ImageChoiceQuestionView(QuizController controller, QuizData data) {
        Stage stage = controller.getStage();

        // Body: ScrollPane mit Bildoptionen
        HBox imageBox = new HBox(30);
        imageBox.setAlignment(Pos.CENTER);
        imageBox.getStyleClass().add("question-box");

        ScrollPane scrollPane = new ScrollPane(imageBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(300);
        scrollPane.getStyleClass().add("question-box");

        List<ImageOption> shuffledOptions = new ArrayList<>(data.images != null ? data.images : List.of());
        Collections.shuffle(shuffledOptions);

        for (ImageOption option : shuffledOptions) {
            // Bild als file:-URL aus AppData laden
            ImageView previewView = new ImageView(loadImageFromAppData(option.path));
            previewView.setPreserveRatio(true);
            previewView.fitWidthProperty().bind(stage.widthProperty().divide(6));
            previewView.fitHeightProperty().bind(stage.heightProperty().divide(6));

            // ToggleButton mit Bild
            ToggleButton button = new ToggleButton();
            button.setGraphic(previewView);
            button.setToggleGroup(toggleGroup);
            button.setUserData(option); // direkt das ImageOption-Objekt speichern
            button.setFocusTraversable(false);
            button.prefWidthProperty().bind(previewView.fitWidthProperty());
            button.prefHeightProperty().bind(previewView.fitHeightProperty());

            // Zoom-Button
            ImageView magnifierIcon = new ImageView(
                    new Image(getClass().getResource("/images/magnifying_glass.png").toExternalForm(), 24, 24, true, true)
            );
            Button zoomButton = new Button();
            zoomButton.setGraphic(magnifierIcon);
            zoomButton.setStyle("-fx-background-color: transparent;");
            zoomButton.setPrefSize(30, 30);
            zoomButton.setFocusTraversable(false);
            Tooltip.install(zoomButton, new Tooltip("Bild vergrößern"));
            zoomButton.setOnAction(e -> showFullImage(option));

            // Label unter dem Bild
            Label imageLabel = new Label(option.label != null ? option.label : "");
            imageLabel.setWrapText(true);
            imageLabel.maxWidthProperty().bind(stage.widthProperty().divide(6));

            VBox imageWithLabel = new VBox(5, button, imageLabel, zoomButton);
            imageWithLabel.setAlignment(Pos.CENTER);
            imageBox.getChildren().add(imageWithLabel);
        }

        // Check-Button + Feedback
        Button checkButton = new Button("Antwort prüfen");
        Label feedbackLabel = new Label();
        checkButton.setOnAction(e -> checkAnswer(controller, data, checkButton, feedbackLabel));

        VBox body = new VBox(10, scrollPane);

        // Übergabe an QuizDesign
        root = QuizDesign.createBaseLayout(controller, data, body, checkButton, feedbackLabel);
    }

    public VBox getRoot() {
        return root;
    }

    private void checkAnswer(QuizController controller, QuizData data, Button checkButton, Label feedbackLabel) {
        Toggle selected = toggleGroup.getSelectedToggle();
        if (selected == null) {
            feedbackLabel.setText("Bitte wähle ein Bild aus.");
            return;
        }

        QuizData.ImageOption chosen = (QuizData.ImageOption) selected.getUserData();
        boolean isCorrect = Boolean.TRUE.equals(chosen.correct);

        checkButton.setDisable(true);

        // Auswahl sperren
        for (Toggle toggle : toggleGroup.getToggles()) {
            ((Control) toggle).setDisable(true);
        }

        if (isCorrect) {
            controller.addPoint();
            checkButton.setText("✅ Richtig");
            feedbackLabel.setText("");
        } else {
            checkButton.setText("❌ Falsch");
            if (!controller.isExamMode()) {
                String solution = (data.images != null ? data.images : List.<ImageOption>of()).stream()
                        .filter(o -> Boolean.TRUE.equals(o.correct))
                        .map(o -> o.label != null ? o.label : "")
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                feedbackLabel.setText("Richtige Antwort: " + solution);
            } else {
                feedbackLabel.setText("");
            }
        }
    }

    private void showFullImage(ImageOption option) {
        Image fullImage = loadImageFromAppData(option.path);
        ImageView fullView = new ImageView(fullImage);
        fullView.setPreserveRatio(true);
        fullView.setFitWidth(800);

        VBox popupContent = new VBox(10, fullView);
        popupContent.setPadding(new javafx.geometry.Insets(20));
        popupContent.setAlignment(Pos.CENTER);

        Scene popupScene = new Scene(popupContent);
        Stage popupStage = new Stage();
        popupStage.setTitle("Bild: " + (option.label != null ? option.label : ""));
        popupStage.setScene(popupScene);
        popupStage.show();
    }

    // Hilfsmethode: relativen /images/... Pfad zu absolutem AppData-Path und gültiger file:-URL konvertieren
    private Image loadImageFromAppData(String relPath) {
        try {
            if (relPath == null || relPath.isBlank()) {
                return new Image((String) null); // leer -> lädt nichts
            }
            // relPath beginnt mit "/images/..."; führendes "/" entfernen
            String withoutLeadingSlash = relPath.startsWith("/") ? relPath.substring(1) : relPath;
            Path absolute = AppFiles.getAppDataDir().resolve(withoutLeadingSlash);
            return new Image(absolute.toUri().toString(), true);
        } catch (Exception ex) {
            // Fallback: leeres Image, verhindert IllegalArgumentException
            System.err.println("Bild konnte nicht geladen werden: " + relPath + " -> " + ex.getMessage());
            return new Image((String) null);
        }
    }
}
