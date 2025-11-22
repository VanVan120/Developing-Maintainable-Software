package com.comp2042.controller.classicBattle;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.layout.VBox;

/**
 * Small service to update the "next" preview boxes periodically.
 * Encapsulates the Timeline used previously inside ClassicBattle.
 */
public class ClassicBattlePreviewService {

    private final com.comp2042.controller.gameControl.GameController leftController;
    private final com.comp2042.controller.guiControl.GuiController leftGui;
    private final VBox leftNextContent;

    private final com.comp2042.controller.gameControl.GameController rightController;
    private final com.comp2042.controller.guiControl.GuiController rightGui;
    private final VBox rightNextContent;

    private Timeline poller;

    public ClassicBattlePreviewService(com.comp2042.controller.gameControl.GameController leftController,
                                      com.comp2042.controller.guiControl.GuiController leftGui,
                                      VBox leftNextContent,
                                      com.comp2042.controller.gameControl.GameController rightController,
                                      com.comp2042.controller.guiControl.GuiController rightGui,
                                      VBox rightNextContent) {
        this.leftController = leftController;
        this.leftGui = leftGui;
        this.leftNextContent = leftNextContent;
        this.rightController = rightController;
        this.rightGui = rightGui;
        this.rightNextContent = rightNextContent;
    }

    public void start() {
        if (poller != null) {
            try { poller.play(); } catch (Exception ignored) {}
            return;
        }
        poller = new Timeline(new KeyFrame(Duration.millis(300), _ev -> {
            try {
                if (leftController != null && leftGui != null) {
                    java.util.List<com.comp2042.logic.Brick> up = leftController.getUpcomingBricks(3);
                    if (leftNextContent != null) {
                        javafx.scene.layout.VBox built = leftGui.buildNextPreview(up);
                        leftNextContent.getChildren().clear();
                        if (built != null) leftNextContent.getChildren().addAll(built.getChildren());
                    } else {
                        if (up != null) leftGui.showNextBricks(up);
                    }
                }
                if (rightController != null && rightGui != null) {
                    java.util.List<com.comp2042.logic.Brick> up2 = rightController.getUpcomingBricks(3);
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
        poller.setCycleCount(Timeline.INDEFINITE);
        try { poller.play(); } catch (Exception ignored) {}
    }

    public void pause() {
        /**
         * Pause the preview update timeline.
         */
        if (poller != null) poller.pause();
    }

    public void play() {
        /**
         * Resume preview updates. If the service was stopped it will be
         * (re)started.
         */
        if (poller != null) {
            try { poller.play(); } catch (Exception ignored) {}
        } else {
            try { start(); } catch (Exception ignored) {}
        }
    }

    public void stop() {
        /**
         * Stop the preview service and release its timeline.
         */
        if (poller != null) {
            try { poller.stop(); } catch (Exception ignored) {}
            poller = null;
        }
    }
}
