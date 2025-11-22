package com.comp2042.audio.audioSettingsTest;

import com.comp2042.audio.audioSettings.AudioSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AudioSettings placed in the test package `com.tetris.audio` as requested.
 * Tests modify static properties but restore the previous preference values after each test
 * to avoid persistent side-effects in the developer's environment.
 */
public class AudioSettingsTest {

    private static final Preferences PREFS = Preferences.userNodeForPackage(AudioSettings.class);
    private double oldMaster;
    private double oldMusic;
    private double oldSfx;

    @BeforeEach
    public void saveOld() {
        oldMaster = PREFS.getDouble("audio.master", 1.0);
        oldMusic = PREFS.getDouble("audio.music", 0.6);
        oldSfx = PREFS.getDouble("audio.sfx", 0.8);
    }

    @AfterEach
    public void restoreOld() {
        // restore preferences and reset properties
        PREFS.putDouble("audio.master", oldMaster);
        PREFS.putDouble("audio.music", oldMusic);
        PREFS.putDouble("audio.sfx", oldSfx);
        AudioSettings.setMasterVolume(oldMaster);
        AudioSettings.setMusicVolume(oldMusic);
        AudioSettings.setSfxVolume(oldSfx);
    }

    @Test
    public void testSetAndGetVolumes() {
        // set new values
        AudioSettings.setMasterVolume(0.42);
        AudioSettings.setMusicVolume(0.21);
        AudioSettings.setSfxVolume(0.77);

        assertEquals(0.42, AudioSettings.getMasterVolume(), 1e-9);
        assertEquals(0.21, AudioSettings.getMusicVolume(), 1e-9);
        assertEquals(0.77, AudioSettings.getSfxVolume(), 1e-9);

        // properties reflect same values
        assertEquals(0.42, AudioSettings.masterProperty().get(), 1e-9);
        assertEquals(0.21, AudioSettings.musicProperty().get(), 1e-9);
        assertEquals(0.77, AudioSettings.sfxProperty().get(), 1e-9);
    }

    @Test
    public void testPropertyListeners() {
        final double[] observed = new double[1];
        AudioSettings.masterProperty().addListener((obs, o, n) -> observed[0] = n.doubleValue());
        AudioSettings.setMasterVolume(0.33);
        assertEquals(0.33, observed[0], 1e-9);
    }
}
