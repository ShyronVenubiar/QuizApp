package de.felix.quiz.controller;

import javafx.scene.control.Alert;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AppFiles {
    private AppFiles() {}
    private static final Logger LOG = Logger.getLogger(AppFiles.class.getName());

    public static Path getAppDataDir() {
        String os = System.getProperty("os.name").toLowerCase();
        Path base;

        if (os.contains("win")) {
            String appData = System.getenv("APPDATA");           // roaming
            String localAppData = System.getenv("LOCALAPPDATA"); // local
            base = (localAppData != null && !localAppData.isBlank()) ? Paths.get(localAppData)
                    : (appData != null && !appData.isBlank()) ? Paths.get(appData)
                    : Paths.get(System.getProperty("user.home"), "AppData", "Roaming");
        } else if (os.contains("mac")) {
            base = Paths.get(System.getProperty("user.home"), "Library", "Application Support");
        } else {
            String xdg = System.getenv("XDG_DATA_HOME");
            base = (xdg != null && !xdg.isBlank()) ? Paths.get(xdg) : Paths.get(System.getProperty("user.home"), ".local", "share");
        }

        Path appDir = base.resolve("QuizApp");
        try {
            Files.createDirectories(appDir);
            migrateLegacyDotFolder(appDir); // optional: verschiebe ~/.quizapp falls vorhanden
        } catch (IOException e) {
            e.printStackTrace();
        }
        return appDir;
    }

    private static void migrateLegacyDotFolder(Path targetAppDir) {
        try {
            Path legacy = Paths.get(System.getProperty("user.home"), ".quizapp");
            if (Files.exists(legacy) && Files.isDirectory(legacy)) {
                // nur migrieren, wenn target leer oder nicht existiert
                boolean targetEmpty = !Files.exists(targetAppDir) || Files.list(targetAppDir).findAny().isEmpty();
                if (targetEmpty) {
                    Files.createDirectories(targetAppDir);
                    try (DirectoryStream<Path> ds = Files.newDirectoryStream(legacy)) {
                        for (Path p : ds) {
                            Path dest = targetAppDir.resolve(p.getFileName());
                            if (Files.isDirectory(p)) {
                                copyRecursive(p, dest);
                            } else {
                                Files.copy(p, dest, StandardCopyOption.REPLACE_EXISTING);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Migration darf nicht brechen; nur loggen
            System.err.println("Migration legacy .quizapp fehlgeschlagen: " + e.getMessage());
        }
    }

    private static void copyRecursive(Path src, Path dst) throws IOException {
        Files.walk(src).forEach(s -> {
            try {
                Path rel = src.relativize(s);
                Path target = dst.resolve(rel);
                if (Files.isDirectory(s)) {
                    Files.createDirectories(target);
                } else {
                    Files.copy(s, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
    }

    public static Path getQuizzesDir() {
        Path d = getAppDataDir().resolve("quizzes");
        try { Files.createDirectories(d); } catch (IOException ignored) {}
        return d;
    }

    public static Path getImagesDir() {
        Path d = getAppDataDir().resolve("images");
        try { Files.createDirectories(d); } catch (IOException ignored) {}
        return d;
    }

    /**
     * Kopiert beim ersten Start packaged quizzes (und optional images) in den AppData-Ordner,
     * wenn dort noch keine Dateien existieren. Erwartet eine index.txt unter /quizzes/index.txt
     * mit einer Zeile pro Dateiname (z.B. myquiz.json).
     *
     * @param callerClass Klasse zum Aufrufen von getResourceAsStream
     */
    public static void ensurePackagedQuizzesCopied(Class<?> callerClass) {
        Path appQuizzes = getQuizzesDir();
        try {
            boolean empty = Files.list(appQuizzes).findAny().isEmpty();
            if (!empty) return; // bereits Inhalte vorhanden -> nichts tun
        } catch (IOException ignored) {}

        // Versuche index.txt zu lesen
        try (InputStream idx = callerClass.getResourceAsStream("/quizzes/index.txt")) {
            if (idx == null) return; // kein Index vorhanden
            try (BufferedReader br = new BufferedReader(new InputStreamReader(idx))) {
                String name;
                while ((name = br.readLine()) != null) {
                    name = name.trim();
                    if (name.isEmpty()) continue;
                    // copy quiz json
                    try (InputStream in = callerClass.getResourceAsStream("/quizzes/" + name)) {
                        if (in != null) {
                            Path target = appQuizzes.resolve(name);
                            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        new Alert(Alert.AlertType.INFORMATION, "AppData wurde erstellt.").show();

        // Optional: copy packaged images for each quiz folder if you have an index for images.
        // For simplicity: do not attempt to enumerate images here unless you provide an index.
    }

    /**
     * Versucht, eine Ressource vom Klassenpfad als InputStream zu öffnen.
     * @param callerClass Klasse zum Aufrufen von getResourceAsStream
     * @param resourcePath klassenpfad-relativer Pfad mit oder ohne führendem '/'
     * @return InputStream oder null
     */
    public static InputStream openPackagedResource(Class<?> callerClass, String resourcePath) {
        String normalized = resourcePath.replaceFirst("^/", "");
        return callerClass.getResourceAsStream("/" + normalized);
    }

    /**
     * Hilfsfunktion: prüft, ob ein Bild im AppData images-Ordner existiert und liefert Path.
     */
    public static Path resolveAppImagePath(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return getImagesDir().resolve("missing.png");
        // Entferne führenden Slash
        String normalized = imagePath.replaceFirst("^/", "");
        // Falls der gespeicherte Pfad bereits mit "images/" beginnt, entferne dieses Segment
        if (normalized.startsWith("images/")) {
            normalized = normalized.substring("images/".length());
        }
        // Jetzt liegt normalized z.B. vor: "<quizname>/file.png"
        return getImagesDir().resolve(normalized);
    }




    /**
     * Kopiert eine Datei in den AppData images/<quizName> Ordner und gibt den Zielpfad zurück.
     */
    public static Path copyImageToAppData(Path source, String quizName) throws IOException {
        Path targetDir = getImagesDir().resolve(quizName);
        Files.createDirectories(targetDir);
        Path targetFile = targetDir.resolve(source.getFileName().toString());
        Files.copy(source, targetFile, StandardCopyOption.REPLACE_EXISTING);
        return targetFile;
    }


    /**
     * Löscht die Quiz-Datei (quizzes/<quizName>.json) und den images/<quizName> Ordner rekursiv.
     * Gibt true zurück, wenn mindestens eine Ressource gelöscht wurde, false wenn nichts gefunden wurde.
     */
    public static boolean deleteQuizData(String quizName) {
        boolean deletedSomething = false;
        try {
            // 1) Quiz-Datei löschen
            Path quizFile = getQuizzesDir().resolve(quizName + ".json");
            if (Files.exists(quizFile)) {
                Files.deleteIfExists(quizFile);
                deletedSomething = true;
                LOG.info("Gelöscht: " + quizFile);
            }

            // 2) images/<quizName> Ordner löschen (rekursiv)
            Path imagesFolder = getImagesDir().resolve(quizName);
            if (Files.exists(imagesFolder)) {
                Files.walk(imagesFolder)
                        .sorted(Comparator.reverseOrder())
                        .forEach(p -> {
                            try {
                                Files.deleteIfExists(p);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        });
                deletedSomething = true;
                LOG.info("Gelöscht: " + imagesFolder);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Fehler beim Löschen der Quiz-Daten für: " + quizName, ex);
            return false;
        }
        return deletedSomething;
    }
}
