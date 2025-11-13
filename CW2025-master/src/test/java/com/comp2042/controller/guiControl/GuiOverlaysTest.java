package com.comp2042.controller.guiControl;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Constructor;
import static org.junit.jupiter.api.Assertions.*;

class GuiOverlaysTest {
    @Test
    void pauseTimelinesAndPrivateCtor() throws Exception {
        GuiController c = new GuiController(){};
        // should not throw
        GuiOverlays.pauseTimelinesAndNotify(c);

        // coverage for private ctor
        Constructor<GuiOverlays> ctr = GuiOverlays.class.getDeclaredConstructor();
        ctr.setAccessible(true);
        GuiOverlays inst = ctr.newInstance();
        assertNotNull(inst);
    }
}
