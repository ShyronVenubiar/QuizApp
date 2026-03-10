package de.felix.quiz.view;

import de.felix.quiz.controller.AppFiles;
import de.felix.quiz.controller.QuizController;
import de.felix.quiz.model.QuizData;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextBoundsType;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuizDesign {
    public static VBox createBaseLayout(QuizController controller, QuizData data, Node body, Button checkButton, Label feedbackLabel) {
        VBox root = new VBox(15);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.TOP_LEFT);

        // Intro
        if (data.intro != null && !data.intro.isEmpty())
            root.getChildren().add(makeIntro(data.intro));

        // Picture
        if (data.questionPicture != null && !data.questionPicture.isEmpty())
            root.getChildren().add(makeImageBox(data.questionPicture, controller.getStage()));

        // Question
        if (data.question != null && !data.question.isEmpty() && !"FILL_IN_THE_BLANK".equals(data.type)) {
            Text questionText = new Text(data.question);
            questionText.getStyleClass().add("question-label");
            questionText.setBoundsType(TextBoundsType.VISUAL);

            TextFlow flow = new TextFlow(questionText);
            flow.setMaxWidth(Double.MAX_VALUE);

            flow.parentProperty().addListener((obs, olsParent, newParent) -> {
                if (newParent instanceof Region region) {
                    flow.prefWidthProperty().bind(region.widthProperty());
                }
            });
            VBox.setVgrow(flow, Priority.NEVER);
            root.getChildren().add(flow);
        }

        // Outro
        if (data.outro != null && !data.outro.isEmpty())
            root.getChildren().add(makeOutro(data.outro));

        // Body
        if (body != null) {
            root.getChildren().add(body);
        }

        // Check + Feedback
        if (checkButton != null && feedbackLabel != null) {
            HBox buttonBox = new HBox(10, checkButton, feedbackLabel);
            buttonBox.setAlignment(Pos.CENTER_LEFT);
            buttonBox.setPadding(new Insets(20, 0, 0, 0));
            root.getChildren().add(buttonBox);
        }

        // Font-Size Listener für dynamische Skalierung
        controller.getStage().heightProperty().addListener((obs, oldVal, newVal) -> {
            double baseSize = Math.max(16, newVal.doubleValue() / 40.0);
            root.setStyle("-fx-font-size: " + (baseSize + 2) + "px;");
        });

        //Scrollpane
        ScrollPane scrollPane = new ScrollPane(root);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox scrollRoot = new VBox(scrollPane);

        return scrollRoot;
    }


    private static Node makeIntro(String text) {
        Text introText = new Text(text);
        introText.setBoundsType(TextBoundsType.VISUAL);
        introText.getStyleClass().add("intro-label");

        TextFlow flow = new TextFlow(introText);
        flow.setMaxWidth(Double.MAX_VALUE);

        flow.parentProperty().addListener((obs, olsParent, newParent) -> {
            if (newParent instanceof Region region) {
                flow.prefWidthProperty().bind(region.widthProperty());
            }
        });

        return flow;
    }

    public static Node makeImageBox(String imagePath, Stage stage) {
        System.out.println("Requested imagePath: " + imagePath);
        Image image = loadImageSafely(imagePath);
        if (image == null) {
            Label errorLabel = new Label("⚠️ Bild nicht gefunden: " + imagePath);
            errorLabel.setStyle("-fx-text-fill: red;");
            return errorLabel;
        }

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.fitHeightProperty().bind(stage.heightProperty().multiply(0.15));
        imageView.fitWidthProperty().bind(stage.widthProperty().multiply(0.9));

        Image magnifier = loadImageSafely("/images/magnifying_glass.png");
        ImageView magnifierIcon = magnifier != null ? new ImageView(magnifier) : new ImageView();
        magnifierIcon.setFitWidth(12);
        magnifierIcon.setFitHeight(12);

        Button zoomButton = new Button("", magnifierIcon);
        zoomButton.setStyle("-fx-background-color: transparent;");
        zoomButton.setOnAction(e -> showZoomedImage(imagePath));

        HBox imageBox = new HBox(10, imageView, zoomButton);
        imageBox.setAlignment(Pos.CENTER_LEFT);
        return imageBox;
    }

    private static Node makeOutro(String text) {
        Text outroText = new Text(text);
        outroText.getStyleClass().add("outro-label");
        outroText.setBoundsType(TextBoundsType.VISUAL);

        TextFlow flow = new TextFlow(outroText);
        flow.setMaxWidth(Double.MAX_VALUE);

        flow.parentProperty().addListener((obs, olsParent, newParent) -> {
            if (newParent instanceof Region region) {
                flow.prefWidthProperty().bind(region.widthProperty());
            }
        });

        return flow;
    }




    /**
     * Lädt ein Image synchron und robust:
     * 1) AppData images (plattformkonformer Ordner)
     * 2) Klassenpfad-Ressource (liest Bytes und baut Image aus ByteArray)
     * 3) Absoluter Dateisystempfad
     */
    private static Image loadImageSafely(String imagePath) {
        try {
            String normalized = imagePath == null ? "" : imagePath.replaceFirst("^/", "");
            // 1) AppData file (korrekt aufgelöst)
            Path appImage = AppFiles.resolveAppImagePath(imagePath);
            System.out.println("Resolved app image path: " + appImage.toAbsolutePath());
            if (Files.exists(appImage)) {
                return new Image(appImage.toUri().toString(), false);
            }

            // 2) Klassenpfad resource: lade Bytes und erstelle Image aus ByteArrayInputStream
            try (InputStream in = AppFiles.openPackagedResource(QuizDesign.class, "/" + normalized)) {
                if (in != null) {
                    byte[] bytes = in.readAllBytes();
                    return new Image(new ByteArrayInputStream(bytes));
                } else {
                    System.out.println("Packaged resource not found: /" + normalized);
                }
            } catch (IOException e) {
                System.err.println("Fehler beim Lesen der Klassenpfad-Ressource: " + e.getMessage());
            }

            // 3) absolute filesystem fallback
            Path p = Path.of(imagePath);
            if (Files.exists(p)) {
                return new Image(p.toUri().toString(), false);
            }
        } catch (Exception ex) {
            System.err.println("loadImageSafely Fehler: " + ex.getMessage());
        }
        return null;
    }

    public static void showZoomedImage(String imagePath) {
        Stage zoomStage = new Stage();
        zoomStage.setTitle("Bildansicht");

        Image image = loadImageSafely(imagePath);
        if (image == null) {
            Label error = new Label("Bild nicht gefunden: " + imagePath);
            Scene scene = new Scene(new VBox(error), 400, 200);
            zoomStage.setScene(scene);
            zoomStage.show();
            return;
        }

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setFitWidth(image.getWidth());

        ScrollPane scrollPane = new ScrollPane(imageView);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        Scene scene = new Scene(scrollPane, 850, 600);
        zoomStage.setScene(scene);
        zoomStage.show();
    }
}
