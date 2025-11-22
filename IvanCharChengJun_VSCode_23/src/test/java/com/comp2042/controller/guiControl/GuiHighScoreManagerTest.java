package com.comp2042.controller.guiControl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiHighScoreManagerTest {
    @Test
    void loadSaveAndOnNewScore() {
        GuiController c = new GuiController(){};
        GuiHighScoreManager m = new GuiHighScoreManager(c);
        m.loadHighScore();
        int before = m.getHighScore();
        m.onNewScore(before + 10);
        assertTrue(m.getHighScore() >= before);
        m.saveHighScore();
    }
}
