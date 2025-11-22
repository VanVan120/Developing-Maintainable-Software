package com.comp2042.audio.audioSettings;

import javafx.beans.property.DoubleProperty;

/**
 * Backwards-compatible static facade for application audio settings.
 *
 * <p>This final utility class delegates all operations to an instance of
 * {@link AudioSettingsService} (by default {@link DefaultAudioSettings}).
 * The static API is preserved for existing callers while allowing tests or
 * advanced callers to replace the underlying implementation via
 * {@link #setImplementation(AudioSettingsService)}.
 */
public final class AudioSettings {
    private static AudioSettingsService impl = new DefaultAudioSettings();

    private AudioSettings() {}

    /**
     * Access the master volume property.
     *
     * @return the master volume property (range 0.0-1.0)
     */
    public static DoubleProperty masterProperty() { return impl.masterProperty(); }

    /**
     * Access the music volume property.
     *
     * @return the music volume property (range 0.0-1.0)
     */
    public static DoubleProperty musicProperty() { return impl.musicProperty(); }

    /**
     * Access the sound-effects (sfx) volume property.
     *
     * @return the sfx volume property (range 0.0-1.0)
     */
    public static DoubleProperty sfxProperty() { return impl.sfxProperty(); }

    /**
     * Return the current master volume value.
     */
    public static double getMasterVolume() { return impl.getMasterVolume(); }

    /**
     * Return the current music volume value.
     */
    public static double getMusicVolume() { return impl.getMusicVolume(); }

    /**
     * Return the current sfx volume value.
     */
    public static double getSfxVolume() { return impl.getSfxVolume(); }

    /**
     * Set the master volume.
     *
     * @param v volume value in the range 0.0 to 1.0
     */
    public static void setMasterVolume(double v) { impl.setMasterVolume(v); }

    /**
     * Set the music volume.
     *
     * @param v volume value in the range 0.0 to 1.0
     */
    public static void setMusicVolume(double v) { impl.setMusicVolume(v); }

    /**
     * Set the sfx volume.
     *
     * @param v volume value in the range 0.0 to 1.0
     */
    public static void setSfxVolume(double v) { impl.setSfxVolume(v); }

    /**
     * Replace the internal {@link AudioSettingsService} implementation. This
     * method is intended for tests or advanced usage where a different
     * persistence/behaviour is required.
     *
     * @param service non-null service to delegate audio settings operations to
     * @throws IllegalArgumentException if {@code service} is {@code null}
     */
    public static void setImplementation(AudioSettingsService service) {
        if (service == null) throw new IllegalArgumentException("service cannot be null");
        impl = service;
    }

    /**
     * Restore the default implementation that persists settings via
     * {@link java.util.prefs.Preferences}.
     */
    public static void restoreDefaultImplementation() {
        impl = new DefaultAudioSettings();
    }
}
