package com.comp2042.controller.mainMenu;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javafx.scene.input.KeyCode;

public class MainMenuControlSettingsTest {

    @Test
    public void testFields_canBeSetAndRead() {
        MainMenuControlSettings s = new MainMenuControlSettings();
        s.spLeft = KeyCode.A;
        s.spRight = KeyCode.D;
        s.mpLeft_left = KeyCode.LEFT;
        assertEquals(KeyCode.A, s.spLeft);
        assertEquals(KeyCode.D, s.spRight);
        assertEquals(KeyCode.LEFT, s.mpLeft_left);
    }
}
