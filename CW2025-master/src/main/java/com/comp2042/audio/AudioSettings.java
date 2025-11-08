package com.comp2042.audio;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import java.util.prefs.Preferences;

public final class AudioSettings {
    private static final Preferences PREFS = Preferences.userNodeForPackage(AudioSettings.class);
    private static final String KEY_MASTER = "audio.master";
    private static final String KEY_MUSIC = "audio.music";
    private static final String KEY_SFX = "audio.sfx";

    private static final double DEFAULT_MASTER = 1.0;
    private static final double DEFAULT_MUSIC = 0.6;
    private static final double DEFAULT_SFX = 0.8;

    private static final DoubleProperty master = new SimpleDoubleProperty(load(KEY_MASTER, DEFAULT_MASTER));
    private static final DoubleProperty music = new SimpleDoubleProperty(load(KEY_MUSIC, DEFAULT_MUSIC));
    private static final DoubleProperty sfx = new SimpleDoubleProperty(load(KEY_SFX, DEFAULT_SFX));

    static {
        master.addListener((obs, o, n) -> PREFS.putDouble(KEY_MASTER, n.doubleValue()));
        music.addListener((obs, o, n) -> PREFS.putDouble(KEY_MUSIC, n.doubleValue()));
        sfx.addListener((obs, o, n) -> PREFS.putDouble(KEY_SFX, n.doubleValue()));
    }

    private static double load(String key, double def) {
        return PREFS.getDouble(key, def);
    }

    private AudioSettings() {}

    public static DoubleProperty masterProperty() { return master; }
    public static DoubleProperty musicProperty() { return music; }
    public static DoubleProperty sfxProperty() { return sfx; }

    public static double getMasterVolume() { return master.get(); }
    public static double getMusicVolume() { return music.get(); }
    public static double getSfxVolume() { return sfx.get(); }

    public static void setMasterVolume(double v) { master.set(v); }
    public static void setMusicVolume(double v) { music.set(v); }
    public static void setSfxVolume(double v) { sfx.set(v); }
}
