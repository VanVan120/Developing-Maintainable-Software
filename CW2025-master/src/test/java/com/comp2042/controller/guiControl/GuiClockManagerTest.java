package com.comp2042.controller.guiControl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiClockManagerTest {
    @Test
    void smoke() {
        GuiController c = new GuiController(){};
        GuiClockManager m = new GuiClockManager(c);
        assertNotNull(m);
        m.resetClock();
        m.updateClock();
        m.pauseAndRecord();
        m.stopClock();
    }
}
