package de.felix.quiz.view;

import de.felix.quiz.controller.QuizController;
import de.felix.quiz.model.QuizData;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FillInTheBlankQuestionView {
    private VBox root;

    public FillInTheBlankQuestionView(QuizController controller, QuizData data) {
        // Frage mit Lücken
        String raw = data.question == null ? "" : data.question;
        List<String> correctAnswers = data.correctAnswers != null
                ? data.correctAnswers.stream()
                .filter(Objects::nonNull)
                .filter(s -> !"null".equals(s.trim()))
                .toList()
                : List.of();

        String[] parts = raw.split("___", -1);

        TextFlow flow = new TextFlow();
        flow.setLineSpacing(2);
        flow.setPrefWidth(Region.USE_COMPUTED_SIZE);

        List<TextField> inputFields = new ArrayList<>();

        for (int i = 0; i < parts.length; i++) {
            if (!parts[i].isEmpty()) {
                Text t = new Text(parts[i]);
                t.getStyleClass().add("fib-text"); // StyleClass setzen
                flow.getChildren().add(t);
            }

            if (i < correctAnswers.size()) {
                TextField tf = new TextField();
                tf.setPromptText("...");
                tf.setPrefWidth(120);
                tf.getStyleClass().add("inline-blank");
                inputFields.add(tf);
                flow.getChildren().add(tf);
            }
        }

        if (correctAnswers.size() > parts.length) {
            for (int i = parts.length; i < correctAnswers.size(); i++) {
                TextField tf = new TextField();
                tf.setPromptText("...");
                tf.setPrefWidth(120);
                tf.getStyleClass().add("inline-blank");
                inputFields.add(tf);
                flow.getChildren().add(tf);
            }
        }

        VBox body = new VBox(flow);

        // Check-Button + Feedback
        Button checkButton = new Button("Antwort prüfen");
        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);

        checkButton.setOnAction(e -> {
            List<String> userAnswers = inputFields.stream()
                    .map(TextField::getText)
                    .map(String::trim)
                    .toList();

            boolean isCorrect = userAnswers.equals(correctAnswers);

            checkButton.setDisable(true);
            inputFields.forEach(tf -> {
                tf.setDisable(true);
                tf.setEditable(false);
            });

            if (isCorrect) {
                controller.addPoint();
                checkButton.setText("✅ Richtig");
                feedbackLabel.setText("");
            } else {
                checkButton.setText("❌ Falsch");
                if (!controller.isExamMode() && !correctAnswers.isEmpty()) {
                    feedbackLabel.setText("Richtige Antworten: " + String.join(", ", correctAnswers));
                } else {
                    feedbackLabel.setText("");
                }
            }
        });

        // Übergabe an QuizDesign
        root = QuizDesign.createBaseLayout(controller, data, body, checkButton, feedbackLabel);
    }

    public VBox getRoot() {
        return root;
    }
}
