package com.tetris.audio.audioSettingsTest;

import com.comp2042.audio.audioSettings.DefaultAudioSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class DefaultAudioSettingsTest {

    private Preferences prefs;

    @BeforeEach
    void setUp() throws Exception {
        prefs = Preferences.userNodeForPackage(DefaultAudioSettings.class);
        // clear any existing persisted values to ensure deterministic defaults
        prefs.remove("audio.master");
        prefs.remove("audio.music");
        prefs.remove("audio.sfx");
    }

    @AfterEach
    void tearDown() throws Exception {
        // try to clean up after ourselves
        prefs.remove("audio.master");
        prefs.remove("audio.music");
        prefs.remove("audio.sfx");
    }

    @Test
    void defaults_are_expected() {
        DefaultAudioSettings ds = new DefaultAudioSettings();
        assertEquals(1.0, ds.getMasterVolume(), 1e-9);
        assertEquals(0.6, ds.getMusicVolume(), 1e-9);
        assertEquals(0.8, ds.getSfxVolume(), 1e-9);
    }

    @Test
    void changing_property_is_persisted_and_survives_new_instance() throws Exception {
        DefaultAudioSettings ds = new DefaultAudioSettings();
        ds.setMusicVolume(0.33);

        // create a fresh instance that should read the persisted value
        DefaultAudioSettings ds2 = new DefaultAudioSettings();
        assertEquals(0.33, ds2.getMusicVolume(), 1e-9);
    }
}
