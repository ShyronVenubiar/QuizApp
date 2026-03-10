package de.felix.quiz.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.felix.quiz.model.QuizData;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

public class QuizLoader {

    /**
     * Lädt ein Quiz aus:
     * - file: URI (z.B. file:/C:/.../quiz.json)
     * - Dateisystem-Pfad (z.B. C:\... oder ./app/quizzes/...)
     * - Klassenpfad-Ressource (z.B. /quizzes/quiz.json)
     *
     * Liefert bei Fehlern oder wenn nichts gefunden wurde eine leere Liste.
     */
    public static List<QuizData> load(String resourcePath) {
        System.out.println("Lade Quiz-Datei: " + resourcePath);
        ObjectMapper mapper = new ObjectMapper();

        if (resourcePath == null || resourcePath.isBlank()) {
            System.err.println("QuizLoader: leerer Pfad übergeben.");
            return Collections.emptyList();
        }

        // 1) file: URI
        try {
            if (resourcePath.startsWith("file:")) {
                Path p = Paths.get(URI.create(resourcePath));
                if (Files.exists(p)) {
                    String json = Files.readString(p, StandardCharsets.UTF_8);
                    return mapper.readValue(json, new TypeReference<List<QuizData>>() {});
                } else {
                    System.err.println("QuizLoader: file: URI existiert nicht: " + p);
                }
            }
        } catch (Exception e) {
            System.err.println("QuizLoader: Fehler beim Lesen der file: URI: " + e.getMessage());
        }

        // 2) Dateisystem-Pfad
        try {
            Path p = Paths.get(resourcePath);
            if (Files.exists(p)) {
                String json = Files.readString(p, StandardCharsets.UTF_8);
                return mapper.readValue(json, new TypeReference<List<QuizData>>() {});
            }
        } catch (Exception e) {
            // nicht fatal, weiter zu Klassenpfad
            System.err.println("QuizLoader: Fehler beim Lesen als Dateipfad: " + e.getMessage());
        }

        // 3) Klassenpfad-Ressource (normalize leading slash)
        String normalized = resourcePath.replaceFirst("^/", "");
        try (InputStream in = QuizLoader.class.getResourceAsStream("/" + normalized)) {
            if (in != null) {
                return mapper.readValue(in, new TypeReference<List<QuizData>>() {});
            } else {
                System.err.println("QuizLoader: Klassenpfad-Ressource nicht gefunden: /" + normalized);
            }
        } catch (IOException e) {
            System.err.println("QuizLoader: Fehler beim Lesen der Klassenpfad-Ressource: " + e.getMessage());
        }

        // nichts gefunden
        System.err.println("QuizLoader: Konnte Quiz nicht laden: " + resourcePath);
        return Collections.emptyList();
    }
}
