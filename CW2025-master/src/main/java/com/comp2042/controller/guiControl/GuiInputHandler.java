package com.comp2042.controller.guiControl;

import com.comp2042.input.EventSource;
import com.comp2042.input.EventType;
import com.comp2042.input.MoveEvent;
import com.comp2042.model.DownData;
import com.comp2042.model.ViewData;
import com.comp2042.view.NotificationPanel;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.Scene;

/**
 * Extracted input handling (key press/release and hard-drop logic) for GuiController.
 */
class GuiInputHandler {
    private final GuiController owner;
    private javafx.event.EventHandler<KeyEvent> pressHandler;
    private javafx.event.EventHandler<KeyEvent> releaseHandler;
    private javafx.event.EventHandler<KeyEvent> escHandler;

    GuiInputHandler(GuiController owner) {
        this.owner = owner;
    }
    /**
     * Attach key handlers to the provided scene and record the attached scene on the controller.
     *
     * <p>Handlers include press/release processing and an ESC handler which
     * toggles the pause overlay.</p>
     */
    void attachToScene(Scene s) {
        if (s == null) return;
        try {
            this.pressHandler = (KeyEvent keyEvent) -> { this.processKeyPressed(keyEvent); };
            this.releaseHandler = (KeyEvent keyEvent) -> { this.processKeyReleased(keyEvent); };
            this.escHandler = (KeyEvent e) -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    try { owner.togglePauseOverlay(); } catch (Exception ignored) {}
                    e.consume();
                }
            };

