package de.felix.quiz.view;

import de.felix.quiz.controller.QuizController;
import de.felix.quiz.model.QuizData;
import javafx.scene.control.*;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class OrderQuestionView {
    private VBox root;

    public OrderQuestionView(QuizController controller, QuizData data) {
        // Body: Drag&Drop-Liste
        ListView<String> listView = new ListView<>();
        List<String> shuffledOptions = new ArrayList<>(data.orderOptions);
        Collections.shuffle(shuffledOptions);
        listView.getItems().addAll(shuffledOptions);
        listView.prefHeightProperty().bind(controller.getStage().heightProperty().multiply(0.8));
        listView.prefWidthProperty().bind(controller.getStage().widthProperty().subtract(100));
        listView.getStyleClass().add("question-box");

        setupDragAndDrop(listView);

        // Check-Button + Feedback
        Button checkButton = new Button("Antwort prüfen");
        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);

        checkButton.setOnAction(e -> checkAnswer(controller, data, listView, checkButton, feedbackLabel));

        HBox controlBox = new HBox(10, checkButton);
        controlBox.setAlignment(javafx.geometry.Pos.CENTER);
        controlBox.setPadding(new javafx.geometry.Insets(10, 0, 0, 0));

        VBox body = new VBox(10, listView, controlBox);

        // Übergabe an QuizDesign
        root = QuizDesign.createBaseLayout(controller, data, body, checkButton, feedbackLabel);
    }

    public VBox getRoot() {
        return root;
    }

    private void checkAnswer(QuizController controller, QuizData data,
                             ListView<String> listView, Button checkButton, Label feedbackLabel) {
        boolean isCorrect = listView.getItems().equals(data.correctOrder);

        checkButton.setDisable(true);
        listView.setMouseTransparent(true);
        listView.setFocusTraversable(false);

        if (isCorrect) {
            controller.addPoint();
            checkButton.setText("✅ Richtig");
            feedbackLabel.setText("");
        } else {
            checkButton.setText("❌ Falsch");
            if (!controller.isExamMode()) {
                String solutionText = String.join(", ", data.correctOrder);
                feedbackLabel.setText("Richtige Reihenfolge: " + solutionText);
            } else {
                feedbackLabel.setText("");
            }
        }
    }

    private void setupDragAndDrop(ListView<String> listView) {
        listView.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-background-color: transparent; -fx-border-width: 0;");
                    } else {
                        setText(item);
                        setStyle("");
                    }
                }
            };

            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(cell.getItem());
                    db.setContent(content);
                    event.consume();
                }
            });

            cell.setOnDragOver(event -> {
                if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                    event.acceptTransferModes(TransferMode.MOVE);
                }
                event.consume();
            });

            cell.setOnDragDropped(event -> {
                Dragboard db = event.getDragboard();
                if (db.hasString()) {
                    int draggedIdx = listView.getItems().indexOf(db.getString());
                    int targetIdx = cell.getIndex();

                    if (draggedIdx != targetIdx) {
                        String draggedItem = listView.getItems().remove(draggedIdx);
                        listView.getItems().add(targetIdx, draggedItem);
                    }
                    event.setDropCompleted(true);
                    event.consume();
                }
            });

            return cell;
        });
    }
}
