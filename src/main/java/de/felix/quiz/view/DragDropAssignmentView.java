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

public class DragDropAssignmentView {
    private VBox root;

    public DragDropAssignmentView(QuizController controller, QuizData data) {
        // Anzahl der Slots anhand der korrekten Reihenfolge
        int slotCount = (int) data.correctOrder.stream()
                .filter(c -> c != null && !"null".equals(c.trim()))
                .count();

        // Ziel-Liste (Slots)
        ListView<String> targetList = new ListView<>();
        targetList.getItems().addAll(generateEmptySlots(slotCount));
        targetList.getStyleClass().add("question-box");

        // Quell-Liste (Optionen)
        ListView<String> sourceList = new ListView<>();
        List<String> shuffledOptions = new ArrayList<>(data.orderOptions);
        Collections.shuffle(shuffledOptions);
        sourceList.getItems().addAll(shuffledOptions);

        // Dynamische Breite/Höhe
        double spacing = 40;
        double totalMargin = 100 + spacing;
        targetList.prefWidthProperty().bind(controller.getStage().widthProperty().subtract(totalMargin).divide(2));
        sourceList.prefWidthProperty().bind(controller.getStage().widthProperty().subtract(totalMargin).divide(2));
        targetList.prefHeightProperty().bind(controller.getStage().heightProperty().multiply(0.5));
        sourceList.prefHeightProperty().bind(controller.getStage().heightProperty().multiply(0.5));

        setupDragAndDrop(sourceList, targetList);

        HBox listsBox = new HBox(spacing, targetList, sourceList);
        listsBox.setAlignment(javafx.geometry.Pos.CENTER);
        listsBox.setPadding(new javafx.geometry.Insets(10));

        // Check-Button + Feedback
        Button checkButton = new Button("Antwort prüfen");
        Label feedbackLabel = new Label();
        feedbackLabel.setWrapText(true);

        checkButton.setOnAction(e -> checkAnswer(controller, data, targetList, checkButton, feedbackLabel));

        VBox body = new VBox(15, listsBox);

        // Übergabe an QuizDesign
        root = QuizDesign.createBaseLayout(controller, data, body, checkButton, feedbackLabel);
    }

    public VBox getRoot() {
        return root;
    }

    private List<String> generateEmptySlots(int count) {
        List<String> slots = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            slots.add(null);
        }
        return slots;
    }

    private void setupDragAndDrop(ListView<String> source, ListView<String> target) {
        // Quelle: Optionen rechts
        source.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    int index = getIndex();
                    int maxIndex = lv.getItems().size() - 1;

                    if (empty || index > maxIndex) {
                        // Unsichtbar machen
                        setText(null);
                        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                        setMouseTransparent(true);
                        setPrefHeight(0);
                    } else {
                        // Normale Darstellung für echte Optionen
                        setText(item);
                        setStyle("");
                        setMouseTransparent(false);
                        setPrefHeight(Region.USE_COMPUTED_SIZE);
                    }
                }
            };

            // Drag starten
            cell.setOnDragDetected(event -> {
                if (!cell.isEmpty()) {
                    Dragboard db = cell.startDragAndDrop(TransferMode.MOVE);
                    ClipboardContent content = new ClipboardContent();
                    content.putString(cell.getItem());
                    db.setContent(content);
                    event.consume();
                }
            });

            return cell;
        });


        // Ziel: Slots links
        target.setCellFactory(lv -> {
            ListCell<String> cell = new ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    int index = getIndex();
                    int maxIndex = lv.getItems().size() - 1;

                    if (empty || index > maxIndex) {
                        setText(null);
                        setStyle("-fx-background-color: transparent; -fx-border-color: transparent;");
                        setMouseTransparent(true);
                        setPrefHeight(0);
                    } else if (item == null) {
                        setText(""); // Leeres Drop-Ziel
                        setStyle(""); // Sichtbar, aber ohne Inhalt
                        setMouseTransparent(false);
                        setPrefHeight(Region.USE_COMPUTED_SIZE);
                    } else {
                        setText(item);
                        setStyle(""); // Normale Darstellung
                        setMouseTransparent(false);
                        setPrefHeight(Region.USE_COMPUTED_SIZE);
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
                    String draggedItem = db.getString();
                    int draggedIdx = target.getItems().indexOf(draggedItem);
                    int targetIdx = cell.getIndex();

                    if (draggedIdx != -1 && targetIdx != -1 && draggedIdx != targetIdx) {
                        String dragged = target.getItems().remove(draggedIdx);
                        target.getItems().add(targetIdx, dragged);
                    } else if (target.getItems().get(targetIdx) == null) {
                        target.getItems().set(targetIdx, draggedItem);
                        source.getItems().remove(draggedItem);
                    }
                    event.setDropCompleted(true);
                    event.consume();
                }
            });

            return cell;
        });

        // Rückgabe nach rechts
        source.setOnDragOver(event -> {
            if (event.getGestureSource() != source && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        source.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            if (db.hasString()) {
                String item = db.getString();
                if (!source.getItems().contains(item)) {
                    source.getItems().add(item);
                    int idx = target.getItems().indexOf(item);
                    if (idx != -1) target.getItems().set(idx, null);
                }
                event.setDropCompleted(true);
                event.consume();
            }
        });
    }

    private void checkAnswer(QuizController controller, QuizData data,
                             ListView<String> targetList, Button checkButton, Label feedbackLabel) {
        List<String> userAnswers = targetList.getItems();
        boolean isCorrect = userAnswers.equals(data.correctOrder);

        checkButton.setDisable(true);
        targetList.setMouseTransparent(true);
        targetList.setFocusTraversable(false);

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
}
