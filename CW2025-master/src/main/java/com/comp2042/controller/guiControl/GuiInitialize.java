package com.comp2042.controller.guiControl;

import com.comp2042.audio.soundManager.SoundManager;
import javafx.application.Platform;

import javafx.scene.effect.Reflection;
import javafx.scene.text.Font;

import java.net.URL;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * Small initialization helpers used by {@link GuiController} to keep the
 * controller bootstrap code compact and testable.
 *
 * <p>Responsibilities include loading fonts, wiring up the {@link com.comp2042.audio.soundManager.SoundManager},
 * attaching listeners for game-state changes and creating common layout bindings.</p>
 */
public final class GuiInitialize {

    private GuiInitialize() {}

    public static void initialize(GuiController controller, URL location, ResourceBundle resources) {
        // moved from GuiController: keep thin delegations here
        loadFontAndFocus(controller);
        initSoundManager(controller);

        // Attach generic sound handlers to commonly-interacted controls (pause button etc.)
        try { if (controller.pauseBtn != null) controller.attachButtonSoundHandlers(controller.pauseBtn); } catch (Exception ignored) {}

        setupMusicAndGameListeners(controller);
        setupSceneKeyHandlers(controller);

        try { if (controller.gameOverPanel != null) controller.gameOverPanel.setVisible(false); } catch (Exception ignored) {}

        setupReflection(controller);
        setupLayoutBindings(controller);
    }

    static void loadFontAndFocus(GuiController controller) {
        try {
            Font.loadFont(controller.getClass().getClassLoader().getResource("digital.ttf").toExternalForm(), 38);
        } catch (Exception ignored) {}
        try { if (controller.gamePanel != null) { controller.gamePanel.setFocusTraversable(true); controller.gamePanel.requestFocus(); } } catch (Exception ignored) {}
    }

    static void initSoundManager(GuiController controller) {
        try {
            controller.soundManager = new SoundManager(controller.getClass());
            if (controller.soundManager != null) controller.soundManager.init();
        } catch (Exception ex) {
            System.err.println("[GuiInitialize] sound manager init failed: " + ex.getMessage());
        }
    }

    static void setupMusicAndGameListeners(GuiController controller) {
        try {
            controller.countdownFinishedProperty().addListener((obs, oldV, newV) -> {
                Objects.requireNonNull(obs);
                Objects.requireNonNull(oldV);
                if (Boolean.TRUE.equals(newV)) {
                    if (!controller.shouldStartSingleplayerMusic()) return;
                    SoundManager sm = controller.getSoundManager();
                    if (sm != null) {
                        try { sm.startSingleplayerMusic(); } catch (Exception ignored) {}
                    }
                }
            });

            controller.isGameOverProperty().addListener((obs, oldV, newV) -> {
                Objects.requireNonNull(obs);
                Objects.requireNonNull(oldV);
                if (Boolean.TRUE.equals(newV)) {
                    try {
                        SoundManager sm = controller.getSoundManager();
                        if (sm != null) {
                            try { sm.stopSingleplayerMusic(); } catch (Exception ignored) {}
                            try { sm.playGameOverMusic(); } catch (Exception ignored) {}
                        }
                    } catch (Exception ignored) {}
                }
            });
        } catch (Exception ignored) {}
    }

    static void setupSceneKeyHandlers(GuiController controller) {
        // delegate scene key handler setup to the input handler
        try { controller.setupInputHandlers(); } catch (Exception ignored) {}
    }

    static void setupReflection(GuiController controller) {
        try {
            final Reflection reflection = new Reflection();
            reflection.setFraction(0.8);
            reflection.setTopOpacity(0.9);
            reflection.setTopOffset(-12);
        } catch (Exception ignored) {}
    }

