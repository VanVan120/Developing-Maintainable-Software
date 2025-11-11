package com.comp2042.controller.controls;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Small UI helper that encapsulates common view operations used by ControlsController.
 * Keeps warnings, button text and hover effects out of the controller so the controller
 * can focus on event wiring and key-binding logic.
 */
public class ControlsView {
    private final Label lblInfo;
    private final Label lblHeader;
    private final Button btnReset;
    private final Button btnSave;
    private final Button btnCancel;
    private Timeline warningTimeline = null;
    private static final Logger LOGGER = Logger.getLogger(ControlsView.class.getName());

    public ControlsView(Label lblInfo, Label lblHeader, Button btnReset, Button btnSave, Button btnCancel) {
        this.lblInfo = lblInfo;
        this.lblHeader = lblHeader;
        this.btnReset = btnReset;
        this.btnSave = btnSave;
        this.btnCancel = btnCancel;
    }

    public void setButtonKey(Button b, javafx.scene.input.KeyCode code, boolean isInteractive) {
        if (b == null) return;
        b.setText(code != null ? code.getName() : "[NOT SET]");
        b.setDisable(!isInteractive);
    }

    public void addHoverEffect(Button b) {
        if (b == null) return;
        b.setOnMouseEntered(e -> {
            if (!b.getStyleClass().contains("keybind-hover")) b.getStyleClass().add("keybind-hover");
        });
        b.setOnMouseExited(e -> b.getStyleClass().remove("keybind-hover"));
    }

    public void showInlineWarning(String message) {
        if (lblInfo == null) return;
        setInfoText(message);
        if (!lblInfo.getStyleClass().contains("warning")) lblInfo.getStyleClass().add("warning");
        if (warningTimeline != null) warningTimeline.stop();
        warningTimeline = new Timeline(new KeyFrame(Duration.seconds(3), event -> {
            lblInfo.getStyleClass().remove("warning");
            setInfoText("");
        }));
        warningTimeline.setCycleCount(1);
        warningTimeline.play();
        try {
            Platform.runLater(() -> {
                try {
                    Alert a = new Alert(Alert.AlertType.WARNING);
                    try {
                        if (lblInfo.getScene() != null && lblInfo.getScene().getWindow() != null) {
                            a.initOwner(lblInfo.getScene().getWindow());
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.FINER, "Could not init owner for warning dialog", e);
                    }
                    a.initModality(javafx.stage.Modality.APPLICATION_MODAL);
                    a.setTitle("Key already assigned");
                    a.setHeaderText("Key already assigned");
                    a.setContentText(message);
                    a.showAndWait();
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to show inline warning dialog", e);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Platform.runLater failed for inline warning", e);
        }
    }

    public void setInfoText(String text) {
        if (lblInfo != null) {
            lblInfo.setText(text != null ? text : "");
        }
    }

    public void hideActionButtons() {
        try {
            if (btnReset != null) { btnReset.setVisible(false); btnReset.setManaged(false); }
            if (btnSave != null) { btnSave.setVisible(false); btnSave.setManaged(false); }
            if (btnCancel != null) { btnCancel.setVisible(false); btnCancel.setManaged(false); }
            if (lblInfo != null) { lblInfo.setVisible(false); lblInfo.setManaged(false); }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to hide action buttons", e);
        }
    }

    public void setHeaderText(String text) {
        try {
            if (lblHeader != null) lblHeader.setText(text != null ? text : "");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set header text", e);
        }
    }
}
