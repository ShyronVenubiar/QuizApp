package de.felix.quiz.view;

import de.felix.quiz.controller.QuizController;
import de.felix.quiz.model.QuizData;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultipleChoiceQuestionView {
    private VBox root;
    private List<CheckBox> checkBoxes = new ArrayList<>();

    public MultipleChoiceQuestionView(QuizController controller, QuizData data) {
        // Body: nur die Antwortoptionen
        VBox optionsBox = new VBox(10);
        VBox.setVgrow(optionsBox, Priority.ALWAYS);

        List<QuizData.Option> shuffledOptions = new ArrayList<>(data.options);
        Collections.shuffle(shuffledOptions);

        for (QuizData.Option opt : shuffledOptions) {
            CheckBox checkBox = new CheckBox(opt.text);
            checkBox.setUserData(opt); // direkt das Option-Objekt speichern
            checkBox.setWrapText(true);
            checkBox.maxWidthProperty().bind(controller.getStage().widthProperty().subtract(100));
            checkBoxes.add(checkBox);
            optionsBox.getChildren().add(checkBox);
        }
        optionsBox.getStyleClass().add("question-box");

        ScrollPane scrollPane = new ScrollPane(optionsBox);
        scrollPane.setFitToWidth(true);
        scrollPane.prefViewportHeightProperty().bind(controller.getStage().heightProperty().multiply(0.7));
        scrollPane.getStyleClass().add("question-box");
        scrollPane.setMinHeight(100);

        // Check-Button + Feedback
        Button checkButton = new Button("Antwort prüfen");
        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);

        checkButton.setOnAction(e -> checkAnswer(controller, data, checkButton, feedbackLabel));

        // Übergabe an QuizDesign: Intro, Bild, Frage, Outro, Body, CheckButton, Feedback
        root = QuizDesign.createBaseLayout(controller, data, scrollPane, checkButton, feedbackLabel);
    }

    public VBox getRoot() {
        return root;
    }

    private void checkAnswer(QuizController controller, QuizData data,
                             Button checkButton, Label feedbackLabel) {
        List<QuizData.Option> selected = new ArrayList<>();
        for (CheckBox cb : checkBoxes) {
            if (cb.isSelected()) {
                selected.add((QuizData.Option) cb.getUserData());
            }
            cb.setDisable(true); // Auswahl nach Prüfung sperren
        }

        boolean allCorrect = selected.stream().allMatch(o -> o.correct);
        boolean missed = data.options.stream().anyMatch(o -> o.correct && !selected.contains(o));

        checkButton.setDisable(true);

        if (allCorrect && !missed) {
            controller.addPoint();
            checkButton.setText("✅ Richtig");
            feedbackLabel.setText("");
        } else {
            checkButton.setText("❌ Falsch");

            if (!controller.isExamMode()) {
                String solutionText = data.options.stream()
                        .filter(o -> o.correct)
                        .map(o -> o.text)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                feedbackLabel.setText("Richtige Antwort(en): " + solutionText);
            } else {
                feedbackLabel.setText("");
            }
        }
    }
}