    static void setupLayoutBindings(GuiController controller) {
        Platform.runLater(() -> {
            try {
                // layout/binding helpers moved here to keep initialization grouped
                GuiInitialize.bindGameBoardCenter(controller);
                GuiInitialize.bindScoreBox(controller);
                GuiInitialize.bindGameBoardFrame(controller);
                GuiInitialize.bindTimeBox(controller);
                GuiInitialize.styleScoreValue(controller);
                GuiInitialize.bindGroupNotification(controller);
                GuiInitialize.bindNextBox(controller);
                GuiInitialize.bindLevelBox(controller);
            } catch (Exception ignored) {}
        });
    }

    /* Layout binding helpers (moved from GuiController) */
    static void bindGameBoardCenter(GuiController controller) {
        try {
            if (controller.gameBoard == null) return;
            if (controller.gameBoard.getParent() instanceof javafx.scene.layout.Region) {
                javafx.scene.layout.Region parent = (javafx.scene.layout.Region) controller.gameBoard.getParent();
                controller.gameBoard.layoutXProperty().bind(parent.widthProperty().subtract(controller.gameBoard.widthProperty()).divide(2));
                controller.gameBoard.layoutYProperty().bind(parent.heightProperty().subtract(controller.gameBoard.heightProperty()).divide(2));
            } else if (controller.gameBoard.getScene() != null) {
                // fallback to centering within the Scene
                controller.gameBoard.layoutXProperty().bind(controller.gameBoard.getScene().widthProperty().subtract(controller.gameBoard.widthProperty()).divide(2));
                controller.gameBoard.layoutYProperty().bind(controller.gameBoard.getScene().heightProperty().subtract(controller.gameBoard.heightProperty()).divide(2));
            }
        } catch (Exception ignored) {}
    }

    static void bindScoreBox(GuiController controller) {
        try {
            if (controller.scoreBox != null && controller.gameBoard != null) {
                controller.scoreBox.layoutXProperty().bind(controller.gameBoard.layoutXProperty().subtract(250.0));
                controller.scoreBox.layoutYProperty().bind(controller.gameBoard.layoutYProperty().add(controller.gameBoard.heightProperty().subtract(120.0)));
            }
        } catch (Exception ignored) {}
    }

    static void bindGameBoardFrame(GuiController controller) {
        try {
            if (controller.gameBoardFrame != null && controller.gameBoard != null) {
                controller.gameBoardFrame.widthProperty().bind(controller.gameBoard.widthProperty());
                controller.gameBoardFrame.heightProperty().bind(controller.gameBoard.heightProperty());
                controller.gameBoardFrame.layoutXProperty().bind(controller.gameBoard.layoutXProperty());
                controller.gameBoardFrame.layoutYProperty().bind(controller.gameBoard.layoutYProperty());
                controller.gameBoardFrame.setArcWidth(24);
                controller.gameBoardFrame.setArcHeight(24);
                controller.gameBoardFrame.setStrokeWidth(8);
                controller.gameBoardFrame.setStroke(javafx.scene.paint.Color.web("#2A5058"));
                controller.gameBoardFrame.setFill(javafx.scene.paint.Color.web("#111"));
            }
        } catch (Exception ignored) {}
    }

