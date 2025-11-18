package com.comp2042.controller.guiControl;

/**
 * High score manager stub. Kept minimal for incremental refactor.
 */
/**
 * Minimal high-score persistence helper used by {@link GuiController}.
 *
 * <p>Persists a single integer high score to a file in the user's home
 * directory and updates the controller's UI when the stored value changes.</p>
 */
class GuiHighScoreManager {
    private final GuiController owner;
    private int highScore = 0;
    private final String HIGHSCORE_FILE = System.getProperty("user.home") + java.io.File.separator + ".tetris_highscore";

    GuiHighScoreManager(GuiController owner) {
        this.owner = owner;
    }

    /** Load the persisted high score from disk and update the UI label. */
    void loadHighScore() {
        java.nio.file.Path p = java.nio.file.Paths.get(HIGHSCORE_FILE);
        if (java.nio.file.Files.exists(p)) {
            try {
                String s = java.nio.file.Files.readString(p, java.nio.charset.StandardCharsets.UTF_8).trim();
                highScore = Integer.parseInt(s);
            } catch (Exception ignored) { highScore = 0; }
        } else {
            highScore = 0;
        }
        try { if (owner.highScoreValue != null) owner.highScoreValue.setText("Highest: " + highScore); } catch (Exception ignored) {}
    }

    /** Persist the current high score to disk (best-effort, errors ignored). */
    void saveHighScore() {
        java.nio.file.Path p = java.nio.file.Paths.get(HIGHSCORE_FILE);
        try { java.nio.file.Files.writeString(p, Integer.toString(highScore), java.nio.charset.StandardCharsets.UTF_8); } catch (java.io.IOException ignored) {}
    }

    /** Return the currently loaded high score. */
    int getHighScore() { return highScore; }

    /**
     * Notify the manager of a new score; if it exceeds the stored high score
     * the value is updated, persisted and the UI label briefly animated.
     */
    void onNewScore(int current) {
        if (current > highScore) {
            highScore = current;
            saveHighScore();
            try {
                if (owner.highScoreValue != null) {
                    owner.highScoreValue.setText("Highest: " + highScore);
                    try {
                        javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200), owner.highScoreValue);
                        st.setFromX(1.0);
                        st.setFromY(1.0);
                        st.setToX(1.25);
                        st.setToY(1.25);
                        st.setCycleCount(2);
                        st.setAutoReverse(true);
                        st.play();
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
    }

    // future high-score related helpers can be added here
}
