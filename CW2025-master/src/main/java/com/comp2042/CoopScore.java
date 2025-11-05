package com.comp2042;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

public final class CoopScore {

    private final IntegerProperty score = new SimpleIntegerProperty(0);
    private final IntegerProperty highScore = new SimpleIntegerProperty(0);

    private static final Path COOP_HIGHSCORE_PATH = Paths.get(System.getProperty("user.home"), ".tetris_highscore_coop");
    private static final Logger LOGGER = Logger.getLogger(CoopScore.class.getName());

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
            Path p = COOP_HIGHSCORE_PATH;
            if (Files.exists(p)) {
                String s = Files.readString(p, StandardCharsets.UTF_8).trim();
                try {
                    highScore.set(Integer.parseInt(s));
                } catch (NumberFormatException nfe) {
                    LOGGER.log(Level.WARNING, "Invalid coop highscore content, resetting to 0: " + s, nfe);
                    highScore.set(0);
                }
            } else {
                highScore.set(0);
            }
        } catch (Exception ignored) { highScore.set(0); }
    }

    private void saveHighScore() {
        Path target = COOP_HIGHSCORE_PATH;
        try {
            Path dir = target.getParent();
            if (dir == null) dir = Paths.get(System.getProperty("user.home"));
            // create parent dir if necessary
            try { Files.createDirectories(dir); } catch (Exception e) { /* ignore - may already exist */ }
            Path tmp = Files.createTempFile(dir, ".coop_hs", ".tmp");
            Files.writeString(tmp, Integer.toString(highScore.get()), StandardCharsets.UTF_8);
            try {
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException amnse) {
                // fallback to non-atomic move
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to save coop highscore to " + target + ": " + e.getMessage(), e);
        }
    }
}
