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

public class ScoreBattleController implements Initializable {

    @FXML
    private StackPane leftHolder;

    @FXML
    private StackPane rightHolder;

    // optional external preview containers (added to scoreBattleLayout.fxml)
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
        // nothing here; will load children when scene is shown
        if (backBtn != null) {
            backBtn.setOnAction(this::onBack);
        }
        // Load the digital font used by single-player so the Next: label matches exactly
        try {
            java.net.URL fontUrl = getClass().getClassLoader().getResource("digital.ttf");
            if (fontUrl != null) {
                javafx.scene.text.Font.loadFont(fontUrl.toExternalForm(), 38);
            }
        } catch (Exception ignored) {}
        // ensure the external labels use the same CSS class as single-player
        try {
            if (leftNextLabel != null) leftNextLabel.getStyleClass().add("nextBrickLabel");
            if (rightNextLabel != null) rightNextLabel.getStyleClass().add("nextBrickLabel");
            // Also apply inline styling to guarantee parity even if stylesheet wasn't applied
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
                // stop match timer and preview poller while restarting
                try { if (matchTimer != null) matchTimer.stop(); } catch (Exception ignored) {}
                try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}

                // reinitialize both game models via controllers
                try { if (leftController != null) leftController.createNewGame(); } catch (Exception ignored) {}
                try { if (rightController != null) rightController.createNewGame(); } catch (Exception ignored) {}
                // ensure embedded GUIs are not left in game-over state so input resumes after countdown
                try { if (leftGui != null) leftGui.isGameOverProperty().set(false); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.isGameOverProperty().set(false); } catch (Exception ignored) {}

                // run synchronized countdowns for both GUIs; the listeners set in initBothGames will restart the match timer
                try { if (leftGui != null) leftGui.startCountdown(3); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.startCountdown(3); } catch (Exception ignored) {}

                // reset match timer value and UI so it shows the full match length at restart
                try {
                    remainingSeconds = 5 * 60; // reset to configured match length
                    if (matchTimerText != null) matchTimerText.setText(formatTime(remainingSeconds));
                    updateMatchScoreText();
                } catch (Exception ignored) {}

                // restart the preview poller
                try { if (previewPoller != null) previewPoller.play(); } catch (Exception ignored) {}
                // clear match-ended flag and remove any existing overlay we created earlier
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
        // load left game layout
        URL gameLayout = getClass().getClassLoader().getResource("gameLayout.fxml");
        FXMLLoader leftLoader = new FXMLLoader(gameLayout);
        Parent leftRoot = leftLoader.load();
        leftGui = leftLoader.getController();

        // load right game layout
        FXMLLoader rightLoader = new FXMLLoader(gameLayout);
        Parent rightRoot = rightLoader.load();
        rightGui = rightLoader.getController();

        // Ensure embedded roots do NOT apply the global '.root' background or load their own stylesheets
        try {
            // remove any stylesheet that would apply the scenic background per-root
            leftRoot.getStylesheets().clear();
            rightRoot.getStylesheets().clear();
            leftRoot.getStyleClass().remove("root");
            rightRoot.getStyleClass().remove("root");
            // force transparent background so the scene root's scenic image is visible once behind both boards
            String transparent = "-fx-background-color: transparent;";
            leftRoot.setStyle(transparent);
            rightRoot.setStyle(transparent);
        } catch (Exception ignored) {}

