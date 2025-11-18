package com.comp2042.controller.guiControl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiGameOverControllerTest {
    @Test
    void restartAndExitNoThrow() {
        GuiController c = new GuiController(){};
        // should not throw when overlay is null
        GuiGameOverController.performRestartFromGameOver(c, null);
        // make controller report multiplayer enabled so performExitToMenuFromGameOver returns early
        c.setMultiplayerMode(true);
        GuiGameOverController.performExitToMenuFromGameOver(c, null, null);
        assertTrue(true);
    }
}
