package com.comp2042.audio.audioSettings;

import javafx.beans.property.DoubleProperty;

/**
 * Contract describing instance-oriented audio settings handling.
 *
 * <p>Implementations expose JavaFX {@link javafx.beans.property.DoubleProperty}
 * properties for master, music and sfx volumes and provide getters/setters
 * that operate on the numeric values. Implementations are expected to handle
 * persistence (for example via {@link java.util.prefs.Preferences}) if desired.
 */
public interface AudioSettingsService {
    /**
     * @return property representing the master volume (0.0-1.0)
     */
    DoubleProperty masterProperty();

    /**
     * @return property representing the music volume (0.0-1.0)
     */
    DoubleProperty musicProperty();

    /**
     * @return property representing the sound-effects volume (0.0-1.0)
     */
    DoubleProperty sfxProperty();

    /**
     * @return current master volume value
     */
    double getMasterVolume();

    /**
     * @return current music volume value
     */
    double getMusicVolume();

    /**
     * @return current sfx volume value
     */
    double getSfxVolume();

    /**
     * Set the master volume value.
     * @param v volume in the range 0.0-1.0
     */
    void setMasterVolume(double v);

    /**
     * Set the music volume value.
     * @param v volume in the range 0.0-1.0
     */
    void setMusicVolume(double v);

    /**
     * Set the sfx volume value.
     * @param v volume in the range 0.0-1.0
     */
    void setSfxVolume(double v);
}
