package com.comp2042.audio.soundManager;

import javafx.beans.value.ChangeListener;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import com.comp2042.audio.audioSettings.AudioSettings;

import javax.sound.sampled.Clip;
import java.awt.*;
import java.net.URL;

/**
 * High-level sound manager used by the application to play short effects and
 * music tracks.
 *
 * <p>The manager loads commonly used sounds via {@link ClipLoader}, creates
 * music {@link MediaPlayer} instances via {@link MediaPlayerFactory} and
 * responds to volume changes driven by
 * {@link com.comp2042.audio.audioSettings.AudioSettings}.
 */
public class SoundManager {
    private final Class<?> resourceOwner;

    private AudioClip hoverClip;
    private AudioClip clickClip;
    private AudioClip hardDropClip;
    private Clip hoverFallback;
    private Clip clickFallback;
    private Clip hardDropFallback;

    private MediaPlayer singleplayerMusicPlayer;
    private MediaPlayer gameOverMusicPlayer;
    private MediaPlayer countdownMusicPlayer;
    private final MediaPlayerFactory mediaFactory;
    private final ClipLoader clipLoader;
    private ChangeListener<Number> settingsVolumeListener = null;

    public SoundManager(Class<?> resourceOwner) {
        this.resourceOwner = resourceOwner == null ? getClass() : resourceOwner;
        this.mediaFactory = new MediaPlayerFactory(this.resourceOwner);
        this.clipLoader = new ClipLoader(this.resourceOwner);
    }

    /**
     * Initialize and preload short sound effects and attach volume listeners.
     * This method should be called once before attempting to play sounds.
     */
    public void init() {
        ClipLoader.AudioLoadResult r1 = clipLoader.load("/sounds/hover.wav");
        hoverClip = r1.audioClip;
        hoverFallback = r1.fallbackClip;

        ClipLoader.AudioLoadResult r2 = clipLoader.load("/sounds/click.wav");
        clickClip = r2.audioClip;
        clickFallback = r2.fallbackClip;

        ClipLoader.AudioLoadResult r3 = clipLoader.load("/sounds/HardDrop.wav");
        hardDropClip = r3.audioClip;
        hardDropFallback = r3.fallbackClip;

        ChangeListener<Number> volumeListener = (obs, o, n) -> applyVolumes();
        settingsVolumeListener = volumeListener;
        AudioSettings.masterProperty().addListener(settingsVolumeListener);
        AudioSettings.musicProperty().addListener(settingsVolumeListener);
        AudioSettings.sfxProperty().addListener(settingsVolumeListener);
        applyVolumes();
    }

    public void dispose() {
        try {
            try {
                if (settingsVolumeListener != null) {
                    try { AudioSettings.masterProperty().removeListener(settingsVolumeListener); } catch (Exception ignored) {}
                    try { AudioSettings.musicProperty().removeListener(settingsVolumeListener); } catch (Exception ignored) {}
                    try { AudioSettings.sfxProperty().removeListener(settingsVolumeListener); } catch (Exception ignored) {}
                    settingsVolumeListener = null;
                }
            } catch (Exception ignored) {}

            try { stopSingleplayerMusic(); } catch (Exception ignored) {}
            try { stopGameOverMusic(); } catch (Exception ignored) {}
            try { stopCountdownMusic(); } catch (Exception ignored) {}

            try {
                // dispose any MediaPlayers created via the factory
                mediaFactory.disposeAll();
            } catch (Exception ignored) {}

            try { if (hoverFallback != null) { hoverFallback.close(); hoverFallback = null; } } catch (Exception ignored) {}
            try { if (clickFallback != null) { clickFallback.close(); clickFallback = null; } } catch (Exception ignored) {}
            try { if (hardDropFallback != null) { hardDropFallback.close(); hardDropFallback = null; } } catch (Exception ignored) {}

            try { hoverClip = null; } catch (Exception ignored) {}
            try { clickClip = null; } catch (Exception ignored) {}
            try { hardDropClip = null; } catch (Exception ignored) {}
        } catch (Throwable ignored) {}
    }

    // Audio loading moved to ClipLoader; helper methods removed to reduce duplication.

    /**
     * Apply the current audio settings to all loaded players and clips.
     * This updates both sfx and music volumes to reflect current settings.
     */
    private void applyVolumes() {
        double master = AudioSettings.getMasterVolume();
        double music = AudioSettings.getMusicVolume();
        double sfx = AudioSettings.getSfxVolume();
        double s = Math.max(0.0, Math.min(1.0, master * sfx));
        double m = Math.max(0.0, Math.min(1.0, master * music));
        try { if (hoverClip != null) hoverClip.setVolume(s); } catch (Exception ignored) {}
        try { if (clickClip != null) clickClip.setVolume(s); } catch (Exception ignored) {}
        try { if (hardDropClip != null) hardDropClip.setVolume(s); } catch (Exception ignored) {}
        try { if (singleplayerMusicPlayer != null) singleplayerMusicPlayer.setVolume(m); } catch (Exception ignored) {}
        try { if (gameOverMusicPlayer != null) gameOverMusicPlayer.setVolume(m); } catch (Exception ignored) {}
        try { if (countdownMusicPlayer != null) countdownMusicPlayer.setVolume(m); } catch (Exception ignored) {}
    }

