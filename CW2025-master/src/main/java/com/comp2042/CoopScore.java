package com.comp2042;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Cooperative-mode scoring helper. Tracks current score and a persisted high score
 * specifically for cooperative battle mode (separate from single-player high score).
 */
public final class CoopScore {

    private final IntegerProperty score = new SimpleIntegerProperty(0);
    private final IntegerProperty highScore = new SimpleIntegerProperty(0);

    private static final String COOP_HIGHSCORE_FILE = System.getProperty("user.home") + java.io.File.separator + ".tetris_highscore_coop";

    public CoopScore() {
        loadHighScore();
    }

    public IntegerProperty scoreProperty() { return score; }
    public IntegerProperty highScoreProperty() { return highScore; }

    public int getScore() { return score.get(); }
    public int getHighScore() { return highScore.get(); }

    public void add(int i) {
        if (i == 0) return;
        score.set(score.get() + i);
        // update persisted high score if exceeded
        if (score.get() > highScore.get()) {
            highScore.set(score.get());
            saveHighScore();
        }
    }

    public void reset() {
        score.set(0);
    }

    private void loadHighScore() {
        try {
            Path p = Paths.get(COOP_HIGHSCORE_FILE);
            if (Files.exists(p)) {
                String s = Files.readString(p, StandardCharsets.UTF_8).trim();
                try { highScore.set(Integer.parseInt(s)); } catch (Exception ignored) { highScore.set(0); }
            } else {
                highScore.set(0);
            }
        } catch (Exception ignored) { highScore.set(0); }
    }

    private void saveHighScore() {
        try {
            Path p = Paths.get(COOP_HIGHSCORE_FILE);
            Files.writeString(p, Integer.toString(highScore.get()), StandardCharsets.UTF_8);
        } catch (Exception ignored) {}
    }
}
