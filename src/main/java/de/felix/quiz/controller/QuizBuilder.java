package de.felix.quiz.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.felix.quiz.model.QuizData;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class QuizBuilder {
    private final BorderPane root;
    private final List<QuizData> quizQuestions = new ArrayList<>();
    private final Stage stage;
    private final Scene menuScene;
    private final Runnable onRefresh; // Callback: Menü-Liste aktualisieren
    private int currentIndex = -1;

    private String currentFileName = "–";

    // Info-Panel Labels
    private final Label fileNameLabel = new Label("Datei: –");
    private final Label totalQuestionsLabel = new Label("Fragen gesamt: 0");
    private final Label currentQuestionLabel = new Label("Aktuelle Frage: –");
    private final Label currentTypeLabel = new Label("Fragetyp: –");

    private final ListView<String> questionListView = new ListView<>();
    private TextField quizNameField;
    private Path currentFilePath;

    private boolean clearAnswers = true;

    private static final Logger LOG = Logger.getLogger(QuizBuilder.class.getName());

    //Hotkeys
    final KeyCombination HK_SAVE_QUIZ = new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN); //Quiz speichern STRG+S
    final KeyCombination HK_SAVE_QUESTION = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN); //Frage speichern STRG+F
    final KeyCombination HK_NEW_QUESTION = new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN); //Neue Frage STRG+N
    final KeyCombination HK_OPEN_QUIZ = new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN); //Quiz öffnen STRG+O
    final KeyCombination HK_NEW_ANSWER = new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN); //Neue Antwort STRG+Q
    final KeyCombination HK_PREV_QUESTION = new KeyCodeCombination(KeyCode.LEFT); //Vorherige Frage Pfeil Links
    final KeyCombination HK_NEXT_QUESTION = new KeyCodeCombination(KeyCode.RIGHT); //Nächste Frage Pfeil Rechts
    Button addAnswerButton;


    public QuizBuilder(Scene menuScene, Stage stage, Runnable onRefresh) {
        this.menuScene = menuScene;
        this.stage = stage;
        this.onRefresh = onRefresh;
        root = new BorderPane();

        VBox formBox = new VBox(15);
        formBox.setPadding(new Insets(20));

        quizNameField = new TextField();
        quizNameField.setPromptText("Quizname eingeben");
        styleInput(quizNameField);

        Button newQuestionButton = new Button("Neue Frage");
        Button saveQuestionButton = new Button("Frage speichern");
        Button deleteQuestionButton = new Button("Frage löschen");
        Button saveQuizButton = new Button("Quiz speichern");
        Button openQuizButton = new Button("Quiz öffnen");
        Button deleteQuizButton = new Button("Quiz löschen");
        Button backButton = new Button("Zurück ins Menü");

        // Navigation
        Button prevButton = new Button("←");
        Button nextButton = new Button("→");

        TextField introField = new TextField();
        introField.setPromptText("Introsatz");
        styleInput(introField);

        Button imageButton = new Button("Bild hinzufügen");
        Label imagePathLabel = new Label("Kein Bild ausgewählt");

        TextField questionField = new TextField();
        questionField.setPromptText("Frage");
        styleInput(questionField);

        TextField outroField = new TextField();
        outroField.setPromptText("Outro");
        styleInput(outroField);

        // EventFilter für Hotkeys
        root.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            try {
                if (HK_SAVE_QUIZ.match(e)) {
                    e.consume();
                    saveQuizButton.fire();
                } else if (HK_SAVE_QUESTION.match(e)) {
                    e.consume();
                    saveQuestionButton.fire();
                } else if (HK_NEW_QUESTION.match(e)) {
                    e.consume();
                    newQuestionButton.fire();
                } else if (HK_OPEN_QUIZ.match(e)) {
                    e.consume();
                    openQuizButton.fire();
                } else if (HK_PREV_QUESTION.match(e)) {
                    e.consume();
                    prevButton.fire();
                } else if (HK_NEXT_QUESTION.match(e)) {
                    e.consume();
                    nextButton.fire();
                } else if (HK_NEW_ANSWER.match(e)) {
                    e.consume();
                    addAnswerButton.fire();
                }
            } catch (Exception ex) {
                LOG.log(Level.SEVERE, "Fehler beim Ausführen eines Hotkeys", ex);
            }
        });


        // Antwortenbereich + Add-Button
        VBox answersBox = new VBox(10);
        HBox addRowBox = new HBox();
        VBox answersContainer = new VBox(10, answersBox, addRowBox);

        ScrollPane scrollPane = new ScrollPane(formBox);
        scrollPane.setFitToWidth(true);

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);
        HBox questionActionBox = new HBox(10, newQuestionButton, saveQuestionButton, actionSpacer, deleteQuestionButton);
        questionActionBox.setAlignment(Pos.CENTER);
        questionActionBox.setPadding(new Insets(10));

        formBox.getChildren().addAll(
                new Label("Quizname:"), quizNameField,
                new Label("Introsatz:"), introField,
                new Label("Bild:"), imageButton, imagePathLabel,
                new Label("Frage:"), questionField,
                new Label("Outro:"), outroField,
                new Label("Antworten:"), answersContainer,
                questionActionBox,
                saveQuizButton,
                openQuizButton,
                deleteQuizButton,
                backButton
        );

        // Fragetyp-Dropdown
        ComboBox<String> typeSelector = new ComboBox<>();
        typeSelector.getItems().addAll(
                "SINGLE_CHOICE",
                "MULTIPLE_CHOICE",
                "IMAGE_CHOICE",
                "ORDER",
                "DRAG_DROP_ASSIGNMENT",
                "FILL_IN_THE_BLANK"
        );
        typeSelector.setValue("SINGLE_CHOICE");

        HBox typeBox = new HBox(10, new Label("Fragetyp:"), typeSelector);
        typeBox.setPadding(new Insets(10));

        // Typwechsel-Listener
        typeSelector.valueProperty().addListener((obs, oldType, newType) -> {
            if (!(newType.equals("DRAG_DROP_ASSIGNMENT")) && !(oldType.equals("DRAG_DROP_ASSIGNMENT")))
                clearAnswers = false;
            LOG.info("clearAnswers: false");
            QuizData dummy = new QuizData();
            dummy.type = newType;
            buildUIForType(dummy, answersBox, addRowBox, typeSelector);
            LOG.info("Type selected " + newType);
        });

        // ComboBox aufklappen bei Fokussierung
        typeSelector.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal) {
                typeSelector.show();
            }
        });

        prevButton.setOnAction(e -> {
            if (currentIndex > 0) {
                currentIndex--;
                QuizData q = quizQuestions.get(currentIndex);
                introField.setText(nullToEmpty(q.intro));
                questionField.setText(nullToEmpty(q.question));
                outroField.setText(nullToEmpty(q.outro));
                imagePathLabel.setText(q.questionPicture != null && !q.questionPicture.isBlank() ? q.questionPicture : "Kein Bild ausgewählt");
                buildUIForType(q, answersBox, addRowBox, typeSelector);
                updateInfoPanel(currentFileName, q.type);
                questionListView.getSelectionModel().select(currentIndex);
            }
        });
        prevButton.setTooltip(new Tooltip("Pfeil nach Links"));

        nextButton.setOnAction(e -> {
            if (currentIndex < quizQuestions.size() - 1) {
                currentIndex++;
                QuizData q = quizQuestions.get(currentIndex);
                introField.setText(nullToEmpty(q.intro));
                questionField.setText(nullToEmpty(q.question));
                outroField.setText(nullToEmpty(q.outro));
                imagePathLabel.setText(q.questionPicture != null && !q.questionPicture.isBlank() ? q.questionPicture : "Kein Bild ausgewählt");
                buildUIForType(q, answersBox, addRowBox, typeSelector);
                updateInfoPanel(currentFileName, q.type);
                questionListView.getSelectionModel().select(currentIndex);
            }
        });
        nextButton.setTooltip(new Tooltip("Pfeil nach Rechts"));

        HBox navBox = new HBox(10, prevButton, nextButton);
        navBox.setAlignment(Pos.CENTER);

        VBox infoBox = new VBox(5, fileNameLabel, totalQuestionsLabel, currentQuestionLabel, currentTypeLabel, typeBox, navBox);
        infoBox.setPadding(new Insets(10));

        // Fragenliste Navigation
        questionListView.setOnMouseClicked(e -> {
            int selectedIndex = questionListView.getSelectionModel().getSelectedIndex();
            if (selectedIndex >= 0) {
                currentIndex = selectedIndex;
                QuizData q = quizQuestions.get(currentIndex);
                introField.setText(nullToEmpty(q.intro));
                questionField.setText(nullToEmpty(q.question));
                outroField.setText(nullToEmpty(q.outro));
                imagePathLabel.setText(q.questionPicture != null && !q.questionPicture.isBlank() ? q.questionPicture : "Kein Bild ausgewählt");
                buildUIForType(q, answersBox, addRowBox, typeSelector);
                updateInfoPanel(currentFileName, q.type);
            }
        });

        VBox rightPanel = new VBox(10, infoBox, new Label("Fragenübersicht:"), questionListView);
        rightPanel.setPadding(new Insets(10));
        rightPanel.getStyleClass().add("sidebar");

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(scrollPane, rightPanel);
        splitPane.setDividerPositions(0.7);
        root.setCenter(splitPane);

        // Bild-Upload (Fragebild) mit AppData-relativem Speichern
        imageButton.setOnAction(e -> {
            LOG.warning("imageButtonAction");
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bilder", "*.png", "*.jpg", "*.jpeg"));
            File selected = chooser.showOpenDialog(stage);
            if (selected != null) {
                try {
                    String quizName = quizNameField.getText().trim();
                    if (quizName.isEmpty()) quizName = "default";
                    Path targetFile = AppFiles.copyImageToAppData(selected.toPath(), quizName);
                    imagePathLabel.setText("/images/" + quizName + "/" + targetFile.getFileName().toString());
                } catch (IOException ex) {
                    ex.printStackTrace();
                    new Alert(Alert.AlertType.ERROR, "Fehler beim Kopieren des Bildes: " + ex.getMessage()).showAndWait();
                }
            }
        });


        // Neue Frage
        newQuestionButton.setOnAction(e -> {
            introField.clear();
            questionField.clear();
            outroField.clear();
            imagePathLabel.setText("Kein Bild ausgewählt");
            currentIndex = -1;
            QuizData dummy = new QuizData();
            dummy.type = "SINGLE_CHOICE";
            buildUIForType(dummy, answersBox, addRowBox, typeSelector);
            typeSelector.setValue("SINGLE_CHOICE");
            updateInfoPanel(currentFileName, null);
            focusControl(introField);
        });
        newQuestionButton.setTooltip(new Tooltip("STRG + N"));

        // Frage speichern
        saveQuestionButton.setOnAction(e -> {
            LOG.warning("saveQuestionButtonAction");
            QuizData data = new QuizData();
            data.type = typeSelector.getValue();
            data.intro = introField.getText();
            data.questionPicture = imagePathLabel.getText().equals("Kein Bild ausgewählt") ? "" : imagePathLabel.getText();
            data.question = questionField.getText();
            data.outro = outroField.getText();

            // SINGLE/MULTIPLE
            if (data.type.equals("SINGLE_CHOICE") || data.type.equals("MULTIPLE_CHOICE")) {
                List<QuizData.Option> opts = new ArrayList<>();
                for (Node n : answersBox.getChildren()) {
                    if (n instanceof HBox row) {
                        if (row.getChildren().size() >= 2 && row.getChildren().get(0) instanceof TextField && row.getChildren().get(1) instanceof ComboBox) {
                            TextField tf = (TextField) row.getChildren().get(0);
                            @SuppressWarnings("unchecked")
                            ComboBox<Boolean> cb = (ComboBox<Boolean>) row.getChildren().get(1);
                            opts.add(new QuizData.Option(tf.getText(), cb.getValue()));
                        }
                    }
                }
                data.options = opts;
            }

            // FILL_IN_THE_BLANK
            if (data.type.equals("FILL_IN_THE_BLANK")) {
                List<String> blanks = new ArrayList<>();
                for (Node n : answersBox.getChildren()) {
                    if (n instanceof HBox row) {
                        if (!row.getChildren().isEmpty() && row.getChildren().get(0) instanceof TextField) {
                            TextField tf = (TextField) row.getChildren().get(0);
                            blanks.add(tf.getText());
                        }
                    }
                }
                data.correctAnswers = blanks;
            }

            // ORDER
            if (data.type.equals("ORDER")) {
                List<String> order = new ArrayList<>();
                for (Node n : answersBox.getChildren()) {
                    if (n instanceof HBox row) {
                        if (!row.getChildren().isEmpty() && row.getChildren().get(0) instanceof TextField) {
                            TextField tf = (TextField) row.getChildren().get(0);
                            order.add(tf.getText());
                        }
                    }
                }
                data.orderOptions = new ArrayList<>(order);
                data.correctOrder = new ArrayList<>(order);
            }

            // DRAG_DROP_ASSIGNMENT
            if (data.type.equals("DRAG_DROP_ASSIGNMENT")) {
                if (!answersBox.getChildren().isEmpty() && answersBox.getChildren().get(0) instanceof HBox lists) {
                    VBox leftBox = (VBox) lists.getChildren().get(0);
                    VBox rightBox = (VBox) lists.getChildren().get(1);

                    @SuppressWarnings("unchecked")
                    ListView<String> optionsList = (ListView<String>) leftBox.getChildren().get(2);
                    @SuppressWarnings("unchecked")
                    ListView<String> correctList = (ListView<String>) rightBox.getChildren().get(1);

                    data.orderOptions = new ArrayList<>(optionsList.getItems());
                    data.correctOrder = new ArrayList<>(correctList.getItems());
                }
            }

            // IMAGE_CHOICE (wie vorher, Pfadkopie in AppData)
            if (data.type.equals("IMAGE_CHOICE")) {
                LOG.warning("IMAGE_CHOICE ist aktiv");
                List<QuizData.ImageOption> imageOpts = new ArrayList<>();
                String quizName = quizNameField.getText().trim();

                for (Node n : answersBox.getChildren()) {
                    if (n instanceof HBox row) {
                        if (row.getChildren().size() >= 4 &&
                                row.getChildren().get(0) instanceof TextField &&
                                row.getChildren().get(2) instanceof Label &&
                                row.getChildren().get(3) instanceof ComboBox) {

                            TextField labelField = (TextField) row.getChildren().get(0);
                            Label pathLabel = (Label) row.getChildren().get(2);
                            @SuppressWarnings("unchecked")
                            ComboBox<Boolean> cb = (ComboBox<Boolean>) row.getChildren().get(3);

                            String path = pathLabel.getText();
                            if ("Kein Bild ausgewählt".equals(path) || path == null || path.isBlank()) {
                                path = "";
                                LOG.warning("Kein Bild ausgewählt!!!!!");
                            }
                            imageOpts.add(new QuizData.ImageOption(path, labelField.getText(), cb.getValue()));
                            LOG.warning("Pfad: " + path + "\nPfadlabel: " + pathLabel);
                        }
                    }
                }
                data.images = imageOpts;
            }

            // Frage speichern (in Liste oder ersetzen)
            if (currentIndex >= 0) {
                quizQuestions.set(currentIndex, data);
            } else {
                quizQuestions.add(data);
                currentIndex = quizQuestions.size() - 1;
            }

            updateInfoPanel(currentFileName, data.type);
            refreshQuestionList();
            questionListView.getSelectionModel().select(currentIndex);
        });
        saveQuestionButton.setTooltip(new Tooltip("STRG + F"));

        // Frage löschen
        deleteQuestionButton.setOnAction(e -> {
            if (currentIndex >= 0 && currentIndex < quizQuestions.size()) {
                // Bestätigungsdialog
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                confirm.setTitle("Frage löschen");
                confirm.setHeaderText("Frage wirklich löschen?");
                confirm.setContentText("Möchtest du die ausgewählte Frage wirklich löschen? Diese Aktion kann nicht rückgängig gemacht werden.");
                var result = confirm.showAndWait();
                if (result.isPresent() && result.get() == ButtonType.OK) {
                    quizQuestions.remove(currentIndex);
                    refreshQuestionList();
                    if (quizQuestions.isEmpty()) {
                        currentIndex = -1;
                        introField.clear();
                        questionField.clear();
                        outroField.clear();
                        imagePathLabel.setText("Kein Bild ausgewählt");
                        QuizData dummy = new QuizData();
                        dummy.type = "SINGLE_CHOICE";
                        buildUIForType(dummy, answersBox, addRowBox, typeSelector);
                        typeSelector.setValue("SINGLE_CHOICE");
                        updateInfoPanel(currentFileName, null);
                    } else {
                        currentIndex = Math.max(0, currentIndex - 1);
                        QuizData q = quizQuestions.get(currentIndex);
                        introField.setText(nullToEmpty(q.intro));
                        questionField.setText(nullToEmpty(q.question));
                        outroField.setText(nullToEmpty(q.outro));
                        imagePathLabel.setText(q.questionPicture != null && !q.questionPicture.isBlank() ? q.questionPicture : "Kein Bild ausgewählt");
                        buildUIForType(q, answersBox, addRowBox, typeSelector);
                        updateInfoPanel(currentFileName, q.type);
                        questionListView.getSelectionModel().select(currentIndex);
                    }
                } // sonst Abbruch, nichts löschen
            }
        });


        // Quiz speichern (Liste)
        saveQuizButton.setOnAction(e -> {
            if (quizQuestions.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Keine Fragen vorhanden!").showAndWait();
                return;
            }
            try {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                String json = gson.toJson(quizQuestions);

                Path file;
                if (currentFilePath != null) {
                    // Überschreibe die geladene Datei (falls aus AppData)
                    file = currentFilePath;
                } else {
                    String quizName = quizNameField.getText().trim();
                    if (quizName.isEmpty()) {
                        quizName = "quiz_" + System.currentTimeMillis();
                    }
                    // Speichere IMMER in AppData/quizzes
                    file = AppFiles.getQuizzesDir().resolve(quizName + ".json");
                }

                Files.createDirectories(file.getParent());
                Files.writeString(file, json, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                currentFilePath = file;
                currentFileName = file.getFileName().toString();

                updateInfoPanel(currentFileName, quizQuestions.get(Math.max(currentIndex, 0)).type);
                refreshQuestionList();

                // Menü-Liste sofort aktualisieren
                if (onRefresh != null) onRefresh.run();

                new Alert(Alert.AlertType.INFORMATION, "Quiz gespeichert: " + file.toAbsolutePath()).showAndWait();
            } catch (IOException ex) {
                ex.printStackTrace();
                new Alert(Alert.AlertType.ERROR, "Fehler beim Speichern: " + ex.getMessage()).showAndWait();
            }
        });
        saveQuizButton.setTooltip(new Tooltip("STRG + S"));


        // Quiz öffnen
        openQuizButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Quiz-Dateien", "*.json"));

            // AppData-Quizzes als Startordner vorschlagen (nur wenn vorhanden und ein Verzeichnis)
            File defaultDir = AppFiles.getQuizzesDir().toFile();
            if (defaultDir.exists() && defaultDir.isDirectory()) {
                try {
                    chooser.setInitialDirectory(defaultDir);
                } catch (IllegalArgumentException iae) {
                    // Falls das System den Ordner aus irgendeinem Grund nicht akzeptiert, ignoriere es still
                    System.err.println("Initial directory not accepted: " + defaultDir.getAbsolutePath());
                }
            }

            File selected = chooser.showOpenDialog(stage);
            if (selected != null) {
                currentFilePath = selected.toPath();
                try {
                    String json = Files.readString(selected.toPath());
                    Type listType = new TypeToken<List<QuizData>>(){}.getType();
                    List<QuizData> loaded = new Gson().fromJson(json, listType);

                    quizQuestions.clear();
                    quizQuestions.addAll(loaded);
                    currentIndex = quizQuestions.isEmpty() ? -1 : 0;

                    String quizName = selected.getName().replaceFirst("\\.json$", "");
                    currentFileName = quizName;

                    refreshQuestionList();

                    if (currentIndex >= 0) {
                        QuizData q = quizQuestions.get(currentIndex);
                        introField.setText(nullToEmpty(q.intro));
                        questionField.setText(nullToEmpty(q.question));
                        outroField.setText(nullToEmpty(q.outro));
                        imagePathLabel.setText(q.questionPicture != null && !q.questionPicture.isBlank()
                                                       ? q.questionPicture : "Kein Bild ausgewählt");

                        buildUIForType(q, answersBox, addRowBox, typeSelector);
                        updateInfoPanel(currentFileName, q.type);
                        questionListView.getSelectionModel().select(currentIndex);
                    } else {
                        updateInfoPanel(currentFileName, null);
                        new Alert(Alert.AlertType.INFORMATION, "Die geladene Datei enthält keine Fragen.").showAndWait();
                    }
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, "Fehler beim Öffnen", ex);
                    new Alert(Alert.AlertType.ERROR, "Fehler beim Öffnen: " + ex.getMessage()).showAndWait();
                }
            }
        });
        openQuizButton.setTooltip(new Tooltip("STRG + O"));

        deleteQuizButton.getStyleClass().add("delete-button");
        deleteQuizButton.setOnAction(ev -> {
            if (currentFileName == null || currentFileName.isBlank()) {
                new Alert(Alert.AlertType.INFORMATION, "Kein Quiz geöffnet zum Löschen.").showAndWait();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Quiz löschen");
            confirm.setHeaderText("Quiz wirklich löschen?");
            confirm.setContentText("Möchtest du das Quiz \"" + currentFileName + "\" und alle zugehörigen Bilder löschen? Diese Aktion kann nicht rückgängig gemacht werden.");
            var result = confirm.showAndWait();

            if (result.isPresent() && result.get() == ButtonType.OK) {
                boolean ok = AppFiles.deleteQuizData(currentFileName);
                if (ok) {
                    quizQuestions.clear();
                    currentIndex = -1;
                    currentFileName = null;
                    currentFilePath = null;
                    refreshQuestionList();
                    //clear all fields
                    introField.clear();
                    questionField.clear();
                    outroField.clear();
                    imagePathLabel.setText("Kein Bild ausgewählt");
                    answersBox.getChildren().clear();
                    typeSelector.setValue("SINGLE_CHOICE");
                    updateInfoPanel(null, null);

                    new Alert(Alert.AlertType.INFORMATION, "Quiz wurde gelöscht.").showAndWait();
                } else {
                    new Alert(Alert.AlertType.ERROR, "Fehler beim Löschen des Quiz.").showAndWait();
                }
            }
        });


        backButton.setOnAction(e -> {
            stage.setScene(menuScene);
            stage.setTitle("Quiz Auswahl");
        });

        // Initiale UI
        QuizData initial = new QuizData();
        initial.type = "SINGLE_CHOICE";
        buildUIForType(initial, answersBox, addRowBox, typeSelector);
    }

    // UI-Aufbau je Typ (einheitlich für Wechsel/Öffnen/Navi)
    private void buildUIForType(QuizData q, VBox answersBox, HBox addRowBox, ComboBox<String> typeSelector) {
        LOG.warning("buildUIForType aufgerufen");
        String type = q.type != null ? q.type : "SINGLE_CHOICE";
        typeSelector.setValue(type);

//        TODO: Clear only, when new Question or switch Question, no clear when switching QuestionType
        //TODO: boolean for switching
        //TODO: no new field, when not clearing
        if (clearAnswers) {
            answersBox.getChildren().clear();
            addRowBox.getChildren().clear();
            LOG.info("cleared answersBox & addRowBox");
        }

        switch (type) {
            case "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> {
                if (q.options != null && !q.options.isEmpty()) {
                    for (QuizData.Option opt : q.options) {
                        TextField tf = new TextField();
                        tf = new TextField(opt.text);
                        styleInput(tf);
                        ComboBox<Boolean> cb = new ComboBox<>();
                        cb.getItems().addAll(true, false);
                        cb.setValue(opt.correct);
                        Button remove = new Button("−");
                        HBox row = new HBox(10, tf, cb, remove);
                        remove.setOnAction(ev -> answersBox.getChildren().remove(row));
                        answersBox.getChildren().add(row);
                    }
                } else if (clearAnswers) {
                    addAnswerRow(answersBox);
                }
                setAddButtonForType(type, answersBox, addRowBox);
            }
            case "FILL_IN_THE_BLANK" -> {
                if (q.correctAnswers != null && !q.correctAnswers.isEmpty()) {
                    for (String ans : q.correctAnswers) {
                        TextField tf = new TextField(ans);
                        Button remove = new Button("−");
                        HBox row = new HBox(10, tf, remove);
                        remove.setOnAction(ev -> answersBox.getChildren().remove(row));
                        answersBox.getChildren().add(row);
                    }
                } else if (clearAnswers) {
                    addFillBlankRow(answersBox);
                }
                setAddButtonForType(type, answersBox, addRowBox);
            }
            case "ORDER" -> {
                Label hint = new Label("Antworten in der korrekten Reihenfolge eingeben:");
                answersBox.getChildren().add(hint);
                if (q.correctOrder != null && !q.correctOrder.isEmpty()) {
                    for (String ans : q.correctOrder) {
                        TextField tf = new TextField(ans);
                        Button remove = new Button("−");
                        HBox row = new HBox(10, tf, remove);
                        remove.setOnAction(ev -> answersBox.getChildren().remove(row));
                        answersBox.getChildren().add(row);
                    }
                } else if (clearAnswers) {
                    addOrderRow(answersBox);
                }
                setAddButtonForType(type, answersBox, addRowBox);
            }
            case "DRAG_DROP_ASSIGNMENT" -> {
                answersBox.getChildren().clear();
                addRowBox.getChildren().clear();
                addDragDropUI(answersBox);
                // Füllen, falls vorhanden
                if (q.orderOptions != null || q.correctOrder != null) {
                    HBox lists = (HBox) answersBox.getChildren().get(0);
                    VBox left = (VBox) lists.getChildren().get(0);
                    VBox right = (VBox) lists.getChildren().get(1);
                    @SuppressWarnings("unchecked")
                    ListView<String> opts = (ListView<String>) left.getChildren().get(2);
                    opts.getStyleClass().add("builder-input");
                    @SuppressWarnings("unchecked")
                    ListView<String> correct = (ListView<String>) right.getChildren().get(1);
                    correct.getStyleClass().add("builder-input");
                    if (q.orderOptions != null) opts.getItems().setAll(q.orderOptions);
                    if (q.correctOrder != null) correct.getItems().setAll(q.correctOrder);
                }
                // kein Add-Button hier; Eingabezeile Bestandteil der UI
            }
            case "IMAGE_CHOICE" -> {
                LOG.warning("case IMAGE_CHOICE");
                if (q.images != null && !q.images.isEmpty()) {
                    for (QuizData.ImageOption imgOpt : q.images) {
                        LOG.warning("imgOpt: " + imgOpt);
                        TextField labelField = new TextField(nullToEmpty(imgOpt.label));
                        Button optionImageButton = new Button("Bild auswählen");
                        LOG.warning("OptionImageButton erzeugt");
                        Label pathLabel = new Label((imgOpt.path != null && !imgOpt.path.isBlank())
                                                            ? imgOpt.path : "Kein Bild ausgewählt");
                        ComboBox<Boolean> cb = new ComboBox<>();
                        cb.getItems().addAll(true, false);
                        cb.setValue(imgOpt.correct);

                        optionImageButton.setOnAction(ev -> {
                            LOG.warning("OptionImageButton gedrückt");
                            FileChooser chooser = new FileChooser();
                            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Bilder", "*.png", "*.jpg", "*.jpeg"));
                            File selected = chooser.showOpenDialog(stage);
                            LOG.warning("File ausgewählt: " + chooser);
                            if (selected != null) {
                                try {
                                    String quizName = quizNameField.getText().trim();
                                    if (quizName.isEmpty()) quizName = "default";
                                    Path targetFile = AppFiles.copyImageToAppData(selected.toPath(), quizName);
                                    pathLabel.setText("/images/" + quizName + "/" + targetFile.getFileName().toString());
                                    LOG.log(Level.WARNING, "Kopiert nach: " + targetFile.toAbsolutePath());
                                    LOG.log(Level.WARNING, "Label gesetzt auf: " + pathLabel.getText());
                                } catch (IOException ex) {
                                    LOG.log(Level.SEVERE, "Fehler beim Kopieren", ex);
                                    new Alert(Alert.AlertType.ERROR, "Fehler beim Kopieren: " + ex.getMessage()).showAndWait();
                                }
                            }
                        });

                        Button remove = new Button("−");
                        HBox row = new HBox(10, labelField, optionImageButton, pathLabel, cb, remove);
                        remove.setOnAction(ev -> answersBox.getChildren().remove(row));
                        answersBox.getChildren().add(row);
                    }
                } else if (clearAnswers) {
                    answersBox.getChildren().add(createImageChoiceRow());
                }
                setAddButtonForType(type, answersBox, addRowBox);
            }
            default -> {
                addAnswerRow(answersBox);
                setAddButtonForType("SINGLE_CHOICE", answersBox, addRowBox);
            }
        }
        if(!(clearAnswers))
            clearAnswers = true;
    }

    private void setAddButtonForType(String type, VBox answersBox, HBox addRowBox) {
        addRowBox.getChildren().clear();
        switch (type) {
            case "SINGLE_CHOICE", "MULTIPLE_CHOICE" -> {
                addAnswerButton = new Button("+ Antwort hinzufügen");
                addAnswerButton.setOnAction(e -> addAnswerRow(answersBox));
                addRowBox.getChildren().add(addAnswerButton);
            }
            case "FILL_IN_THE_BLANK" -> {
                addAnswerButton = new Button("+ Antwort hinzufügen");
                addAnswerButton.setOnAction(e -> addFillBlankRow(answersBox));
                addRowBox.getChildren().add(addAnswerButton);
            }
            case "ORDER" -> {
                addAnswerButton = new Button("+ Antwort hinzufügen");
                addAnswerButton.setOnAction(e -> addOrderRow(answersBox));
                addRowBox.getChildren().add(addAnswerButton);
            }
            case "IMAGE_CHOICE" -> {
                addAnswerButton = new Button("+ Antwort hinzufügen");
                addAnswerButton.setOnAction(e -> answersBox.getChildren().add(createImageChoiceRow()));
                addRowBox.getChildren().add(addAnswerButton);
            }
            case "DRAG_DROP_ASSIGNMENT" -> {
                // kein Add-Button; Eingabezeile Bestandteil der UI
            }
        }
    }

    // Single/Multiple Choice UI
    private void addAnswerRow(VBox answersBox) {
        TextField tf = new TextField();
        tf.setPromptText("Antworttext");
        styleInput(tf);
        ComboBox<Boolean> cb = new ComboBox<>();
        cb.getItems().addAll(true, false);
        cb.setValue(false);
        Button removeButton = new Button("−");
        HBox row = new HBox(10, tf, cb, removeButton);
        removeButton.setOnAction(e -> answersBox.getChildren().remove(row));
        answersBox.getChildren().add(row);

        // Fokus auf neu erstelltes Feld setzen
        focusControl(tf);
    }

    // Fill in the Blank UI
    private void addFillBlankRow(VBox answersBox) {
        TextField tf = new TextField();
        tf.setPromptText("Antworttext");
        styleInput(tf);
        Button removeButton = new Button("−");
        HBox row = new HBox(10, tf, removeButton);
        removeButton.setOnAction(e -> answersBox.getChildren().remove(row));
        answersBox.getChildren().add(row);

        // Fokus auf neu erstelltes Feld setzen
        focusControl(tf);
    }

    // Order UI (Eingabereihenfolge = korrekte Reihenfolge)
    private void addOrderRow(VBox answersBox) {
        TextField tf = new TextField();
        tf.setPromptText("Antworttext (in korrekter Reihenfolge eingeben)");
        styleInput(tf);
        Button removeButton = new Button("−");
        HBox row = new HBox(10, tf, removeButton);
        removeButton.setOnAction(e -> answersBox.getChildren().remove(row));
        answersBox.getChildren().add(row);

        // Fokus auf neu erstelltes Feld setzen
        focusControl(tf);
    }

    // Drag & Drop UI (mit Eingabefeld + zwei Listen)
    private void addDragDropUI(VBox container) {
        TextField optionInput = new TextField();
        optionInput.setPromptText("Antwortmöglichkeit eingeben");
        styleInput(optionInput);

        Button addOptionButton = new Button("+");
        ListView<String> optionsList = new ListView<>();
        ListView<String> correctList = new ListView<>();

        optionsList.setPrefHeight(180);
        correctList.setPrefHeight(180);

        addOptionButton.setOnAction(ev -> {
            String text = optionInput.getText().trim();
            if (!text.isEmpty()) {
                optionsList.getItems().add(text);
                optionInput.clear();
                // Fokus auf neu erstelltes Feld setzen
                focusControl(optionInput);
            }
        });

        // Drag from options
        optionsList.setOnDragDetected(e -> {
            String selected = optionsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                Dragboard db = optionsList.startDragAndDrop(TransferMode.MOVE);
                ClipboardContent content = new ClipboardContent();
                content.putString(selected);
                db.setContent(content);
            }
            e.consume();
        });

        // Accept drop on correct list
        correctList.setOnDragOver(e -> {
            if (e.getGestureSource() != correctList && e.getDragboard().hasString()) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        // Drop onto correct list (Mehrfachverwendung erlaubt)
        correctList.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            if (db.hasString()) {
                correctList.getItems().add(db.getString());
                e.setDropCompleted(true);
            } else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        VBox left = new VBox(5,
                             new Label("Antwortmöglichkeiten:"),
                             new HBox(5, optionInput, addOptionButton),
                             optionsList
        );
        VBox right = new VBox(5,
                              new Label("Korrekte Antworten:"),
                              correctList
        );

        HBox lists = new HBox(20, left, right);
        lists.setAlignment(Pos.CENTER);

        container.getChildren().add(lists);
    }

    // Eine Bild-Antwortzeile für IMAGE_CHOICE (speichert klassenpfad-relativ)
    private HBox createImageChoiceRow() {
        TextField labelField = new TextField();
        labelField.setPromptText("Bild-Beschriftung");
        styleInput(labelField);

        Button optionImageButton = new Button("Bild auswählen");
        Label pathLabel = new Label("Kein Bild ausgewählt");

        ComboBox<Boolean> cb = new ComboBox<>();
        cb.getItems().addAll(true, false);
        cb.setValue(false);

        optionImageButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Bilder", "*.png", "*.jpg", "*.jpeg")
            );
            File selected = chooser.showOpenDialog(stage);
            if (selected != null) {
                try {
                    String quizName = quizNameField.getText().trim();
                    if (quizName.isEmpty()) quizName = "default";

                    // Korrekt: Kopie in AppData
                    Path targetFile = AppFiles.copyImageToAppData(selected.toPath(), quizName);

                    // AppData-relativen Pfad ins Label schreiben
                    pathLabel.setText("/images/" + quizName + "/" + targetFile.getFileName().toString());
                } catch (IOException ex) {
                    LOG.log(Level.SEVERE, "Fehler beim Kopieren des Bildes", ex);
                    new Alert(Alert.AlertType.ERROR, "Fehler beim Kopieren des Bildes: " + ex.getMessage()).showAndWait();
                }
            }
        });

        Button removeButton = new Button("−");
        HBox row = new HBox(10, labelField, optionImageButton, pathLabel, cb, removeButton);
        removeButton.setOnAction(e -> {
            Node parent = row.getParent();
            if (parent instanceof VBox box) {
                box.getChildren().remove(row);
            }
        });

        // Fokus auf neu erstelltes Feld setzen
        focusControl(labelField);

        return row;
    }


    private void updateInfoPanel(String fileName, String type) {
        fileNameLabel.setText("Datei: " + (fileName != null && !fileName.isBlank() ? fileName : "–"));
        totalQuestionsLabel.setText("Fragen gesamt: " + quizQuestions.size());
        currentQuestionLabel.setText("Aktuelle Frage: " + (currentIndex >= 0 ? (currentIndex + 1) : "–"));
        currentTypeLabel.setText("Fragetyp: " + (type != null && !type.isBlank() ? type : "–"));
    }

    private void refreshQuestionList() {
        questionListView.getItems().clear();
        for (int i = 0; i < quizQuestions.size(); i++) {
            String text = quizQuestions.get(i).question;
            if (text == null) text = "";
            text = text.length() > 40 ? text.substring(0, 40) + "..." : text;
            questionListView.getItems().add((i + 1) + ". " + text);
        }
    }

    public Scene buildScene(boolean darkMode) {
        Scene scene = new Scene(root, 1000, 650);
        String stylesheet = darkMode ? "/styles/dark.css" : "/styles/light.css";
        scene.getStylesheets().add(getClass().getResource(stylesheet).toExternalForm());
        return scene;
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private void styleInput(javafx.scene.control.Control c) {
        if (c == null) return;
        // fügt die CSS-Klasse hinzu, falls noch nicht vorhanden
        if (!c.getStyleClass().contains("builder-input")) {
            c.getStyleClass().add("builder-input");
        }
    }

    private void focusControl(Control control) {
        Platform.runLater(control::requestFocus);
    }
}