            s.addEventFilter(KeyEvent.KEY_PRESSED, this.pressHandler);
            s.addEventFilter(KeyEvent.KEY_RELEASED, this.releaseHandler);
            s.addEventFilter(KeyEvent.KEY_PRESSED, this.escHandler);
            owner.attachedScene = s;
        } catch (Exception ignored) {}
    }

    /**
     * Remove previously attached handlers from the scene.
     */
    void detachFromScene(Scene s) {
        if (s == null) return;
        try {
            if (this.pressHandler != null) s.removeEventFilter(KeyEvent.KEY_PRESSED, this.pressHandler);
            if (this.releaseHandler != null) s.removeEventFilter(KeyEvent.KEY_RELEASED, this.releaseHandler);
            if (this.escHandler != null) s.removeEventFilter(KeyEvent.KEY_PRESSED, this.escHandler);
            if (owner.attachedScene == s) owner.attachedScene = null;
        } catch (Exception ignored) {}
    }

    /**
     * Ensure the handlers are attached to the current gamePanel scene, and watch for scene changes.
     */
    void setupSceneKeyHandlers() {
        javafx.application.Platform.runLater(() -> {
            try {
                if (owner.gamePanel != null && owner.gamePanel.getScene() != null) {
                    Scene s = owner.gamePanel.getScene();
                    attachToScene(s);
                } else if (owner.gamePanel != null) {
                    owner.gamePanel.sceneProperty().addListener(new javafx.beans.value.ChangeListener<>() {
                        @Override
                        public void changed(javafx.beans.value.ObservableValue<? extends javafx.scene.Scene> observable, javafx.scene.Scene oldScene, javafx.scene.Scene newScene) {
                            try {
                                if (oldScene != null) {
                                    detachFromScene(oldScene);
                                }
                                if (newScene != null) {
                                    attachToScene(newScene);
                                }
                            } catch (Exception ignored) {}
                        }
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    /**
     * Handle key-press events and map them to game input actions via the
     * controller's {@code eventListener}. This method implements default
     * key bindings and honours any custom key assignments stored on the
     * {@link GuiController} instance.
     */
    void processKeyPressed(KeyEvent keyEvent) {
        try { if (this.owner instanceof com.comp2042.controller.cooperateBattle.coopGUI.CoopGuiController) return; } catch (Exception ignored) {}

        try {
            if (Boolean.FALSE.equals(owner.isPause.getValue()) && Boolean.FALSE.equals(owner.isGameOver.getValue())) {
                KeyCode code = keyEvent.getCode();
                boolean handled = false;

                if ((owner.ctrlMoveLeft != null && code == owner.ctrlMoveLeft) || (owner.ctrlMoveLeft == null && (code == KeyCode.LEFT || code == KeyCode.A))) {
                    owner.refreshBrick(owner.eventListener.onLeftEvent(new MoveEvent(EventType.LEFT, EventSource.USER)));
                    handled = true;
                } else if ((owner.ctrlMoveRight != null && code == owner.ctrlMoveRight) || (owner.ctrlMoveRight == null && (code == KeyCode.RIGHT || code == KeyCode.D))) {
                    owner.refreshBrick(owner.eventListener.onRightEvent(new MoveEvent(EventType.RIGHT, EventSource.USER)));
                    handled = true;
                } else if ((owner.ctrlRotate != null && code == owner.ctrlRotate) || (owner.ctrlRotate == null && (code == KeyCode.UP || code == KeyCode.W))) {
                    owner.refreshBrick(owner.eventListener.onRotateEvent(new MoveEvent(EventType.ROTATE, EventSource.USER)));
                    handled = true;
                } else if ((owner.ctrlSoftDrop != null && code == owner.ctrlSoftDrop) || (owner.ctrlSoftDrop == null && (code == KeyCode.DOWN || code == KeyCode.S))) {
                    if (owner.timeLine != null) owner.timeLine.setRate(4.0);
                    owner.moveDown(new MoveEvent(EventType.DOWN, EventSource.USER));
                    handled = true;
                } else if ((owner.ctrlHardDrop != null && code == owner.ctrlHardDrop) || (owner.ctrlHardDrop == null && (code == KeyCode.SPACE || code == KeyCode.SHIFT))) {
                    if (owner.isHardDropAllowed()) {
                        owner.setLastWasHardDrop(true);
                        hardDrop();
                        handled = true;
                    }
                }

                if (!handled && owner.ctrlSwap != null && code == owner.ctrlSwap) {
                    try { if (owner.eventListener != null) owner.eventListener.onSwapEvent(); } catch (Exception ignored) {}
                    handled = true;
                }

                if (handled) keyEvent.consume();
            }
        } catch (Exception ignored) {}

        if (keyEvent.getCode() == KeyCode.N) {
            owner.newGame(null);
        }
    }

    /**
     * Handle key-release events (primarily to restore soft-drop rate).
     */
    void processKeyReleased(KeyEvent keyEvent) {
        try { if (this.owner instanceof com.comp2042.controller.cooperateBattle.coopGUI.CoopGuiController) return; } catch (Exception ignored) {}
        KeyCode code = keyEvent.getCode();
        boolean hasCustom = (owner.ctrlSoftDrop != null);
        if (hasCustom) {
            if (code == owner.ctrlSoftDrop) {
                if (owner.timeLine != null) owner.timeLine.setRate(1.0);
                keyEvent.consume();
            }
        } else {
            if (code == KeyCode.DOWN || code == KeyCode.S) {
                if (owner.timeLine != null) owner.timeLine.setRate(1.0);
                keyEvent.consume();
            }
        }
    }

    /**
     * Execute a hard-drop: repeatedly invoke the down-event until the piece
     * locks, play effects and resume the timeline. This method performs a
     * safety-limited loop to avoid infinite iteration.
     */
    void hardDrop() {
        try {
            if (Boolean.FALSE.equals(owner.isPause.getValue()) && Boolean.FALSE.equals(owner.isGameOver.getValue()) && owner.eventListener != null) {
                if (owner.timeLine != null) owner.timeLine.pause();
                ViewData startViewForEffect = owner.currentViewData;
                int safety = 0;
                while (safety++ < 1000) {
                    DownData d = owner.eventListener.onDownEvent(new MoveEvent(EventType.DOWN, EventSource.USER));
                    if (d == null) break;
                    ViewData v = d.getViewData();
                    if (d.getClearRow() != null && d.getClearRow().getLinesRemoved() > 0) {
                        NotificationPanel notificationPanel = new NotificationPanel("+" + d.getClearRow().getScoreBonus());
                        owner.groupNotification.getChildren().add(notificationPanel);
                        notificationPanel.showScore(owner.groupNotification.getChildren());
                    }
                    owner.refreshBrick(v);
                    if (d.getClearRow() != null) {
                        try {
                            if (owner.isLastWasHardDrop()) {
                                try { owner.playHardDropSound(); } catch (Exception ignored) {}
                                owner.playLockEffect(startViewForEffect, d.getViewData(), true);
                            }
                        } catch (Exception ignored) {}
                        owner.setLastWasHardDrop(false);
                        if (d.getClearRow().getLinesRemoved() > 0) {
                            try { owner.spawnExplosion(d.getClearRow(), d.getViewData()); } catch (Exception ignored) {}
                        }
                        break;
                    }
                }
                if (owner.timeLine != null) owner.timeLine.play();
            }
        } catch (Exception ignored) {}
    }
}
