package com.comp2042.controller.guiControl;

import javafx.scene.effect.DropShadow;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.geometry.Pos;
import javafx.scene.paint.Color;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.HBox;
import javafx.scene.control.Button;

/**
 * Small UI helper that provides game-over title, pulse animation and subtitle builder.
 */
/**
 * UI building helpers for the game-over overlay. Provides a title node,
 * pulsing animation and a convenience method that assembles and shows a
 * fully interactive game-over dialog wired to the provided controller.
 */
public final class GuiGameOverUI {

    private GuiGameOverUI() {}

    /** Create a styled title {@link Text} node for the game-over dialog. */
    public static Text createGameOverTitle() {
        Text title = new Text("Match Over");
        try { title.setStyle("-fx-font-weight: 700;"); } catch (Exception ignored) {}
        try { title.setFill(javafx.scene.paint.LinearGradient.valueOf("from 0% 0% to 100% 0% , #ffd166 0%, #ff7b7b 100%")); } catch (Exception ignored) {}
        try { title.setOpacity(1.0); } catch (Exception ignored) {}
        try { title.setFont(Font.font(72)); } catch (Exception ignored) {}
        return title;
    }

    /**
     * Start a pulsing animation which emphasises the provided title node.
     * Returns the running {@link javafx.animation.Animation} so callers may stop it.
     */
    public static javafx.animation.Animation startGameOverPulse(Text title) {
        try {
            DropShadow glow = new DropShadow();
            glow.setColor(javafx.scene.paint.Color.web("#ffd166"));
            glow.setRadius(10);
            glow.setSpread(0.45);
            title.setEffect(glow);

            ScaleTransition scalePulse = new ScaleTransition(javafx.util.Duration.millis(900), title);
            scalePulse.setFromX(1.0); scalePulse.setFromY(1.0);
            scalePulse.setToX(1.05); scalePulse.setToY(1.05);
            scalePulse.setCycleCount(javafx.animation.Animation.INDEFINITE);
            scalePulse.setAutoReverse(true);

            Timeline glowTimeline = new Timeline(
                new KeyFrame(javafx.util.Duration.ZERO, new KeyValue(glow.radiusProperty(), 10)),
                new KeyFrame(javafx.util.Duration.millis(900), new KeyValue(glow.radiusProperty(), 36))
            );
            glowTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            glowTimeline.setAutoReverse(true);

            Timeline colorPulse = new Timeline(
                new KeyFrame(javafx.util.Duration.ZERO, ae -> { title.setFill(Color.WHITE); }),
                new KeyFrame(javafx.util.Duration.millis(420), ae -> { title.setFill(Color.web("#ffd166")); }),
                new KeyFrame(javafx.util.Duration.millis(900), ae -> { title.setFill(Color.WHITE); })
            );
            colorPulse.setCycleCount(javafx.animation.Animation.INDEFINITE);

            ParallelTransition combined = new ParallelTransition(scalePulse, glowTimeline, colorPulse);
            combined.setCycleCount(javafx.animation.Animation.INDEFINITE);
            combined.play();
            return combined;
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Build a subtitle box showing score/time and previous-high information.
     * This helper returns a small {@link VBox} suitable for inclusion in the dialog.
     */
    public static VBox buildGameOverSubtitleBox(String scoreStr, int currentScore, String timePlayed, int prevHighBeforeGame) {
        Text scoreText = new Text("Score: " + (scoreStr.isEmpty() ? ("0") : scoreStr));
        try { scoreText.setStyle("-fx-font-size: 28px; -fx-fill: white; -fx-opacity: 0.95;"); } catch (Exception ignored) {}
        Text timeText = new Text("Time: " + timePlayed);
        try { timeText.setStyle("-fx-font-size: 22px; -fx-fill: #dddddd; -fx-opacity: 0.95;"); } catch (Exception ignored) {}

        Text compareText = new Text();
        try {
            if (prevHighBeforeGame <= 0) {
                compareText.setText("No previous record");
                compareText.setStyle("-fx-font-size: 18px; -fx-fill: #cccccc;");
            } else if (currentScore >= 0 && currentScore > prevHighBeforeGame) {
                compareText.setText("New High Score! Previous: " + prevHighBeforeGame);
                compareText.setStyle("-fx-font-size: 20px; -fx-fill: #ffd166; -fx-font-weight: bold;");
            } else {
                compareText.setText("Previous Best: " + prevHighBeforeGame);
                compareText.setStyle("-fx-font-size: 18px; -fx-fill: #cccccc;");
            }
        } catch (Exception ignored) { compareText.setText(""); }

        VBox subtitleBox = new VBox(6);
        try { subtitleBox.setAlignment(Pos.CENTER); subtitleBox.getChildren().addAll(scoreText, timeText, compareText); } catch (Exception ignored) {}
        return subtitleBox;
    }

    /**
     * Build and show the game-over overlay for the given controller. Runs UI
     * work on the JavaFX thread and wires buttons to call back into the
     * controller for restart / exit behaviour.
     */
    public static void showGameOver(GuiController controller) {
        if (controller == null) return;
        javafx.application.Platform.runLater(() -> {
            try {
                if (controller.gameBoard == null || controller.gameBoard.getScene() == null) return;
                Scene scene = controller.gameBoard.getScene();

                // overlay container
                StackPane overlay = new StackPane();
                try { overlay.setPickOnBounds(true); } catch (Exception ignored) {}
                try { overlay.setStyle("-fx-background-color: transparent;"); } catch (Exception ignored) {}

                Rectangle dark = new Rectangle();
                try { dark.widthProperty().bind(scene.widthProperty()); dark.heightProperty().bind(scene.heightProperty()); } catch (Exception ignored) {}
                try { dark.setFill(Color.rgb(0,0,0,0.95)); } catch (Exception ignored) {}

                VBox dialog = new VBox(14);
                try { dialog.setAlignment(Pos.CENTER); dialog.setMouseTransparent(false); dialog.setStyle("-fx-background-color: rgba(0,0,0,1.0); -fx-padding: 18px; -fx-background-radius: 8px;"); } catch (Exception ignored) {}

                Text title = createGameOverTitle();
                javafx.animation.Animation pulse = startGameOverPulse(title);
                try { controller.setGameOverPulse(pulse); } catch (Exception ignored) {}

                // compute score/time values (use controller getter if available)
                int currentScore = -1;
                try { currentScore = controller.getCurrentScore(); } catch (Exception ignored) {}
                String scoreStr = currentScore >= 0 ? Integer.toString(currentScore) : "";
                String timePlayed = (controller.timeValue != null) ? controller.timeValue.getText() : "00:00";

                VBox subtitleBox = buildGameOverSubtitleBox(scoreStr, currentScore, timePlayed, controller.getPrevHighBeforeGame());

                // buttons
                HBox buttons = new HBox(12);
                try { buttons.setAlignment(Pos.CENTER); } catch (Exception ignored) {}

                Button btnRestart = new Button("Restart");
                Button btnMenu = new Button("Main Menu");
                try { btnRestart.getStyleClass().add("menu-button"); btnMenu.getStyleClass().add("menu-button"); } catch (Exception ignored) {}
                try { controller.attachButtonSoundHandlers(btnRestart); } catch (Exception ignored) {}
                try { controller.attachButtonSoundHandlers(btnMenu); } catch (Exception ignored) {}

                btnRestart.setOnAction(ev -> {
                    ev.consume();
                    try {
                        controller.performRestartFromGameOver(overlay);
                    } catch (Exception ex) { ex.printStackTrace(); }
                    // extra guard: ensure any GameOver music is stopped on the sound manager
                    try { if (controller.getSoundManager() != null) controller.getSoundManager().stopGameOverMusic(); } catch (Exception ignored) {}
                    try { if (controller.getSoundManager() != null) controller.getSoundManager().stopCountdownMusic(); } catch (Exception ignored) {}
                    try { if (controller.getSoundManager() != null) controller.getSoundManager().stopSingleplayerMusic(); } catch (Exception ignored) {}
                });

                btnMenu.setOnAction(ev -> {
                    ev.consume();
                    try { controller.performExitToMenuFromGameOver(overlay, scene); } catch (Exception ex) { ex.printStackTrace(); }
                    // extra guard: stop any game-over/countdown music
                    try { if (controller.getSoundManager() != null) controller.getSoundManager().stopGameOverMusic(); } catch (Exception ignored) {}
                    try { if (controller.getSoundManager() != null) controller.getSoundManager().stopCountdownMusic(); } catch (Exception ignored) {}
                });

                buttons.getChildren().addAll(btnRestart, btnMenu);

                dialog.getChildren().addAll(title, subtitleBox, buttons);
                dialog.setTranslateY(0);

                overlay.setOnMouseClicked(event -> event.consume());
                overlay.getChildren().addAll(dark, dialog);

                // store dialog node for later animations
                try { overlay.getProperties().put("dialogNode", dialog); } catch (Exception ignored) {}

                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    root.getChildren().add(overlay);
                } else if (controller.groupNotification != null) {
                    controller.groupNotification.getChildren().add(overlay);
                }

                // subtle entrance animation
                try {
                    Object dlg = overlay.getProperties().get("dialogNode");
                    if (dlg instanceof javafx.scene.Node) {
                        javafx.scene.Node dialogNode = (javafx.scene.Node) dlg;
                        javafx.animation.FadeTransition f = new javafx.animation.FadeTransition(javafx.util.Duration.millis(420), dialogNode);
                        dialogNode.setOpacity(0.0);
                        f.setFromValue(0.0);
                        f.setToValue(1.0);
                        f.play();
                    }
                } catch (Exception ignored) {}

            } catch (Exception ignored) {}
        });
    }
}