    static void bindTimeBox(GuiController controller) {
        try {
            if (controller.timeBox != null && controller.gameBoard != null) {
                controller.timeBox.layoutXProperty().bind(
                    javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> controller.gameBoard.getLayoutX() + controller.timeBoxOffsetXProperty().get() - controller.timeBox.getWidth(),
                        controller.gameBoard.layoutXProperty(), controller.gameBoard.widthProperty(), controller.timeBox.widthProperty(), controller.timeBoxOffsetXProperty()
                    )
                );
                controller.timeBox.layoutYProperty().bind(
                    javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> controller.gameBoard.getLayoutY() + controller.timeBoxOffsetYProperty().get(),
                        controller.gameBoard.layoutYProperty(), controller.gameBoard.heightProperty(), controller.timeBox.heightProperty(), controller.timeBoxOffsetYProperty()
                    )
                );
            }
        } catch (Exception ignored) {}
    }

    static void styleScoreValue(GuiController controller) {
        try {
            if (controller.scoreValue != null) {
                controller.scoreValue.getStyleClass().remove("scoreClass");
                controller.scoreValue.getStyleClass().add("highScoreClass");
            }
        } catch (Exception ignored) {}
    }

    static void bindGroupNotification(GuiController controller) {
        try {
            if (controller.groupNotification != null && controller.groupNotification.getParent() != null && controller.gameBoard != null && controller.gameBoard.getParent() != null) {
                controller.groupNotification.layoutXProperty().bind(
                    javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> controller.gameBoard.getLayoutX() + controller.gameBoard.getWidth() / 2.0 - controller.groupNotification.getLayoutBounds().getWidth() / 2.0,
                        controller.gameBoard.layoutXProperty(), controller.gameBoard.widthProperty(), controller.groupNotification.layoutBoundsProperty()
                    )
                );

                controller.groupNotification.layoutYProperty().bind(
                    javafx.beans.binding.Bindings.createDoubleBinding(
                        () -> controller.gameBoard.getLayoutY() + controller.gameBoard.getHeight() / 2.0 - controller.groupNotification.getLayoutBounds().getHeight() / 2.0,
                        controller.gameBoard.layoutYProperty(), controller.gameBoard.heightProperty(), controller.groupNotification.layoutBoundsProperty()
                    )
                );
            }
        } catch (Exception ignored) {}
    }

    static void bindNextBox(GuiController controller) {
        try {
            if (controller.nextBox == null || controller.gameBoard == null) return;
            final double outsideGap = 70.0;
            controller.nextBox.layoutXProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> controller.gameBoard.getLayoutX() + controller.gameBoard.getWidth() + outsideGap,
                    controller.gameBoard.layoutXProperty(), controller.gameBoard.widthProperty()
                )
            );

            controller.nextBox.layoutYProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> controller.gameBoard.getLayoutY() + 8.0,
                    controller.gameBoard.layoutYProperty(), controller.gameBoard.heightProperty()
                )
            );

            if (controller.nextBoxFrame != null && controller.nextBox != null) {
                controller.nextBoxFrame.widthProperty().bind(controller.nextBox.widthProperty());
                controller.nextBoxFrame.heightProperty().bind(controller.nextBox.heightProperty());
                controller.nextBoxFrame.layoutXProperty().bind(controller.nextBox.layoutXProperty());
                controller.nextBoxFrame.layoutYProperty().bind(controller.nextBox.layoutYProperty());
                controller.nextBoxFrame.setArcWidth(24);
                controller.nextBoxFrame.setArcHeight(24);
                controller.nextBoxFrame.setStrokeWidth(8);
                controller.nextBoxFrame.setStroke(javafx.scene.paint.Color.web("#2A5058"));
                controller.nextBoxFrame.setFill(javafx.scene.paint.Color.web("#111"));
            }
        } catch (Exception ignored) {}
    }

    static void bindLevelBox(GuiController controller) {
        try {
            if (controller.levelBox == null || controller.gameBoard == null) return;
            final double outsideGap = 70.0;
            controller.levelBox.layoutXProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> controller.gameBoard.getLayoutX() + controller.gameBoard.getWidth() + outsideGap,
                    controller.gameBoard.layoutXProperty(), controller.gameBoard.widthProperty()
                )
            );
            controller.levelBox.layoutYProperty().bind(
                javafx.beans.binding.Bindings.createDoubleBinding(
                    () -> controller.gameBoard.getLayoutY() + controller.gameBoard.getHeight() - controller.levelBox.getHeight() - 12.0,
                    controller.gameBoard.layoutYProperty(), controller.gameBoard.heightProperty(), controller.levelBox.heightProperty()
                )
            );
        } catch (Exception ignored) {}
    }
}
