package com.comp2042.controller.mainMenu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javafx.scene.input.KeyCode;

public class MainMenuPreferencesTest {

    @BeforeAll
    public static void init() throws Exception { JfxInitializer.init(); }

    @Test
    public void saveAndLoadControlSettings_roundTrip() {
        MainMenuPreferences p = new MainMenuPreferences();
        MainMenuControlSettings cs = new MainMenuControlSettings();
        cs.spLeft = KeyCode.A; cs.spRight = KeyCode.S; cs.spRotate = KeyCode.W;
        p.saveControlSettings(cs);

        MainMenuControlSettings loaded = p.loadControlSettings();
        assertEquals(KeyCode.A, loaded.spLeft);
        assertEquals(KeyCode.S, loaded.spRight);
        assertEquals(KeyCode.W, loaded.spRotate);
    }

    @Test
    public void saveAndLoadHandlingSettings_roundTrip() {
        MainMenuPreferences p = new MainMenuPreferences();
        MainMenuHandlingSettings hs = new MainMenuHandlingSettings();
        hs.settingArrMs = 77; hs.settingDasMs = 88; hs.settingDcdMs = 99; hs.settingSdf = 0.42; hs.settingHardDropEnabled = false;
        p.saveHandlingSettings(hs);

        MainMenuHandlingSettings loaded = p.loadHandlingSettings();
        assertEquals(77, loaded.settingArrMs);
        assertEquals(88, loaded.settingDasMs);
        assertEquals(99, loaded.settingDcdMs);
        assertEquals(0.42, loaded.settingSdf, 0.0001);
        assertEquals(false, loaded.settingHardDropEnabled);
    }
}
