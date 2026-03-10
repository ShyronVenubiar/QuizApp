// QuizData.java
package de.felix.quiz.model;

import java.util.List;

public class QuizData {
    public String type;
    public String question;

    public static class Option {
        public String text;
        public boolean correct;

        public Option(){};
        public Option(String text, boolean correct) {
            this.text = text;
            this.correct = correct;
        }
    }
    public List<Option> options;

    public static class ImageOption {
        public String path;
        public String label;
        public boolean correct;

        public ImageOption() {}
        public ImageOption(String path, String label, boolean correct) {
            this.path = path;
            this.label = label;
            this.correct = correct;
        }
    }

    public List<String> orderOptions;
    public List<String> correctOrder;
    public List<ImageOption> images;
    public List<String> correctAnswers;
    public String intro;
    public String questionPicture;
    public String outro;
}
