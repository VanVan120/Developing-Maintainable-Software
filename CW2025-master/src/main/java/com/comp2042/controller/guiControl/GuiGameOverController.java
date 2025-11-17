package com.comp2042.controller.guiControl;

import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import java.net.URL;

public final class GuiGameOverController {
    private GuiGameOverController() {}

    public static void performRestartFromGameOver(GuiController controller, StackPane overlay) {
        try {
            // stop music and clear pulse via controller wrappers
            try { controller.stopGameOverMusicInternal(); } catch (Exception ignored) {}
            try { controller.stopCountdownMusicInternal(); } catch (Exception ignored) {}
            try { controller.stopAndClearGameOverPulseInternal(); } catch (Exception ignored) {}
            if (overlay != null) {
                if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                    ((javafx.scene.layout.Pane) overlay.getParent()).getChildren().remove(overlay);
                } else if (controller.groupNotification != null) {
                    controller.groupNotification.getChildren().remove(overlay);
                }
            }
        } catch (Exception ignored) {}

        if (controller.isMultiplayerEnabled()) {
            try { controller.runMultiplayerRestartHandler(); } catch (Exception ignored) {}
            // Best-effort: stop any lingering music/pulse now and schedule follow-up
            try { controller.stopGameOverMusicInternal(); } catch (Exception ignored) {}
            try { controller.stopCountdownMusicInternal(); } catch (Exception ignored) {}
            try { controller.stopAndClearGameOverPulseInternal(); } catch (Exception ignored) {}
            try {
                // Also schedule a follow-up stop on the JavaFX thread to catch async restarts
                javafx.application.Platform.runLater(() -> {
                    try { controller.stopGameOverMusicInternal(); } catch (Exception ignored) {}
                    try { controller.stopCountdownMusicInternal(); } catch (Exception ignored) {}
                    try { controller.stopAndClearGameOverPulseInternal(); } catch (Exception ignored) {}
                });
                javafx.animation.Timeline t = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), ae -> {
                    try { controller.stopGameOverMusicInternal(); } catch (Exception ignored) {}
                    try { controller.stopCountdownMusicInternal(); } catch (Exception ignored) {}
                    try { controller.stopAndClearGameOverPulseInternal(); } catch (Exception ignored) {}
                }));
                t.setCycleCount(1);
                t.play();
            } catch (Exception ignored) {}
            return;
        }

        try { if (controller.eventListener != null) controller.eventListener.createNewGame(); } catch (Exception ignored) {}
        try {
            if (controller.timeLine != null) controller.timeLine.stop();
            if (controller.gameOverPanel != null) controller.gameOverPanel.setVisible(false);
            try { if (controller.clockManager != null) controller.clockManager.resetClock(); } catch (Exception ignored) {}
            try { if (controller.clockManager != null) controller.clockManager.stopClock(); } catch (Exception ignored) {}
            controller.isPause.setValue(Boolean.TRUE);
            controller.isGameOver.setValue(Boolean.FALSE);
            controller.startCountdown(3);
        } catch (Exception ignored) {}
    }

    public static void performExitToMenuFromGameOver(GuiController controller, StackPane overlay, Scene scene) {
        try {
            try { controller.stopGameOverMusicInternal(); } catch (Exception ignored) {}
            try { controller.stopCountdownMusicInternal(); } catch (Exception ignored) {}
            try { controller.stopAndClearGameOverPulseInternal(); } catch (Exception ignored) {}
            if (controller.isMultiplayerEnabled()) {
                try { controller.detachSceneKeyHandlersInternal(); } catch (Exception ignored) {}
                try { controller.runMultiplayerExitToMenuHandler(); } catch (Exception ignored) {}
                return;
            }
            try { controller.detachSceneKeyHandlersInternal(); } catch (Exception ignored) {}
            try { controller.stopSingleplayerMusicInternal(); } catch (Exception ignored) {}
            URL loc = controller.getClass().getClassLoader().getResource("mainMenu.fxml");
            if (loc == null) return;
            FXMLLoader loader = new FXMLLoader(loc);
            Parent menuRoot = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) scene.getWindow();
            if (stage.getScene() != null) {
                stage.getScene().setRoot(menuRoot);
                try {
                    String css = controller.getClass().getClassLoader().getResource("css/menu.css").toExternalForm();
                    if (!stage.getScene().getStylesheets().contains(css)) stage.getScene().getStylesheets().add(css);
                } catch (Exception ignored) {}
            } else {
                Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                try {
                    String css = controller.getClass().getClassLoader().getResource("css/menu.css").toExternalForm();
                    s2.getStylesheets().add(css);
                } catch (Exception ignored) {}
                stage.setScene(s2);
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
