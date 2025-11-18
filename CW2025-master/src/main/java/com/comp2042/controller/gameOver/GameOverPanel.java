package com.comp2042.controller.gameOver;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

/**
 * Simple reusable panel that displays a prominent "game over" message.
 *
 * <p>The panel centers a {@link Label} containing the message text and exposes
 * helpers to update or retrieve the message. A CSS style class of
 * {@link #DEFAULT_STYLE_CLASS} is applied to the label so the look can be
 * customised by the application's stylesheet.</p>
 */
public class GameOverPanel extends BorderPane {

    /** Default message text used when none is provided. */
    public static final String DEFAULT_TEXT = "GAME OVER";
    /** Default CSS style class applied to the internal label. */
    public static final String DEFAULT_STYLE_CLASS = "gameOverStyle";

    private final Label messageLabel;

    /**
     * Create a panel using the default message text.
     */
    public GameOverPanel() {
        this(DEFAULT_TEXT);
    }

    /**
     * Create a GameOverPanel with custom text.
     *
     * @param text the message to display; if {@code null} the default text is used
     */
    public GameOverPanel(String text) {
        messageLabel = new Label(text != null ? text : DEFAULT_TEXT);
        messageLabel.getStyleClass().add(DEFAULT_STYLE_CLASS);
        setCenter(messageLabel);
    }

    /**
     * Update the displayed message. Passing {@code null} restores the default text.
     *
     * @param text new message text or {@code null} to use the default
     */
    public void setMessage(String text) {
        messageLabel.setText(text != null ? text : DEFAULT_TEXT);
    }

    /**
     * Return the currently displayed message text.
     */
    public String getMessage() {
        return messageLabel.getText();
    }

    /**
     * Return the underlying {@link Label} node for further customization if needed.
     */
    public Label getMessageLabel() {
        return messageLabel;
    }
}
