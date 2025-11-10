package com.comp2042.audio.audioSettings;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import java.util.prefs.Preferences;

/**
 * Default implementation of {@link AudioSettingsService} which persists
 * values to the java.util.prefs.Preferences node.
 */
public class DefaultAudioSettings implements AudioSettingsService {
    private final Preferences prefs;
    private final String KEY_MASTER = "audio.master";
    private final String KEY_MUSIC = "audio.music";
    private final String KEY_SFX = "audio.sfx";

    private final double DEFAULT_MASTER = 1.0;
    private final double DEFAULT_MUSIC = 0.6;
    private final double DEFAULT_SFX = 0.8;

    private final DoubleProperty master;
    private final DoubleProperty music;
    private final DoubleProperty sfx;

    public DefaultAudioSettings() {
        this.prefs = Preferences.userNodeForPackage(DefaultAudioSettings.class);
        this.master = new SimpleDoubleProperty(load(KEY_MASTER, DEFAULT_MASTER));
        this.music = new SimpleDoubleProperty(load(KEY_MUSIC, DEFAULT_MUSIC));
        this.sfx = new SimpleDoubleProperty(load(KEY_SFX, DEFAULT_SFX));

        master.addListener((obs, o, n) -> prefs.putDouble(KEY_MASTER, n.doubleValue()));
        music.addListener((obs, o, n) -> prefs.putDouble(KEY_MUSIC, n.doubleValue()));
        sfx.addListener((obs, o, n) -> prefs.putDouble(KEY_SFX, n.doubleValue()));
    }

    private double load(String key, double def) {
        return prefs.getDouble(key, def);
    }

    @Override
    public DoubleProperty masterProperty() { return master; }

    @Override
    public DoubleProperty musicProperty() { return music; }

    @Override
    public DoubleProperty sfxProperty() { return sfx; }

    @Override
    public double getMasterVolume() { return master.get(); }

    @Override
    public double getMusicVolume() { return music.get(); }

    @Override
    public double getSfxVolume() { return sfx.get(); }

    @Override
    public void setMasterVolume(double v) { master.set(v); }

    @Override
    public void setMusicVolume(double v) { music.set(v); }

    @Override
    public void setSfxVolume(double v) { sfx.set(v); }
}
