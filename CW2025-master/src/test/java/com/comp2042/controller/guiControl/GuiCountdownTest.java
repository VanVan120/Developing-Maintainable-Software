package com.comp2042.controller.guiControl;

import javafx.animation.Timeline;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiCountdownTest {
    @Test
    void startCountdownWithContext() {
        GuiCountdownContext ctx = new GuiCountdownContext();
        Timeline t = GuiCountdown.startCountdown(1, ctx);
        assertNotNull(t);
        assertTrue(t.getCycleCount() >= 1);
    }
}
