package com.comp2042.controller.mainMenu;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.control.Button;
import javafx.application.Platform;

import java.net.URL;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Data holder and UI helper for gameplay handling settings used by the
 * main menu.
 *
 * <p>Fields represent commonly-tunable values (auto-repeat rate, DAS/DCD,
 * speed drop factor, and whether hard-drop is enabled). Fields are public
 * to allow simple copy/assign semantics when loading/saving preferences.
 *
 * <p>The {@code showHandlingControls} helper builds and displays an
 * overlay that allows the user to edit these values. The helper adds the
 * overlay to the provided {@code rootStack} and hides/restores the
 * {@code settingsOptions} pane; it returns the chosen values through the
 * {@code onSave} callback.
 *
 * <p>Threading & side-effects:
 * - The method performs JavaFX UI operations and uses {@code
 *   Platform.runLater} for scene-dependent bindings; callers should invoke
 *   it from the FX thread or accept that the helper will schedule UI work.
 * - The method writes to the scene graph (adds/removes nodes) and therefore
 *   may throw runtime exceptions in restricted environments; it swallows
 *   many exceptions to remain resilient but does print stack traces on
 *   unexpected errors.
 */
public class MainMenuHandlingSettings {
    public int settingArrMs = 50;
    public int settingDasMs = 120;
    public int settingDcdMs = 20;
    public double settingSdf = 1.0;
    public boolean settingHardDropEnabled = true;

    /**
     * Display the handling settings overlay and invoke {@code onSave} when the
     * user presses Save.
     *
     * @param loaderClassLoader class loader used to locate FXML/resources
     * @param arrMs initial auto-repeat rate (ms)
     * @param dasMs initial DAS (ms)
     * @param dcdMs initial DCD (ms)
     * @param sdf initial soft-drop factor (multiplier)
     * @param hardDropEnabled initial hard-drop enabled flag
     * @param rootStack root {@link StackPane} where the overlay will be added
     * @param settingsOptions the settings pane to hide while the overlay is shown
     * @param onSave callback invoked with chosen settings when Save is pressed
     *               (may be {@code null})
     * @param closeOverlayCaller function used to close the overlay with an
     *                          animation: {@code (overlay, onFinished)}
     * @param transitionCaller function used to run the opening transition for
     *                         the overlay: {@code (overlay)}
     */
    public void showHandlingControls(ClassLoader loaderClassLoader,
                                     int arrMs, int dasMs, int dcdMs, double sdf, boolean hardDropEnabled,
                                     StackPane rootStack, StackPane settingsOptions,
                                     Consumer<MainMenuHandlingSettings> onSave,
                                     BiConsumer<StackPane, Runnable> closeOverlayCaller,
                                     Consumer<StackPane> transitionCaller) {
        try {
            URL loc = loaderClassLoader.getResource("handling.fxml");
            if (loc == null) {
                System.err.println("Cannot find handling.fxml");
                return;
            }

            FXMLLoader fx = new FXMLLoader(loc);
            StackPane pane = fx.load();
            com.comp2042.controller.handlingControl.HandlingController hc = fx.getController();

            // initialize with stored values
            hc.init(arrMs, dasMs, dcdMs, sdf, hardDropEnabled);
            try { hc.setHeaderText("Handling"); } catch (Exception ignored) {}
            try { hc.hideActionButtons(); } catch (Exception ignored) {}

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

            javafx.scene.text.Text header = new javafx.scene.text.Text("Handling");
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

            // Reset
            btnReset.setOnAction(ev -> {
                try { hc.resetToDefaults(); } catch (Exception ignored) {}
            });

            // Cancel
            btnCancel.setOnAction(ev -> {
                ev.consume();
                closeOverlayCaller.accept(overlay, () -> {
                    try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                    try {
                        if (settingsOptions != null) {
                            settingsOptions.setVisible(true);
                            settingsOptions.setTranslateX(0);
                            settingsOptions.setOpacity(1.0);
                        }
                    } catch (Exception ignored) {}
                });
            });

            // Save persist settings
            btnSave.setOnAction(ev -> {
                ev.consume();
                try {
                    MainMenuHandlingSettings out = new MainMenuHandlingSettings();
                    out.settingArrMs = hc.getArrMs();
                    out.settingDasMs = hc.getDasMs();
                    out.settingDcdMs = hc.getDcdMs();
                    out.settingSdf = hc.getSdf();
                    out.settingHardDropEnabled = hc.isHardDropEnabled();
                    // callback
                    try { if (onSave != null) onSave.accept(out); } catch (Exception ignored) {}
                    closeOverlayCaller.accept(overlay, () -> {
                        try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                        try { if (settingsOptions != null) settingsOptions.setVisible(true); } catch (Exception ignored) {}
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            overlay.getChildren().addAll(dark, container);
            try { if (settingsOptions != null) settingsOptions.setVisible(false); rootStack.getChildren().add(overlay); } catch (Exception ignored) {}
            transitionCaller.accept(overlay);

        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