        // Hide UI chrome that shouldn't appear in multiplayer: Back button (scene-level) and
        // per-board Pause / Next-box elements that create thin vertical separators.
        try {
            if (backBtn != null) {
                backBtn.setVisible(false);
                backBtn.setManaged(false);
            }

            // helper to hide several nodes by id inside an embedded root
            java.util.function.Consumer<Parent> hideEmbeddedUi = (root) -> {
                // hide pause button, decorative frame and embedded next box if present
                try {
                    javafx.scene.Node n = root.lookup("#pauseBtn");
                    if (n != null) { n.setVisible(false); n.setManaged(false); }
                    n = root.lookup("#nextBoxFrame");
                    if (n != null) { n.setVisible(false); n.setManaged(false); }
                    javafx.scene.Node nb = root.lookup("#nextBox");
                    if (nb != null) { nb.setVisible(false); nb.setManaged(false); }
                } catch (Exception e) {
                    // log at least to stderr during maintenance to surface unexpected UI lookup issues
                    e.printStackTrace();
                }
            };

            hideEmbeddedUi.accept(leftRoot);
            hideEmbeddedUi.accept(rightRoot);
        } catch (Exception ignored) {}

        // attempt to measure the internal gameBoard node so we can size the SubScene to match single-player
        double measuredW = -1, measuredH = -1;
        try {
            leftRoot.applyCss();
            leftRoot.layout();
            // try lookup by fx:id first, then by style class
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

        // create SubScenes sized to the measured single-player board area and center them in the holders
        // Do NOT apply the scenic background to each embedded root. Instead, the Score Battle scene's root
        // should hold the single background image so it appears once behind both playfields. We ensure the
        // embedded roots remain transparent and have no stylesheets so they don't draw or re-apply backgrounds.

        SubScene leftSub = new SubScene(leftRoot, initialW, initialH);
        leftSub.setFill(Color.TRANSPARENT);
        SubScene rightSub = new SubScene(rightRoot, initialW, initialH);
        rightSub.setFill(Color.TRANSPARENT);

        // center subscenes in their holders
        leftHolder.setAlignment(Pos.CENTER);
        rightHolder.setAlignment(Pos.CENTER);

        // add to holders
        leftHolder.getChildren().add(leftSub);
        rightHolder.getChildren().add(rightSub);

        // ensure the loaded root Pane matches the subscene size so GuiController's layout measurements work
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

    // start controllers and keep references
    leftController = new GameController(leftGui);
    rightController = new GameController(rightGui);

        // Listen for individual player gameOver events so we can end match early and announce winner
        try {
            leftGui.isGameOverProperty().addListener((obs, oldV, newV) -> {
                // reference unused params to satisfy static analyzers
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
                if (newV == Boolean.TRUE && !matchEnded) {
                    matchEnded = true;
                    // left lost -> right wins
                    try { if (matchTimer != null) matchTimer.stop(); } catch (Exception ignored) {}
                    try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}
                    // Ensure both GUIs are transitioned to game-over so their timelines and input handlers stop
                    try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
                    try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}
                    int lscore = (leftController != null ? leftController.getScoreProperty().get() : 0);
                    int rscore = (rightController != null ? rightController.getScoreProperty().get() : 0);
                    String reason = "Winner by survival (opponent lost)";
                    // if the loser had a higher score, make that explicit
                    if (lscore > rscore) reason += String.format(" — opponent had higher score (%d vs %d)", lscore, rscore);
                    showWinnerOverlay("Right Player Wins!", lscore, rscore, reason);
                }
            });
        } catch (Exception ignored) {}
        try {
            rightGui.isGameOverProperty().addListener((obs, oldV, newV) -> {
                // reference unused params to satisfy static analyzers
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
                if (newV == Boolean.TRUE && !matchEnded) {
                    matchEnded = true;
                    // right lost -> left wins
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

            // mark embedded GUIs as multiplayer so they can adjust visuals/logic
            try { leftGui.setMultiplayerMode(true); } catch (Exception ignored) {}
            try { rightGui.setMultiplayerMode(true); } catch (Exception ignored) {}

            // register a restart handler so an embedded GuiController's Retry can request a full match restart
            try { leftGui.setMultiplayerRestartHandler(this::restartMatch); } catch (Exception ignored) {}
            try { rightGui.setMultiplayerRestartHandler(this::restartMatch); } catch (Exception ignored) {}

            // Register pause handlers so pausing one player pauses the other. Use applyExternalPause
            // to avoid reentrant notifications.
            try {
                leftGui.setMultiplayerPauseHandler(paused -> {
                    try {
                        // Forward pause to the other GUI so both show the overlay
                        if (rightGui != null) rightGui.applyExternalPause(paused);
                        // Pause/resume match-level timers so the match time and preview updates stop
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

        // set level text or drop intervals if desired
        leftGui.setLevelText("Score Battle");
        rightGui.setLevelText("Score Battle");

        leftGui.setDropIntervalMs(1000);
        rightGui.setDropIntervalMs(1000);

        // start countdowns for both
        leftGui.startCountdown(3);
        rightGui.startCountdown(3);

        // configure per-player controls so each board only responds to its assigned keys
        try {
            // left player: W=rotate, A=left, S=soft-drop, D=right, SHIFT=hard-drop
            leftGui.setControlKeys(javafx.scene.input.KeyCode.A, javafx.scene.input.KeyCode.D, javafx.scene.input.KeyCode.W, javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCode.SHIFT);
            // left player swap key = Q
            leftGui.setSwapKey(javafx.scene.input.KeyCode.Q);
            // right player: Left=left, Right=right, Up=rotate, Down=soft-drop, Space=hard-drop
            rightGui.setControlKeys(javafx.scene.input.KeyCode.LEFT, javafx.scene.input.KeyCode.RIGHT, javafx.scene.input.KeyCode.UP, javafx.scene.input.KeyCode.DOWN, javafx.scene.input.KeyCode.SPACE);
            // right player swap key = C
            rightGui.setSwapKey(javafx.scene.input.KeyCode.C);
        } catch (Exception ignored) {}

        // create a centered overlay showing remaining time and combined scores
        try {
            javafx.application.Platform.runLater(() -> {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;
                centerOverlay = new javafx.scene.layout.StackPane();
                centerOverlay.setPickOnBounds(false);
                // overlay should not block gameplay input while match is running
                centerOverlay.setMouseTransparent(true);

                javafx.scene.layout.VBox v = new javafx.scene.layout.VBox(6); // tighter vertical spacing for top layout
                v.setAlignment(javafx.geometry.Pos.TOP_CENTER);

                matchTimerText = new javafx.scene.text.Text(formatTime(remainingSeconds));
                // slightly smaller font to avoid overlap on typical resolutions
                matchTimerText.setStyle("-fx-font-size: 56px; -fx-fill: yellow; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 6, 0.0, 0, 2);");

                matchScoreText = new javafx.scene.text.Text("0  —  0");
                matchScoreText.setStyle("-fx-font-size: 36px; -fx-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 6, 0.0, 0, 2);");

                v.getChildren().addAll(matchTimerText, matchScoreText);
                centerOverlay.getChildren().add(v);

                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    root.getChildren().add(centerOverlay);
                    // make overlay fill the scene width so StackPane alignment centers children correctly
                    centerOverlay.prefWidthProperty().bind(scene.widthProperty());
                    centerOverlay.prefHeightProperty().bind(scene.heightProperty());
                    // position at top-center with a small top margin (6% down the window)
                    StackPane.setAlignment(v, javafx.geometry.Pos.TOP_CENTER);
                    v.translateYProperty().bind(scene.heightProperty().multiply(0.06));
                }
            });
        } catch (Exception ignored) {}

        // start match timer which updates every second (but only after both countdowns complete)
        matchTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), _e -> {
            // reference the event to avoid unused-parameter warnings in some compilers
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

        // Only play the match timer once both embedded UIs have finished their start countdowns
        try {
            final javafx.beans.property.BooleanProperty leftDone = leftGui.countdownFinishedProperty();
            final javafx.beans.property.BooleanProperty rightDone = rightGui.countdownFinishedProperty();
            javafx.beans.InvalidationListener startWhenReady = _obs -> {
                // reference parameter so linters/compiler don't complain about unused lambda args
                java.util.Objects.requireNonNull(_obs);
                try {
                    if (leftDone.get() && rightDone.get()) {
                        matchTimer.play();
                    }
                } catch (Exception ignored) {}
            };
            leftDone.addListener(startWhenReady);
            rightDone.addListener(startWhenReady);
            // also check immediately in case both already finished
            if (leftDone.get() && rightDone.get()) matchTimer.play();
        } catch (Exception ignored) {
            // fallback: start immediately
            matchTimer.play();
        }

        // start a small poller to refresh each player's next-three previews from their GameController
        try {
            // Timeline handlers already run on the JavaFX Application Thread, so we can safely
            // update UI elements directly from the handler. Avoid extra Platform.runLater calls
            // which can cause unnecessary queuing under load.
            previewPoller = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), _ev -> {
                try {
                    // reference the event to satisfy compilers that flag unused lambda parameters
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
                    // log unexpected exceptions during preview update to aid debugging
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
        // return to main menu by replacing the current scene root with mainMenu.fxml
        try {
            URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
            if (loc == null) return;
            FXMLLoader loader = new FXMLLoader(loc);
            Parent menuRoot = loader.load();
            Stage stage = (Stage) backBtn.getScene().getWindow();
            if (stage.getScene() != null) {
                stage.getScene().setRoot(menuRoot);
                // restore shared menu stylesheet (if available) so buttons keep their look
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
        // stop both games
        try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
        try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}
        try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}

        // determine winner
        int ls = (leftController != null) ? leftController.getScoreProperty().get() : 0;
        int rs = (rightController != null) ? rightController.getScoreProperty().get() : 0;
        String title;
        if (ls > rs) title = "Left Player Wins!";
        else if (rs > ls) title = "Right Player Wins!";
        else title = "Draw!";

        // create an impressive animated announcement overlay
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

                // reuse the centralized winner overlay (with reason)
                showWinnerOverlay(title, ls, rs, reason);

                // animations handled by the centralized overlay (if desired)
            } catch (Exception ignored) {}
        });
    }

    /**
     * Show a winner overlay that indicates which player won and the scores. Provides Restart and Main Menu buttons.
     */
    private void showWinnerOverlay(String title, int ls, int rs, String reason) {
        // Use the same centered Match Over layout used elsewhere but with a fully black background
        javafx.application.Platform.runLater(() -> {
            try {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;

                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                // solid black background
                overlay.setStyle("-fx-background-color: rgba(0,0,0,1.0);");
                // remember overlay so we can remove it deterministically
                activeOverlay = overlay;

                VBox dialog = new VBox(12);
                dialog.setAlignment(Pos.CENTER);

                javafx.scene.text.Text titleText = new javafx.scene.text.Text(title);
                titleText.setStyle("-fx-font-size: 72px; -fx-fill: linear-gradient(from 0% 0% to 100% 0%, #ffd166 0%, #ff7b7b 100%); -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 12, 0.0, 0, 4);");

                javafx.scene.text.Text scoreText = new javafx.scene.text.Text(String.format("%d : %d", ls, rs));
                scoreText.setStyle("-fx-font-size: 48px; -fx-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 8, 0.0, 0, 3);");

                // create centered Match Over panel matching single-player design
                VBox centerBox = new VBox(10);
                centerBox.setAlignment(Pos.CENTER);
                centerBox.setStyle("-fx-background-color: rgba(20,20,20,0.85); -fx-padding: 28px; -fx-background-radius: 6px;");

                // show the winner title (e.g. "Left Player Wins!")
                javafx.scene.text.Text matchTitle = new javafx.scene.text.Text(title);
                matchTitle.setStyle("-fx-font-size: 72px; -fx-fill: linear-gradient(from 0% 0% to 100% 0%, #ffd166 0%, #ff7b7b 100%);");

                // show both players' labeled scores as separate nodes so we can highlight the winner
                javafx.scene.text.Text leftScoreText = new javafx.scene.text.Text(String.format("Left: %d", ls));
                javafx.scene.text.Text sepText = new javafx.scene.text.Text("  —  ");
                javafx.scene.text.Text rightScoreText = new javafx.scene.text.Text(String.format("Right: %d", rs));
                leftScoreText.setStyle("-fx-font-size: 28px; -fx-fill: white;");
                sepText.setStyle("-fx-font-size: 28px; -fx-fill: white;");
                rightScoreText.setStyle("-fx-font-size: 28px; -fx-fill: white;");
                // highlight the winner's score
                if (ls > rs) {
                    leftScoreText.setStyle("-fx-font-size: 28px; -fx-fill: #ffd166; -fx-font-weight: bold;");
                } else if (rs > ls) {
                    rightScoreText.setStyle("-fx-font-size: 28px; -fx-fill: #ffd166; -fx-font-weight: bold;");
                }
                javafx.scene.layout.HBox scoreBox = new javafx.scene.layout.HBox(4);
                scoreBox.setAlignment(Pos.CENTER);
                scoreBox.getChildren().addAll(leftScoreText, sepText, rightScoreText);

                javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(12);
                btnRow.setAlignment(Pos.CENTER);
                Button btnRestart = new Button("Restart");
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
                // reason text explaining why this player won
                javafx.scene.text.Text reasonText = new javafx.scene.text.Text(reason != null ? reason : "");
                reasonText.setStyle("-fx-font-size: 18px; -fx-fill: #dddddd; -fx-opacity: 0.95;");
                centerBox.getChildren().addAll(matchTitle, scoreBox, reasonText, btnRow);

                overlay.getChildren().add(centerBox);

                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    root.getChildren().add(overlay);
                    // center overlay
                    overlay.prefWidthProperty().bind(scene.widthProperty());
                    overlay.prefHeightProperty().bind(scene.heightProperty());
                    StackPane.setAlignment(centerBox, Pos.CENTER);

                    // subtle entrance animation: scale+fade the centerBox (longer + easing)
                    try {
                        centerBox.setOpacity(0.0);
                        centerBox.setScaleX(0.85);
                        centerBox.setScaleY(0.85);
                        javafx.animation.FadeTransition fade = new javafx.animation.FadeTransition(javafx.util.Duration.millis(480), centerBox);
                        fade.setFromValue(0.0); fade.setToValue(1.0);
                        javafx.animation.ScaleTransition scale = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(480), centerBox);
                        scale.setToX(1.0); scale.setToY(1.0);
                        scale.setFromX(0.85); scale.setFromY(0.85);
                        scale.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
                        fade.setInterpolator(javafx.animation.Interpolator.EASE_OUT);
                        // When entrance completes, start a pulsing glow on the winner score
                        scale.setOnFinished(ae -> {
                            java.util.Objects.requireNonNull(ae);
                            try {
                                // pulse the big winner title (matchTitle) instead of the score
                                // only pulse when there is a clear winner
                                if (ls != rs) {
                                    javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
                                    glow.setColor(javafx.scene.paint.Color.web("#ffd166"));
                                    glow.setRadius(18);
                                    glow.setSpread(0.55);
                                    matchTitle.setEffect(glow);
                                    javafx.animation.ScaleTransition pulse = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(800), matchTitle);
                                    pulse.setFromX(1.0); pulse.setFromY(1.0);
                                    pulse.setToX(1.06); pulse.setToY(1.06);
                                    pulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
                                    pulse.setAutoReverse(true);
                                    pulse.setInterpolator(javafx.animation.Interpolator.EASE_BOTH);
                                    pulse.play();
                                    activePulse = pulse;
                                }
                            } catch (Exception ignored) {}
                        });
                        scale.play(); fade.play();
                    } catch (Exception ignored) {}
                }

            } catch (Exception ignored) {}
        });
    }
}
