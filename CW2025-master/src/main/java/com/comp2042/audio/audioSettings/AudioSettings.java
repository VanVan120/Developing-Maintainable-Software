package com.comp2042.audio.audioSettings;

import javafx.beans.property.DoubleProperty;

/**
 * Backwards-compatible static facade for audio settings. Internally delegates
 * to an instance of {@link AudioSettingsService} (defaulting to
 * {@link DefaultAudioSettings}). This allows tests and callers to inject a
 * different implementation while preserving the existing static API.
 */
public final class AudioSettings {
    private static AudioSettingsService impl = new DefaultAudioSettings();

    private AudioSettings() {}

    // ---- facade methods delegating to impl ----
    public static DoubleProperty masterProperty() { return impl.masterProperty(); }
    public static DoubleProperty musicProperty() { return impl.musicProperty(); }
    public static DoubleProperty sfxProperty() { return impl.sfxProperty(); }

    public static double getMasterVolume() { return impl.getMasterVolume(); }
    public static double getMusicVolume() { return impl.getMusicVolume(); }
    public static double getSfxVolume() { return impl.getSfxVolume(); }

    public static void setMasterVolume(double v) { impl.setMasterVolume(v); }
    public static void setMusicVolume(double v) { impl.setMusicVolume(v); }
    public static void setSfxVolume(double v) { impl.setSfxVolume(v); }

    // ---- testing / injection helpers ----
    /**
     * Replace the internal implementation. Intended for tests or advanced usage.
     */
    public static void setImplementation(AudioSettingsService service) {
        if (service == null) throw new IllegalArgumentException("service cannot be null");
        impl = service;
    }

    /**
     * Restore the default implementation.
     */
    public static void restoreDefaultImplementation() {
        impl = new DefaultAudioSettings();
    }
}
