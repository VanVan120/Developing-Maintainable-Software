package com.comp2042.controller;

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
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

public class ClassicBattle implements Initializable {

    @FXML private StackPane leftHolder;

    @FXML private StackPane rightHolder;

    @FXML private javafx.scene.layout.VBox leftNextBox;
    @FXML private javafx.scene.layout.VBox leftNextContent;
    @FXML private javafx.scene.text.Text leftNextLabel;

    @FXML private javafx.scene.layout.VBox rightNextBox;
    @FXML private javafx.scene.layout.VBox rightNextContent;
    @FXML private javafx.scene.text.Text rightNextLabel;

    @FXML private Button backBtn;

    private GuiController leftGui;
    private GuiController rightGui;
    private GameController leftController;
    private GameController rightController;
    private javafx.scene.layout.StackPane centerOverlay;
    private javafx.scene.text.Text matchTimerText;
    private javafx.scene.text.Text matchScoreText;
    private javafx.animation.Timeline matchTimer;
    private int remainingSeconds = 5 * 60;
    private javafx.animation.Timeline previewPoller;
    private javafx.scene.media.MediaPlayer classicBattleMusicPlayer = null;
    private MediaPlayer matchGameOverPlayer = null;
    private Clip matchGameOverClipFallback = null;
    private MediaPlayer matchCountdownPlayer = null;
    private Clip matchCountdownClipFallback = null;

