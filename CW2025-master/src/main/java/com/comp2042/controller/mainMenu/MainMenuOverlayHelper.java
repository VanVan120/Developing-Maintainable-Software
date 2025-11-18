package com.comp2042.controller.mainMenu;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

/**
 * Helper responsible for animating transitions between the main menu
 * button column and overlay panes (settings, controls, etc.).
 *
 * <p>It performs simple translate/fade animations and is deliberately
 * lightweight so the controller can reuse it across overlays.
 */
public class MainMenuOverlayHelper {
    private final VBox mainButtons;
    private final Text titleText;

    /**
     * Create a new overlay helper.
     *
     * @param mainButtons the main buttons column that will be moved/hidden
     * @param titleText the title text that will be faded when overlays open
     */
    public MainMenuOverlayHelper(VBox mainButtons, Text titleText) {
        this.mainButtons = mainButtons;
        this.titleText = titleText;
    }

    public void transitionTo(StackPane toPane) {
        /**
         * Animate an overlay in from the right and make it visible.
         *
         * @param toPane the overlay pane to show; ignored if {@code null}
         */
        if (toPane == null) return;
        toPane.setVisible(true);
        toPane.setOpacity(1.0);
        Platform.runLater(() -> {
            try {
                double startX = toPane.getScene() != null ? toPane.getScene().getWidth() : toPane.getWidth();
                toPane.setTranslateX(startX);
                TranslateTransition tt = new TranslateTransition(Duration.millis(300), toPane);
                tt.setFromX(startX);
                tt.setToX(0);
                tt.setInterpolator(Interpolator.EASE_BOTH);
                tt.play();
            } catch (Exception ignored) {}
        });
    }

    public void transitionFrom(StackPane fromPane) {
        /**
         * Animate the provided overlay out to the right and hide it.
         *
         * @param fromPane the overlay pane to hide; ignored if {@code null}
         */
        if (fromPane == null) return;
        Platform.runLater(() -> {
            try {
                double endX = fromPane.getScene() != null ? fromPane.getScene().getWidth() : fromPane.getWidth();
                TranslateTransition tt = new TranslateTransition(Duration.millis(220), fromPane);
                tt.setFromX(fromPane.getTranslateX());
                tt.setToX(endX);
                tt.setInterpolator(Interpolator.EASE_BOTH);
                tt.setOnFinished(ev -> {
                    try { fromPane.setVisible(false); fromPane.setTranslateX(0); } catch (Exception ignored) {}
                });
                tt.play();
            } catch (Exception ignored) {
                try { fromPane.setVisible(false); } catch (Exception ignored2) {}
            }
        });
    }

    public void closeOverlayWithAnimation(StackPane fromPane, Runnable onFinished) {
        /**
         * Close the overlay with the same animation used by
         * {@link #transitionFrom(StackPane)} and run {@code onFinished}
         * when the animation completes.
         *
         * @param fromPane the overlay to close
         * @param onFinished runnable executed after the close transition
         */
        if (fromPane == null) {
            if (onFinished != null) Platform.runLater(onFinished);
            return;
        }
        Platform.runLater(() -> {
            try {
                double endX = fromPane.getScene() != null ? fromPane.getScene().getWidth() : fromPane.getWidth();
                TranslateTransition tt = new TranslateTransition(Duration.millis(220), fromPane);
                tt.setFromX(fromPane.getTranslateX());
                tt.setToX(endX);
                tt.setInterpolator(Interpolator.EASE_BOTH);
                tt.setOnFinished(ev -> {
                    try { fromPane.setVisible(false); fromPane.setTranslateX(0); } catch (Exception ex1) {}
                    try { if (onFinished != null) onFinished.run(); } catch (Exception ex2) {}
                });
                tt.play();
            } catch (Exception ex3) {
                try { fromPane.setVisible(false); } catch (Exception ex4) {}
                try { if (onFinished != null) onFinished.run(); } catch (Exception ex5) {}
            }
        });
    }

    public void showOverlay(StackPane overlay) {
        /**
         * Show an overlay: hide the main buttons, fade the title and slide the
         * requested overlay in. This method is resilient to missing children
         * and will fall back to direct visibility changes if an animation
         * fails.
         *
         * @param overlay pane to show
         */
        if (overlay == null) return;
        try {
            if (mainButtons != null) {
                Timeline t = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(mainButtons.translateXProperty(), 0), new KeyValue(mainButtons.opacityProperty(), 1.0)),
                        new KeyFrame(Duration.millis(260), new KeyValue(mainButtons.translateXProperty(), -40, Interpolator.EASE_BOTH), new KeyValue(mainButtons.opacityProperty(), 0.08, Interpolator.EASE_BOTH))
                );
                t.play();
            }
            if (titleText != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(200), titleText);
                ft.setFromValue(1.0);
                ft.setToValue(0.0);
                ft.play();
            }
            transitionTo(overlay);
        } catch (Exception ex) {
            try { if (mainButtons != null) mainButtons.setVisible(false); } catch (Exception ignored) {}
            try { overlay.setVisible(true); } catch (Exception ignored) {}
            try { if (titleText != null) titleText.setVisible(false); } catch (Exception ignored) {}
        }
    }
}