    /**
     * Play the configured hover sound effect (or fallback). Be tolerant of
     * failures; a system beep is used as a last resort.
     */
    public void playHoverSound() {
        try {
            if (hoverClip != null) { hoverClip.play(); return; }
            if (hoverFallback != null) { playFallback(hoverFallback); return; }
        } catch (Throwable ignored) {}
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Play the configured click sound effect (or fallback).
     */
    public void playClickSound() {
        try {
            if (clickClip != null) { clickClip.play(); return; }
            if (clickFallback != null) { playFallback(clickFallback); return; }
        } catch (Throwable ignored) {}
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Play the hard-drop sound effect (or fallback) used by the game.
     */
    public void playHardDropSound() {
        try {
            if (hardDropClip != null) { hardDropClip.play(); return; }
            if (hardDropFallback != null) { playFallback(hardDropFallback); return; }
        } catch (Throwable ignored) {}
        Toolkit.getDefaultToolkit().beep();
    }

    /**
     * Play a javax.sound {@link Clip} as a fallback mechanism for platforms
     * where JavaFX {@link AudioClip} is unavailable.
     */
    private void playFallback(Clip c) {
        try {
            if (c.isRunning()) c.stop();
            c.setFramePosition(0);
            c.start();
        } catch (Throwable ignored) {}
    }

    public void startSingleplayerMusic() {
        try {
            if (singleplayerMusicPlayer != null) return;
            URL mus = resourceOwner.getResource("/sounds/Singleplayer.wav");
            if (mus == null) return;
            Media m = new Media(mus.toExternalForm());
            singleplayerMusicPlayer = new MediaPlayer(m);
            singleplayerMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            singleplayerMusicPlayer.setVolume(AudioSettings.getMasterVolume() * AudioSettings.getMusicVolume());
            singleplayerMusicPlayer.setAutoPlay(true);
        } catch (Throwable ex) {
            System.err.println("[SoundManager] Failed to start singleplayer music: " + ex);
        }
    }

    public void stopSingleplayerMusic() {
        try {
            if (singleplayerMusicPlayer != null) {
                try { singleplayerMusicPlayer.stop(); } catch (Exception ignored) {}
                try { singleplayerMusicPlayer.dispose(); } catch (Exception ignored) {}
                singleplayerMusicPlayer = null;
            }
        } catch (Throwable ignored) {}
    }

    public void playGameOverMusic() {
        try {
            stopSingleplayerMusic();
            if (gameOverMusicPlayer != null) return;
            // use factory which will attach volume listeners for us
            MediaPlayer mp = mediaFactory.createMediaPlayer("/sounds/GameOver.wav", false, null);
            if (mp == null) return;
            gameOverMusicPlayer = mp;
            gameOverMusicPlayer.setCycleCount(1);
            gameOverMusicPlayer.setAutoPlay(true);
        } catch (Throwable ex) {
            System.err.println("[SoundManager] Failed to play GameOver music: " + ex);
        }
    }

    public void stopGameOverMusic() {
        try {
            if (gameOverMusicPlayer != null) {
                mediaFactory.disposeMediaPlayer(gameOverMusicPlayer);
                gameOverMusicPlayer = null;
            }
        } catch (Throwable ignored) {}
    }

    public void playCountdownMusic() {
        try {
            if (countdownMusicPlayer != null) return;
            MediaPlayer mp = mediaFactory.createMediaPlayer("/sounds/Countdown.wav", true, null);
            if (mp == null) return;
            countdownMusicPlayer = mp;
            countdownMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            countdownMusicPlayer.setAutoPlay(true);
        } catch (Throwable ex) {
            System.err.println("[SoundManager] Failed to play Countdown music: " + ex);
        }
    }

    public void stopCountdownMusic() {
        try {
            if (countdownMusicPlayer != null) {
                mediaFactory.disposeMediaPlayer(countdownMusicPlayer);
                countdownMusicPlayer = null;
            }
        } catch (Throwable ignored) {}
    }

    // Media player creation/disposal moved to MediaPlayerFactory

    /**
     * Compatibility delegate to {@link MediaPlayerFactory#createMediaPlayer}.
     *
     * @see MediaPlayerFactory#createMediaPlayer(String, boolean, Double)
     */
    public MediaPlayer createMediaPlayer(String resourcePath, boolean loop, Double volumeFactor) {
        return mediaFactory.createMediaPlayer(resourcePath, loop, volumeFactor);
    }

    /**
     * Convenience overload accepting a primitive volume factor.
     */
    public MediaPlayer createMediaPlayer(String resourcePath, boolean loop, double volumeFactor) {
        return createMediaPlayer(resourcePath, loop, Double.valueOf(volumeFactor));
    }

    /**
     * Dispose a {@link MediaPlayer} previously created by this manager.
     * Delegates disposal to {@link MediaPlayerFactory}.
     */
    public void disposeMediaPlayer(MediaPlayer mp) {
        mediaFactory.disposeMediaPlayer(mp);
    }
}
