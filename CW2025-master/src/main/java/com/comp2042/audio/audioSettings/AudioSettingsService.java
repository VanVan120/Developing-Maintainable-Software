package com.comp2042.audio.audioSettings;

import javafx.beans.property.DoubleProperty;

/**
 * Instance-oriented contract for audio settings. Implementations manage the
 * master/music/sfx volume properties and persist them to preferences.
 */
public interface AudioSettingsService {
    DoubleProperty masterProperty();
    DoubleProperty musicProperty();
    DoubleProperty sfxProperty();

    double getMasterVolume();
    double getMusicVolume();
    double getSfxVolume();

    void setMasterVolume(double v);
    void setMusicVolume(double v);
    void setSfxVolume(double v);
}
