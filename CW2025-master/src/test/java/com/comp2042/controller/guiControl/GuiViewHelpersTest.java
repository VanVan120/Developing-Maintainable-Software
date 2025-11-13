package com.comp2042.controller.guiControl;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiViewHelpersTest {
    @Test
    void buildNextPreviewHandlesNull() {
        GuiController c = new GuiController(){};
        // null upcoming should return non-null but empty container
        javafx.scene.layout.VBox v = GuiViewHelpers.buildNextPreview(c, null);
        assertNotNull(v);
        assertTrue(v.getChildren().isEmpty());
    }
}
