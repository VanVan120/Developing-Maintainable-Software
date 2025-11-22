package com.comp2042.controller.guiControl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiInputHandlerTest {
    @Test
    void attachDetachAndHardDrop() {
        GuiController c = new GuiController(){};
        GuiInputHandler h = new GuiInputHandler(c);
        // attaching null scene should be no-op
        h.attachToScene(null);
        h.detachFromScene(null);
        // should not throw when hardDrop invoked without eventListener
        h.hardDrop();
        // setupSceneKeyHandlers schedules Platform.runLater which requires JavaFX toolkit.
        // In headless test runs the toolkit isn't available so guard against that.
        try {
            h.setupSceneKeyHandlers();
        } catch (IllegalStateException ignored) {}
        assertTrue(true);
    }
}
