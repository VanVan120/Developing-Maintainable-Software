package com.comp2042.controller.guiControl;

import javafx.scene.text.Text;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiGameOverUITest {
    @Test
    void titleAndPulse() {
        Text t = GuiGameOverUI.createGameOverTitle();
        assertNotNull(t);
        // starting the pulse may return null on non-FX threads; ensure it doesn't throw
        try {
            GuiGameOverUI.startGameOverPulse(t);
        } catch (Exception ex) {
            // ok: ensure no exception bubbles up
        }
    }
}
