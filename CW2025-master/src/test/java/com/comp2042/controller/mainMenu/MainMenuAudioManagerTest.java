package com.comp2042.controller.mainMenu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainMenuAudioManagerTest {

    @BeforeAll
    public static void init() throws Exception { JfxInitializer.init(); }

    @Test
    public void loadAndApplyVolumes_noExceptions() {
        MainMenuAudioManager m = new MainMenuAudioManager();
        // load resources (may be missing) and apply volumes
        m.loadAll(MainMenuAudioManager.class.getClassLoader());
        m.registerListeners();
        m.applyVolumes();
        m.setMusicVolume(0.5);
        m.setSfxVolume(0.4);
        // playback fallbacks should not throw
        m.playHover();
        m.playClick();
        // cleanup
        m.cleanup();
        assertTrue(true);
    }
}
