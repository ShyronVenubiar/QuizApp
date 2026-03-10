package de.felix.quiz.controller;

import de.felix.quiz.model.QuizData;
import de.felix.quiz.view.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizController {
    private Stage stage;
    private final Scene menuScene;
    private String quizFile;
    private int width, height;
    private boolean fullscreen;
    private boolean darkMode;
    private boolean examMode;
    private double baseFontSize;

    private List<VBox> questionViews = new ArrayList<>();
    private int currentIndex = 0;
    private int score = 0;
    private Label scoreLabel = new Label("Punkte: 0");
    private ProgressBar progressBar = new ProgressBar(0);
    private int totalQuestions = 0;


    public QuizController(Stage stage, Scene menuScene, String quizFile, int width, int height, boolean fullscreen, boolean darkMode, boolean examMode) {
        this.stage = stage;
        this.menuScene = menuScene;
        this.quizFile = quizFile;
        this.width = width;
        this.height = height;
        this.fullscreen = fullscreen;
        this.darkMode = darkMode;
        this.examMode = examMode;
        this.baseFontSize = Math.max(16, height / 40.0); // dynamisch + lesbar

        loadQuestions();
        // Wenn keine Fragen geladen wurden, informiere den Nutzer und kehre ins Menü zurück
        if (questionViews.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Das Quiz konnte nicht geladen werden oder enthält keine Fragen.");
            alert.showAndWait();
            // zurück ins Menü (menuScene wurde übergeben)
            if (menuScene != null) {
                stage.setScene(menuScene);
                stage.setTitle("Quiz Auswahl");
            }
            return;
        }

        // Sonst normal starten
        showCurrentQuestion();
    }

    private void loadQuestions() {
        try {
            List<QuizData> quizItems = QuizLoader.load(quizFile);
            if (quizItems == null || quizItems.isEmpty()) {
                System.err.println("QuizLoader lieferte keine Fragen für: " + quizFile);
                totalQuestions = 0;
                return;
            }
            Collections.shuffle(quizItems);
            System.out.println("Geladene Fragen: " + quizItems.size());
            totalQuestions = quizItems.size();
            for (QuizData item : quizItems) {
                switch (item.type) {
                    case "MULTIPLE_CHOICE" -> questionViews.add(new MultipleChoiceQuestionView(this, item).getRoot());
                    case "ORDER" -> questionViews.add(new OrderQuestionView(this, item).getRoot());
                    case "IMAGE_CHOICE" -> questionViews.add(new ImageChoiceQuestionView(this, item).getRoot());
                    case "DRAG_DROP_ASSIGNMENT" -> questionViews.add(new DragDropAssignmentView(this, item).getRoot());
                    case "SINGLE_CHOICE" -> questionViews.add(new SingleChoiceQuestionView(this, item).getRoot());
                    case "FILL_IN_THE_BLANK" -> questionViews.add(new FillInTheBlankQuestionView(this, item).getRoot());
                    default -> System.err.println("Unbekannter Fragetyp: " + item.type);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR, "Quiz konnte nicht geladen werden:\n" + e.getMessage());
            alert.showAndWait();
        }
    }



    private void showCurrentQuestion() {
        VBox currentView = questionViews.get(currentIndex);
        Button nextButton = new Button("Weiter");
        nextButton.setOnAction(e -> {
            currentIndex++;
            if (currentIndex < questionViews.size()) {
                showCurrentQuestion();
            } else {
                showResult();
            }
        });

        double progress = (double) currentIndex / questionViews.size();
        progressBar.setProgress(progress);
        progressBar.getStyleClass().add("progress-bar");


        BorderPane layout = new BorderPane();
        layout.setPadding(new Insets(20));

        VBox topBox = new VBox(10, scoreLabel, progressBar);
        layout.setTop(topBox);
        layout.setCenter(currentView);

        Button backButton = new Button("Zurück");
        backButton.setOnAction(e -> previousQuestion());
        backButton.setDisable(!hasPreviousQuestion());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottomBox = new HBox(10, backButton, spacer, nextButton);
        bottomBox.setAlignment(Pos.CENTER);
        bottomBox.setPadding(new Insets(20, 0, 0, 0));
        layout.setBottom(bottomBox);

        Scene scene = new Scene(layout, width, height);
        scene.getRoot().setStyle("-fx-font-size: " + (baseFontSize + 2) + "px;");
        applyStylesheet(scene);
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            double newFontSize = Math.max(16, newVal.doubleValue() / 40.0);
            scene.getRoot().setStyle("-fx-font-size: " + (newFontSize + 2) + "px;");
        });
        stage.setScene(scene);
        stage.setFullScreen(fullscreen);
    }

    private void showResult() {
        progressBar.setProgress(1.0);
        Label resultLabel = new Label("Quiz abgeschlossen!");
        Label scoreSummary = new Label("Du hast " + score + " von " + totalQuestions + " Fragen richtig beantwortet.");

        Button restartButton = new Button("Quiz neu starten");
        restartButton.setOnAction(e -> restartQuiz());

        Button backToMenuButton = new Button("Zurück zum Hauptmenü");
        backToMenuButton.setOnAction(e -> {
            stage.setScene(menuScene);
            stage.setTitle("Quiz Auswahl");
        });



        VBox resultLayout = new VBox(20, progressBar, resultLabel, scoreSummary, restartButton, backToMenuButton);
        resultLayout.setPadding(new Insets(20));

        Scene scene = new Scene(resultLayout, width, height);
        scene.getRoot().setStyle("-fx-font-size: " + (baseFontSize + 2) + "px;");
        applyStylesheet(scene);
        stage.setScene(scene);
        stage.setFullScreen(fullscreen);
    }

    private void applyStylesheet(Scene scene) {
        String stylesheet = darkMode ? "/styles/dark.css" : "/styles/light.css";
        try {
            String path = getClass().getResource(stylesheet).toExternalForm();
            scene.getStylesheets().add(path);
        } catch (Exception e) {
            System.err.println("Stylesheet konnte nicht geladen werden: " + stylesheet);
            e.printStackTrace();
        }
    }


    private void restartQuiz() {
        score = 0;
        currentIndex = 0;
        scoreLabel.setText("Punkte: 0");
        progressBar.setProgress(0);
        questionViews.clear();
        loadQuestions();
        showCurrentQuestion();
    }

    public void addPoint() {
        score++;
        scoreLabel.setText("Punkte: " + score);
    }

    public Stage getStage() {
        return stage;
    }

    public boolean hasPreviousQuestion() {
        return currentIndex > 0;
    }

    public void previousQuestion() {
        if (currentIndex > 0) {
            currentIndex--;
            showCurrentQuestion();
        }
    }

    public Scene buildScene() {
        // Starte mit der ersten Frage
        showCurrentQuestion();
        return stage.getScene();
    }


    public boolean isExamMode() {
        return examMode;
    }
}
