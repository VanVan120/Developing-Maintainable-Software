package com.comp2042.controller.gameOver;

import javafx.embed.swing.JFXPanel;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GameOverPanelTest {

    @BeforeAll
    public static void initToolkit() {
        // Initialize JavaFX toolkit for tests that touch JavaFX classes.
        // Creating a JFXPanel forces the JavaFX runtime to start.
        new JFXPanel();
    }

    @Test
    public void defaultConstructor_setsDefaultText() {
        GameOverPanel panel = new GameOverPanel();
        assertEquals(GameOverPanel.DEFAULT_TEXT, panel.getMessage());
    }

    @Test
    public void constructor_withCustomText_setsText() {
        String msg = "You Lose";
        GameOverPanel panel = new GameOverPanel(msg);
        assertEquals(msg, panel.getMessage());
    }

    @Test
    public void constructor_withNull_usesDefaultText() {
        GameOverPanel panel = new GameOverPanel(null);
        assertEquals(GameOverPanel.DEFAULT_TEXT, panel.getMessage());
    }

    @Test
    public void setMessage_withNull_usesDefaultText() {
        GameOverPanel panel = new GameOverPanel("tmp");
        panel.setMessage(null);
        assertEquals(GameOverPanel.DEFAULT_TEXT, panel.getMessage());
    }

    @Test
    public void getMessageLabel_returnsLabel_withStyleClass() {
        GameOverPanel panel = new GameOverPanel();
        Label label = panel.getMessageLabel();
        assertNotNull(label);
        assertTrue(label.getStyleClass().contains(GameOverPanel.DEFAULT_STYLE_CLASS),
                "Label should include the default style class");
    }
}
