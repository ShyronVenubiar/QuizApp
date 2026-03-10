package de.felix.quiz.view;

import de.felix.quiz.controller.QuizController;
import de.felix.quiz.model.QuizData;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SingleChoiceQuestionView {
    private VBox root;
    private ToggleGroup toggleGroup = new ToggleGroup();

    public SingleChoiceQuestionView(QuizController controller, QuizData data) {
        // Body: nur die Antwortoptionen
        VBox body = new VBox(10);
        body.setFillWidth(true);

        ScrollPane scrollPane = new ScrollPane(body);
        scrollPane.setFitToWidth(true);
        scrollPane.prefViewportHeightProperty().bind(controller.getStage().heightProperty().multiply(0.7));
        scrollPane.getStyleClass().add("question-box");
        scrollPane.setMinHeight(100);

        List<QuizData.Option> shuffledOptions = new ArrayList<>(data.options);
        Collections.shuffle(shuffledOptions);

        for (QuizData.Option opt : shuffledOptions) {
            RadioButton rb = new RadioButton(opt.text);
            rb.setWrapText(true);
            rb.setMaxWidth(Double.MAX_VALUE);
            rb.setToggleGroup(toggleGroup);
            rb.setUserData(opt); // direkt das Option-Objekt speichern
            rb.getStyleClass().add("answer-option");
            body.getChildren().add(rb);
        }

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

    private void checkAnswer(QuizController controller, QuizData data, Button checkButton, Label feedbackLabel) {
        Toggle selected = toggleGroup.getSelectedToggle();
        if (selected == null) {
            checkButton.setText("⚠️ Bitte eine Antwort wählen");
            return;
        }

        QuizData.Option chosen = (QuizData.Option) selected.getUserData();
        boolean isCorrect = chosen.correct;

        checkButton.setDisable(true);
        toggleGroup.getToggles().forEach(t -> ((Node) t).setDisable(true));

        if (isCorrect) {
            controller.addPoint();
            checkButton.setText("✅ Richtig");
            feedbackLabel.setText("");
        } else {
            checkButton.setText("❌ Falsch");
            if (!controller.isExamMode()) {
                String solution = data.options.stream()
                        .filter(o -> o.correct)
                        .map(o -> o.text)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                feedbackLabel.setText("Richtige Antwort: " + solution);
            } else {
                feedbackLabel.setText("");
            }
        }
    }
}
