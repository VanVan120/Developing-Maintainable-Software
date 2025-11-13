package com.comp2042.controller.guiControl;

import javafx.animation.Timeline;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiCountdownControllerTest {
    @Test
    void startCountdownReturnsTimeline() {
        GuiController c = new GuiController(){};
        Timeline t = GuiCountdownController.startCountdown(c, 1);
        assertNotNull(t);
        assertTrue(t.getCycleCount() >= 1);
    }
}
