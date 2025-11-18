package com.comp2042.controller.scoreBattle;

import com.comp2042.controller.controls.ControlsController;
import com.comp2042.controller.guiControl.GuiController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import java.util.prefs.Preferences;

/**
 * Helper class that builds and shows the multiplayer controls overlay used by
 * `ScoreBattleController`. The original large method was moved here and split
 * into smaller helpers for readability and maintainability.
 */
public class ScoreBattleControlsOverlay {
    private final Scene scene;
    private final GuiController leftGui;
    private final GuiController rightGui;

    /**
     * Create an overlay helper that builds and shows the multiplayer controls
     * panel inside the supplied `Scene` and for the two embedded GUI
     * controllers.
     *
     * @param scene the host scene where the overlay will be attached; may be null
     * @param leftGui left player's `GuiController`; may be null
     * @param rightGui right player's `GuiController`; may be null
     */
    public ScoreBattleControlsOverlay(Scene scene, GuiController leftGui, GuiController rightGui) {
        this.scene = scene;
        this.leftGui = leftGui;
        this.rightGui = rightGui;
    }

    /**
     * Public entry point to display the controls overlay.
     *
     * Ensures construction and scene-graph changes occur on the JavaFX
     * Application Thread. The provided `requester` will be passed to the
     * internal implementation to allow returning focus when the overlay closes.
     *
     * @param requester the GUI controller that requested the overlay; may be null
     */
    public void show(com.comp2042.controller.guiControl.GuiController requester) {
        // run on FX thread if not already
        javafx.application.Platform.runLater(() -> {
            try {
                showInternal(requester);
            } catch (Exception ignored) {}
        });
    }

