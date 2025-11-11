package com.comp2042.controller.cooperateBattle.coopGUI;

import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

/**
 * Extracted controls overlay builder. Keeps the logic previously embedded in
 * CoopGuiController.showMultiplayerControlsOverlay but lives in its own file.
 */
public class CoopControlsOverlay {
    public static class KeySet {
        public javafx.scene.input.KeyCode left, right, rotate, down, hard, swap;
        public KeySet(javafx.scene.input.KeyCode left, javafx.scene.input.KeyCode right, javafx.scene.input.KeyCode rotate, javafx.scene.input.KeyCode down, javafx.scene.input.KeyCode hard, javafx.scene.input.KeyCode swap) {
            this.left = left; this.right = right; this.rotate = rotate; this.down = down; this.hard = hard; this.swap = swap;
        }
    }

    public static void show(Scene scene, KeySet leftCurrent, KeySet leftDefaults, KeySet rightCurrent, KeySet rightDefaults, java.util.function.BiConsumer<KeySet,KeySet> onSave, Runnable onCancel, Runnable onReset) {
        javafx.application.Platform.runLater(() -> {
            try {
                if (scene == null) return;

                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                Rectangle dark = new Rectangle();
                dark.widthProperty().bind(scene.widthProperty());
                dark.heightProperty().bind(scene.heightProperty());
                dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));

                BorderPane container = new BorderPane();
                container.getStyleClass().add("coop-overlay-container");

                Text header = new Text("Controls");
                header.getStyleClass().add("coop-overlay-header");
                HBox actionBox = new HBox(10);
                actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
                Button btnResetTop = new Button("Reset");
                Button btnCancel = new Button("Cancel");
                Button btnSave = new Button("Save");
                btnResetTop.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
                actionBox.getChildren().addAll(btnResetTop, btnCancel, btnSave);
                BorderPane topBar = new BorderPane();
                topBar.setLeft(header);
                topBar.setRight(actionBox);
                topBar.getStyleClass().add("coop-overlay-topbar");
                container.setTop(topBar);

                HBox center = new HBox(120);
                center.getStyleClass().add("coop-overlay-center");
                center.setAlignment(javafx.geometry.Pos.CENTER);

                FXMLLoader leftFx = new FXMLLoader(Thread.currentThread().getContextClassLoader().getResource("controls.fxml"));
                StackPane leftPane = leftFx.load();
                com.comp2042.controller.controls.ControlsController leftCC = leftFx.getController();

                FXMLLoader rightFx = new FXMLLoader(Thread.currentThread().getContextClassLoader().getResource("controls.fxml"));
                StackPane rightPane = rightFx.load();
                com.comp2042.controller.controls.ControlsController rightCC = rightFx.getController();

                // initialize with current keys and set panel defaults
                try { leftCC.init(leftCurrent.left, leftCurrent.right, leftCurrent.rotate, leftCurrent.down, leftCurrent.hard, leftCurrent.swap); } catch (Exception ignored) {}
                try { rightCC.init(rightCurrent.left, rightCurrent.right, rightCurrent.rotate, rightCurrent.down, rightCurrent.hard, rightCurrent.swap); } catch (Exception ignored) {}
                try { leftCC.setDefaultKeys(leftDefaults.left, leftDefaults.right, leftDefaults.rotate, leftDefaults.down, leftDefaults.hard, leftDefaults.swap); } catch (Exception ignored) {}
                try { rightCC.setDefaultKeys(rightDefaults.left, rightDefaults.right, rightDefaults.rotate, rightDefaults.down, rightDefaults.hard, rightDefaults.swap); } catch (Exception ignored) {}

                try { leftCC.hideActionButtons(); } catch (Exception ignored) {}
                try { rightCC.hideActionButtons(); } catch (Exception ignored) {}

                try { leftPane.setPrefWidth(520); } catch (Exception ignored) {}
                try { rightPane.setPrefWidth(520); } catch (Exception ignored) {}
                center.getChildren().addAll(leftPane, rightPane);
                container.setCenter(center);

                overlay.getChildren().addAll(dark, container);

                if (scene.getRoot() instanceof Pane) {
                    Pane root = (Pane) scene.getRoot();
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

                btnResetTop.setOnAction(ev -> {
                    ev.consume();
                    try { leftCC.resetToPanelDefaults(); } catch (Exception ignored) {}
                    try { rightCC.resetToPanelDefaults(); } catch (Exception ignored) {}
                    if (onReset != null) try { onReset.run(); } catch (Exception ignored) {}
                });

                btnSave.setOnAction(ev -> {
                    ev.consume();
                    try {
                        KeySet lks = new KeySet(leftCC.getLeft(), leftCC.getRight(), leftCC.getRotate(), leftCC.getDown(), leftCC.getHard(), leftCC.getSwitch());
                        KeySet rks = new KeySet(rightCC.getLeft(), rightCC.getRight(), rightCC.getRotate(), rightCC.getDown(), rightCC.getHard(), rightCC.getSwitch());
                        if (onSave != null) onSave.accept(lks, rks);
                    } catch (Exception ignored) {}
                    try {
                        if (overlay.getParent() instanceof Pane) {
                            Pane root = (Pane) overlay.getParent();
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
                        if (overlay.getParent() instanceof Pane) {
                            Pane root = (Pane) overlay.getParent();
                            root.getChildren().remove(overlay);
                        }
                        Object o = overlay.getProperties().get("hiddenPauseNodes");
                        if (o instanceof java.util.List<?>) {
                            for (Object n : (java.util.List<?>) o) {
                                if (n instanceof javafx.scene.Node) ((javafx.scene.Node) n).setVisible(true);
                            }
                        }
                        if (onCancel != null) onCancel.run();
                    } catch (Exception ignored) {}
                });

            } catch (Exception ignored) {}
        });
    }
}
