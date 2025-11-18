package com.comp2042.controller.guiControl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiCountdownContextTest {
    @Test
    void defaults() {
        GuiCountdownContext ctx = new GuiCountdownContext();
        assertNull(ctx.gameBoard);
        assertNull(ctx.brickPanel);
        assertNull(ctx.groupNotification);
        assertNull(ctx.timeLine);
        assertNull(ctx.resetClock);
    }
}
