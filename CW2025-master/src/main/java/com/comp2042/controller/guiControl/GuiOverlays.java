package com.comp2042.controller.guiControl;

import com.comp2042.controller.controls.ControlsController;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.application.Platform;

import javafx.scene.Node;
import java.util.ArrayList;
import java.util.List;

/**
 * UI builders for pause and controls overlays extracted from GuiController.
 */
public final class GuiOverlays {

    private GuiOverlays() {}

    public static StackPane buildPauseOverlay(GuiController controller, Scene scene) {
        StackPane overlay = new StackPane();
        overlay.setId("GLOBAL_PAUSE_OVERLAY");
        overlay.setPickOnBounds(true);

        Rectangle dark = new Rectangle();
        dark.widthProperty().bind(scene.widthProperty());
        dark.heightProperty().bind(scene.heightProperty());
        dark.setFill(Color.rgb(0,0,0,0.55));

        VBox dialog = new VBox(14);
        dialog.setAlignment(Pos.CENTER);
        dialog.setStyle("-fx-background-color: rgba(30,30,30,0.85); -fx-padding: 18px; -fx-background-radius: 8px;");

        Label title = new Label("Paused");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 36px; -fx-font-weight: bold;");

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);
        Button resume = new Button("Resume");
        Button settings = new Button("Settings");
        resume.getStyleClass().add("menu-button");
        settings.getStyleClass().add("menu-button");
        controller.attachButtonSoundHandlers(resume);
        controller.attachButtonSoundHandlers(settings);

        resume.setOnAction(ev -> { ev.consume(); controller.hidePauseOverlay(); });

        settings.setOnAction(ev -> { ev.consume(); controller.showControlsOverlay(); });

        buttons.getChildren().addAll(resume, settings);
        dialog.getChildren().addAll(title, buttons);

