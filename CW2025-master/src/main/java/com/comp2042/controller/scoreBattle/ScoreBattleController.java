package com.comp2042.controller.scoreBattle;

import com.comp2042.audio.soundManager.SoundManager;
import com.comp2042.controller.gameControl.GameController;
import com.comp2042.controller.guiControl.GuiController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ScoreBattleController implements Initializable {

    @FXML StackPane leftHolder;
    @FXML StackPane rightHolder;
    @FXML javafx.scene.layout.VBox leftNextBox;
    @FXML private javafx.scene.layout.VBox leftNextContent;
    @FXML private javafx.scene.text.Text leftNextLabel;
    @FXML private javafx.scene.layout.VBox rightNextBox;
    @FXML private javafx.scene.layout.VBox rightNextContent;
    @FXML private javafx.scene.text.Text rightNextLabel;
    @FXML private Button backBtn;

    GuiController leftGui;
    GuiController rightGui;
    GameController leftController;
    GameController rightController;

    javafx.scene.layout.StackPane centerOverlay;

    javafx.scene.text.Text matchTimerText;
    javafx.scene.text.Text matchScoreText;
    javafx.animation.Timeline matchTimer;
    int remainingSeconds = 300; 
    javafx.animation.Timeline previewPoller;
    MediaPlayer scoreBattleMusicPlayer = null;
    MediaPlayer matchCountdownPlayer = null;
    MediaPlayer matchGameOverPlayer = null;
    SoundManager soundManager = null;

    volatile boolean matchEnded = false;

    javafx.scene.layout.StackPane activeOverlay = null;
    javafx.animation.Animation activePulse = null;
    javafx.beans.value.ChangeListener<Boolean> leftIsGameOverListener;
    javafx.beans.value.ChangeListener<Boolean> rightIsGameOverListener;
    javafx.beans.value.ChangeListener<Boolean> sharedCountdownStartedListener;
    javafx.beans.value.ChangeListener<Boolean> bothFinishedListener;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            java.net.URL fontUrl = getClass().getClassLoader().getResource("digital.ttf");
            if (fontUrl != null) {
                javafx.scene.text.Font.loadFont(fontUrl.toExternalForm(), 38);
            }
        } catch (Exception ignored) {}
        try {
            soundManager = new SoundManager(getClass());
            soundManager.init();
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

    private void playMatchGameOverSound() {
        try {
            if (soundManager != null) {
                soundManager.playGameOverMusic();
            } else {
                // fallback: try to play via local MediaPlayer
                try { if (matchGameOverPlayer != null) { matchGameOverPlayer.stop(); matchGameOverPlayer.dispose(); matchGameOverPlayer = null; } } catch (Exception ignored) {}
                URL musicUrl = getClass().getClassLoader().getResource("sounds/GameOver.wav");
                if (musicUrl == null) musicUrl = getClass().getClassLoader().getResource("sounds/GameOver.mp3");
                if (musicUrl != null) {
                    Media m = new Media(musicUrl.toExternalForm());
                    matchGameOverPlayer = new MediaPlayer(m);
                    matchGameOverPlayer.setCycleCount(1);
                    matchGameOverPlayer.setAutoPlay(true);
                    matchGameOverPlayer.setOnEndOfMedia(() -> {
                        try { matchGameOverPlayer.dispose(); } catch (Exception ignored) {}
                        matchGameOverPlayer = null;
                    });
                }
            }
        } catch (Exception ignored) {}
    }

    private void stopMatchGameOverSound() {
        try {
            if (soundManager != null) {
                soundManager.stopGameOverMusic();
            } else {
                if (matchGameOverPlayer != null) {
                    try { matchGameOverPlayer.stop(); } catch (Exception ignored) {}
                    try { matchGameOverPlayer.dispose(); } catch (Exception ignored) {}
                    matchGameOverPlayer = null;
                }
            }
        } catch (Exception ignored) {}
    }

    void playMatchCountdownSound() {
        try {
            if (soundManager != null) {
                soundManager.playCountdownMusic();
            } else {
                try { if (matchCountdownPlayer != null) { matchCountdownPlayer.stop(); matchCountdownPlayer.dispose(); matchCountdownPlayer = null; } } catch (Exception ignored) {}
                URL musicUrl = getClass().getClassLoader().getResource("sounds/Countdown.wav");
                if (musicUrl == null) musicUrl = getClass().getClassLoader().getResource("sounds/Countdown.mp3");
                if (musicUrl != null) {
                    try {
                        Media m = new Media(musicUrl.toExternalForm());
                        matchCountdownPlayer = new MediaPlayer(m);
                        matchCountdownPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                        matchCountdownPlayer.setAutoPlay(true);
                        matchCountdownPlayer.setVolume(0.75);
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Stop and dispose the centralized countdown sound if playing.
     */
    private void stopMatchCountdownSound() {
        try {
            if (soundManager != null) {
                soundManager.stopCountdownMusic();
            } else {
                if (matchCountdownPlayer != null) {
                    try { matchCountdownPlayer.stop(); } catch (Exception ignored) {}
                    try { matchCountdownPlayer.dispose(); } catch (Exception ignored) {}
                    matchCountdownPlayer = null;
                }
            }
        } catch (Exception ignored) {}
    }

    void showMultiplayerControlsOverlay(GuiController requester) {
        javafx.application.Platform.runLater(() -> {
            try {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;
                // Ensure both embedded GUIs and match-level timers are paused while the overlay is open
                try { if (leftGui != null) leftGui.applyExternalPause(true); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.applyExternalPause(true); } catch (Exception ignored) {}
                new ScoreBattleControlsOverlay(scene, leftGui, rightGui).show(requester);
            } catch (Exception ignored) {}
        });
    }

    public void restartMatch() {
        javafx.application.Platform.runLater(() -> {
            try {
                // stop any playing match music before restarting
                try { if (scoreBattleMusicPlayer != null) { try { if (soundManager != null) soundManager.disposeMediaPlayer(scoreBattleMusicPlayer); else { scoreBattleMusicPlayer.stop(); scoreBattleMusicPlayer.dispose(); } } catch (Exception ignored) {} scoreBattleMusicPlayer = null; } } catch (Exception ignored) {}
                try { stopMatchCountdownSound(); } catch (Exception ignored) {}
                try { stopMatchGameOverSound(); } catch (Exception ignored) {}
                // Also ensure embedded GUIs stop any local game-over / countdown audio they may have started
                try { if (leftGui != null) leftGui.stopOverlayAudio(); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.stopOverlayAudio(); } catch (Exception ignored) {}
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
        // preserve backward compatibility: delegate to the new overload with null swaps
        initBothGames(null, null);
    }

    public void initBothGames(javafx.scene.input.KeyCode leftSwap, javafx.scene.input.KeyCode rightSwap) throws IOException {
        // delegate into the initializer which contains the refactored logic
        ScoreBattleInitializer.initBothGames(this, leftSwap, rightSwap);
    }

    void onBack(ActionEvent ev) {
        try {
            URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
            if (loc == null) return;
            FXMLLoader loader = new FXMLLoader(loc);
            Parent menuRoot = loader.load();

            // prefer leftHolder's scene as the host; fallback to rightHolder
            Stage stage = null;
            try { if (leftHolder != null && leftHolder.getScene() != null) stage = (Stage) leftHolder.getScene().getWindow(); } catch (Exception ignored) {}
            try { if (stage == null && rightHolder != null && rightHolder.getScene() != null) stage = (Stage) rightHolder.getScene().getWindow(); } catch (Exception ignored) {}
            if (stage == null) return;

            if (stage.getScene() != null) {
                try { cleanup(); } catch (Exception ignored) {}
                stage.getScene().setRoot(menuRoot);
                try {
                    String css = getClass().getClassLoader().getResource("css/menu.css").toExternalForm();
                    if (!stage.getScene().getStylesheets().contains(css)) stage.getScene().getStylesheets().add(css);
                } catch (Exception ignored) {}
            } else {
                Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                try {
                    String css = getClass().getClassLoader().getResource("css/menu.css").toExternalForm();
                    s2.getStylesheets().add(css);
                } catch (Exception ignored) {}
                stage.setScene(s2);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void ensureScoreBattleStylesheet(Scene scene) {
        try {
            if (scene == null) return;
            URL cssUrl = getClass().getClassLoader().getResource("css/score-battle.css");
            if (cssUrl != null) {
                String css = cssUrl.toExternalForm();
                if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
            }
        } catch (Exception ignored) {}
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    public void cleanup() {
        try { if (matchTimer != null) { matchTimer.stop(); matchTimer = null; } } catch (Exception ignored) {}
        try { if (previewPoller != null) { previewPoller.stop(); previewPoller = null; } } catch (Exception ignored) {}
        try { stopMatchCountdownSound(); } catch (Exception ignored) {}
        try { stopMatchGameOverSound(); } catch (Exception ignored) {}
        try { if (scoreBattleMusicPlayer != null) { try { if (soundManager != null) soundManager.disposeMediaPlayer(scoreBattleMusicPlayer); else { scoreBattleMusicPlayer.stop(); scoreBattleMusicPlayer.dispose(); } } catch (Exception ignored) {} scoreBattleMusicPlayer = null; } } catch (Exception ignored) {}

        try { if (activePulse != null) { activePulse.stop(); activePulse = null; } } catch (Exception ignored) {}
        try { activeOverlay = null; } catch (Exception ignored) {}

        try {
            if (leftGui != null) {
                try { if (leftIsGameOverListener != null) leftGui.isGameOverProperty().removeListener(leftIsGameOverListener); } catch (Exception ignored) {}
                try { if (sharedCountdownStartedListener != null) leftGui.countdownStartedProperty().removeListener(sharedCountdownStartedListener); } catch (Exception ignored) {}
                try { if (bothFinishedListener != null) leftGui.countdownFinishedProperty().removeListener(bothFinishedListener); } catch (Exception ignored) {}
                try { leftGui.setMultiplayerRequestControlsHandler(null); } catch (Exception ignored) {}
                try { leftGui.setMultiplayerPauseHandler(null); } catch (Exception ignored) {}
                try { leftGui.setMultiplayerRestartHandler(null); } catch (Exception ignored) {}
                try { leftGui.setMultiplayerExitToMenuHandler(null); } catch (Exception ignored) {}
                try { leftGui.cleanup(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try {
            if (rightGui != null) {
                try { if (rightIsGameOverListener != null) rightGui.isGameOverProperty().removeListener(rightIsGameOverListener); } catch (Exception ignored) {}
                try { if (sharedCountdownStartedListener != null) rightGui.countdownStartedProperty().removeListener(sharedCountdownStartedListener); } catch (Exception ignored) {}
                try { if (bothFinishedListener != null) rightGui.countdownFinishedProperty().removeListener(bothFinishedListener); } catch (Exception ignored) {}
                try { rightGui.setMultiplayerRequestControlsHandler(null); } catch (Exception ignored) {}
                try { rightGui.setMultiplayerPauseHandler(null); } catch (Exception ignored) {}
                try { rightGui.setMultiplayerRestartHandler(null); } catch (Exception ignored) {}
                try { rightGui.setMultiplayerExitToMenuHandler(null); } catch (Exception ignored) {}
                try { rightGui.cleanup(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try { leftController = null; rightController = null; } catch (Exception ignored) {}
        try { leftGui = null; rightGui = null; } catch (Exception ignored) {}
        try { leftIsGameOverListener = null; rightIsGameOverListener = null; sharedCountdownStartedListener = null; bothFinishedListener = null; } catch (Exception ignored) {}
    }

    private void updateMatchScoreText() {
        try {
            int ls = (leftController != null) ? leftController.getScoreProperty().get() : 0;
            int rs = (rightController != null) ? rightController.getScoreProperty().get() : 0;
            if (matchScoreText != null) matchScoreText.setText(String.format("%d  —  %d", ls, rs));
        } catch (Exception ignored) {}
    }

    void scheduleStartMusicWhenCountdownsDone() {
        try {
            if (leftGui == null || rightGui == null) return;

            bothFinishedListener = buildBothFinishedListener();
            attachCountdownFinishedListeners();
            triggerBothFinishedIfAlreadyDone();
        } catch (Exception ignored) {}
    }

    private javafx.beans.value.ChangeListener<Boolean> buildBothFinishedListener() {
        return (obs, oldV, newV) -> {
            try {
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
                java.util.Objects.requireNonNull(newV);
                boolean l = false, r = false;
                try { l = leftGui.countdownFinishedProperty().get(); } catch (Exception ignored) {}
                try { r = rightGui.countdownFinishedProperty().get(); } catch (Exception ignored) {}
                if (l && r) {
                    onBothCountdownsFinished();
                }
            } catch (Exception ignored) {}
        };
    }

    private void attachCountdownFinishedListeners() {
        try { leftGui.countdownFinishedProperty().addListener(bothFinishedListener); } catch (Exception ignored) {}
        try { rightGui.countdownFinishedProperty().addListener(bothFinishedListener); } catch (Exception ignored) {}
    }

    private void triggerBothFinishedIfAlreadyDone() {
        try {
            if (leftGui.countdownFinishedProperty().get() && rightGui.countdownFinishedProperty().get() && bothFinishedListener != null) {
                bothFinishedListener.changed(null, Boolean.FALSE, Boolean.TRUE);
            }
        } catch (Exception ignored) {}
    }

    private void onBothCountdownsFinished() {
        try {
            try {
                if (matchTimer != null && matchTimer.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                    matchTimer.play();
                }
            } catch (Exception ignored) {}

            try {
                if (scoreBattleMusicPlayer == null) {
                    try {
                        if (soundManager != null) {
                            scoreBattleMusicPlayer = soundManager.createMediaPlayer("/sounds/ScoreBattle.wav", true, null);
                            if (scoreBattleMusicPlayer != null) scoreBattleMusicPlayer.play();
                        } else {
                            URL musicUrl = getClass().getClassLoader().getResource("sounds/ScoreBattle.wav");
                            if (musicUrl == null) musicUrl = getClass().getClassLoader().getResource("sounds/ScoreBattle.mp3");
                            if (musicUrl != null) {
                                Media m = new Media(musicUrl.toExternalForm());
                                scoreBattleMusicPlayer = new MediaPlayer(m);
                                scoreBattleMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                                scoreBattleMusicPlayer.setAutoPlay(true);
                            }
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                } else {
                    try { scoreBattleMusicPlayer.play(); } catch (Exception ignored) {}
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception ignored) {}
    }

    void endMatchAndAnnounceWinner() {
        // stop any match music immediately
    try { if (scoreBattleMusicPlayer != null) { try { if (soundManager != null) soundManager.disposeMediaPlayer(scoreBattleMusicPlayer); else { scoreBattleMusicPlayer.stop(); scoreBattleMusicPlayer.dispose(); } } catch (Exception ignored) {} scoreBattleMusicPlayer = null; } } catch (Exception ignored) {}
        try { stopMatchCountdownSound(); } catch (Exception ignored) {}
        try { playMatchGameOverSound(); } catch (Exception ignored) {}
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
                // ensure our stylesheet is loaded so overlay classes apply
                ensureScoreBattleStylesheet(scene);
                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                overlay.getStyleClass().add("score-battle-overlay");

                // reuse the centralized winner overlay (with reason)
                showWinnerOverlay(title, ls, rs, reason);

                // animations handled by the centralized overlay (if desired)
            } catch (Exception ignored) {}
        });
    }

    private void showWinnerOverlay(String title, int ls, int rs, String reason) {
        // Use the same centered Match Over layout used elsewhere but with a fully black background
        javafx.application.Platform.runLater(() -> {
            try {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;
                // ensure external stylesheet is available
                ensureScoreBattleStylesheet(scene);

                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                // solid black background via stylesheet
                overlay.getStyleClass().add("score-battle-overlay");
                // remember overlay so we can remove it deterministically
                activeOverlay = overlay;

                // create centered Match Over panel matching single-player design
                VBox centerBox = new VBox(10);
                centerBox.setAlignment(Pos.CENTER);
                centerBox.getStyleClass().add("score-battle-centerbox");

                // show the winner title (e.g. "Left Player Wins!")
                javafx.scene.text.Text matchTitle = new javafx.scene.text.Text(title);
                matchTitle.getStyleClass().add("score-battle-match-title");

                // show both players' labeled scores as separate nodes so we can highlight the winner
                javafx.scene.text.Text leftScoreText = new javafx.scene.text.Text(String.format("Left: %d", ls));
                javafx.scene.text.Text sepText = new javafx.scene.text.Text("  —  ");
                javafx.scene.text.Text rightScoreText = new javafx.scene.text.Text(String.format("Right: %d", rs));
                leftScoreText.getStyleClass().add("score-battle-winner-score");
                sepText.getStyleClass().add("score-battle-winner-score");
                rightScoreText.getStyleClass().add("score-battle-winner-score");
                // highlight the winner's score using an additional class
                if (ls > rs) {
                    leftScoreText.getStyleClass().add("highlight");
                } else if (rs > ls) {
                    rightScoreText.getStyleClass().add("highlight");
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
                        // stop score battle music when returning to menu from the winner overlay
                        try { if (scoreBattleMusicPlayer != null) { scoreBattleMusicPlayer.stop(); scoreBattleMusicPlayer.dispose(); scoreBattleMusicPlayer = null; } } catch (Exception ignored) {}
                        try { stopMatchGameOverSound(); } catch (Exception ignored) {}
                        URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
                        if (loc == null) return;
                        FXMLLoader loader = new FXMLLoader(loc);
                        Parent menuRoot = loader.load();
                        Stage stage = (Stage) scene.getWindow();
                        if (stage.getScene() != null) {
                            try { if (leftGui != null) leftGui.cleanup(); } catch (Exception ignored) {}
                            try { if (rightGui != null) rightGui.cleanup(); } catch (Exception ignored) {}
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
                reasonText.getStyleClass().add("score-battle-reason");
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
