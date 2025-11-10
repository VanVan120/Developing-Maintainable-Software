package com.comp2042.audio.soundManager;

import javafx.beans.value.ChangeListener;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import com.comp2042.audio.audioSettings.AudioSettings;

import javax.sound.sampled.Clip;
import java.awt.*;
import java.net.URL;

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

    public void playHoverSound() {
        try {
            if (hoverClip != null) { hoverClip.play(); return; }
            if (hoverFallback != null) { playFallback(hoverFallback); return; }
        } catch (Throwable ignored) {}
        Toolkit.getDefaultToolkit().beep();
    }

    public void playClickSound() {
        try {
            if (clickClip != null) { clickClip.play(); return; }
            if (clickFallback != null) { playFallback(clickFallback); return; }
        } catch (Throwable ignored) {}
        Toolkit.getDefaultToolkit().beep();
    }

    public void playHardDropSound() {
        try {
            if (hardDropClip != null) { hardDropClip.play(); return; }
            if (hardDropFallback != null) { playFallback(hardDropFallback); return; }
        } catch (Throwable ignored) {}
        Toolkit.getDefaultToolkit().beep();
    }

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
     * Compatibility delegate to MediaPlayerFactory.
     */
    public MediaPlayer createMediaPlayer(String resourcePath, boolean loop, Double volumeFactor) {
        return mediaFactory.createMediaPlayer(resourcePath, loop, volumeFactor);
    }

    /**
     * Overload accepting primitive double for existing callers.
     */
    public MediaPlayer createMediaPlayer(String resourcePath, boolean loop, double volumeFactor) {
        return createMediaPlayer(resourcePath, loop, Double.valueOf(volumeFactor));
    }

    /**
     * Delegate disposal to the factory (keeps old API intact while moving
     * implementation into MediaPlayerFactory).
     */
    public void disposeMediaPlayer(MediaPlayer mp) {
        mediaFactory.disposeMediaPlayer(mp);
    }
}
