package com.comp2042;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * ClassicBattle implements the same multiplayer wiring and UI as ScoreBattle so it behaves
 * like a separate multiplayer mode. Currently it mirrors ScoreBattleController's behavior
 * exactly to provide a ready-to-use Classic Battle mode.
 */
public class ClassicBattle implements Initializable {

    @FXML
    private StackPane leftHolder;

    @FXML
    private StackPane rightHolder;

    // optional external preview containers (added to classicBattleLayout.fxml)
    @FXML
    private javafx.scene.layout.VBox leftNextBox;
    @FXML
    private javafx.scene.layout.VBox leftNextContent;
    @FXML
    private javafx.scene.text.Text leftNextLabel;

    @FXML
    private javafx.scene.layout.VBox rightNextBox;
    @FXML
    private javafx.scene.layout.VBox rightNextContent;
    @FXML
    private javafx.scene.text.Text rightNextLabel;

    @FXML
    private Button backBtn;

    private GuiController leftGui;
    private GuiController rightGui;
    // keep controller handles so we can query/stop games
    private GameController leftController;
    private GameController rightController;

    // match UI elements
    private javafx.scene.layout.StackPane centerOverlay;
    private javafx.scene.text.Text matchTimerText;
    private javafx.scene.text.Text matchScoreText;
    private javafx.animation.Timeline matchTimer;
    private int remainingSeconds = 5 * 60; // 5 minutes

    // timeline to poll and update per-player next previews
    private javafx.animation.Timeline previewPoller;

