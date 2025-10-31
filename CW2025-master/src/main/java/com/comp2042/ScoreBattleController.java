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
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.BorderPane;
// HBox referenced via fully-qualified name in method to avoid unused-import warnings
import java.util.prefs.Preferences;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;

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

    // background music player for score battle
    private MediaPlayer scoreBattleMusicPlayer = null;
    // centralized countdown player for multiplayer (played once for both GUIs)
    private MediaPlayer matchCountdownPlayer = null;
    // fallback Clip if JavaFX Media fails for WAV playback
    private Clip matchCountdownClipFallback = null;
    // single-shot game-over player for multiplayer (plays GameOver.wav once)
    private MediaPlayer matchGameOverPlayer = null;
    // fallback Clip if JavaFX Media fails for WAV playback
    private Clip matchGameOverClipFallback = null;

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
     * Play the centralized GameOver sound for the match (single-shot).
     */
    private void playMatchGameOverSound() {
        try {
            // if already playing, restart/cleanup
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
                return;
            }
        } catch (Exception ignored) {}

        // Fallback using javax.sound Clip for WAV files
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

    /**
     * Stop and dispose the centralized match game-over sound if playing.
     */
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

    /**
     * Play the centralized countdown sound for the match (loops while countdown visuals are active).
     */
    private void playMatchCountdownSound() {
        try {
            // cleanup any previous player
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

        // Fallback using javax.sound Clip for WAV files
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

    /**
     * Stop and dispose the centralized countdown sound if playing.
     */
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

    /**
     * Show a combined controls overlay allowing both players to edit their keybindings.
     * The requesting GuiController is provided so we can keep pause state consistent.
     */
    private void showMultiplayerControlsOverlay(GuiController requester) {
        javafx.application.Platform.runLater(() -> {
                try {
                    // Do NOT stop match music when opening the Controls overlay; allow music to continue playing
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

                // Top bar with title and Save/Cancel
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

                // Load two Controls panes side-by-side (left/right)
                // Increase spacing so the left and right player panels are visually separated
                javafx.scene.layout.HBox center = new javafx.scene.layout.HBox(120);
                center.setStyle("-fx-padding:12; -fx-background-color: transparent;");
                center.setAlignment(Pos.CENTER);

                FXMLLoader leftFx = new FXMLLoader(getClass().getClassLoader().getResource("controls.fxml"));
                javafx.scene.layout.StackPane leftPane = leftFx.load();
                ControlsController leftCC = leftFx.getController();

                FXMLLoader rightFx = new FXMLLoader(getClass().getClassLoader().getResource("controls.fxml"));
                javafx.scene.layout.StackPane rightPane = rightFx.load();
                ControlsController rightCC = rightFx.getController();

                // Initialize each controls pane with the current keys from their GuiControllers
                // Pre-read Preferences so both left/right initialization blocks can reuse them
                Preferences overlayPrefs = Preferences.userNodeForPackage(com.comp2042.MainMenuController.class);
                try {
                    leftCC.init(leftGui.getCtrlMoveLeft() != null ? leftGui.getCtrlMoveLeft() : javafx.scene.input.KeyCode.A,
                                leftGui.getCtrlMoveRight() != null ? leftGui.getCtrlMoveRight() : javafx.scene.input.KeyCode.D,
                                leftGui.getCtrlRotate() != null ? leftGui.getCtrlRotate() : javafx.scene.input.KeyCode.W,
                                leftGui.getCtrlSoftDrop() != null ? leftGui.getCtrlSoftDrop() : javafx.scene.input.KeyCode.S,
                                leftGui.getCtrlHardDrop() != null ? leftGui.getCtrlHardDrop() : javafx.scene.input.KeyCode.SHIFT,
                                leftGui.getCtrlSwap() != null ? leftGui.getCtrlSwap() : javafx.scene.input.KeyCode.Q);

                    // Determine panel default keys from persisted preferences where available so the
                    // Default column reflects what the user saved previously (fall back to WASD)
                    javafx.scene.input.KeyCode defLLeft = null;
                    javafx.scene.input.KeyCode defLRight = null;
                    javafx.scene.input.KeyCode defLRotate = null;
                    javafx.scene.input.KeyCode defLDown = null;
                    javafx.scene.input.KeyCode defLHard = null;
                    javafx.scene.input.KeyCode defLSwap = null;
                    try { String s = overlayPrefs.get("mpLeft_left", ""); if (!s.isEmpty()) defLLeft = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_right", ""); if (!s.isEmpty()) defLRight = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_rotate", ""); if (!s.isEmpty()) defLRotate = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_down", ""); if (!s.isEmpty()) defLDown = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_hard", ""); if (!s.isEmpty()) defLHard = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpLeft_switch", ""); if (!s.isEmpty()) defLSwap = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}

                    leftCC.setDefaultKeys(
                        defLLeft != null ? defLLeft : javafx.scene.input.KeyCode.A,
                        defLRight != null ? defLRight : javafx.scene.input.KeyCode.D,
                        defLRotate != null ? defLRotate : javafx.scene.input.KeyCode.W,
                        defLDown != null ? defLDown : javafx.scene.input.KeyCode.S,
                        defLHard != null ? defLHard : javafx.scene.input.KeyCode.SHIFT,
                        defLSwap != null ? defLSwap : javafx.scene.input.KeyCode.Q
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

                    // Use persisted preferences for right-player panel defaults if present, otherwise
                    // fall back to the traditional arrow-key defaults (so menu and in-game overlays match).
                    javafx.scene.input.KeyCode defRLeft = null;
                    javafx.scene.input.KeyCode defRRight = null;
                    javafx.scene.input.KeyCode defRRotate = null;
                    javafx.scene.input.KeyCode defRDown = null;
                    javafx.scene.input.KeyCode defRHard = null;
                    javafx.scene.input.KeyCode defRSwap = null;
                    try { String s = overlayPrefs.get("mpRight_left", ""); if (!s.isEmpty()) defRLeft = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_right", ""); if (!s.isEmpty()) defRRight = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_rotate", ""); if (!s.isEmpty()) defRRotate = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_down", ""); if (!s.isEmpty()) defRDown = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_hard", ""); if (!s.isEmpty()) defRHard = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}
                    try { String s = overlayPrefs.get("mpRight_switch", ""); if (!s.isEmpty()) defRSwap = javafx.scene.input.KeyCode.valueOf(s); } catch (Exception ignored) {}

                    rightCC.setDefaultKeys(
                        defRLeft != null ? defRLeft : javafx.scene.input.KeyCode.LEFT,
                        defRRight != null ? defRRight : javafx.scene.input.KeyCode.RIGHT,
                        defRRotate != null ? defRRotate : javafx.scene.input.KeyCode.UP,
                        defRDown != null ? defRDown : javafx.scene.input.KeyCode.DOWN,
                        defRHard != null ? defRHard : javafx.scene.input.KeyCode.SPACE,
                        defRSwap != null ? defRSwap : javafx.scene.input.KeyCode.C
                    );
                    rightCC.setHeaderText("Right Player Controls");
                // Prevent duplicate key assignments between the two panels: consult the other pane's current keys
                try {
                    leftCC.setKeyAvailabilityChecker((code, btn) -> {
                        try {
                            // reference btn to satisfy static analyzers
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

                // Hide embedded action buttons since we provide top Save/Cancel
                try { leftCC.hideActionButtons(); } catch (Exception ignored) {}
                try { rightCC.hideActionButtons(); } catch (Exception ignored) {}

                // Ensure each pane has a reasonable width so spacing appears consistent
                try { leftPane.setPrefWidth(520); } catch (Exception ignored) {}
                try { rightPane.setPrefWidth(520); } catch (Exception ignored) {}
                // Place panes into center HBox with the larger gap between them
                center.getChildren().addAll(leftPane, rightPane);
                container.setCenter(center);

                overlay.getChildren().addAll(dark, container);

                // Add overlay to scene root and hide any existing GLOBAL_PAUSE_OVERLAY nodes
                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    // hide existing global pause overlays so pause stays visible beneath
                    java.util.List<javafx.scene.Node> hidden = new java.util.ArrayList<>();
                    for (javafx.scene.Node n : new java.util.ArrayList<>(root.getChildren())) {
                        if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) {
                            n.setVisible(false);
                            hidden.add(n);
                        }
                    }
                    // store hidden nodes so we can restore them later
                    overlay.getProperties().put("hiddenPauseNodes", hidden);
                    root.getChildren().add(overlay);
                }

                // Reset top action: reset both panes to their panel defaults
                btnResetTop.setOnAction(ev -> {
                    ev.consume();
                    try { leftCC.resetToPanelDefaults(); } catch (Exception ignored) {}
                    try { rightCC.resetToPanelDefaults(); } catch (Exception ignored) {}
                });

                // Save action: apply changes to GUIs and persist per-player preferences
                btnSave.setOnAction(ev -> {
                    ev.consume();
                    try {
                        // left player
                        try {
                            javafx.scene.input.KeyCode lLeft = leftCC.getLeft();
                            javafx.scene.input.KeyCode lRight = leftCC.getRight();
                            javafx.scene.input.KeyCode lRotate = leftCC.getRotate();
                            javafx.scene.input.KeyCode lDown = leftCC.getDown();
                            javafx.scene.input.KeyCode lHard = leftCC.getHard();
                            javafx.scene.input.KeyCode lSwap = leftCC.getSwitch();
                            // update running GUI
                            leftGui.setControlKeys(lLeft, lRight, lRotate, lDown, lHard);
                            leftGui.setSwapKey(lSwap);
                            // persist
                            Preferences prefs = Preferences.userNodeForPackage(com.comp2042.MainMenuController.class);
                            prefs.put("mpLeft_left", lLeft != null ? lLeft.name() : "");
                            prefs.put("mpLeft_right", lRight != null ? lRight.name() : "");
                            prefs.put("mpLeft_rotate", lRotate != null ? lRotate.name() : "");
                            prefs.put("mpLeft_down", lDown != null ? lDown.name() : "");
                            prefs.put("mpLeft_hard", lHard != null ? lHard.name() : "");
                            prefs.put("mpLeft_switch", lSwap != null ? lSwap.name() : "");
                        } catch (Exception ignored) {}
                        // right player
                        try {
                            javafx.scene.input.KeyCode rLeft = rightCC.getLeft();
                            javafx.scene.input.KeyCode rRight = rightCC.getRight();
                            javafx.scene.input.KeyCode rRotate = rightCC.getRotate();
                            javafx.scene.input.KeyCode rDown = rightCC.getDown();
                            javafx.scene.input.KeyCode rHard = rightCC.getHard();
                            javafx.scene.input.KeyCode rSwap = rightCC.getSwitch();
                            rightGui.setControlKeys(rLeft, rRight, rRotate, rDown, rHard);
                            rightGui.setSwapKey(rSwap);
                            Preferences prefs = Preferences.userNodeForPackage(com.comp2042.MainMenuController.class);
                            prefs.put("mpRight_left", rLeft != null ? rLeft.name() : "");
                            prefs.put("mpRight_right", rRight != null ? rRight.name() : "");
                            prefs.put("mpRight_rotate", rRotate != null ? rRotate.name() : "");
                            prefs.put("mpRight_down", rDown != null ? rDown.name() : "");
                            prefs.put("mpRight_hard", rHard != null ? rHard.name() : "");
                            prefs.put("mpRight_switch", rSwap != null ? rSwap.name() : "");
                        } catch (Exception ignored) {}
                    } catch (Exception ignored) {}
                    // Close overlay and restore pause nodes
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

    /**
     * Restart the multiplayer match: reset both game models and run a synchronized countdown.
     * Safe to call from any thread (will post to JavaFX Application Thread as needed).
     */
    public void restartMatch() {
        javafx.application.Platform.runLater(() -> {
            try {
                // stop any playing match music before restarting
                try { if (scoreBattleMusicPlayer != null) { scoreBattleMusicPlayer.stop(); scoreBattleMusicPlayer.dispose(); scoreBattleMusicPlayer = null; } } catch (Exception ignored) {}
                try { stopMatchCountdownSound(); } catch (Exception ignored) {}
                try { stopMatchGameOverSound(); } catch (Exception ignored) {}
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

    /**
     * Initialize both embedded games and apply per-player swap keys.
     * If a swap key is null the controller will fall back to the previous defaults (Left: Q, Right: C).
     */
    public void initBothGames(javafx.scene.input.KeyCode leftSwap, javafx.scene.input.KeyCode rightSwap) throws IOException {
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
                        // stop match music immediately so we can play a game-over track later
                        try { if (scoreBattleMusicPlayer != null) { scoreBattleMusicPlayer.stop(); scoreBattleMusicPlayer.dispose(); scoreBattleMusicPlayer = null; } } catch (Exception ignored) {}
                        // play centralized match GameOver sound
                        try { playMatchGameOverSound(); } catch (Exception ignored) {}
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
                        // stop match music immediately so we can play a game-over track later
                        try { if (scoreBattleMusicPlayer != null) { scoreBattleMusicPlayer.stop(); scoreBattleMusicPlayer.dispose(); scoreBattleMusicPlayer = null; } } catch (Exception ignored) {}
                        // play centralized match GameOver sound
                        try { playMatchGameOverSound(); } catch (Exception ignored) {}
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

            // identify each GUI so in-game control saves persist the correct mpLeft_/mpRight_ keys
            try { leftGui.setMultiplayerPlayerId("left"); } catch (Exception ignored) {}
            try { rightGui.setMultiplayerPlayerId("right"); } catch (Exception ignored) {}
            // Allow the coordinator to present a combined multiplayer controls UI
            try { leftGui.setMultiplayerRequestControlsHandler(this::showMultiplayerControlsOverlay); } catch (Exception ignored) {}
            try { rightGui.setMultiplayerRequestControlsHandler(this::showMultiplayerControlsOverlay); } catch (Exception ignored) {}

            // register a restart handler so an embedded GuiController's Retry can request a full match restart
            try { leftGui.setMultiplayerRestartHandler(this::restartMatch); } catch (Exception ignored) {}
            try { rightGui.setMultiplayerRestartHandler(this::restartMatch); } catch (Exception ignored) {}
            // register a handler so an embedded GuiController's Main Menu button can delegate
            // the navigation to the multiplayer coordinator which is responsible for stopping
            // the shared score-battle music player.
            try { leftGui.setMultiplayerExitToMenuHandler(() -> onBack(null)); } catch (Exception ignored) {}
            try { rightGui.setMultiplayerExitToMenuHandler(() -> onBack(null)); } catch (Exception ignored) {}

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
        // Attach listeners to start a single shared countdown sound for both embedded GUIs
        try {
            final javafx.beans.value.ChangeListener<Boolean> startCountdownListener = (obs, oldV, newV) -> {
                try {
                    java.util.Objects.requireNonNull(obs);
                    java.util.Objects.requireNonNull(oldV);
                    java.util.Objects.requireNonNull(newV);
                    boolean l = false, r = false;
                    try { l = leftGui.countdownStartedProperty().get(); } catch (Exception ignored) {}
                    try { r = rightGui.countdownStartedProperty().get(); } catch (Exception ignored) {}
                    // if either GUI started the countdown, ensure the coordinator starts the shared countdown sound
                    if (l || r) {
                        try { playMatchCountdownSound(); } catch (Exception ignored) {}
                    } else {
                        // both not started -> stop any shared countdown audio
                        try { stopMatchCountdownSound(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            };
            try { leftGui.countdownStartedProperty().addListener(startCountdownListener); } catch (Exception ignored) {}
            try { rightGui.countdownStartedProperty().addListener(startCountdownListener); } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        leftGui.startCountdown(3);
        rightGui.startCountdown(3);

        // configure per-player controls so each board only responds to its assigned keys
        try {
            // Attempt to load any persisted multiplayer overrides from Preferences so saved settings
            // apply automatically when the match starts. Fall back to sensible defaults when
            // values are missing or invalid.
            Preferences prefs = Preferences.userNodeForPackage(com.comp2042.MainMenuController.class);

            // left player keys
            javafx.scene.input.KeyCode lLeft = null;
            javafx.scene.input.KeyCode lRight = null;
            javafx.scene.input.KeyCode lRotate = null;
            javafx.scene.input.KeyCode lDown = null;
            javafx.scene.input.KeyCode lHard = null;
            try {
                String s = prefs.get("mpLeft_left", ""); if (!s.isEmpty()) lLeft = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}
            try {
                String s = prefs.get("mpLeft_right", ""); if (!s.isEmpty()) lRight = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}
            try {
                String s = prefs.get("mpLeft_rotate", ""); if (!s.isEmpty()) lRotate = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}
            try {
                String s = prefs.get("mpLeft_down", ""); if (!s.isEmpty()) lDown = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}
            try {
                String s = prefs.get("mpLeft_hard", ""); if (!s.isEmpty()) lHard = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}

            // right player keys
            javafx.scene.input.KeyCode rLeft = null;
            javafx.scene.input.KeyCode rRight = null;
            javafx.scene.input.KeyCode rRotate = null;
            javafx.scene.input.KeyCode rDown = null;
            javafx.scene.input.KeyCode rHard = null;
            try {
                String s = prefs.get("mpRight_left", ""); if (!s.isEmpty()) rLeft = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}
            try {
                String s = prefs.get("mpRight_right", ""); if (!s.isEmpty()) rRight = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}
            try {
                String s = prefs.get("mpRight_rotate", ""); if (!s.isEmpty()) rRotate = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}
            try {
                String s = prefs.get("mpRight_down", ""); if (!s.isEmpty()) rDown = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}
            try {
                String s = prefs.get("mpRight_hard", ""); if (!s.isEmpty()) rHard = javafx.scene.input.KeyCode.valueOf(s);
            } catch (Exception ignored) {}

            // Apply left player keys (fall back to WASD + SHIFT)
            leftGui.setControlKeys(
                    lLeft != null ? lLeft : javafx.scene.input.KeyCode.A,
                    lRight != null ? lRight : javafx.scene.input.KeyCode.D,
                    lRotate != null ? lRotate : javafx.scene.input.KeyCode.W,
                    lDown != null ? lDown : javafx.scene.input.KeyCode.S,
                    lHard != null ? lHard : javafx.scene.input.KeyCode.SHIFT
            );
            leftGui.setSwapKey(leftSwap != null ? leftSwap : javafx.scene.input.KeyCode.Q);

            // Apply right player keys (fall back to NumPad defaults)
            rightGui.setControlKeys(
                    rLeft != null ? rLeft : javafx.scene.input.KeyCode.NUMPAD4,
                    rRight != null ? rRight : javafx.scene.input.KeyCode.NUMPAD6,
                    rRotate != null ? rRotate : javafx.scene.input.KeyCode.NUMPAD8,
                    rDown != null ? rDown : javafx.scene.input.KeyCode.NUMPAD5,
                    rHard != null ? rHard : javafx.scene.input.KeyCode.SPACE
            );
            rightGui.setSwapKey(rightSwap != null ? rightSwap : javafx.scene.input.KeyCode.C);
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

        // Ensure match music starts when both countdowns finish (and only then)
        scheduleStartMusicWhenCountdownsDone();

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
                // cleanup embedded GUIs so they detach their scene handlers and stop timelines/music
                try { if (leftGui != null) leftGui.cleanup(); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.cleanup(); } catch (Exception ignored) {}
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
    try { if (scoreBattleMusicPlayer != null) { scoreBattleMusicPlayer.stop(); scoreBattleMusicPlayer.dispose(); scoreBattleMusicPlayer = null; } } catch (Exception ignored) {}
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

    /**
     * Ensure the match timer and background music start only once both embedded GUI countdowns finish.
     * This method attaches listeners to each GUI's countdownFinishedProperty and will start the
     * match timer and create/play a looping MediaPlayer when both are ready. Listeners remain
     * attached so restarting a match (which restarts countdowns) will also restart music/timer.
     */
    private void scheduleStartMusicWhenCountdownsDone() {
        try {
            if (leftGui == null || rightGui == null) return;

            final javafx.beans.value.ChangeListener<Boolean> bothFinishedListener = (obs, oldV, newV) -> {
                try {
                    java.util.Objects.requireNonNull(obs);
                    java.util.Objects.requireNonNull(oldV);
                    java.util.Objects.requireNonNull(newV);
                    boolean l = false, r = false;
                    try { l = leftGui.countdownFinishedProperty().get(); } catch (Exception ignored) {}
                    try { r = rightGui.countdownFinishedProperty().get(); } catch (Exception ignored) {}
                    if (l && r) {
                        // start match timer if not already running
                        try {
                            if (matchTimer != null && matchTimer.getStatus() != javafx.animation.Animation.Status.RUNNING) {
                                matchTimer.play();
                            }
                        } catch (Exception ignored) {}

                        // start or resume match music and ensure it loops indefinitely
                        try {
                            if (scoreBattleMusicPlayer == null) {
                                URL musicUrl = getClass().getClassLoader().getResource("sounds/ScoreBattle.wav");
                                if (musicUrl == null) musicUrl = getClass().getClassLoader().getResource("sounds/ScoreBattle.mp3");
                                if (musicUrl != null) {
                                    Media m = new Media(musicUrl.toExternalForm());
                                    scoreBattleMusicPlayer = new MediaPlayer(m);
                                    scoreBattleMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                                    scoreBattleMusicPlayer.setAutoPlay(true);
                                }
                            } else {
                                try { scoreBattleMusicPlayer.play(); } catch (Exception ignored) {}
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception ignored) {}
            };

            // attach listener to both countdownFinished properties
            try { leftGui.countdownFinishedProperty().addListener(bothFinishedListener); } catch (Exception ignored) {}
            try { rightGui.countdownFinishedProperty().addListener(bothFinishedListener); } catch (Exception ignored) {}

            // If both already finished (edge case), trigger immediately
            try {
                if (leftGui.countdownFinishedProperty().get() && rightGui.countdownFinishedProperty().get()) {
                    bothFinishedListener.changed(null, Boolean.FALSE, Boolean.TRUE);
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private void endMatchAndAnnounceWinner() {
        // stop any match music immediately
        try { if (scoreBattleMusicPlayer != null) { scoreBattleMusicPlayer.stop(); scoreBattleMusicPlayer.dispose(); scoreBattleMusicPlayer = null; } } catch (Exception ignored) {}
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
