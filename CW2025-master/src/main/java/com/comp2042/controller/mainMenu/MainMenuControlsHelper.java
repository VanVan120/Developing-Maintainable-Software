package com.comp2042.controller.mainMenu;

import com.comp2042.controller.controls.ControlsController;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Helper to show the multiplayer controls overlay and collect the chosen key bindings.
 * This is extracted from MainMenuController to reduce its size and surface area.
 */
public class MainMenuControlsHelper {

    public static class Result {
        /**
         * Container for chosen key bindings returned by the controls overlays.
         * Fields may be {@code null} if the user left them unset.
         */
        public KeyCode mpLeft_left;
        public KeyCode mpLeft_right;
        public KeyCode mpLeft_rotate;
        public KeyCode mpLeft_down;
        public KeyCode mpLeft_hard;
        public KeyCode mpLeft_switch;

        public KeyCode mpRight_left;
        public KeyCode mpRight_right;
        public KeyCode mpRight_rotate;
        public KeyCode mpRight_down;
        public KeyCode mpRight_hard;
        public KeyCode mpRight_switch;
    }

    /**
     * Show the multiplayer controls overlay.
     *
     * @param loaderClassLoader class loader used to load FXML/resources
     * @param mpLeft_left current left-player left key (may be null)
     * @param mpLeft_right current left-player right key
     * @param mpLeft_rotate current left-player rotate key
     * @param mpLeft_down current left-player down key
     * @param mpLeft_hard current left-player hard-drop key
     * @param mpLeft_switch current left-player switch key
     * @param mpRight_left current right-player left key
     * @param mpRight_right current right-player right key
     * @param mpRight_rotate current right-player rotate key
     * @param mpRight_down current right-player down key
     * @param mpRight_hard current right-player hard-drop key
     * @param mpRight_switch current right-player switch key
     * @param rootStack the root stack where overlay will be added
     * @param controlsOptions the controls options pane (to hide/restore)
     * @param onSave callback invoked with the selected key bindings when Save is pressed
     * @param closeOverlayCaller caller to run close animation: (overlay, onFinished)
     * @param transitionCaller caller to run transitionTo(overlay)
     */
    public void show(ClassLoader loaderClassLoader,
                     KeyCode mpLeft_left, KeyCode mpLeft_right, KeyCode mpLeft_rotate, KeyCode mpLeft_down, KeyCode mpLeft_hard, KeyCode mpLeft_switch,
                     KeyCode mpRight_left, KeyCode mpRight_right, KeyCode mpRight_rotate, KeyCode mpRight_down, KeyCode mpRight_hard, KeyCode mpRight_switch,
                     StackPane rootStack, StackPane controlsOptions,
                     Consumer<Result> onSave,
                     BiConsumer<StackPane, Runnable> closeOverlayCaller,
                     Consumer<StackPane> transitionCaller) {
        try {
            URL loc = loaderClassLoader.getResource("controls.fxml");
            if (loc == null) {
                System.err.println("Cannot find controls.fxml");
                return;
            }

            FXMLLoader fx1 = new FXMLLoader(loc);
            StackPane pane1 = fx1.load();
            ControlsController cc1 = fx1.getController();
            KeyCode defaultLeft_left = (mpLeft_left != null) ? mpLeft_left : KeyCode.A;
            KeyCode defaultLeft_right = (mpLeft_right != null) ? mpLeft_right : KeyCode.D;
            KeyCode defaultLeft_rotate = (mpLeft_rotate != null) ? mpLeft_rotate : KeyCode.W;
            KeyCode defaultLeft_down = (mpLeft_down != null) ? mpLeft_down : KeyCode.S;
            KeyCode defaultLeft_hard = (mpLeft_hard != null) ? mpLeft_hard : KeyCode.SHIFT;
            KeyCode defaultLeft_switch = (mpLeft_switch != null) ? mpLeft_switch : KeyCode.Q;
            cc1.init(defaultLeft_left, defaultLeft_right, defaultLeft_rotate, defaultLeft_down, defaultLeft_hard, defaultLeft_switch);
            try { cc1.setDefaultKeys(defaultLeft_left, defaultLeft_right, defaultLeft_rotate, defaultLeft_down, defaultLeft_hard, defaultLeft_switch); } catch (Exception ignored) {}
            try { cc1.hideActionButtons(); } catch (Exception ignored) {}
            try { cc1.setHeaderText("Left Player Controls"); } catch (Exception ignored) {}

            FXMLLoader fx2 = new FXMLLoader(loc);
            StackPane pane2 = fx2.load();
            ControlsController cc2 = fx2.getController();
            KeyCode defaultRight_switch = (mpRight_switch != null) ? mpRight_switch : KeyCode.C;
            cc2.init(mpRight_left, mpRight_right, mpRight_rotate, mpRight_down, mpRight_hard, defaultRight_switch);
            try { cc2.setDefaultKeys(mpRight_left != null ? mpRight_left : KeyCode.LEFT, mpRight_right != null ? mpRight_right : KeyCode.RIGHT, mpRight_rotate != null ? mpRight_rotate : KeyCode.UP, mpRight_down != null ? mpRight_down : KeyCode.DOWN, mpRight_hard != null ? mpRight_hard : KeyCode.SPACE, defaultRight_switch); } catch (Exception ignored) {}
            try { cc2.hideActionButtons(); } catch (Exception ignored) {}
            try { cc2.setHeaderText("Right Player Controls"); } catch (Exception ignored) {}

            try {
                cc1.setKeyAvailabilityChecker((code, btn) -> {
                    if (code == null) return true;
                    try {
                        return !(code.equals(cc2.getLeft()) || code.equals(cc2.getRight()) || code.equals(cc2.getRotate()) || code.equals(cc2.getDown()) || code.equals(cc2.getHard()) || code.equals(cc2.getSwitch()));
                    } catch (Exception ignored) { return true; }
                });

                cc2.setKeyAvailabilityChecker((code, btn) -> {
                    if (code == null) return true;
                    try {
                        return !(code.equals(cc1.getLeft()) || code.equals(cc1.getRight()) || code.equals(cc1.getRotate()) || code.equals(cc1.getDown()) || code.equals(cc1.getHard()) || code.equals(cc1.getSwitch()));
                    } catch (Exception ignored) { return true; }
                });
            } catch (Exception ignored) {}

            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("menu-overlay");

            Rectangle dark = new Rectangle();
            dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
            dark.getStyleClass().add("menu-overlay-dark");
            Platform.runLater(() -> {
                try {
                    if (overlay.getScene() != null) {
                        dark.widthProperty().bind(overlay.getScene().widthProperty());
                        dark.heightProperty().bind(overlay.getScene().heightProperty());
                    }
                } catch (Exception ignored) {}
            });

            BorderPane container = new BorderPane();
            container.setMaxWidth(Double.MAX_VALUE);
            container.setMaxHeight(Double.MAX_VALUE);
            container.getStyleClass().add("menu-overlay-container");

            // Title (left)
            javafx.scene.text.Text header = new javafx.scene.text.Text("Multiplayer");
            header.getStyleClass().add("menu-overlay-header");
            BorderPane.setAlignment(header, javafx.geometry.Pos.CENTER_LEFT);
            container.setTop(new StackPane(header));

            // action buttons (right)
            HBox actionBox = new HBox(10);
            actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            Button btnReset = new Button("Reset");
            Button btnCancel = new Button("Cancel");
            Button btnSave = new Button("Save");
            btnReset.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
            actionBox.getChildren().addAll(btnReset, btnCancel, btnSave);

            BorderPane topBar = new BorderPane();
            topBar.setLeft(header);
            topBar.setRight(actionBox);
            topBar.getStyleClass().add("menu-overlay-topbar");
            container.setTop(topBar);

            VBox center = new VBox(18);
            center.getStyleClass().add("menu-overlay-center");

            center.getChildren().addAll(pane1, pane2);

            ScrollPane sp = new ScrollPane(center);
            sp.setFitToWidth(true);
            sp.getStyleClass().add("menu-overlay-scroll");
            container.setCenter(sp);

            btnReset.setOnAction(ev -> {
                try {
                    cc1.init(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.Q);
                    try { cc1.setDefaultKeys(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.C); } catch (Exception ignored) {}
                    cc2.init(null, null, null, null, null);
                } catch (Exception ignored) {}
            });

            btnCancel.setOnAction(ev -> {
                ev.consume();
                closeOverlayCaller.accept(overlay, () -> {
                    try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                    try {
                        if (controlsOptions != null) {
                            controlsOptions.setVisible(true);
                            controlsOptions.setTranslateX(0);
                            controlsOptions.setOpacity(1.0);
                        }
                    } catch (Exception ignored) {}
                });
            });

            btnSave.setOnAction(ev -> {
                ev.consume();
                try {
                    KeyCode[] keys = new KeyCode[] {
                        cc1.getLeft(), cc1.getRight(), cc1.getRotate(), cc1.getDown(), cc1.getHard(), cc1.getSwitch(),
                        cc2.getLeft(), cc2.getRight(), cc2.getRotate(), cc2.getDown(), cc2.getHard(), cc2.getSwitch()
                    };
                    Set<KeyCode> set = new HashSet<>();
                    String dupFound = null;
                    for (KeyCode k : keys) {
                        if (k == null) continue;
                        if (set.contains(k)) { dupFound = k.getName(); break; }
                        set.add(k);
                    }
                    if (dupFound != null) {
                        Alert a = new Alert(Alert.AlertType.WARNING);
                        a.setTitle("Duplicate Key");
                        a.setHeaderText("Key already assigned");
                        a.setContentText("The key '" + dupFound + "' is used multiple times across players. Please ensure each key is unique.");
                        a.showAndWait();
                        return;
                    }

                    Result r = new Result();
                    r.mpLeft_left = cc1.getLeft(); r.mpLeft_right = cc1.getRight(); r.mpLeft_rotate = cc1.getRotate(); r.mpLeft_down = cc1.getDown(); r.mpLeft_hard = cc1.getHard(); r.mpLeft_switch = cc1.getSwitch();
                    r.mpRight_left = cc2.getLeft(); r.mpRight_right = cc2.getRight(); r.mpRight_rotate = cc2.getRotate(); r.mpRight_down = cc2.getDown(); r.mpRight_hard = cc2.getHard(); r.mpRight_switch = cc2.getSwitch();

                    try { onSave.accept(r); } catch (Exception ignored) {}

                    closeOverlayCaller.accept(overlay, () -> {
                        try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                        try { if (controlsOptions != null) controlsOptions.setVisible(true); } catch (Exception ignored) {}
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            overlay.getChildren().addAll(dark, container);
            try {
                if (controlsOptions != null) controlsOptions.setVisible(false);
                rootStack.getChildren().add(overlay);
            } catch (Exception ignored) {}
            transitionCaller.accept(overlay);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Show the single-player controls overlay and return the chosen keys via onSave.
     * Parameters mirror the original method moved from MainMenuController but use
     * caller-provided animation/close handlers for consistency with multiplayer.
     */
    public void showSinglePlayerControls(ClassLoader loaderClassLoader,
                                         KeyCode spLeft, KeyCode spRight, KeyCode spRotate, KeyCode spDown, KeyCode spHard, KeyCode spSwitch,
                                         StackPane rootStack, StackPane controlsOptions,
                                         Consumer<Result> onSave,
                                         BiConsumer<StackPane, Runnable> closeOverlayCaller,
                                         Consumer<StackPane> transitionCaller) {
        try {
            URL loc = loaderClassLoader.getResource("controls.fxml");
            if (loc == null) {
                System.err.println("Cannot find controls.fxml");
                return;
            }

            FXMLLoader fx = new FXMLLoader(loc);
            StackPane pane = fx.load();
            com.comp2042.controller.controls.ControlsController cc = fx.getController();

            // Initialize with stored values including switch (if previously set)
            cc.init(spLeft, spRight, spRotate, spDown, spHard, spSwitch);
            try { cc.setHeaderText("Single Player Configuration"); } catch (Exception ignored) {}
            try { cc.hideActionButtons(); } catch (Exception ignored) {}

            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("menu-overlay");

            Rectangle dark = new Rectangle();
            dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
            dark.getStyleClass().add("menu-overlay-dark");
            Platform.runLater(() -> {
                try {
                    if (overlay.getScene() != null) {
                        dark.widthProperty().bind(overlay.getScene().widthProperty());
                        dark.heightProperty().bind(overlay.getScene().heightProperty());
                    }
                } catch (Exception ignored) {}
            });

            BorderPane container = new BorderPane();
            container.setMaxWidth(Double.MAX_VALUE);
            container.setMaxHeight(Double.MAX_VALUE);
            container.getStyleClass().add("menu-overlay-container");

            javafx.scene.text.Text header = new javafx.scene.text.Text("Single Player");
            header.getStyleClass().add("menu-overlay-header");
            BorderPane.setAlignment(header, javafx.geometry.Pos.CENTER_LEFT);

            HBox actionBox = new HBox(10);
            actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            Button btnReset = new Button("Reset");
            Button btnCancel = new Button("Cancel");
            Button btnSave = new Button("Save");
            btnReset.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
            actionBox.getChildren().addAll(btnReset, btnCancel, btnSave);

            BorderPane topBar = new BorderPane();
            topBar.setLeft(header);
            topBar.setRight(actionBox);
            topBar.getStyleClass().add("menu-overlay-topbar");
            container.setTop(topBar);

            VBox center = new VBox(18);
            center.getStyleClass().add("menu-overlay-center");
            center.getChildren().add(pane);
            container.setCenter(center);

            btnReset.setOnAction(ev -> {
                try {
                    cc.resetToDefaults();
                } catch (Exception ignored) {}
            });

            btnCancel.setOnAction(ev -> {
                ev.consume();
                closeOverlayCaller.accept(overlay, () -> {
                    try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                    try {
                        if (controlsOptions != null) {
                            controlsOptions.setVisible(true);
                            controlsOptions.setTranslateX(0);
                            controlsOptions.setOpacity(1.0);
                        }
                    } catch (Exception ignored) {}
                });
            });

            btnSave.setOnAction(ev -> {
                ev.consume();
                try {
                    Result r = new Result();
                    // populate left-player fields with single-player choices
                    r.mpLeft_left = cc.getLeft(); r.mpLeft_right = cc.getRight(); r.mpLeft_rotate = cc.getRotate(); r.mpLeft_down = cc.getDown(); r.mpLeft_hard = cc.getHard(); r.mpLeft_switch = cc.getSwitch();
                    // right-player remains unset for single-player
                    r.mpRight_left = null; r.mpRight_right = null; r.mpRight_rotate = null; r.mpRight_down = null; r.mpRight_hard = null; r.mpRight_switch = null;

                    try { if (onSave != null) onSave.accept(r); } catch (Exception ignored) {}

                    closeOverlayCaller.accept(overlay, () -> {
                        try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                        try { if (controlsOptions != null) controlsOptions.setVisible(true); } catch (Exception ignored) {}
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            overlay.getChildren().addAll(dark, container);
            try {
                if (controlsOptions != null) controlsOptions.setVisible(false);
                rootStack.getChildren().add(overlay);
            } catch (Exception ignored) {}
            transitionCaller.accept(overlay);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