    /**
     * Internal implementation that constructs the overlay UI and attaches it
     * to the scene. This method mutates the scene graph and therefore must be
     * called on the JavaFX Application Thread. It may throw exceptions when
     * loading FXML resources.
     *
     * @param requester the GUI that requested the overlay; may be null
     * @throws Exception when FXML loading or initialization fails
     */
    private void showInternal(com.comp2042.controller.guiControl.GuiController requester) throws Exception {
        if (scene == null) return;

        StackPane overlay = new StackPane();
        overlay.setPickOnBounds(true);

        Rectangle dark = new Rectangle();
        dark.widthProperty().bind(scene.widthProperty());
        dark.heightProperty().bind(scene.heightProperty());
        dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));

        BorderPane container = new BorderPane();
        container.setStyle("-fx-padding:18;");

        // Top bar
        javafx.scene.text.Text header = new javafx.scene.text.Text("Controls");
        header.setStyle("-fx-font-size:34px; -fx-fill: #9fb0ff; -fx-font-weight:700;");
        HBox actionBox = new HBox(10);
        actionBox.setAlignment(Pos.CENTER_RIGHT);
        Button btnResetTop = new Button("Reset");
        Button btnCancel = new Button("Cancel");
        Button btnSave = new Button("Save");
        btnResetTop.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
        actionBox.getChildren().addAll(btnResetTop, btnCancel, btnSave);
        BorderPane topBar = new BorderPane();
        topBar.setLeft(header);
        topBar.setRight(actionBox);
        topBar.setStyle("-fx-padding:8 18 18 18;");
        container.setTop(topBar);

        HBox center = new HBox(120);
        center.setStyle("-fx-padding:12; -fx-background-color: transparent;");
        center.setAlignment(Pos.CENTER);

        // Load two controls panes
        FXMLLoader leftFx = new FXMLLoader(getClass().getClassLoader().getResource("controls.fxml"));
        StackPane leftPane = leftFx.load();
        ControlsController leftCC = leftFx.getController();

        FXMLLoader rightFx = new FXMLLoader(getClass().getClassLoader().getResource("controls.fxml"));
        StackPane rightPane = rightFx.load();
        ControlsController rightCC = rightFx.getController();

        // init panels from running GUIs
        initPanels(leftPane, leftCC, rightPane, rightCC);

        // Hide embedded action buttons and set widths
        try { leftCC.hideActionButtons(); } catch (Exception ignored) {}
        try { rightCC.hideActionButtons(); } catch (Exception ignored) {}
        try { leftPane.setPrefWidth(520); } catch (Exception ignored) {}
        try { rightPane.setPrefWidth(520); } catch (Exception ignored) {}

        center.getChildren().addAll(leftPane, rightPane);
        container.setCenter(center);
        overlay.getChildren().addAll(dark, container);

        // add overlay to root and hide existing GLOBAL_PAUSE_OVERLAY nodes
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

        // actions
        btnResetTop.setOnAction(ev -> {
            ev.consume();
            try { leftCC.resetToPanelDefaults(); } catch (Exception ignored) {}
            try { rightCC.resetToPanelDefaults(); } catch (Exception ignored) {}
        });

        btnSave.setOnAction(ev -> {
            ev.consume();
            try { applyAndPersist(leftCC, rightCC); } catch (Exception ignored) {}
            closeAndRestore(overlay);
        });

        btnCancel.setOnAction(ev -> {
            ev.consume();
            closeAndRestore(overlay);
        });
    }

    /**
     * Initialize the two controls panels from the running GUIs and stored
     * preferences. Applies sensible defaults when preferences are missing and
     * sets up key-availability checks to prevent duplicate bindings.
     */
    private void initPanels(StackPane leftPane, ControlsController leftCC, StackPane rightPane, ControlsController rightCC) {
        Preferences overlayPrefs = Preferences.userNodeForPackage(com.comp2042.controller.mainMenu.MainMenuController.class);
        try {
            leftCC.init(leftGui.getCtrlMoveLeft() != null ? leftGui.getCtrlMoveLeft() : javafx.scene.input.KeyCode.A,
                        leftGui.getCtrlMoveRight() != null ? leftGui.getCtrlMoveRight() : javafx.scene.input.KeyCode.D,
                        leftGui.getCtrlRotate() != null ? leftGui.getCtrlRotate() : javafx.scene.input.KeyCode.W,
                        leftGui.getCtrlSoftDrop() != null ? leftGui.getCtrlSoftDrop() : javafx.scene.input.KeyCode.S,
                        leftGui.getCtrlHardDrop() != null ? leftGui.getCtrlHardDrop() : javafx.scene.input.KeyCode.SHIFT,
                        leftGui.getCtrlSwap() != null ? leftGui.getCtrlSwap() : javafx.scene.input.KeyCode.Q);

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
        } catch (Exception ignored) {}

        // Prevent duplicate keys
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
                } catch (Exception ignored) { return true; }
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
                } catch (Exception ignored) { return true; }
            });
        } catch (Exception ignored) {}
    }

    /**
     * Apply the selected key bindings from the controls panels to the running
     * `GuiController` instances and persist them into `Preferences`.
     *
     * This operation is best-effort and catches exceptions internally to
     * avoid leaving the overlay in an inconsistent state.
     */
    private void applyAndPersist(ControlsController leftCC, ControlsController rightCC) {
        try {
            // left
            try {
                javafx.scene.input.KeyCode lLeft = leftCC.getLeft();
                javafx.scene.input.KeyCode lRight = leftCC.getRight();
                javafx.scene.input.KeyCode lRotate = leftCC.getRotate();
                javafx.scene.input.KeyCode lDown = leftCC.getDown();
                javafx.scene.input.KeyCode lHard = leftCC.getHard();
                javafx.scene.input.KeyCode lSwap = leftCC.getSwitch();
                leftGui.setControlKeys(lLeft, lRight, lRotate, lDown, lHard);
                leftGui.setSwapKey(lSwap);
                Preferences prefs = Preferences.userNodeForPackage(com.comp2042.controller.mainMenu.MainMenuController.class);
                prefs.put("mpLeft_left", lLeft != null ? lLeft.name() : "");
                prefs.put("mpLeft_right", lRight != null ? lRight.name() : "");
                prefs.put("mpLeft_rotate", lRotate != null ? lRotate.name() : "");
                prefs.put("mpLeft_down", lDown != null ? lDown.name() : "");
                prefs.put("mpLeft_hard", lHard != null ? lHard.name() : "");
                prefs.put("mpLeft_switch", lSwap != null ? lSwap.name() : "");
            } catch (Exception ignored) {}

            // right
            try {
                javafx.scene.input.KeyCode rLeft = rightCC.getLeft();
                javafx.scene.input.KeyCode rRight = rightCC.getRight();
                javafx.scene.input.KeyCode rRotate = rightCC.getRotate();
                javafx.scene.input.KeyCode rDown = rightCC.getDown();
                javafx.scene.input.KeyCode rHard = rightCC.getHard();
                javafx.scene.input.KeyCode rSwap = rightCC.getSwitch();
                rightGui.setControlKeys(rLeft, rRight, rRotate, rDown, rHard);
                rightGui.setSwapKey(rSwap);
                Preferences prefs = Preferences.userNodeForPackage(com.comp2042.controller.mainMenu.MainMenuController.class);
                prefs.put("mpRight_left", rLeft != null ? rLeft.name() : "");
                prefs.put("mpRight_right", rRight != null ? rRight.name() : "");
                prefs.put("mpRight_rotate", rRotate != null ? rRotate.name() : "");
                prefs.put("mpRight_down", rDown != null ? rDown.name() : "");
                prefs.put("mpRight_hard", rHard != null ? rHard.name() : "");
                prefs.put("mpRight_switch", rSwap != null ? rSwap.name() : "");
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    /**
     * Close the overlay and restore any nodes which were temporarily hidden
     * (for example, the global pause overlay). Safe to call when the overlay
     * is already removed.
     */
    private void closeAndRestore(StackPane overlay) {
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
    }
}