    private void playMatchGameOverSound() {
        try {
            try { if (matchGameOverPlayer != null) { matchGameOverPlayer.stop(); matchGameOverPlayer.dispose(); matchGameOverPlayer = null; } } catch (Exception ignored) {}
            URL musicUrl = getClass().getClassLoader().getResource("sounds/GameOver.wav");
            if (musicUrl == null) musicUrl = getClass().getClassLoader().getResource("sounds/GameOver.mp3");
            if (musicUrl != null) {
                try {
                    Media m = new Media(musicUrl.toExternalForm());
                    matchGameOverPlayer = new MediaPlayer(m);
                    matchGameOverPlayer.setCycleCount(1);
                    matchGameOverPlayer.setAutoPlay(true);
                    matchGameOverPlayer.setOnEndOfMedia(() -> {
                        try { matchGameOverPlayer.dispose(); } catch (Exception ignored) {}
                        matchGameOverPlayer = null;
                    });
                    return;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try {
            try { if (matchGameOverClipFallback != null && matchGameOverClipFallback.isRunning()) { matchGameOverClipFallback.stop(); matchGameOverClipFallback.close(); matchGameOverClipFallback = null; } } catch (Exception ignored) {}
            java.net.URL u = getClass().getClassLoader().getResource("sounds/GameOver.wav");
            if (u != null) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(u);
                matchGameOverClipFallback = AudioSystem.getClip();
                matchGameOverClipFallback.open(ais);
                matchGameOverClipFallback.start();
            }
        } catch (Exception ignored) {}
    }

    private void stopMatchGameOverSound() {
        try {
            if (matchGameOverPlayer != null) {
                try { matchGameOverPlayer.stop(); } catch (Exception ignored) {}
                try { matchGameOverPlayer.dispose(); } catch (Exception ignored) {}
                matchGameOverPlayer = null;
            }
        } catch (Exception ignored) {}
        try {
            if (matchGameOverClipFallback != null) {
                try { matchGameOverClipFallback.stop(); } catch (Exception ignored) {}
                try { matchGameOverClipFallback.close(); } catch (Exception ignored) {}
                matchGameOverClipFallback = null;
            }
        } catch (Exception ignored) {}
    }

    private void playMatchCountdownSound() {
        try {
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
                    return;
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try {
            try { if (matchCountdownClipFallback != null && matchCountdownClipFallback.isRunning()) { matchCountdownClipFallback.stop(); matchCountdownClipFallback.close(); matchCountdownClipFallback = null; } } catch (Exception ignored) {}
            java.net.URL u = getClass().getClassLoader().getResource("sounds/Countdown.wav");
            if (u != null) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(u);
                matchCountdownClipFallback = AudioSystem.getClip();
                matchCountdownClipFallback.open(ais);
                matchCountdownClipFallback.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Exception ignored) {}
    }

    private void stopMatchCountdownSound() {
        try {
            if (matchCountdownPlayer != null) {
                try { matchCountdownPlayer.stop(); } catch (Exception ignored) {}
                try { matchCountdownPlayer.dispose(); } catch (Exception ignored) {}
                matchCountdownPlayer = null;
            }
        } catch (Exception ignored) {}
        try {
            if (matchCountdownClipFallback != null) {
                try { matchCountdownClipFallback.stop(); } catch (Exception ignored) {}
                try { matchCountdownClipFallback.close(); } catch (Exception ignored) {}
                matchCountdownClipFallback = null;
            }
        } catch (Exception ignored) {}
    }

    private void registerGameOverListener(GuiController gui, boolean isLeft) {
        try {
            if (gui == null) return;
            gui.isGameOverProperty().addListener((obs, oldV, newV) -> {
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
                if (newV == Boolean.TRUE && !matchEnded) {
                    matchEnded = true;
                    try { if (matchTimer != null) matchTimer.stop(); } catch (Exception ignored) {}
                    try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}
                    try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
                    try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}
                    try { if (classicBattleMusicPlayer != null) { classicBattleMusicPlayer.stop(); classicBattleMusicPlayer.dispose(); classicBattleMusicPlayer = null; } } catch (Exception ignored) {}
                    try { playMatchGameOverSound(); } catch (Exception ignored) {}

                    int lscore = (leftController != null ? leftController.getScoreProperty().get() : 0);
                    int rscore = (rightController != null ? rightController.getScoreProperty().get() : 0);
                    boolean winnerIsLeft = !isLeft;
                    int winnerScore = winnerIsLeft ? lscore : rscore;
                    int loserScore = winnerIsLeft ? rscore : lscore;
                    String title = winnerIsLeft ? "Left Player Wins!" : "Right Player Wins!";
                    String reason = "Winner by survival (opponent lost)";
                    if (winnerScore != loserScore) {
                        reason += String.format(" — winner had higher score (%d vs %d)", winnerScore, loserScore);
                    }
                    showWinnerOverlay(title, lscore, rscore, reason);
                }
            });
        } catch (Exception ignored) {}
    }

    private volatile boolean matchEnded = false;
    private javafx.scene.layout.StackPane activeOverlay = null;
    private javafx.animation.Animation activePulse = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
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

    public void restartMatch() {
        javafx.application.Platform.runLater(() -> {
            try {
                try { if (classicBattleMusicPlayer != null) { classicBattleMusicPlayer.stop(); classicBattleMusicPlayer.dispose(); classicBattleMusicPlayer = null; } } catch (Exception ignored) {}
                try { stopMatchCountdownSound(); } catch (Exception ignored) {}
                try { stopMatchGameOverSound(); } catch (Exception ignored) {}
                try { if (matchTimer != null) matchTimer.stop(); } catch (Exception ignored) {}
                try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}
                try { if (leftController != null) leftController.createNewGame(); } catch (Exception ignored) {}
                try { if (rightController != null) rightController.createNewGame(); } catch (Exception ignored) {}
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

    public void initBothGames() throws IOException {
        initBothGames(null, null);
    }

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
        try { registerGameOverListener(leftGui, true); } catch (Exception ignored) {}
        try { registerGameOverListener(rightGui, false); } catch (Exception ignored) {}
        try { leftGui.setMultiplayerMode(true); } catch (Exception ignored) {}
        try { rightGui.setMultiplayerMode(true); } catch (Exception ignored) {}
        try { leftGui.setMultiplayerRequestControlsHandler(this::showMultiplayerControlsOverlay); } catch (Exception ignored) {}
        try { rightGui.setMultiplayerRequestControlsHandler(this::showMultiplayerControlsOverlay); } catch (Exception ignored) {}
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

        try {
            final javafx.beans.value.ChangeListener<Boolean> startCountdownListener = (obs, oldV, newV) -> {
                try {
                    java.util.Objects.requireNonNull(obs);
                    java.util.Objects.requireNonNull(oldV);
                    java.util.Objects.requireNonNull(newV);
                    boolean l = false, r = false;
                    try { l = leftGui.countdownStartedProperty().get(); } catch (Exception ignored) {}
                    try { r = rightGui.countdownStartedProperty().get(); } catch (Exception ignored) {}
                    if (l || r) {
                        try { playMatchCountdownSound(); } catch (Exception ignored) {}
                    } else {
                        try { stopMatchCountdownSound(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            };
            try { leftGui.countdownStartedProperty().addListener(startCountdownListener); } catch (Exception ignored) {}
            try { rightGui.countdownStartedProperty().addListener(startCountdownListener); } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        leftGui.startCountdown(3);
        rightGui.startCountdown(3);

        try {
            leftGui.setControlKeys(javafx.scene.input.KeyCode.A, javafx.scene.input.KeyCode.D, javafx.scene.input.KeyCode.W, javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCode.SHIFT);
            leftGui.setSwapKey(leftSwap != null ? leftSwap : javafx.scene.input.KeyCode.Q);
            rightGui.setControlKeys(javafx.scene.input.KeyCode.NUMPAD4, javafx.scene.input.KeyCode.NUMPAD6, javafx.scene.input.KeyCode.NUMPAD8, javafx.scene.input.KeyCode.NUMPAD5, javafx.scene.input.KeyCode.SPACE);
            rightGui.setSwapKey(rightSwap != null ? rightSwap : javafx.scene.input.KeyCode.C);
        } catch (Exception ignored) {}
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
                        try {
                            if (classicBattleMusicPlayer == null) {
                                URL musicUrl = getClass().getClassLoader().getResource("sounds/ClassicBattle.wav");
                                if (musicUrl == null) musicUrl = getClass().getClassLoader().getResource("sounds/ClassicBattle.mp3");
                                if (musicUrl != null) {
                                    javafx.scene.media.Media m = new javafx.scene.media.Media(musicUrl.toExternalForm());
                                    classicBattleMusicPlayer = new javafx.scene.media.MediaPlayer(m);
                                    classicBattleMusicPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
                                    classicBattleMusicPlayer.setAutoPlay(true);
                                }
                            } else {
                                try { classicBattleMusicPlayer.play(); } catch (Exception ignored) {}
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
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
                try { if (leftGui != null) leftGui.cleanup(); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.cleanup(); } catch (Exception ignored) {}
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
        try { if (classicBattleMusicPlayer != null) { classicBattleMusicPlayer.stop(); classicBattleMusicPlayer.dispose(); classicBattleMusicPlayer = null; } } catch (Exception ignored) {}
        try { stopMatchGameOverSound(); } catch (Exception ignored) {}
        try { stopMatchCountdownSound(); } catch (Exception ignored) {}
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

    private void showMultiplayerControlsOverlay(GuiController requester) {
        javafx.application.Platform.runLater(() -> {
            try {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;

                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                Rectangle dark = new Rectangle();
                dark.widthProperty().bind(scene.widthProperty());
                dark.heightProperty().bind(scene.heightProperty());
                dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));

                BorderPane container = new BorderPane();
                container.setStyle("-fx-padding:18;");
                javafx.scene.text.Text header = new javafx.scene.text.Text("Controls");
                header.setStyle("-fx-font-size:34px; -fx-fill: #9fb0ff; -fx-font-weight:700;");
                javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(10);
                actionBox.setAlignment(Pos.CENTER_RIGHT);
                javafx.scene.control.Button btnResetTop = new javafx.scene.control.Button("Reset");
                javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Cancel");
                javafx.scene.control.Button btnSave = new javafx.scene.control.Button("Save");
                btnResetTop.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
                actionBox.getChildren().addAll(btnResetTop, btnCancel, btnSave);
                BorderPane topBar = new BorderPane();
                topBar.setLeft(header);
                topBar.setRight(actionBox);
                topBar.setStyle("-fx-padding:8 18 18 18;");
                container.setTop(topBar);

                javafx.scene.layout.HBox center = new javafx.scene.layout.HBox(120);
                center.setStyle("-fx-padding:12; -fx-background-color: transparent;");
                center.setAlignment(Pos.CENTER);

                FXMLLoader leftFx = new FXMLLoader(getClass().getClassLoader().getResource("controls.fxml"));
                javafx.scene.layout.StackPane leftPane = leftFx.load();
                ControlsController leftCC = leftFx.getController();

                FXMLLoader rightFx = new FXMLLoader(getClass().getClassLoader().getResource("controls.fxml"));
                javafx.scene.layout.StackPane rightPane = rightFx.load();
                ControlsController rightCC = rightFx.getController();

                java.util.prefs.Preferences overlayPrefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.controller.MainMenuController.class);
                try {
                    leftCC.init(leftGui.getCtrlMoveLeft() != null ? leftGui.getCtrlMoveLeft() : javafx.scene.input.KeyCode.A,
                                leftGui.getCtrlMoveRight() != null ? leftGui.getCtrlMoveRight() : javafx.scene.input.KeyCode.D,
                                leftGui.getCtrlRotate() != null ? leftGui.getCtrlRotate() : javafx.scene.input.KeyCode.W,
                                leftGui.getCtrlSoftDrop() != null ? leftGui.getCtrlSoftDrop() : javafx.scene.input.KeyCode.S,
                                leftGui.getCtrlHardDrop() != null ? leftGui.getCtrlHardDrop() : javafx.scene.input.KeyCode.SHIFT,
                                leftGui.getCtrlSwap() != null ? leftGui.getCtrlSwap() : javafx.scene.input.KeyCode.Q);

                    javafx.scene.input.KeyCode defLLeft = null;
                    try { String s = overlayPrefs.get("mpLeft_left", ""); if (!s.isEmpty()) defLLeft = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    leftCC.setDefaultKeys(
                        defLLeft != null ? defLLeft : javafx.scene.input.KeyCode.A,
                        javafx.scene.input.KeyCode.D,
                        javafx.scene.input.KeyCode.W,
                        javafx.scene.input.KeyCode.S,
                        javafx.scene.input.KeyCode.SHIFT,
                        javafx.scene.input.KeyCode.Q
                    );
                    leftCC.setHeaderText("Left Player Controls");
                } catch (Exception ignored) {}

                try {
                    rightCC.init(rightGui.getCtrlMoveLeft() != null ? rightGui.getCtrlMoveLeft() : javafx.scene.input.KeyCode.NUMPAD4,
                                 rightGui.getCtrlMoveRight() != null ? rightGui.getCtrlMoveRight() : javafx.scene.input.KeyCode.NUMPAD6,
                                 rightGui.getCtrlRotate() != null ? rightGui.getCtrlRotate() : javafx.scene.input.KeyCode.NUMPAD8,
                                 rightGui.getCtrlSoftDrop() != null ? rightGui.getCtrlSoftDrop() : javafx.scene.input.KeyCode.NUMPAD5,
                                 rightGui.getCtrlHardDrop() != null ? rightGui.getCtrlHardDrop() : javafx.scene.input.KeyCode.SPACE,
                                 rightGui.getCtrlSwap() != null ? rightGui.getCtrlSwap() : javafx.scene.input.KeyCode.C);
                    rightCC.setHeaderText("Right Player Controls");
                try {
                    leftCC.setKeyAvailabilityChecker((code, btn) -> {
                        try {
                            java.util.Objects.requireNonNull(btn);
                            if (code == null) return true;
                            return !(code.equals(rightCC.getLeft())
                                    || code.equals(rightCC.getRight())
                                    || code.equals(rightCC.getRotate())
                                    || code.equals(rightCC.getDown())
                                    || code.equals(rightCC.getHard())
                                    || code.equals(rightCC.getSwitch()));
                        } catch (Exception ignored) {
                            return true;
                        }
                    });
                } catch (Exception ignored) {}
                try {
                    rightCC.setKeyAvailabilityChecker((code, btn) -> {
                        try {
                            java.util.Objects.requireNonNull(btn);
                            if (code == null) return true;
                            return !(code.equals(leftCC.getLeft())
                                    || code.equals(leftCC.getRight())
                                    || code.equals(leftCC.getRotate())
                                    || code.equals(leftCC.getDown())
                                    || code.equals(leftCC.getHard())
                                    || code.equals(leftCC.getSwitch()));
                        } catch (Exception ignored) {
                            return true;
                        }
                    });
                } catch (Exception ignored) {}
                } catch (Exception ignored) {}

                try { leftCC.hideActionButtons(); } catch (Exception ignored) {}
                try { rightCC.hideActionButtons(); } catch (Exception ignored) {}

                try { leftPane.setPrefWidth(520); } catch (Exception ignored) {}
                try { rightPane.setPrefWidth(520); } catch (Exception ignored) {}
                center.getChildren().addAll(leftPane, rightPane);
                container.setCenter(center);

                overlay.getChildren().addAll(dark, container);

                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    java.util.List<javafx.scene.Node> hidden = new java.util.ArrayList<>();
                    for (javafx.scene.Node n : new java.util.ArrayList<>(root.getChildren())) {
                        if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) {
                            n.setVisible(false);
                            hidden.add(n);
                        }
                    }
                    overlay.getProperties().put("hiddenPauseNodes", hidden);
                    root.getChildren().add(overlay);
                }

                btnResetTop.setOnAction(ev -> {
                    ev.consume();
                    try { leftCC.resetToPanelDefaults(); } catch (Exception ignored) {}
                    try { rightCC.resetToPanelDefaults(); } catch (Exception ignored) {}
                });

                btnSave.setOnAction(ev -> {
                    ev.consume();
                    try {
                        try {
                            javafx.scene.input.KeyCode lLeft = leftCC.getLeft();
                            javafx.scene.input.KeyCode lRight = leftCC.getRight();
                            javafx.scene.input.KeyCode lRotate = leftCC.getRotate();
                            javafx.scene.input.KeyCode lDown = leftCC.getDown();
                            javafx.scene.input.KeyCode lHard = leftCC.getHard();
                            javafx.scene.input.KeyCode lSwap = leftCC.getSwitch();
                            leftGui.setControlKeys(lLeft, lRight, lRotate, lDown, lHard);
                            leftGui.setSwapKey(lSwap);
                            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.controller.MainMenuController.class);
                            prefs.put("mpLeft_left", lLeft != null ? lLeft.name() : "");
                            prefs.put("mpLeft_right", lRight != null ? lRight.name() : "");
                            prefs.put("mpLeft_rotate", lRotate != null ? lRotate.name() : "");
                            prefs.put("mpLeft_down", lDown != null ? lDown.name() : "");
                            prefs.put("mpLeft_hard", lHard != null ? lHard.name() : "");
                            prefs.put("mpLeft_switch", lSwap != null ? lSwap.name() : "");
                        } catch (Exception ignored) {}
                        try {
                            javafx.scene.input.KeyCode rLeft = rightCC.getLeft();
                            javafx.scene.input.KeyCode rRight = rightCC.getRight();
                            javafx.scene.input.KeyCode rRotate = rightCC.getRotate();
                            javafx.scene.input.KeyCode rDown = rightCC.getDown();
                            javafx.scene.input.KeyCode rHard = rightCC.getHard();
                            javafx.scene.input.KeyCode rSwap = rightCC.getSwitch();
                            rightGui.setControlKeys(rLeft, rRight, rRotate, rDown, rHard);
                            rightGui.setSwapKey(rSwap);
                            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.controller.MainMenuController.class);
                            prefs.put("mpRight_left", rLeft != null ? rLeft.name() : "");
                            prefs.put("mpRight_right", rRight != null ? rRight.name() : "");
                            prefs.put("mpRight_rotate", rRotate != null ? rRotate.name() : "");
                            prefs.put("mpRight_down", rDown != null ? rDown.name() : "");
                            prefs.put("mpRight_hard", rHard != null ? rHard.name() : "");
                            prefs.put("mpRight_switch", rSwap != null ? rSwap.name() : "");
                        } catch (Exception ignored) {}
                    } catch (Exception ignored) {}
                    try {
                        if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) overlay.getParent();
                            root.getChildren().remove(overlay);
                        }
                        Object o = overlay.getProperties().get("hiddenPauseNodes");
                        if (o instanceof java.util.List<?>) {
                            for (Object n : (java.util.List<?>) o) {
                                if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                            }
                        }
                    } catch (Exception ignored) {}
                });

                btnCancel.setOnAction(ev -> {
                    ev.consume();
                    try {
                        if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) overlay.getParent();
                            root.getChildren().remove(overlay);
                        }
                        Object o = overlay.getProperties().get("hiddenPauseNodes");
                        if (o instanceof java.util.List<?>) {
                            for (Object n : (java.util.List<?>) o) {
                                if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                            }
                        }
                    } catch (Exception ignored) {}
                });

            } catch (Exception ignored) {}
        });
    }

    private void endMatchAndAnnounceWinner() {
        try { if (classicBattleMusicPlayer != null) { classicBattleMusicPlayer.stop(); classicBattleMusicPlayer.dispose(); classicBattleMusicPlayer = null; } } catch (Exception ignored) {}
        try { stopMatchCountdownSound(); } catch (Exception ignored) {}
        try { playMatchGameOverSound(); } catch (Exception ignored) {}
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
                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                overlay.setStyle("-fx-background-color: rgba(0,0,0,0.88);");
                activeOverlay = overlay;

                VBox panel = new VBox(14);
                panel.setAlignment(Pos.CENTER);
                panel.setStyle("-fx-background-color: rgba(18,18,20,0.95); -fx-padding: 26px; -fx-background-radius: 8px;");

        javafx.scene.text.Text bigTitle = new javafx.scene.text.Text(title);
        bigTitle.setStyle("-fx-font-size: 64px; -fx-font-weight: 700; -fx-fill: white;");

        try {
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(javafx.scene.paint.Color.web("#ffd166"));
            glow.setRadius(8);
            glow.setSpread(0.45);
            bigTitle.setEffect(glow);

            javafx.animation.ScaleTransition scalePulse = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(900), bigTitle);
            scalePulse.setFromX(1.0); scalePulse.setFromY(1.0);
            scalePulse.setToX(1.03); scalePulse.setToY(1.03);
            scalePulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
            scalePulse.setAutoReverse(true);

            javafx.animation.Timeline glowTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, new javafx.animation.KeyValue(glow.radiusProperty(), 8)),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(900), new javafx.animation.KeyValue(glow.radiusProperty(), 28))
            );
            glowTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            glowTimeline.setAutoReverse(true);

            javafx.animation.Timeline colorPulse = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(javafx.util.Duration.ZERO, ae -> { ae.consume(); bigTitle.setFill(javafx.scene.paint.Color.WHITE); }),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(420), ae -> { ae.consume(); bigTitle.setFill(javafx.scene.paint.Color.web("#ffd166")); }),
                new javafx.animation.KeyFrame(javafx.util.Duration.millis(900), ae -> { ae.consume(); bigTitle.setFill(javafx.scene.paint.Color.WHITE); })
            );
            colorPulse.setCycleCount(javafx.animation.Animation.INDEFINITE);

            javafx.animation.ParallelTransition combined = new javafx.animation.ParallelTransition(scalePulse, glowTimeline, colorPulse);
            combined.setCycleCount(javafx.animation.Animation.INDEFINITE);
            activePulse = combined;
            combined.play();
        } catch (Exception ignored) {}

                javafx.scene.text.Text modeDesc = new javafx.scene.text.Text("Survive. Outlast. Win.");
                modeDesc.setStyle("-fx-font-size: 18px; -fx-fill: #d0d0d0; -fx-text-alignment: center;");
                modeDesc.setWrappingWidth(680);

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
                            try { stopMatchGameOverSound(); } catch (Exception ignored) {}
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