    // flag to avoid showing multiple match-end overlays
    private volatile boolean matchEnded = false;
    // reference to the currently shown overlay so we can remove it deterministically
    private javafx.scene.layout.StackPane activeOverlay = null;
    // active pulsing animation for the winner score (so we can stop it when removing overlay)
    private javafx.animation.Animation activePulse = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // mirror ScoreBattleController initialization
        if (backBtn != null) {
            backBtn.setOnAction(this::onBack);
        }
        try {
            java.net.URL fontUrl = getClass().getClassLoader().getResource("digital.ttf");
            if (fontUrl != null) {
                javafx.scene.text.Font.loadFont(fontUrl.toExternalForm(), 38);
            }
        } catch (Exception ignored) {}
        try {
            if (leftNextLabel != null) leftNextLabel.getStyleClass().add("nextBrickLabel");
            if (rightNextLabel != null) rightNextLabel.getStyleClass().add("nextBrickLabel");
            try {
                javafx.scene.text.Font f = javafx.scene.text.Font.font("Let's go Digital", 26);
                if (leftNextLabel != null) {
                    leftNextLabel.setFont(f);
                    leftNextLabel.setFill(javafx.scene.paint.Color.YELLOW);
                    leftNextLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 6, 0.0, 0, 2); -fx-font-weight: bold;");
                }
                if (rightNextLabel != null) {
                    rightNextLabel.setFont(f);
                    rightNextLabel.setFill(javafx.scene.paint.Color.YELLOW);
                    rightNextLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 6, 0.0, 0, 2); -fx-font-weight: bold;");
                }
            } catch (Exception ignored2) {}
        } catch (Exception ignored) {}
    }

    /**
     * Restart the multiplayer match: reset both game models and run a synchronized countdown.
     * Safe to call from any thread (will post to JavaFX Application Thread as needed).
     */
    public void restartMatch() {
        javafx.application.Platform.runLater(() -> {
            try {
                try { if (matchTimer != null) matchTimer.stop(); } catch (Exception ignored) {}
                try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}

                try { if (leftController != null) leftController.createNewGame(); } catch (Exception ignored) {}
                try { if (rightController != null) rightController.createNewGame(); } catch (Exception ignored) {}
                // ensure embedded GUIs are not left in game-over state so input resumes after countdown
                try { if (leftGui != null) leftGui.isGameOverProperty().set(false); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.isGameOverProperty().set(false); } catch (Exception ignored) {}

                try { if (leftGui != null) leftGui.startCountdown(3); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.startCountdown(3); } catch (Exception ignored) {}

                try {
                    remainingSeconds = 5 * 60;
                    if (matchTimerText != null) matchTimerText.setText(formatTime(remainingSeconds));
                    updateMatchScoreText();
                } catch (Exception ignored) {}

                try { if (previewPoller != null) previewPoller.play(); } catch (Exception ignored) {}
                matchEnded = false;
                try {
                    if (activeOverlay != null) {
                        Scene s = leftHolder.getScene();
                        if (s != null && s.getRoot() instanceof javafx.scene.layout.Pane) {
                            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) s.getRoot();
                            root.getChildren().remove(activeOverlay);
                        }
                        activeOverlay = null;
                        try { if (activePulse != null) { activePulse.stop(); activePulse = null; } } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        });
    }

    // Called by Menu to initialize and start both games
    public void initBothGames() throws IOException {
        // delegate to new overload with null swap keys to preserve previous behaviour
        initBothGames(null, null);
    }

    /**
     * Initialize both embedded games and apply per-player swap keys.
     * If a swap key is null the controller will fall back to the previous defaults (Left: Q, Right: C).
     */
    public void initBothGames(javafx.scene.input.KeyCode leftSwap, javafx.scene.input.KeyCode rightSwap) throws IOException {
        URL gameLayout = getClass().getClassLoader().getResource("gameLayout.fxml");
        FXMLLoader leftLoader = new FXMLLoader(gameLayout);
        Parent leftRoot = leftLoader.load();
        leftGui = leftLoader.getController();

        FXMLLoader rightLoader = new FXMLLoader(gameLayout);
        Parent rightRoot = rightLoader.load();
        rightGui = rightLoader.getController();

        try {
            leftRoot.getStylesheets().clear();
            rightRoot.getStylesheets().clear();
            leftRoot.getStyleClass().remove("root");
            rightRoot.getStyleClass().remove("root");
            String transparent = "-fx-background-color: transparent;";
            leftRoot.setStyle(transparent);
            rightRoot.setStyle(transparent);
        } catch (Exception ignored) {}

        try {
            if (backBtn != null) {
                backBtn.setVisible(false);
                backBtn.setManaged(false);
            }
            java.util.function.Consumer<Parent> hideEmbeddedUi = (root) -> {
                try {
                    javafx.scene.Node n = root.lookup("#pauseBtn");
                    if (n != null) { n.setVisible(false); n.setManaged(false); }
                    n = root.lookup("#nextBoxFrame");
                    if (n != null) { n.setVisible(false); n.setManaged(false); }
                    javafx.scene.Node nb = root.lookup("#nextBox");
                    if (nb != null) { nb.setVisible(false); nb.setManaged(false); }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };
            hideEmbeddedUi.accept(leftRoot);
            hideEmbeddedUi.accept(rightRoot);
        } catch (Exception ignored) {}

        double measuredW = -1, measuredH = -1;
        try {
            leftRoot.applyCss();
            leftRoot.layout();
            javafx.scene.Node gb = leftRoot.lookup("#gameBoard");
            if (gb == null) gb = leftRoot.lookup(".gameBoard");
            if (gb != null) {
                measuredW = gb.prefWidth(-1);
                measuredH = gb.prefHeight(-1);
                if (measuredW <= 0) measuredW = gb.getLayoutBounds().getWidth();
                if (measuredH <= 0) measuredH = gb.getLayoutBounds().getHeight();
            }
        } catch (Exception ignored) {}

        double initialW = (measuredW > 0) ? measuredW : 400;
        double initialH = (measuredH > 0) ? measuredH : 640;

        SubScene leftSub = new SubScene(leftRoot, initialW, initialH);
        leftSub.setFill(Color.TRANSPARENT);
        SubScene rightSub = new SubScene(rightRoot, initialW, initialH);
        rightSub.setFill(Color.TRANSPARENT);

        leftHolder.setAlignment(Pos.CENTER);
        rightHolder.setAlignment(Pos.CENTER);

        leftHolder.getChildren().add(leftSub);
        rightHolder.getChildren().add(rightSub);

        if (leftRoot instanceof Pane) {
            Pane lp = (Pane) leftRoot;
            lp.prefWidthProperty().bind(leftSub.widthProperty());
            lp.prefHeightProperty().bind(leftSub.heightProperty());
        }
        if (rightRoot instanceof Pane) {
            Pane rp = (Pane) rightRoot;
            rp.prefWidthProperty().bind(rightSub.widthProperty());
            rp.prefHeightProperty().bind(rightSub.heightProperty());
        }

        leftController = new GameController(leftGui);
        rightController = new GameController(rightGui);

        try {
            leftGui.isGameOverProperty().addListener((obs, oldV, newV) -> {
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
                if (newV == Boolean.TRUE && !matchEnded) {
                    matchEnded = true;
                    try { if (matchTimer != null) matchTimer.stop(); } catch (Exception ignored) {}
                    try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}
                    // Ensure both GUIs are transitioned to game-over so their timelines and input handlers stop
                    try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
                    try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}
                    int lscore = (leftController != null ? leftController.getScoreProperty().get() : 0);
                    int rscore = (rightController != null ? rightController.getScoreProperty().get() : 0);
                    String reason = "Winner by survival (opponent lost)";
                    if (lscore > rscore) reason += String.format(" — opponent had higher score (%d vs %d)", lscore, rscore);
                    showWinnerOverlay("Right Player Wins!", lscore, rscore, reason);
                }
            });
        } catch (Exception ignored) {}
        try {
            rightGui.isGameOverProperty().addListener((obs, oldV, newV) -> {
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
                if (newV == Boolean.TRUE && !matchEnded) {
                    matchEnded = true;
                    try { if (matchTimer != null) matchTimer.stop(); } catch (Exception ignored) {}
                    try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}
                    // Ensure both GUIs are transitioned to game-over so their timelines and input handlers stop
                    try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
                    try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}
                    int lscore = (leftController != null ? leftController.getScoreProperty().get() : 0);
                    int rscore = (rightController != null ? rightController.getScoreProperty().get() : 0);
                    String reason = "Winner by survival (opponent lost)";
                    if (rscore > lscore) reason += String.format(" — opponent had higher score (%d vs %d)", rscore, lscore);
                    showWinnerOverlay("Left Player Wins!", lscore, rscore, reason);
                }
            });
        } catch (Exception ignored) {}

        try { leftGui.setMultiplayerMode(true); } catch (Exception ignored) {}
        try { rightGui.setMultiplayerMode(true); } catch (Exception ignored) {}

    // For Classic Battle we intentionally hide per-player score/time UI so the
    // center match UI is the only visible scoring/time indicator.
    try { leftGui.hideScoreAndTimeUI(); } catch (Exception ignored) {}
    try { rightGui.hideScoreAndTimeUI(); } catch (Exception ignored) {}

        try { leftGui.setMultiplayerRestartHandler(this::restartMatch); } catch (Exception ignored) {}
        try { rightGui.setMultiplayerRestartHandler(this::restartMatch); } catch (Exception ignored) {}

        try {
            leftGui.setMultiplayerPauseHandler(paused -> {
                try {
                    if (rightGui != null) rightGui.applyExternalPause(paused);
                    if (paused) {
                        try { if (matchTimer != null) matchTimer.pause(); } catch (Exception ignored) {}
                        try { if (previewPoller != null) previewPoller.pause(); } catch (Exception ignored) {}
                    } else {
                        try { if (matchTimer != null) matchTimer.play(); } catch (Exception ignored) {}
                        try { if (previewPoller != null) previewPoller.play(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
        try {
            rightGui.setMultiplayerPauseHandler(paused -> {
                try {
                    if (leftGui != null) leftGui.applyExternalPause(paused);
                    if (paused) {
                        try { if (matchTimer != null) matchTimer.pause(); } catch (Exception ignored) {}
                        try { if (previewPoller != null) previewPoller.pause(); } catch (Exception ignored) {}
                    } else {
                        try { if (matchTimer != null) matchTimer.play(); } catch (Exception ignored) {}
                        try { if (previewPoller != null) previewPoller.play(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}

        leftGui.setLevelText("Classic Battle");
        rightGui.setLevelText("Classic Battle");

        leftGui.setDropIntervalMs(1000);
        rightGui.setDropIntervalMs(1000);

        leftGui.startCountdown(3);
        rightGui.startCountdown(3);

        try {
            leftGui.setControlKeys(javafx.scene.input.KeyCode.A, javafx.scene.input.KeyCode.D, javafx.scene.input.KeyCode.W, javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCode.SHIFT);
            leftGui.setSwapKey(leftSwap != null ? leftSwap : javafx.scene.input.KeyCode.Q);
            rightGui.setControlKeys(javafx.scene.input.KeyCode.LEFT, javafx.scene.input.KeyCode.RIGHT, javafx.scene.input.KeyCode.UP, javafx.scene.input.KeyCode.DOWN, javafx.scene.input.KeyCode.SPACE);
            rightGui.setSwapKey(rightSwap != null ? rightSwap : javafx.scene.input.KeyCode.C);
        } catch (Exception ignored) {}

        // Register clear-row handlers so when one player clears rows we transfer
        // garbage rows to the opponent. Garbage rows have a hole on the rightmost column
        // (so opponent can cancel by filling the hole).
        try {
            leftController.setClearRowHandler(lines -> {
                try {
                    int l = (lines == null) ? 0 : lines.intValue();
                    if (l > 0 && rightController != null) {
                        rightController.addGarbageRows(l, -1);
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
        try {
            rightController.setClearRowHandler(lines -> {
                try {
                    int l = (lines == null) ? 0 : lines.intValue();
                    if (l > 0 && leftController != null) {
                        leftController.addGarbageRows(l, -1);
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}

        try {
            javafx.application.Platform.runLater(() -> {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;
                centerOverlay = new javafx.scene.layout.StackPane();
                centerOverlay.setPickOnBounds(false);
                centerOverlay.setMouseTransparent(true);

                javafx.scene.layout.VBox v = new javafx.scene.layout.VBox(6);
                v.setAlignment(javafx.geometry.Pos.TOP_CENTER);

                matchTimerText = new javafx.scene.text.Text(formatTime(remainingSeconds));
                matchTimerText.setStyle("-fx-font-size: 56px; -fx-fill: yellow; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 6, 0.0, 0, 2);");

                matchScoreText = new javafx.scene.text.Text("0  —  0");
                matchScoreText.setStyle("-fx-font-size: 36px; -fx-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 6, 0.0, 0, 2);");

                v.getChildren().addAll(matchTimerText, matchScoreText);
                centerOverlay.getChildren().add(v);

                // For Classic Battle mode we intentionally do NOT add the center overlay
                // (match timer and combined score) to the scene so players only see the
                // boards and next-boxes. The match logic still runs but UI is suppressed.
            });
        } catch (Exception ignored) {}

        matchTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), _e -> {
            java.util.Objects.requireNonNull(_e);
            remainingSeconds--;
            if (matchTimerText != null) matchTimerText.setText(formatTime(remainingSeconds));
            updateMatchScoreText();
            if (remainingSeconds <= 0) {
                matchTimer.stop();
                endMatchAndAnnounceWinner();
            }
        }));
        matchTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);

        try {
            final javafx.beans.property.BooleanProperty leftDone = leftGui.countdownFinishedProperty();
            final javafx.beans.property.BooleanProperty rightDone = rightGui.countdownFinishedProperty();
            javafx.beans.InvalidationListener startWhenReady = _obs -> {
                java.util.Objects.requireNonNull(_obs);
                try {
                    if (leftDone.get() && rightDone.get()) {
                        matchTimer.play();
                    }
                } catch (Exception ignored) {}
            };
            leftDone.addListener(startWhenReady);
            rightDone.addListener(startWhenReady);
            if (leftDone.get() && rightDone.get()) matchTimer.play();
        } catch (Exception ignored) {
            matchTimer.play();
        }

        try {
            previewPoller = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), _ev -> {
                try {
                    java.util.Objects.requireNonNull(_ev);
                    if (leftController != null && leftGui != null) {
                        java.util.List<com.comp2042.logic.bricks.Brick> up = leftController.getUpcomingBricks(3);
                        if (leftNextContent != null) {
                            javafx.scene.layout.VBox built = leftGui.buildNextPreview(up);
                            leftNextContent.getChildren().clear();
                            if (built != null) leftNextContent.getChildren().addAll(built.getChildren());
                        } else {
                            if (up != null) leftGui.showNextBricks(up);
                        }
                    }
                    if (rightController != null && rightGui != null) {
                        java.util.List<com.comp2042.logic.bricks.Brick> up2 = rightController.getUpcomingBricks(3);
                        if (rightNextContent != null) {
                            javafx.scene.layout.VBox built2 = rightGui.buildNextPreview(up2);
                            rightNextContent.getChildren().clear();
                            if (built2 != null) rightNextContent.getChildren().addAll(built2.getChildren());
                        } else {
                            if (up2 != null) rightGui.showNextBricks(up2);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }));
            previewPoller.setCycleCount(javafx.animation.Animation.INDEFINITE);
            previewPoller.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onBack(ActionEvent ev) {
        try {
            URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
            if (loc == null) return;
            FXMLLoader loader = new FXMLLoader(loc);
            Parent menuRoot = loader.load();
            Stage stage = (Stage) backBtn.getScene().getWindow();
            if (stage.getScene() != null) {
                stage.getScene().setRoot(menuRoot);
                try {
                    String css = getClass().getClassLoader().getResource("menu.css").toExternalForm();
                    if (!stage.getScene().getStylesheets().contains(css)) stage.getScene().getStylesheets().add(css);
                } catch (Exception ignored) {}
            } else {
                Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                try {
                    String css = getClass().getClassLoader().getResource("menu.css").toExternalForm();
                    s2.getStylesheets().add(css);
                } catch (Exception ignored) {}
                stage.setScene(s2);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private void updateMatchScoreText() {
        try {
            int ls = (leftController != null) ? leftController.getScoreProperty().get() : 0;
            int rs = (rightController != null) ? rightController.getScoreProperty().get() : 0;
            if (matchScoreText != null) matchScoreText.setText(String.format("%d  —  %d", ls, rs));
        } catch (Exception ignored) {}
    }

    private void endMatchAndAnnounceWinner() {
        try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
        try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}
        try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}

        int ls = (leftController != null) ? leftController.getScoreProperty().get() : 0;
        int rs = (rightController != null) ? rightController.getScoreProperty().get() : 0;
        String title;
        if (ls > rs) title = "Left Player Wins!";
        else if (rs > ls) title = "Right Player Wins!";
        else title = "Draw!";

        final String reason;
        if (ls == rs) {
            reason = "Tie — both players have the same score";
        } else if (ls > rs) {
            reason = String.format("Winner by higher score (+%d) — time expired", ls - rs);
        } else {
            reason = String.format("Winner by higher score (+%d) — time expired", rs - ls);
        }

        javafx.application.Platform.runLater(() -> {
            try {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;
                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");

                showWinnerOverlay(title, ls, rs, reason);
            } catch (Exception ignored) {}
        });
    }

    private void showWinnerOverlay(String title, int ls, int rs, String reason) {
        javafx.application.Platform.runLater(() -> {
            try {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;
                // Classic Battle: survival-focused overlay. Do not show per-player numbers or time here.
                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                overlay.setStyle("-fx-background-color: rgba(0,0,0,0.88);");
                activeOverlay = overlay;

                VBox panel = new VBox(14);
                panel.setAlignment(Pos.CENTER);
                panel.setStyle("-fx-background-color: rgba(18,18,20,0.95); -fx-padding: 26px; -fx-background-radius: 8px;");

        javafx.scene.text.Text bigTitle = new javafx.scene.text.Text(title);
        bigTitle.setStyle("-fx-font-size: 64px; -fx-font-weight: 700; -fx-fill: white;");

        // Add a color/glow + pulse animation to the winner title to make it pop
        try {
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(javafx.scene.paint.Color.web("#ffd166"));
            glow.setRadius(8);
            glow.setSpread(0.45);
            bigTitle.setEffect(glow);

            // gentle scale pulse for the title
            javafx.animation.ScaleTransition scalePulse = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(900), bigTitle);
            scalePulse.setFromX(1.0); scalePulse.setFromY(1.0);
            scalePulse.setToX(1.03); scalePulse.setToY(1.03);
            scalePulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
            scalePulse.setAutoReverse(true);

            // animate glow radius for a soft breathing effect
            javafx.animation.Timeline glowTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(glow.radiusProperty(), 8)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(900), new javafx.animation.KeyValue(glow.radiusProperty(), 28))
            );
            glowTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            glowTimeline.setAutoReverse(true);

            // alternate fill color briefly to accent the winner (white -> gold -> white)
            javafx.animation.Timeline colorPulse = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, ae -> { ae.consume(); bigTitle.setFill(javafx.scene.paint.Color.WHITE); }),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(420), ae -> { ae.consume(); bigTitle.setFill(javafx.scene.paint.Color.web("#ffd166")); }),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(900), ae -> { ae.consume(); bigTitle.setFill(javafx.scene.paint.Color.WHITE); })
            );
            colorPulse.setCycleCount(javafx.animation.Animation.INDEFINITE);

            javafx.animation.ParallelTransition combined = new javafx.animation.ParallelTransition(scalePulse, glowTimeline, colorPulse);
            combined.setCycleCount(javafx.animation.Animation.INDEFINITE);
            // store to activePulse so restart/removal code can stop it
            activePulse = combined;
            combined.play();
        } catch (Exception ignored) {}

                // Short, punchy description for Classic Battle + animated emphasis
                javafx.scene.text.Text modeDesc = new javafx.scene.text.Text("Survive. Outlast. Win.");
                modeDesc.setStyle("-fx-font-size: 18px; -fx-fill: #d0d0d0; -fx-text-alignment: center;");
                modeDesc.setWrappingWidth(680);

                // Entrance fade and gentle pulsing to draw attention without being distracting
                try {
                    javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(550), modeDesc);
                    ft.setFromValue(0.0);
                    ft.setToValue(1.0);
                    ft.setCycleCount(1);
                    ft.play();

                    javafx.animation.ScaleTransition pulse = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(1200), modeDesc);
                    pulse.setFromX(1.0); pulse.setFromY(1.0);
                    pulse.setToX(1.03); pulse.setToY(1.03);
                    pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
                    pulse.setAutoReverse(true);
                    pulse.setDelay(javafx.util.Duration.millis(250));
                    pulse.play();
                } catch (Exception ignored) {}

                javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(12);
                btnRow.setAlignment(Pos.CENTER);
                Button btnRestart = new Button("Rematch");
                Button btnMenu = new Button("Main Menu");
                btnRestart.getStyleClass().add("menu-button");
                btnMenu.getStyleClass().add("menu-button");

                btnRestart.setOnAction(ev -> {
                    ev.consume();
                    try { if (overlay.getParent() instanceof javafx.scene.layout.Pane) ((javafx.scene.layout.Pane) overlay.getParent()).getChildren().remove(overlay); } catch (Exception ignored) {}
                    restartMatch();
                });

                btnMenu.setOnAction(ev -> {
                    ev.consume();
                    try {
                        URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
                        if (loc == null) return;
                        FXMLLoader loader = new FXMLLoader(loc);
                        Parent menuRoot = loader.load();
                        Stage stage = (Stage) scene.getWindow();
                        if (stage.getScene() != null) {
                            stage.getScene().setRoot(menuRoot);
                        } else {
                            Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                            stage.setScene(s2);
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                });

                btnRow.getChildren().addAll(btnRestart, btnMenu);

                panel.getChildren().addAll(bigTitle, modeDesc, btnRow);

                overlay.getChildren().add(panel);
                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    root.getChildren().add(overlay);
                    overlay.prefWidthProperty().bind(scene.widthProperty());
                    overlay.prefHeightProperty().bind(scene.heightProperty());
                    StackPane.setAlignment(panel, Pos.CENTER);
                }

            } catch (Exception ignored) {}
        });
    }
}