        overlay.getChildren().addAll(dark, dialog);
        return overlay;
    }
    /**
     * Show the pause overlay (build + add to root) and pause timelines/notify.
     *
     * <p>This method runs UI updates on the JavaFX thread and ensures the
     * controller's timelines are paused and multiplayer coordinators are
     * notified.</p>
     */
    public static void showPauseOverlay(GuiController controller) {
        if (controller == null) return;
        javafx.application.Platform.runLater(() -> {
            try {
                Scene scene = (controller.gameBoard != null) ? controller.gameBoard.getScene() : null;
                StackPane overlay = buildPauseOverlay(controller, scene);
                addPauseOverlayToRoot(scene, overlay, controller);
                pauseTimelinesAndNotify(controller);
            } catch (Exception ignored) {}
        });
    }

    /**
     * Add the provided pause overlay to the scene root (or groupNotification fallback),
     * removing any existing pause overlays first.
     */
    public static void addPauseOverlayToRoot(Scene scene, StackPane pauseOverlay, GuiController controller) {
        if (scene == null || pauseOverlay == null) return;
        if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
            try {
                List<Node> toRemove = new ArrayList<>();
                for (Node n : new ArrayList<>(root.getChildren())) {
                    if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove.add(n);
                }
                root.getChildren().removeAll(toRemove);
            } catch (Exception ignored) {}
            root.getChildren().add(pauseOverlay);
        } else if (controller.groupNotification != null) {
            try {
                List<Node> toRemove = new ArrayList<>();
                for (Node n : new ArrayList<>(controller.groupNotification.getChildren())) {
                    if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove.add(n);
                }
                controller.groupNotification.getChildren().removeAll(toRemove);
            } catch (Exception ignored) {}
            controller.groupNotification.getChildren().add(pauseOverlay);
        }
    }

    /**
     * Remove any existing pause overlays from the scene or controller.groupNotification,
     * restore any hidden nodes stored in the overlay properties, and then call
     * controller.resumeFromPauseOverlay() to resume game timelines and state.
     */
    public static void hidePauseOverlay(GuiController controller, StackPane overlay) {
        Platform.runLater(() -> {
            try {
                Scene scene = (controller.gameBoard != null) ? controller.gameBoard.getScene() : null;
                if (scene != null && scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    List<Node> toRemove = new ArrayList<>();
                    for (Node n : new ArrayList<>(root.getChildren())) {
                        if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove.add(n);
                    }
                    root.getChildren().removeAll(toRemove);
                }
                if (controller.groupNotification != null) {
                    List<Node> toRemove2 = new ArrayList<>();
                    for (Node n : new ArrayList<>(controller.groupNotification.getChildren())) {
                        if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) toRemove2.add(n);
                    }
                    controller.groupNotification.getChildren().removeAll(toRemove2);
                }

                // restore any hidden nodes that were stored on the overlay
                if (overlay != null) {
                    Object hidden = overlay.getProperties().get("hiddenPauseNodes");
                    if (hidden instanceof java.util.List<?>) {
                        for (Object n : (java.util.List<?>) hidden) {
                            if (n instanceof Node) ((Node) n).setVisible(true);
                        }
                    }
                }

                // clear overlay reference and resume timelines/state via controller
                try { controller.clearPauseOverlay(); } catch (Exception ignored) {}
                try { controller.resumeFromPauseOverlay(); } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        });
    }

    /**
     * Pause timelines on the given controller and notify multiplayer coordinator.
     */
    public static void pauseTimelinesAndNotify(GuiController controller) {
        if (controller == null) return;
        try {
            // pause timelines and record paused elapsed inside controller
            try { controller.pauseTimelinesInternal(); } catch (Exception ignored) {}
            // set ui state (properties)
            try { controller.setPauseOverlayVisible(true); } catch (Exception ignored) {}
            // notify multiplayer coordinator
            try { controller.notifyMultiplayerPause(true); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }
    public static StackPane buildControlsOverlayUI(GuiController controller, StackPane pane, ControlsController cc) {
        StackPane controlsOverlay = new StackPane();
        controlsOverlay.setStyle("-fx-padding:0;");
        Rectangle dark2 = new Rectangle();
        Scene sceneLocal = controller.gameBoard.getScene();
        if (sceneLocal != null) {
            dark2.widthProperty().bind(sceneLocal.widthProperty());
            dark2.heightProperty().bind(sceneLocal.heightProperty());
        }
        dark2.setFill(Color.rgb(8,8,10,0.82));

        BorderPane container = new BorderPane();
        container.setMaxWidth(Double.MAX_VALUE);
        container.setMaxHeight(Double.MAX_VALUE);
        container.setStyle("-fx-padding:18;");
        javafx.scene.text.Text header = new javafx.scene.text.Text("Controls");
        header.setStyle("-fx-font-size:34px; -fx-fill: #9fb0ff; -fx-font-weight:700;");
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        javafx.scene.control.Button btnCancel2 = new javafx.scene.control.Button("Cancel");
        javafx.scene.control.Button btnSave2 = new javafx.scene.control.Button("Save");
        btnCancel2.getStyleClass().add("menu-button"); btnSave2.getStyleClass().add("menu-button");
        controller.attachButtonSoundHandlers(btnCancel2);
        controller.attachButtonSoundHandlers(btnSave2);
        actionBox.getChildren().addAll(btnCancel2, btnSave2);
        BorderPane topBar = new BorderPane();
        topBar.setLeft(header);
        topBar.setRight(actionBox);
        topBar.setStyle("-fx-padding:8 18 18 18;");
        container.setTop(topBar);
        VBox center = new VBox(18);
        center.setStyle("-fx-padding:12; -fx-background-color: transparent;");
        center.getChildren().add(pane);
        container.setCenter(center);

        // Cancel handler: remove overlay and restore previously-hidden pause nodes
        btnCancel2.setOnAction(ev2 -> {
            ev2.consume();
            if (controlsOverlay.getParent() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) controlsOverlay.getParent();
                root.getChildren().remove(controlsOverlay);
            }
            Object o = controlsOverlay.getProperties().get("hiddenPauseNodes");
            if (o instanceof java.util.List<?>) {
                for (Object n : (java.util.List<?>) o) {
                    if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                }
            }
            controller.setPauseOverlayVisible(true);
        });

        // Save handler: update keys, persist preferences, remove overlay and restore hidden nodes
        btnSave2.setOnAction(ev2 -> {
            ev2.consume();
            try {
                controller.setControlKeys(cc.getLeft(), cc.getRight(), cc.getRotate(), cc.getDown(), cc.getHard());
                controller.setSwapKey(cc.getSwitch());
            } catch (Exception ignored) {}
            try {
                java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.controller.mainMenu.MainMenuController.class);
                if (controller.isMultiplayerMode() && controller.getMultiplayerPlayerId() != null) {
                    if ("left".equalsIgnoreCase(controller.getMultiplayerPlayerId())) {
                        prefs.put("mpLeft_left", controller.getCtrlMoveLeft() != null ? controller.getCtrlMoveLeft().name() : "");
                        prefs.put("mpLeft_right", controller.getCtrlMoveRight() != null ? controller.getCtrlMoveRight().name() : "");
                        prefs.put("mpLeft_rotate", controller.getCtrlRotate() != null ? controller.getCtrlRotate().name() : "");
                        prefs.put("mpLeft_down", controller.getCtrlSoftDrop() != null ? controller.getCtrlSoftDrop().name() : "");
                        prefs.put("mpLeft_hard", controller.getCtrlHardDrop() != null ? controller.getCtrlHardDrop().name() : "");
                        prefs.put("mpLeft_switch", controller.getCtrlSwap() != null ? controller.getCtrlSwap().name() : "");
                    } else if ("right".equalsIgnoreCase(controller.getMultiplayerPlayerId())) {
                        prefs.put("mpRight_left", controller.getCtrlMoveLeft() != null ? controller.getCtrlMoveLeft().name() : "");
                        prefs.put("mpRight_right", controller.getCtrlMoveRight() != null ? controller.getCtrlMoveRight().name() : "");
                        prefs.put("mpRight_rotate", controller.getCtrlRotate() != null ? controller.getCtrlRotate().name() : "");
                        prefs.put("mpRight_down", controller.getCtrlSoftDrop() != null ? controller.getCtrlSoftDrop().name() : "");
                        prefs.put("mpRight_hard", controller.getCtrlHardDrop() != null ? controller.getCtrlHardDrop().name() : "");
                        prefs.put("mpRight_switch", controller.getCtrlSwap() != null ? controller.getCtrlSwap().name() : "");
                    } else {
                        prefs.put("spLeft", controller.getCtrlMoveLeft() != null ? controller.getCtrlMoveLeft().name() : "");
                        prefs.put("spRight", controller.getCtrlMoveRight() != null ? controller.getCtrlMoveRight().name() : "");
                        prefs.put("spRotate", controller.getCtrlRotate() != null ? controller.getCtrlRotate().name() : "");
                        prefs.put("spDown", controller.getCtrlSoftDrop() != null ? controller.getCtrlSoftDrop().name() : "");
                        prefs.put("spHard", controller.getCtrlHardDrop() != null ? controller.getCtrlHardDrop().name() : "");
                        prefs.put("spSwitch", controller.getCtrlSwap() != null ? controller.getCtrlSwap().name() : "");
                    }
                } else {
                    prefs.put("spLeft", controller.getCtrlMoveLeft() != null ? controller.getCtrlMoveLeft().name() : "");
                    prefs.put("spRight", controller.getCtrlMoveRight() != null ? controller.getCtrlMoveRight().name() : "");
                    prefs.put("spRotate", controller.getCtrlRotate() != null ? controller.getCtrlRotate().name() : "");
                    prefs.put("spDown", controller.getCtrlSoftDrop() != null ? controller.getCtrlSoftDrop().name() : "");
                    prefs.put("spHard", controller.getCtrlHardDrop() != null ? controller.getCtrlHardDrop().name() : "");
                    prefs.put("spSwitch", controller.getCtrlSwap() != null ? controller.getCtrlSwap().name() : "");
                }
            } catch (Exception ignored) {}
            if (controlsOverlay.getParent() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) controlsOverlay.getParent();
                root.getChildren().remove(controlsOverlay);
            }
            Object o2 = controlsOverlay.getProperties().get("hiddenPauseNodes");
            if (o2 instanceof java.util.List<?>) {
                for (Object n : (java.util.List<?>) o2) {
                    if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                }
            }
            controller.setPauseOverlayVisible(true);
        });

        controlsOverlay.getChildren().addAll(dark2, container);
        return controlsOverlay;
    }

    /**
     * Load controls.fxml, build controls overlay and add it to the scene root.
     */
    public static void showControlsOverlay(GuiController controller) {
        try {
            javafx.scene.layout.StackPane[] paneOut = new javafx.scene.layout.StackPane[1];
            ControlsController cc = controller.loadControlsController("controls.fxml", paneOut);
            // If a multiplayerRequestControlsHandler was present, loadControlsController
            // will invoke it and return null. In that case we MUST NOT hide the pause
            // overlay here because the multiplayer handler is responsible for showing
            // its own overlay while the game remains paused. Only treat a null pane
            // as an error condition that should remove the pause overlay.
            if (cc == null) {
                // If paneOut[0] was provided by the handler, the handler will manage UI; just return
                if (paneOut[0] != null) return;
                // otherwise treat as an error and restore normal pause behavior
                try { hidePauseOverlay(controller, null); } catch (Exception ignored) {}
                return;
            }

            controller.configureControlsController(cc);
            StackPane controlsOverlay = buildControlsOverlayUI(controller, paneOut[0], cc);

            Scene sceneLocal = (controller.gameBoard != null) ? controller.gameBoard.getScene() : null;
            if (sceneLocal != null && sceneLocal.getRoot() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) sceneLocal.getRoot();
                java.util.List<javafx.scene.Node> hidden = new java.util.ArrayList<>();
                for (javafx.scene.Node n : root.getChildren()) {
                    if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) {
                        n.setVisible(false);
                        hidden.add(n);
                    }
                }
                controlsOverlay.getProperties().put("hiddenPauseNodes", hidden);
                root.getChildren().add(controlsOverlay);
            }
        } catch (Exception ex) {
            try { hidePauseOverlay(controller, null); } catch (Exception ignored) {}
        }
    }
}
