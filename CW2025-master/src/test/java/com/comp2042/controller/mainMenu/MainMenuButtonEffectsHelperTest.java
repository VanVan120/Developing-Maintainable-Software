package com.comp2042.controller.mainMenu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javafx.scene.control.Button;

public class MainMenuButtonEffectsHelperTest {

    @BeforeAll
    public static void init() throws Exception { JfxInitializer.init(); }

    @Test
    public void attachHoverEffects_doesNotThrow() {
        MainMenuButtonEffectsHelper h = new MainMenuButtonEffectsHelper();
        Button b = new Button("Test");
        h.attachHoverEffects(b, 20);
        // should not throw and should have handlers attached
        assertNotNull(b.getOnMouseEntered());
        assertNotNull(b.getOnMouseExited());
    }

    @Test
    public void attachButtonSoundHandlers_doesNotThrow() {
        MainMenuButtonEffectsHelper h = new MainMenuButtonEffectsHelper();
        Button b = new Button("Test2");
        MainMenuAudioManager audio = new MainMenuAudioManager() {
            // use defaults; we only assert no exceptions when attaching
        };
        h.attachButtonSoundHandlers(b, audio);
        // attaching should not throw; no reliable public API to inspect added handlers
        assertTrue(true);
    }
}
