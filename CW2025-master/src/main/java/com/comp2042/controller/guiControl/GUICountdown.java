package com.comp2042.controller.guiControl;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Small UI helper that encapsulates countdown overlay creation and timeline.
 */
/**
 * Small UI helper that encapsulates creation and lifecycle of a visual
 * countdown used before game-start.
 *
 * <p>The countdown displays numeric ticks and a final "Start" label, runs
 * simple scale/fade animations and coordinates with the supplied
 * {@link GuiCountdownContext} for hiding/refreshing the underlying game UI
 * while the countdown is visible.</p>
 */
public class GuiCountdown {

    private static void ignore(Runnable r) { try { r.run(); } catch (Exception ignored) {} }

    /**
     * Create and return a {@link Timeline} that performs a visual countdown.
     *
     * @param seconds number of seconds to count down (defaults to 3 when <= 0)
     * @param ctx context object providing callbacks and UI nodes used by the countdown
     * @return a configured {@link Timeline} ready to be started, or {@code null}
     *         if {@code ctx} is {@code null}
     */
    public static Timeline startCountdown(int seconds, GuiCountdownContext ctx) {
        if (seconds <= 0) seconds = 3;
        if (ctx == null) return null;

        // mirror behavior from original GuiController.startCountdown as closely as possible
        try { ctx.isPause.setValue(Boolean.TRUE); } catch (Exception ignored) {}
        try { ctx.countdownFinished.setValue(Boolean.FALSE); } catch (Exception ignored) {}
        try { ctx.countdownStarted.setValue(Boolean.FALSE); } catch (Exception ignored) {}

        final Text countdown = new Text();
        try { countdown.getStyleClass().add("gameOverStyle"); } catch (Exception ignored) {}
        try { countdown.setStyle("-fx-font-size: 96px; -fx-fill: yellow; -fx-stroke: black; -fx-stroke-width:2;"); } catch (Exception ignored) {}

        // build overlay
        final StackPane overlay = new StackPane();
        overlay.setPickOnBounds(false);
        overlay.getChildren().add(countdown);
        countdown.setTranslateY(-20);

        // attach overlay if scene exists — ensure hide/refresh and attachment run on FX thread
    Scene sTmp = null;
    try { if (ctx.gameBoard != null) sTmp = ctx.gameBoard.getScene(); } catch (Exception ignored) {}
    final Scene s = sTmp;

        try {
            // request a refresh/snap and hide panels before showing countdown overlay
            Platform.runLater(() -> {
                ignore(() -> { if (ctx.refreshAndSnap != null) ctx.refreshAndSnap.run(); });
                ignore(() -> { if (ctx.hidePanels != null) ctx.hidePanels.run(); });
                if (s != null) {
                    try {
                        overlay.prefWidthProperty().bind(s.widthProperty());
                        overlay.prefHeightProperty().bind(s.heightProperty());
                        overlay.setMouseTransparent(true);
                        if (s.getRoot() instanceof javafx.scene.layout.Pane) {
                            ((javafx.scene.layout.Pane) s.getRoot()).getChildren().add(overlay);
                        } else {
                            ignore(() -> { ctx.groupNotification.getChildren().add(countdown); });
                        }
                    } catch (Exception ex) {
                        ignore(() -> { ctx.groupNotification.getChildren().add(countdown); });
                    }
                } else {
                    ignore(() -> { ctx.groupNotification.getChildren().add(countdown); });
                }
            });
        } catch (Exception ex) {}

        final int[] cnt = new int[]{seconds};
        final int initialCount = seconds;
        final Timeline cd = new Timeline();
        KeyFrame kf = new KeyFrame(Duration.seconds(1), (ae) -> {
            if (cnt[0] > 0) {
                try { if (cnt[0] == initialCount) { if (ctx.playCountdownMusic != null) ctx.playCountdownMusic.run(); ctx.countdownStarted.setValue(Boolean.TRUE); } } catch (Exception ignored) {}
                countdown.setText(Integer.toString(cnt[0]));
                ScaleTransition st = new ScaleTransition(Duration.millis(600), countdown);
                st.setFromX(0.2); st.setFromY(0.2); st.setToX(1.0); st.setToY(1.0);
                FadeTransition ft = new FadeTransition(Duration.millis(600), countdown);
                ft.setFromValue(0.0); ft.setToValue(1.0);
                ParallelTransition pt = new ParallelTransition(st, ft);
                pt.play();
            } else if (cnt[0] == 0) {
                countdown.setText("Start");
                ScaleTransition st = new ScaleTransition(Duration.millis(800), countdown);
                st.setFromX(0.5); st.setFromY(0.5); st.setToX(1.2); st.setToY(1.2);
                FadeTransition ft = new FadeTransition(Duration.millis(800), countdown);
                ft.setFromValue(0.0); ft.setToValue(1.0);
                ParallelTransition pt = new ParallelTransition(st, ft);
                pt.play();
            } else {
                try {
                    Platform.runLater(() -> {
                        try {
                            if (overlay.getParent() instanceof javafx.scene.layout.Pane) {
                                ((javafx.scene.layout.Pane) overlay.getParent()).getChildren().remove(overlay);
                            } else {
                                ignore(() -> { ctx.groupNotification.getChildren().remove(countdown); });
                            }
                        } catch (Exception ex) {}
                        ignore(() -> { if (ctx.refreshVisible != null) ctx.refreshVisible.run(); });
                        ignore(() -> { if (ctx.showPanels != null) ctx.showPanels.run(); });
                        ignore(() -> { if (ctx.gamePanelNode != null) ctx.gamePanelNode.requestFocus(); });
                    });
                } catch (Exception ex) {}
                ignore(() -> { if (ctx.timeLine != null) ctx.timeLine.play(); });
                ignore(() -> { if (ctx.resetClock != null) ctx.resetClock.run(); });
                ignore(() -> { if (ctx.startClock != null) ctx.startClock.run(); });
                ignore(() -> { ctx.isPause.setValue(Boolean.FALSE); });
                ignore(() -> { ctx.countdownFinished.setValue(Boolean.TRUE); });
                ignore(() -> { if (ctx.stopCountdownMusic != null) ctx.stopCountdownMusic.run(); });
                ignore(() -> { ctx.countdownStarted.setValue(Boolean.FALSE); });
                ignore(() -> { cd.stop(); });
            }
            cnt[0] = cnt[0] - 1;
        });
        cd.getKeyFrames().add(kf);
        cd.setCycleCount(seconds + 2);
        return cd;
    }
}
