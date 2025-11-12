package com.comp2042.controller.classicBattle;

import com.comp2042.controller.controls.ControlsController;
import com.comp2042.controller.guiControl.GuiController;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Factory responsible for creating overlays used by ClassicBattle (controls and winner overlays).
 * This refactor decomposes the previous large methods into smaller OOP-style helpers so each
 * responsibility is contained and easier to test.
 */
public final class ClassicBattleOverlayFactory {
    private static final Logger LOGGER = Logger.getLogger(ClassicBattleOverlayFactory.class.getName());

    private final Class<?> resourceOwner;

    public ClassicBattleOverlayFactory(Class<?> resourceOwner) {
        this.resourceOwner = resourceOwner == null ? getClass() : resourceOwner;
    }

    /**
     * Build the controls overlay. Returns a StackPane ready to add to a Pane root, or null on failure.
     */
    public StackPane createControlsOverlay(Scene scene, GuiController leftGui, GuiController rightGui) {
        if (scene == null) return null;
        try {
            StackPane overlay = new StackPane();
            overlay.setPickOnBounds(true);

            Rectangle dark = createBackdrop(scene);
            BorderPane container = buildControlsContainer(scene, overlay);

            URL controlsFxml = resourceOwner.getClassLoader().getResource("controls.fxml");
            if (controlsFxml == null) {
                LOGGER.log(Level.FINER, "controls.fxml not found for overlay");
                return null;
            }

            ControlsPair left = loadControlsPanel(controlsFxml);
            ControlsPair right = loadControlsPanel(controlsFxml);
            if (left == null || right == null) return null;

            initializeControllers(left.controller, leftGui, "Left Player Controls", true);
            initializeControllers(right.controller, rightGui, "Right Player Controls", false);

            setDefaultKeys(left.controller, true);
            setDefaultKeys(right.controller, false);

            try { left.pane.setPrefWidth(520); } catch (Exception ex) { LOGGER.log(Level.FINER, "left pane pref width", ex); }
            try { right.pane.setPrefWidth(520); } catch (Exception ex) { LOGGER.log(Level.FINER, "right pane pref width", ex); }

            javafx.scene.layout.HBox center = new javafx.scene.layout.HBox(120);
            center.getStyleClass().add("controls-center");
            center.setAlignment(Pos.CENTER);
            center.getChildren().addAll(left.pane, right.pane);
            container.setCenter(center);

            // action bar
            javafx.scene.text.Text header = new javafx.scene.text.Text("Controls");
            header.getStyleClass().add("controls-header");
            javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(10);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            Button btnResetTop = new Button("Reset");
            Button btnCancel = new Button("Cancel");
            Button btnSave = new Button("Save");
            btnResetTop.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
            actionBox.getChildren().addAll(btnResetTop, btnCancel, btnSave);
            BorderPane topBar = new BorderPane();
            topBar.setLeft(header);
            topBar.setRight(actionBox);
            topBar.getStyleClass().add("controls-topbar");
            container.setTop(topBar);

            overlay.getChildren().addAll(dark, container);

            // Wire button handlers
            btnResetTop.setOnAction(ev -> {
                ev.consume();
                try { left.controller.resetToPanelDefaults(); } catch (Exception ex) { LOGGER.log(Level.FINER, "reset left", ex); }
                try { right.controller.resetToPanelDefaults(); } catch (Exception ex) { LOGGER.log(Level.FINER, "reset right", ex); }
            });

            btnSave.setOnAction(ev -> {
                ev.consume();
                try {
                    persistControls(left.controller, leftGui, "mpLeft_");
                    persistControls(right.controller, rightGui, "mpRight_");
                    removeOverlayAndRestoreHidden(overlay);
                } catch (Exception ex) {
                    LOGGER.log(Level.FINER, "save controls", ex);
                }
            });

            btnCancel.setOnAction(ev -> {
                ev.consume();
                try {
                    removeOverlayAndRestoreHidden(overlay);
                } catch (Exception ex) {
                    LOGGER.log(Level.FINER, "cancel controls", ex);
                }
            });

            // Cross-panel availability checks
            try {
                left.controller.setKeyAvailabilityChecker((code, btn) -> availabilityCheck(code, btn, right.controller));
            } catch (Exception ex) { LOGGER.log(Level.FINER, "left availability", ex); }
            try {
                right.controller.setKeyAvailabilityChecker((code, btn) -> availabilityCheck(code, btn, left.controller));
            } catch (Exception ex) { LOGGER.log(Level.FINER, "right availability", ex); }

            return overlay;
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "createControlsOverlay failed", ex);
            return null;
        }
    }

    private Rectangle createBackdrop(Scene scene) {
        Rectangle dark = new Rectangle();
        dark.widthProperty().bind(scene.widthProperty());
        dark.heightProperty().bind(scene.heightProperty());
        dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
        return dark;
    }

    private BorderPane buildControlsContainer(Scene scene, StackPane overlay) {
        BorderPane container = new BorderPane();
        container.getStyleClass().add("controls-container");
        return container;
    }

    private static final class ControlsPair {
        final StackPane pane;
        final ControlsController controller;
        ControlsPair(StackPane pane, ControlsController controller) { this.pane = pane; this.controller = controller; }
    }

    private ControlsPair loadControlsPanel(URL controlsFxml) {
        try {
            javafx.fxml.FXMLLoader fx = new javafx.fxml.FXMLLoader(controlsFxml);
            StackPane pane = fx.load();
            ControlsController controller = fx.getController();
            return new ControlsPair(pane, controller);
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "loadControlsPanel failed", ex);
            return null;
        }
    }

    private void initializeControllers(ControlsController cc, GuiController gui, String headerText, boolean isLeft) {
        if (cc == null || gui == null) return;
        try {
            cc.init(
                    gui.getCtrlMoveLeft() != null ? gui.getCtrlMoveLeft() : (isLeft ? javafx.scene.input.KeyCode.A : javafx.scene.input.KeyCode.NUMPAD4),
                    gui.getCtrlMoveRight() != null ? gui.getCtrlMoveRight() : (isLeft ? javafx.scene.input.KeyCode.D : javafx.scene.input.KeyCode.NUMPAD6),
                    gui.getCtrlRotate() != null ? gui.getCtrlRotate() : (isLeft ? javafx.scene.input.KeyCode.W : javafx.scene.input.KeyCode.NUMPAD8),
                    gui.getCtrlSoftDrop() != null ? gui.getCtrlSoftDrop() : (isLeft ? javafx.scene.input.KeyCode.S : javafx.scene.input.KeyCode.NUMPAD5),
                    gui.getCtrlHardDrop() != null ? gui.getCtrlHardDrop() : (isLeft ? javafx.scene.input.KeyCode.SHIFT : javafx.scene.input.KeyCode.SPACE),
                    gui.getCtrlSwap() != null ? gui.getCtrlSwap() : (isLeft ? javafx.scene.input.KeyCode.Q : javafx.scene.input.KeyCode.C)
            );
            cc.setHeaderText(headerText);
            try { cc.hideActionButtons(); } catch (Exception ex) { LOGGER.log(Level.FINER, "hide action buttons", ex); }
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "initializeControllers failed", ex);
        }
    }

    private void setDefaultKeys(ControlsController cc, boolean left) {
        if (cc == null) return;
        try {
            if (left) {
                cc.setDefaultKeys(javafx.scene.input.KeyCode.A, javafx.scene.input.KeyCode.D, javafx.scene.input.KeyCode.W, javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCode.SHIFT, javafx.scene.input.KeyCode.Q);
            } else {
                cc.setDefaultKeys(javafx.scene.input.KeyCode.NUMPAD4, javafx.scene.input.KeyCode.NUMPAD6, javafx.scene.input.KeyCode.NUMPAD8, javafx.scene.input.KeyCode.NUMPAD5, javafx.scene.input.KeyCode.SPACE, javafx.scene.input.KeyCode.NUMPAD7);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "setDefaultKeys failed", ex);
        }
    }

    private boolean availabilityCheck(javafx.scene.input.KeyCode code, Button btn, ControlsController other) {
        try {
            Objects.requireNonNull(btn);
            if (code == null) return true;
            return !(code.equals(other.getLeft())
                    || code.equals(other.getRight())
                    || code.equals(other.getRotate())
                    || code.equals(other.getDown())
                    || code.equals(other.getHard())
                    || code.equals(other.getSwitch()));
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "availabilityCheck failed", ex);
            return true;
        }
    }

    private void persistControls(ControlsController cc, GuiController gui, String prefsPrefix) {
        if (cc == null || gui == null) return;
        try {
            javafx.scene.input.KeyCode left = cc.getLeft();
            javafx.scene.input.KeyCode right = cc.getRight();
            javafx.scene.input.KeyCode rotate = cc.getRotate();
            javafx.scene.input.KeyCode down = cc.getDown();
            javafx.scene.input.KeyCode hard = cc.getHard();
            javafx.scene.input.KeyCode sw = cc.getSwitch();
            gui.setControlKeys(left, right, rotate, down, hard);
            gui.setSwapKey(sw);
            java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.comp2042.controller.MainMenuController.class);
            prefs.put(prefsPrefix + "left", left != null ? left.name() : "");
            prefs.put(prefsPrefix + "right", right != null ? right.name() : "");
            prefs.put(prefsPrefix + "rotate", rotate != null ? rotate.name() : "");
            prefs.put(prefsPrefix + "down", down != null ? down.name() : "");
            prefs.put(prefsPrefix + "hard", hard != null ? hard.name() : "");
            prefs.put(prefsPrefix + "switch", sw != null ? sw.name() : "");
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "persistControls failed", ex);
        }
    }

    private void removeOverlayAndRestoreHidden(StackPane overlay) {
        if (overlay == null) return;
        try {
            if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) overlay.getParent();
                root.getChildren().remove(overlay);
            }
            Object o = overlay.getProperties().get("hiddenPauseNodes");
            if (o instanceof List<?>) {
                for (Object n : (List<?>) o) {
                    if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "removeOverlayAndRestoreHidden failed", ex);
        }
    }

    /**
     * Build a winner overlay (rematch/menu handlers provided by caller).
     */
    public StackPane createWinnerOverlay(Scene scene, String title, String reason, Runnable onRematch, Runnable onMenu) {
        if (scene == null) return null;
        try {
            StackPane overlay = new StackPane();
            overlay.setPickOnBounds(true);
            overlay.getStyleClass().add("winner-overlay-strong");

            VBox panel = new VBox(14);
            panel.setAlignment(Pos.CENTER);
            panel.getStyleClass().add("winner-panel");

            javafx.scene.text.Text bigTitle = new javafx.scene.text.Text(title);
            bigTitle.getStyleClass().add("winner-title");
            setupTitleAnimation(bigTitle, overlay);

            javafx.scene.text.Text modeDesc = new javafx.scene.text.Text(reason);
            modeDesc.getStyleClass().add("mode-desc");
            modeDesc.setWrappingWidth(680);

            javafx.scene.layout.HBox btnRow = new javafx.scene.layout.HBox(12);
            btnRow.setAlignment(Pos.CENTER);
            Button btnRestart = new Button("Rematch");
            Button btnMenu = new Button("Main Menu");
            btnRestart.getStyleClass().add("menu-button");
            btnMenu.getStyleClass().add("menu-button");

            btnRestart.setOnAction(ev -> { ev.consume(); try { if (onRematch != null) onRematch.run(); } catch (Exception ex) { LOGGER.log(Level.FINER, "rematch", ex);} });
            btnMenu.setOnAction(ev -> { ev.consume(); try { if (onMenu != null) onMenu.run(); } catch (Exception ex) { LOGGER.log(Level.FINER, "menu", ex);} });

            btnRow.getChildren().addAll(btnRestart, btnMenu);
            panel.getChildren().addAll(bigTitle, modeDesc, btnRow);
            overlay.getChildren().add(panel);

            return overlay;
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "createWinnerOverlay failed", ex);
            return null;
        }
    }

    private void setupTitleAnimation(javafx.scene.text.Text bigTitle, StackPane overlay) {
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
            combined.play();

            try { overlay.getProperties().put("activePulse", combined); } catch (Exception ex) { LOGGER.log(Level.FINER, "store pulse", ex); }
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "setupTitleAnimation failed", ex);
        }
    }

    /**
     * Create and attach the controls overlay to the given scene root.
     * Returns the attached overlay or null on failure.
     */
    public StackPane attachControlsOverlayToScene(Scene scene, GuiController leftGui, GuiController rightGui) {
        if (scene == null) return null;
        try {
            StackPane overlay = createControlsOverlay(scene, leftGui, rightGui);
            if (overlay == null) return null;
            if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                List<javafx.scene.Node> hidden = new ArrayList<>();
                for (javafx.scene.Node n : new ArrayList<>(root.getChildren())) {
                    if (n != null && "GLOBAL_PAUSE_OVERLAY".equals(n.getId())) {
                        n.setVisible(false);
                        hidden.add(n);
                    }
                }
                overlay.getProperties().put("hiddenPauseNodes", hidden);
                root.getChildren().add(overlay);
                overlay.prefWidthProperty().bind(scene.widthProperty());
                overlay.prefHeightProperty().bind(scene.heightProperty());
                StackPane.setAlignment(overlay, Pos.CENTER);
            }
            return overlay;
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "attachControlsOverlayToScene failed", ex);
            return null;
        }
    }

    /**
     * Create and attach the winner overlay to the given scene root.
     * Returns the attached overlay or null on failure.
     */
    public StackPane attachWinnerOverlayToScene(Scene scene, String title, String reason, Runnable onRematch, Runnable onMenu) {
        if (scene == null) return null;
        try {
            StackPane overlay = createWinnerOverlay(scene, title, reason, onRematch, onMenu);
            if (overlay == null) return null;
            if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                root.getChildren().add(overlay);
                overlay.prefWidthProperty().bind(scene.widthProperty());
                overlay.prefHeightProperty().bind(scene.heightProperty());
                StackPane.setAlignment(overlay, Pos.CENTER);
            }
            return overlay;
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "attachWinnerOverlayToScene failed", ex);
            return null;
        }
    }
}
