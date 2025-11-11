package com.comp2042.controller.gameOver;

import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;

public class GameOverPanel extends BorderPane {

    public static final String DEFAULT_TEXT = "GAME OVER";
    public static final String DEFAULT_STYLE_CLASS = "gameOverStyle";

    private final Label messageLabel;

    public GameOverPanel() {
        this(DEFAULT_TEXT);
    }

    /**
     * Create a GameOverPanel with custom text.
     *
     * @param text the message to display; if null the default text is used
     */
    public GameOverPanel(String text) {
        messageLabel = new Label(text != null ? text : DEFAULT_TEXT);
        messageLabel.getStyleClass().add(DEFAULT_STYLE_CLASS);
        setCenter(messageLabel);
    }

    public void setMessage(String text) {
        messageLabel.setText(text != null ? text : DEFAULT_TEXT);
    }

    public String getMessage() {
        return messageLabel.getText();
    }

    public Label getMessageLabel() {
        return messageLabel;
    }
}
