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
 * Simple data holder for handling settings. Also provides a helper to show
 * the handling overlay UI and return modified settings via a callback.
 */
public class MainMenuHandlingSettings {
    public int settingArrMs = 50;
    public int settingDasMs = 120;
    public int settingDcdMs = 20;
    public double settingSdf = 1.0;
    public boolean settingHardDropEnabled = true;

    /**
     * Show the handling overlay. On Save the provided `onSave` will be called
     * with a MainMenuHandlingSettings instance containing the chosen values.
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
