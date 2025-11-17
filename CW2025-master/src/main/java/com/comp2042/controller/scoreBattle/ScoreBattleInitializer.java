package com.comp2042.controller.scoreBattle;

import com.comp2042.controller.gameControl.GameController;
import com.comp2042.controller.guiControl.GuiController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.scene.input.KeyCode;

import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.prefs.Preferences;

/**
 * Extracted initializer for ScoreBattleController.initBothGames.
 * Uses reflection to set private fields on the controller so callers
 * can remain unchanged while logic lives in this helper class.
 */
public final class ScoreBattleInitializer {

    private ScoreBattleInitializer() {}

    public static void initBothGames(ScoreBattleController ctrl, KeyCode leftSwap, KeyCode rightSwap) throws IOException {
        // Use locals and reflection only where we need to read/write controller fields
        URL gameLayout = ScoreBattleInitializer.class.getClassLoader().getResource("gameLayout.fxml");
        FXMLLoader leftLoader = new FXMLLoader(gameLayout);
        Parent leftRoot = leftLoader.load();
        GuiController leftGui = leftLoader.getController();

        FXMLLoader rightLoader = new FXMLLoader(gameLayout);
        Parent rightRoot = rightLoader.load();
        GuiController rightGui = rightLoader.getController();

        // Ensure embedded roots do NOT apply the global '.root' background or load stylesheets
        try {
            leftRoot.getStylesheets().clear();
            rightRoot.getStylesheets().clear();
            leftRoot.getStyleClass().remove("root");
            rightRoot.getStyleClass().remove("root");
            String transparent = "-fx-background-color: transparent;";
            leftRoot.setStyle(transparent);
            rightRoot.setStyle(transparent);
        } catch (Exception ignored) {}

        // hide certain embedded UI nodes via lookup (mirrors previous behaviour)
        try {
            java.util.function.Consumer<Parent> hideEmbeddedUi = (root) -> {
                try {
                    javafx.scene.Node n = root.lookup("#pauseBtn");
                    if (n != null) { n.setVisible(false); n.setManaged(false); }
                    n = root.lookup("#nextBoxFrame");
                    if (n != null) { n.setVisible(false); n.setManaged(false); }
                    javafx.scene.Node nb = root.lookup("#nextBox");
                    if (nb != null) { nb.setVisible(false); nb.setManaged(false); }
                } catch (Exception e) { e.printStackTrace(); }
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

        // attach subscenes to holders retrieved from controller
        try {
            if (ctrl.leftHolder != null) {
                ctrl.leftHolder.setAlignment(javafx.geometry.Pos.CENTER);
                ctrl.leftHolder.getChildren().add(leftSub);
            }
            if (ctrl.rightHolder != null) {
                ctrl.rightHolder.setAlignment(javafx.geometry.Pos.CENTER);
                ctrl.rightHolder.getChildren().add(rightSub);
            }
        } catch (Exception ignored) {}

        try {
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
        } catch (Exception ignored) {}

        // start controllers
        GameController leftController = new GameController(leftGui);
        GameController rightController = new GameController(rightGui);

        // write core GUI/controller references back to the ScoreBattleController early so
        // any background pollers or listeners can access them reliably
        try {
            ctrl.leftGui = leftGui;
            ctrl.rightGui = rightGui;
            ctrl.leftController = leftController;
            ctrl.rightController = rightController;
        } catch (Exception ignored) {}

        // create listener placeholders and attach them to GUI properties as before
        javafx.beans.value.ChangeListener<Boolean> leftIsGameOverListener = (obs, oldV, newV) -> {
            try {
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
                if (newV == Boolean.TRUE) {
                    // if match already ended, ignore
                    Boolean ended = false;
                    try { Object me = getField(ctrl, "matchEnded"); if (me instanceof Boolean) ended = (Boolean) me; } catch (Exception ignored) {}
                    if (ended) return;
                    // mark ended
                    try { setField(ctrl, "matchEnded", true); } catch (Exception ignored) {}

                    // stop timers and pollers
                    try { Timeline mt = (Timeline) getField(ctrl, "matchTimer"); if (mt != null) mt.stop(); } catch (Exception ignored) {}
                    try { Timeline pp = (Timeline) getField(ctrl, "previewPoller"); if (pp != null) pp.stop(); } catch (Exception ignored) {}

                    // ensure both GUIs enter game-over state
                    try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
                    try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}

                    // stop any match music then play game-over sound
                    try { runMethod(ctrl, "stopMatchCountdownSound"); } catch (Exception ignored) {}
                    // stop main match music player if present to avoid overlap
                    try {
                        Object sb = getField(ctrl, "scoreBattleMusicPlayer");
                        if (sb != null) {
                            try { ((javafx.scene.media.MediaPlayer) sb).stop(); } catch (Exception ignored) {}
                            try { ((javafx.scene.media.MediaPlayer) sb).dispose(); } catch (Exception ignored) {}
                            try { setField(ctrl, "scoreBattleMusicPlayer", null); } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                    // ask embedded GUIs to stop any overlay audio they may be playing
                    try { if (leftGui != null) leftGui.stopOverlayAudio(); } catch (Exception ignored) {}
                    try { if (rightGui != null) rightGui.stopOverlayAudio(); } catch (Exception ignored) {}
                    try { runMethod(ctrl, "playMatchGameOverSound"); } catch (Exception ignored) {}

                    // compute scores and show winner overlay (right wins when left lost)
                    int lscore = (leftController != null) ? leftController.getScoreProperty().get() : 0;
                    int rscore = (rightController != null) ? rightController.getScoreProperty().get() : 0;
                    String reason = "Winner by survival (opponent lost)";
                    if (lscore > rscore) reason += String.format(" — opponent had higher score (%d vs %d)", lscore, rscore);
                    try { runMethod(ctrl, "showWinnerOverlay", "Right Player Wins!", lscore, rscore, reason); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        };
        try { leftGui.isGameOverProperty().addListener(leftIsGameOverListener); } catch (Exception ignored) {}

        javafx.beans.value.ChangeListener<Boolean> rightIsGameOverListener = (obs, oldV, newV) -> {
            try {
                java.util.Objects.requireNonNull(obs);
                java.util.Objects.requireNonNull(oldV);
                if (newV == Boolean.TRUE) {
                    Boolean ended = false;
                    try { Object me = getField(ctrl, "matchEnded"); if (me instanceof Boolean) ended = (Boolean) me; } catch (Exception ignored) {}
                    if (ended) return;
                    try { setField(ctrl, "matchEnded", true); } catch (Exception ignored) {}

                    try { Timeline mt = (Timeline) getField(ctrl, "matchTimer"); if (mt != null) mt.stop(); } catch (Exception ignored) {}
                    try { Timeline pp = (Timeline) getField(ctrl, "previewPoller"); if (pp != null) pp.stop(); } catch (Exception ignored) {}

                    try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
                    try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}

                    try { runMethod(ctrl, "stopMatchCountdownSound"); } catch (Exception ignored) {}
                        // stop main match music player if present to avoid overlap
                        try {
                            Object sb = getField(ctrl, "scoreBattleMusicPlayer");
                            if (sb != null) {
                                try { ((javafx.scene.media.MediaPlayer) sb).stop(); } catch (Exception ignored) {}
                                try { ((javafx.scene.media.MediaPlayer) sb).dispose(); } catch (Exception ignored) {}
                                try { setField(ctrl, "scoreBattleMusicPlayer", null); } catch (Exception ignored) {}
                            }
                        } catch (Exception ignored) {}
                        try { if (leftGui != null) leftGui.stopOverlayAudio(); } catch (Exception ignored) {}
                        try { if (rightGui != null) rightGui.stopOverlayAudio(); } catch (Exception ignored) {}
                        try { runMethod(ctrl, "playMatchGameOverSound"); } catch (Exception ignored) {}

                    int lscore = (leftController != null) ? leftController.getScoreProperty().get() : 0;
                    int rscore = (rightController != null) ? rightController.getScoreProperty().get() : 0;
                    String reason = "Winner by survival (opponent lost)";
                    if (rscore > lscore) reason += String.format(" — opponent had higher score (%d vs %d)", rscore, lscore);
                    try { runMethod(ctrl, "showWinnerOverlay", "Left Player Wins!", lscore, rscore, reason); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        };
        try { rightGui.isGameOverProperty().addListener(rightIsGameOverListener); } catch (Exception ignored) {}

        try { leftGui.setMultiplayerMode(true); } catch (Exception ignored) {}
        try { rightGui.setMultiplayerMode(true); } catch (Exception ignored) {}

        try { leftGui.setMultiplayerPlayerId("left"); } catch (Exception ignored) {}
        try { rightGui.setMultiplayerPlayerId("right"); } catch (Exception ignored) {}

        try { leftGui.setMultiplayerRequestControlsHandler(requester -> { try { showMultiplayerControls(ctrl, requester); } catch (Exception ignored) {} }); } catch (Exception ignored) {}
        try { rightGui.setMultiplayerRequestControlsHandler(requester -> { try { showMultiplayerControls(ctrl, requester); } catch (Exception ignored) {} }); } catch (Exception ignored) {}

        try { leftGui.setMultiplayerRestartHandler(() -> { try { ctrl.restartMatch(); } catch (Exception ignored) {} }); } catch (Exception ignored) {}
        try { rightGui.setMultiplayerRestartHandler(() -> { try { ctrl.restartMatch(); } catch (Exception ignored) {} }); } catch (Exception ignored) {}

        try { leftGui.setMultiplayerExitToMenuHandler(() -> { try { ctrl.onBack(null); } catch (Exception ignored) {} }); } catch (Exception ignored) {}
        try { rightGui.setMultiplayerExitToMenuHandler(() -> { try { ctrl.onBack(null); } catch (Exception ignored) {} }); } catch (Exception ignored) {}

        // multiplayer pause handlers forward to the other GUI and pause match-level timelines
        try {
            leftGui.setMultiplayerPauseHandler(paused -> {
                try {
                    if (rightGui != null) rightGui.applyExternalPause(paused);
                    if (paused) {
                        if (ctrl.matchTimer != null) ctrl.matchTimer.pause();
                        if (ctrl.previewPoller != null) ctrl.previewPoller.pause();
                    } else {
                        if (ctrl.matchTimer != null) ctrl.matchTimer.play();
                        if (ctrl.previewPoller != null) ctrl.previewPoller.play();
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}

        try {
            rightGui.setMultiplayerPauseHandler(paused -> {
                try {
                    if (leftGui != null) leftGui.applyExternalPause(paused);
                    if (paused) {
                        if (ctrl.matchTimer != null) ctrl.matchTimer.pause();
                        if (ctrl.previewPoller != null) ctrl.previewPoller.pause();
                    } else {
                        if (ctrl.matchTimer != null) ctrl.matchTimer.play();
                        if (ctrl.previewPoller != null) ctrl.previewPoller.play();
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}

        // Attach a shared countdown-started listener so the coordinator can play shared
        // countdown audio when either board starts counting down.
        try {
            javafx.beans.value.ChangeListener<Boolean> sharedCountdownStartedListener = (obs, oldV, newV) -> {
                try {
                    boolean l = false, r = false;
                    try { l = leftGui.countdownStartedProperty().get(); } catch (Exception ignored) {}
                    try { r = rightGui.countdownStartedProperty().get(); } catch (Exception ignored) {}
                    if (l || r) {
                        try { runMethod(ctrl, "playMatchCountdownSound"); } catch (Exception ignored) {}
                    } else {
                        try { runMethod(ctrl, "stopMatchCountdownSound"); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            };
            try { leftGui.countdownStartedProperty().addListener(sharedCountdownStartedListener); } catch (Exception ignored) {}
            try { rightGui.countdownStartedProperty().addListener(sharedCountdownStartedListener); } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        try {
            // set level text and drop intervals
            leftGui.setLevelText("Score Battle");
            rightGui.setLevelText("Score Battle");
            leftGui.setDropIntervalMs(1000);
            rightGui.setDropIntervalMs(1000);
        } catch (Exception ignored) {}

        // set controls from preferences
        try {
            Preferences prefs = Preferences.userNodeForPackage(com.comp2042.controller.mainMenu.MainMenuController.class);
            // left player keys
            KeyCode lLeft = null, lRight = null, lRotate = null, lDown = null, lHard = null;
            try { String s = prefs.get("mpLeft_left", ""); if (!s.isEmpty()) lLeft = KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpLeft_right", ""); if (!s.isEmpty()) lRight = KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpLeft_rotate", ""); if (!s.isEmpty()) lRotate = KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpLeft_down", ""); if (!s.isEmpty()) lDown = KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpLeft_hard", ""); if (!s.isEmpty()) lHard = KeyCode.valueOf(s); } catch (Exception ignored) {}

            KeyCode rLeft = null, rRight = null, rRotate = null, rDown = null, rHard = null;
            try { String s = prefs.get("mpRight_left", ""); if (!s.isEmpty()) rLeft = KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpRight_right", ""); if (!s.isEmpty()) rRight = KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpRight_rotate", ""); if (!s.isEmpty()) rRotate = KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpRight_down", ""); if (!s.isEmpty()) rDown = KeyCode.valueOf(s); } catch (Exception ignored) {}
            try { String s = prefs.get("mpRight_hard", ""); if (!s.isEmpty()) rHard = KeyCode.valueOf(s); } catch (Exception ignored) {}

            leftGui.setControlKeys(lLeft != null ? lLeft : KeyCode.A, lRight != null ? lRight : KeyCode.D, lRotate != null ? lRotate : KeyCode.W, lDown != null ? lDown : KeyCode.S, lHard != null ? lHard : KeyCode.SHIFT);
            leftGui.setSwapKey(leftSwap != null ? leftSwap : KeyCode.Q);

            rightGui.setControlKeys(rLeft != null ? rLeft : KeyCode.NUMPAD4, rRight != null ? rRight : KeyCode.NUMPAD6, rRotate != null ? rRotate : KeyCode.NUMPAD8, rDown != null ? rDown : KeyCode.NUMPAD5, rHard != null ? rHard : KeyCode.SPACE);
            rightGui.setSwapKey(rightSwap != null ? rightSwap : KeyCode.C);
        } catch (Exception ignored) {}

        // Start countdowns on both embedded GUIs so they show the 3-2-1 effect and
        // delay piece activity until the countdown finishes.
        try {
            try { leftGui.startCountdown(3); } catch (Exception ignored) {}
            try { rightGui.startCountdown(3); } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        // Create and attach match center overlay and timer; use reflection to store on controller
        try {
            Scene scene = null;
            try { Object lh = getField(ctrl, "leftHolder"); if (lh instanceof StackPane) scene = ((StackPane) lh).getScene(); } catch (Exception ignored) {}
            if (scene != null) {
                ensureScoreBattleStylesheet(scene);
                StackPane centerOverlay = new StackPane();
                centerOverlay.setPickOnBounds(false);
                centerOverlay.setMouseTransparent(true);
                VBox v = new VBox(6);
                v.setAlignment(javafx.geometry.Pos.TOP_CENTER);
                Text matchTimerText = new Text(formatTime(((Integer) getField(ctrl, "remainingSeconds")).intValue()));
                matchTimerText.getStyleClass().add("score-battle-match-timer");
                Text matchScoreText = new Text("0  —  0");
                matchScoreText.getStyleClass().add("score-battle-match-score");
                v.getChildren().addAll(matchTimerText, matchScoreText);
                centerOverlay.getChildren().add(v);
                try {
                    Object rootObj = scene.getRoot();
                    if (rootObj instanceof Pane) {
                        Pane root = (Pane) rootObj;
                        root.getChildren().add(centerOverlay);
                        centerOverlay.prefWidthProperty().bind(scene.widthProperty());
                        centerOverlay.prefHeightProperty().bind(scene.heightProperty());
                        StackPane.setAlignment(v, javafx.geometry.Pos.TOP_CENTER);
                        v.translateYProperty().bind(scene.heightProperty().multiply(0.06));
                    }
                } catch (Exception ignored) {}
                // assign centerOverlay and timer texts back to controller via reflection
                setField(ctrl, "centerOverlay", centerOverlay);
                setField(ctrl, "matchTimerText", matchTimerText);
                setField(ctrl, "matchScoreText", matchScoreText);
            }
        } catch (Exception ignored) {}

        // create matchTimer (use reference array to allow lambda to stop it)
        final Timeline[] matchTimerRef = new Timeline[1];
        Timeline matchTimerLocal = new Timeline(new KeyFrame(Duration.seconds(1), _e -> {
            try {
                Field remF = ctrl.getClass().getDeclaredField("remainingSeconds"); remF.setAccessible(true);
                int remaining = remF.getInt(ctrl);
                remaining--;
                remF.setInt(ctrl, remaining);
                Text mt = (Text) getField(ctrl, "matchTimerText"); if (mt != null) mt.setText(formatTime(remaining));
                runMethod(ctrl, "updateMatchScoreText");
                if (remaining <= 0) {
                    if (matchTimerRef[0] != null) matchTimerRef[0].stop();
                    runMethod(ctrl, "endMatchAndAnnounceWinner");
                }
            } catch (Exception ignored) {}
        }));
        matchTimerLocal.setCycleCount(javafx.animation.Animation.INDEFINITE);
        matchTimerRef[0] = matchTimerLocal;

        // schedule music start listener wiring (reuse controller method)
        try { runMethod(ctrl, "scheduleStartMusicWhenCountdownsDone"); } catch (Exception ignored) {}

        // create previewPoller
        Timeline previewPoller = new Timeline(new KeyFrame(Duration.millis(300), _ev -> {
            try {
                Object lCtrlObj = getField(ctrl, "leftController");
                Object lGuiObj = getField(ctrl, "leftGui");
                if (lCtrlObj instanceof GameController && lGuiObj instanceof GuiController) {
                    java.util.List<com.comp2042.logic.Brick> up = ((GameController) lCtrlObj).getUpcomingBricks(3);
                    Object leftNextContent = getField(ctrl, "leftNextContent");
                    if (leftNextContent instanceof javafx.scene.layout.VBox) {
                        javafx.scene.layout.VBox built = ((GuiController) lGuiObj).buildNextPreview(up);
                        ((javafx.scene.layout.VBox) leftNextContent).getChildren().clear();
                        if (built != null) ((javafx.scene.layout.VBox) leftNextContent).getChildren().addAll(built.getChildren());
                    } else {
                        if (up != null) ((GuiController) lGuiObj).showNextBricks(up);
                    }
                }
                Object rCtrlObj = getField(ctrl, "rightController");
                Object rGuiObj = getField(ctrl, "rightGui");
                if (rCtrlObj instanceof GameController && rGuiObj instanceof GuiController) {
                    java.util.List<com.comp2042.logic.Brick> up2 = ((GameController) rCtrlObj).getUpcomingBricks(3);
                    Object rightNextContent = getField(ctrl, "rightNextContent");
                    if (rightNextContent instanceof javafx.scene.layout.VBox) {
                        javafx.scene.layout.VBox built2 = ((GuiController) rGuiObj).buildNextPreview(up2);
                        ((javafx.scene.layout.VBox) rightNextContent).getChildren().clear();
                        if (built2 != null) ((javafx.scene.layout.VBox) rightNextContent).getChildren().addAll(built2.getChildren());
                    } else {
                        if (up2 != null) ((GuiController) rGuiObj).showNextBricks(up2);
                    }
                }
            } catch (Exception e) { e.printStackTrace(); }
        }));
        previewPoller.setCycleCount(javafx.animation.Animation.INDEFINITE);

        // finally, write back timer/poller fields onto the ScoreBattleController and
        // start the preview poller now that controllers are registered.
        try {
            setField(ctrl, "matchTimer", matchTimerRef[0]);
            setField(ctrl, "previewPoller", previewPoller);
        } catch (Exception ignored) {}

        try { previewPoller.play(); } catch (Exception ignored) {}
    }

    private static void showMultiplayerControls(ScoreBattleController ctrl, GuiController requester) {
        try {
            // delegate to controller's existing private method via reflection
            runMethod(ctrl, "showMultiplayerControlsOverlay", requester);
        } catch (Exception ignored) {}
    }

    private static Object getField(Object target, String name) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static void runMethod(Object target, String name, Object... args) throws Exception {
        Class<?>[] types = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) types[i] = args[i] == null ? Object.class : args[i].getClass();
        try {
            java.lang.reflect.Method m = findMethod(target.getClass(), name, types);
            if (m != null) {
                m.setAccessible(true);
                m.invoke(target, args);
            }
        } catch (NoSuchMethodException nsme) {
            // ignore
        }
    }

    private static java.lang.reflect.Method findMethod(Class<?> cls, String name, Class<?>[] types) throws NoSuchMethodException {
        for (java.lang.reflect.Method m : cls.getDeclaredMethods()) {
            if (!m.getName().equals(name)) continue;
            if (m.getParameterCount() != types.length) continue;
            return m;
        }
        throw new NoSuchMethodException(name);
    }

    private static void ensureScoreBattleStylesheet(Scene scene) {
        try {
            if (scene == null) return;
            URL cssUrl = ScoreBattleInitializer.class.getClassLoader().getResource("css/score-battle.css");
            if (cssUrl != null) {
                String css = cssUrl.toExternalForm();
                if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
            }
        } catch (Exception ignored) {}
    }

    private static String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
