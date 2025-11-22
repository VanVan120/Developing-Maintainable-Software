package com.comp2042.controller.guiControl;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiControllerTest {
    @Test
    void basicPropertiesAndControls() {
        GuiController c = new GuiController(){};
        c.setControlKeys(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT);
        c.setSwapKey(KeyCode.C);
        assertEquals(KeyCode.A, c.getCtrlMoveLeft());
        c.setMultiplayerMode(true);
        assertTrue(c.isMultiplayerMode());
        c.setLastWasHardDrop(true);
        assertTrue(c.isLastWasHardDrop());
        c.setDropIntervalMs(200);
        // current score defaults to -1 when not bound
        assertTrue(c.getCurrentScore() <= 0);
    }
}
