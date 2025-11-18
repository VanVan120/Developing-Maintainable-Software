package com.comp2042.controller.guiControl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiInitializeTest {
    @Test
    void initializeHelpersNoThrow() {
        GuiController c = new GuiController(){};
        // these helpers should tolerate nulls and not throw
        GuiInitialize.loadFontAndFocus(c);
        GuiInitialize.setupMusicAndGameListeners(c);
        GuiInitialize.setupSceneKeyHandlers(c);
        GuiInitialize.bindGameBoardCenter(c);
        GuiInitialize.bindScoreBox(c);
        GuiInitialize.bindGameBoardFrame(c);
        GuiInitialize.bindTimeBox(c);
        GuiInitialize.styleScoreValue(c);
        GuiInitialize.bindGroupNotification(c);
        GuiInitialize.bindNextBox(c);
        GuiInitialize.bindLevelBox(c);
        assertTrue(true);
    }
}
