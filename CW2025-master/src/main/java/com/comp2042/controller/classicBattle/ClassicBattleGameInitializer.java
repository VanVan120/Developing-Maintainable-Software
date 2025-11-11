package com.comp2042.controller.classicBattle;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.fxml.FXMLLoader;

import java.io.IOException;
import java.net.URL;

import com.comp2042.controller.gameControl.GameController;

/**
 * Extracted initializer responsible for wiring up the two game sides of ClassicBattle.
 * This class performs the heavy-lifting previously implemented inside ClassicBattle.initBothGames(...)
 * and assigns the resulting controllers and GUI instances back to the owning ClassicBattle instance.
 */
public final class ClassicBattleGameInitializer {

    public void initialize(ClassicBattle owner, javafx.scene.input.KeyCode leftSwap, javafx.scene.input.KeyCode rightSwap) throws IOException {
        URL gameLayout = owner.getClass().getClassLoader().getResource("gameLayout.fxml");
        FXMLLoader leftLoader = new FXMLLoader(gameLayout);
        Parent leftRoot = leftLoader.load();
        owner.leftGui = leftLoader.getController();

        FXMLLoader rightLoader = new FXMLLoader(gameLayout);
        Parent rightRoot = rightLoader.load();
        owner.rightGui = rightLoader.getController();

        try {
            leftRoot.getStylesheets().clear();
            rightRoot.getStylesheets().clear();
            leftRoot.getStyleClass().remove("root");
            rightRoot.getStyleClass().remove("root");
            try {
                String css = owner.getClass().getClassLoader().getResource("css/classic-battle.css").toExternalForm();
                leftRoot.getStylesheets().add(css);
                rightRoot.getStylesheets().add(css);
                leftRoot.getStyleClass().add("transparent-root");
                rightRoot.getStyleClass().add("transparent-root");
            } catch (Exception ignoredCss) {}
        } catch (Exception ignored) {}

        try {
            if (owner.backBtn != null) {
                owner.backBtn.setVisible(false);
                owner.backBtn.setManaged(false);
            }
            owner.hideEmbeddedUi(leftRoot);
            owner.hideEmbeddedUi(rightRoot);
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

        owner.leftHolder.setAlignment(Pos.CENTER);
        owner.rightHolder.setAlignment(Pos.CENTER);

        owner.leftHolder.getChildren().add(leftSub);
        owner.rightHolder.getChildren().add(rightSub);

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

    owner.leftController = new GameController(owner.leftGui);
    owner.rightController = new GameController(owner.rightGui);
        try { owner.leftGui.setMultiplayerMode(true); } catch (Exception ignored) {}
        try { owner.rightGui.setMultiplayerMode(true); } catch (Exception ignored) {}
        try { owner.leftGui.hideScoreAndTimeUI(); } catch (Exception ignored) {}
        try { owner.rightGui.hideScoreAndTimeUI(); } catch (Exception ignored) {}
        try { owner.leftGui.setMultiplayerRestartHandler(owner::restartMatch); } catch (Exception ignored) {}
        try { owner.rightGui.setMultiplayerRestartHandler(owner::restartMatch); } catch (Exception ignored) {}
        try {
            owner.leftGui.setMultiplayerPauseHandler(paused -> {
                try {
                    if (owner.rightGui != null) owner.rightGui.applyExternalPause(paused);
                    if (paused) {
                        try { if (owner.previewService != null) owner.previewService.pause(); } catch (Exception ignored) {}
                    } else {
                        try { if (owner.previewService != null) owner.previewService.play(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
        try {
            owner.rightGui.setMultiplayerPauseHandler(paused -> {
                try {
                    if (owner.leftGui != null) owner.leftGui.applyExternalPause(paused);
                    if (paused) {
                        try { if (owner.previewService != null) owner.previewService.pause(); } catch (Exception ignored) {}
                    } else {
                        try { if (owner.previewService != null) owner.previewService.play(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}

        owner.leftGui.setLevelText("Classic Battle");
        owner.rightGui.setLevelText("Classic Battle");
        owner.leftGui.setDropIntervalMs(1000);
        owner.rightGui.setDropIntervalMs(1000);

        try {
            final javafx.beans.value.ChangeListener<Boolean> startCountdownListener = (obs, oldV, newV) -> {
                try {
                    java.util.Objects.requireNonNull(obs);
                    java.util.Objects.requireNonNull(oldV);
                    java.util.Objects.requireNonNull(newV);
                    boolean l = false, r = false;
                    try { l = owner.leftGui.countdownStartedProperty().get(); } catch (Exception ignored) {}
                    try { r = owner.rightGui.countdownStartedProperty().get(); } catch (Exception ignored) {}
                    if (l || r) {
                        try { owner.audioHelper.playMatchCountdownSound(); } catch (Exception ignored) {}
                    } else {
                        try { owner.audioHelper.stopMatchCountdownSound(); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            };
            try { owner.leftGui.countdownStartedProperty().addListener(startCountdownListener); } catch (Exception ignored) {}
            try { owner.rightGui.countdownStartedProperty().addListener(startCountdownListener); } catch (Exception ignored) {}
        } catch (Exception ignored) {}

        owner.leftGui.startCountdown(3);
        owner.rightGui.startCountdown(3);

        try {
            owner.leftGui.setControlKeys(javafx.scene.input.KeyCode.A, javafx.scene.input.KeyCode.D, javafx.scene.input.KeyCode.W, javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCode.SHIFT);
            owner.leftGui.setSwapKey(leftSwap != null ? leftSwap : javafx.scene.input.KeyCode.Q);
            owner.rightGui.setControlKeys(javafx.scene.input.KeyCode.NUMPAD4, javafx.scene.input.KeyCode.NUMPAD6, javafx.scene.input.KeyCode.NUMPAD8, javafx.scene.input.KeyCode.NUMPAD5, javafx.scene.input.KeyCode.SPACE);
            owner.rightGui.setSwapKey(rightSwap != null ? rightSwap : javafx.scene.input.KeyCode.C);
        } catch (Exception ignored) {}
        try {
            owner.leftController.setClearRowHandler(lines -> {
                try {
                    int l = (lines == null) ? 0 : lines.intValue();
                    if (l > 0 && owner.rightController != null) {
                        owner.rightController.addGarbageRows(l, -1);
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
        try {
            owner.rightController.setClearRowHandler(lines -> {
                try {
                    int l = (lines == null) ? 0 : lines.intValue();
                    if (l > 0 && owner.leftController != null) {
                        owner.leftController.addGarbageRows(l, -1);
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
        try {
            javafx.application.Platform.runLater(() -> {
                Scene scene = owner.leftHolder.getScene();
                if (scene == null) return;
                owner.ensureClassicStylesheet(scene);
                owner.centerOverlay = new javafx.scene.layout.StackPane();
                owner.centerOverlay.setPickOnBounds(false);
                owner.centerOverlay.setMouseTransparent(true);

                javafx.scene.layout.VBox v = new javafx.scene.layout.VBox(6);
                v.setAlignment(javafx.geometry.Pos.TOP_CENTER);

                owner.centerOverlay.getChildren().add(v);
            });
        } catch (Exception ignored) {}

        try {
            final javafx.beans.property.BooleanProperty leftDone = owner.leftGui.countdownFinishedProperty();
            final javafx.beans.property.BooleanProperty rightDone = owner.rightGui.countdownFinishedProperty();
            javafx.beans.InvalidationListener startWhenReady = _obs -> {
                java.util.Objects.requireNonNull(_obs);
                try {
                    if (leftDone.get() && rightDone.get()) {
                        try { if (owner.previewService != null) owner.previewService.play(); } catch (Exception ignoredEx) {}
                        try {
                            if (owner.classicBattleMusicPlayer == null) {
                                URL musicUrl = owner.getClass().getClassLoader().getResource("sounds/ClassicBattle.wav");
                                if (musicUrl == null) musicUrl = owner.getClass().getClassLoader().getResource("sounds/ClassicBattle.mp3");
                                if (musicUrl != null) {
                                    javafx.scene.media.Media m = new javafx.scene.media.Media(musicUrl.toExternalForm());
                                    owner.classicBattleMusicPlayer = new javafx.scene.media.MediaPlayer(m);
                                    owner.classicBattleMusicPlayer.setCycleCount(javafx.scene.media.MediaPlayer.INDEFINITE);
                                    owner.classicBattleMusicPlayer.setAutoPlay(true);
                                }
                            } else {
                                try { owner.classicBattleMusicPlayer.play(); } catch (Exception ignored) {}
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception ignored) {}
            };
            leftDone.addListener(startWhenReady);
            rightDone.addListener(startWhenReady);
            if (leftDone.get() && rightDone.get()) try { if (owner.previewService != null) owner.previewService.play(); } catch (Exception ignoredEx) {}
        } catch (Exception ignored) {
            try { if (owner.previewService != null) owner.previewService.play(); } catch (Exception ignoredEx) {}
        }
        try {
            owner.previewService = new ClassicBattlePreviewService(owner.leftController, owner.leftGui, owner.leftNextContent,
                owner.rightController, owner.rightGui, owner.rightNextContent);
            owner.previewService.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
