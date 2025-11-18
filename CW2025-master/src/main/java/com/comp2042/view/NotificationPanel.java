package com.comp2042.view;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.effect.Effect;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A small popup-style panel used to show one-off notifications (for example
 * score/bonus messages) over the game UI.
 *
 * <p>The panel is a lightweight {@link BorderPane} holding a styled {@link Label}.
 * The visual presentation uses JavaFX animations (translate + fade) and
 * removal from the parent list is executed on the JavaFX Application Thread
 * using {@link Platform#runLater(Runnable)} to avoid threading issues.
 *
 * <p>Usage: create a new instance with the message text and add it to a
 * container's children list; call {@link #showScore(ObservableList)} to start
 * the show-and-remove animation.
 */
public class NotificationPanel extends BorderPane {

    private static final Logger LOGGER = Logger.getLogger(NotificationPanel.class.getName());

    /**
     * Creates a notification panel displaying the given text.
     *
     * @param text the message to show inside the panel (non-null)
     */
    public NotificationPanel(String text) {
        setMinHeight(200);
        setMinWidth(220);
        final Label score = new Label(text);
        score.getStyleClass().add("bonusStyle");
    final Effect glow = new Glow(0.6);
    score.setEffect(glow);
        setCenter(score);

    }

    /**
     * Start the notification animation and remove the panel from the provided
     * children list when the animation completes. The removal is performed on
     * the JavaFX Application Thread.
     *
        * Note: the caller should pass a non-null children list that contains
        * this instance. Removal is scheduled on the JavaFX Application Thread
        * to avoid concurrency issues; the method itself may be invoked from any
        * thread.
        *
        * @param list the observable list of nodes that contains this panel; typically
        *             a parent {@link javafx.scene.layout.Pane#getChildren() children} list
     */
    public void showScore(ObservableList<Node> list) {
        FadeTransition ft = new FadeTransition(Duration.millis(2000), this);
        TranslateTransition tt = new TranslateTransition(Duration.millis(2500), this);
        tt.setToY(this.getLayoutY() - 40);
        ft.setFromValue(1);
        ft.setToValue(0);
        ParallelTransition transition = new ParallelTransition(tt, ft);
        transition.setOnFinished((ActionEvent event) -> {
            try {
                Platform.runLater(() -> {
                    try {
                        list.remove(NotificationPanel.this);
                    } catch (Exception ex) {
                        LOGGER.log(Level.FINER, "Failed to remove NotificationPanel from list", ex);
                    }
                });
            } catch (Exception ex) {
                LOGGER.log(Level.FINER, "Error scheduling NotificationPanel removal", ex);
            }
        });
        try {
            transition.play();
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, "Failed to play NotificationPanel transition", ex);
        }
    }
}
