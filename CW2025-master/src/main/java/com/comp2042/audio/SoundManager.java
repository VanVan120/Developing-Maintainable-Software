package com.comp2042.audio;

import javafx.beans.value.ChangeListener;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
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
    private final java.util.Map<MediaPlayer, javafx.beans.value.ChangeListener<Number>> mpVolumeListeners = new java.util.WeakHashMap<>();
    private ChangeListener<Number> settingsVolumeListener = null;

    public SoundManager(Class<?> resourceOwner) {
        this.resourceOwner = resourceOwner == null ? getClass() : resourceOwner;
    }

    public void init() {
        hoverClip = loadAudioClip("/sounds/hover.wav", c -> hoverClip = c);
        clickClip = loadAudioClip("/sounds/click.wav", c -> clickClip = c);
        hardDropClip = loadAudioClip("/sounds/HardDrop.wav", c -> hardDropClip = c);

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
                java.util.List<MediaPlayer> list = new java.util.ArrayList<>(mpVolumeListeners.keySet());
                for (MediaPlayer mp : list) {
                    try { disposeMediaPlayer(mp); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}

            try { if (hoverFallback != null) { hoverFallback.close(); hoverFallback = null; } } catch (Exception ignored) {}
            try { if (clickFallback != null) { clickFallback.close(); clickFallback = null; } } catch (Exception ignored) {}
            try { if (hardDropFallback != null) { hardDropFallback.close(); hardDropFallback = null; } } catch (Exception ignored) {}

            try { hoverClip = null; } catch (Exception ignored) {}
            try { clickClip = null; } catch (Exception ignored) {}
            try { hardDropClip = null; } catch (Exception ignored) {}
        } catch (Throwable ignored) {}
    }

    private AudioClip loadAudioClip(String resourcePath, java.util.function.Consumer<AudioClip> setter) {
        try {
            URL url = resourceOwner.getResource(resourcePath);
            if (url == null) return initFallbackClip(resourcePath, setter);
            AudioClip ac = new AudioClip(url.toExternalForm());
            setter.accept(ac);
            return ac;
        } catch (Throwable ex) {
            return initFallbackClip(resourcePath, setter);
        }
    }

    private AudioClip initFallbackClip(String resourcePath, java.util.function.Consumer<AudioClip> setter) {
        try {
            URL url = resourceOwner.getResource(resourcePath);
            if (url == null) return null;
            try (AudioInputStream ais = AudioSystem.getAudioInputStream(url)) {
                Clip c = AudioSystem.getClip();
                c.open(ais);
                if (resourcePath.contains("hover")) hoverFallback = c;
                else if (resourcePath.contains("click")) clickFallback = c;
                else if (resourcePath.toLowerCase().contains("harddrop")) hardDropFallback = c;
            }
        } catch (Throwable ignored) {}
        return null;
    }

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
            URL mus = resourceOwner.getResource("/sounds/GameOver.wav");
            if (mus == null) return;
            Media m = new Media(mus.toExternalForm());
            gameOverMusicPlayer = new MediaPlayer(m);
            gameOverMusicPlayer.setCycleCount(1);
            gameOverMusicPlayer.setVolume(AudioSettings.getMasterVolume() * AudioSettings.getMusicVolume());
            gameOverMusicPlayer.setAutoPlay(true);
        } catch (Throwable ex) {
            System.err.println("[SoundManager] Failed to play GameOver music: " + ex);
        }
    }

    public void stopGameOverMusic() {
        try {
            if (gameOverMusicPlayer != null) {
                try { gameOverMusicPlayer.stop(); } catch (Exception ignored) {}
                try { gameOverMusicPlayer.dispose(); } catch (Exception ignored) {}
                gameOverMusicPlayer = null;
            }
        } catch (Throwable ignored) {}
    }

    public void playCountdownMusic() {
        try {
            if (countdownMusicPlayer != null) return;
            URL mus = resourceOwner.getResource("/sounds/Countdown.wav");
            if (mus == null) return;
            Media m = new Media(mus.toExternalForm());
            countdownMusicPlayer = new MediaPlayer(m);
            countdownMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
            countdownMusicPlayer.setVolume(AudioSettings.getMasterVolume() * AudioSettings.getMusicVolume());
            countdownMusicPlayer.setAutoPlay(true);
        } catch (Throwable ex) {
            System.err.println("[SoundManager] Failed to play Countdown music: " + ex);
        }
    }

    public void stopCountdownMusic() {
        try {
            if (countdownMusicPlayer != null) {
                try { countdownMusicPlayer.stop(); } catch (Exception ignored) {}
                try { countdownMusicPlayer.dispose(); } catch (Exception ignored) {}
                countdownMusicPlayer = null;
            }
        } catch (Throwable ignored) {}
    }

    public MediaPlayer createMediaPlayer(String resourcePath, boolean loop, Double volumeFactor) {
        try {
            URL mus = resourceOwner.getResource(resourcePath);
            if (mus == null) mus = resourceOwner.getClassLoader().getResource(resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath);
            if (mus == null) return null;
            Media m = new Media(mus.toExternalForm());
            MediaPlayer mp = new MediaPlayer(m);
            mp.setCycleCount(loop ? MediaPlayer.INDEFINITE : 1);
            double base = AudioSettings.getMasterVolume() * AudioSettings.getMusicVolume();
            double scaled = (volumeFactor != null) ? Math.max(0.0, Math.min(1.0, base * volumeFactor)) : base;
            try { mp.setVolume(scaled); } catch (Exception ignored) {}
            mp.setAutoPlay(false);
            mp.setOnError(() -> System.err.println("[SoundManager] MediaPlayer error for " + resourcePath + ": " + mp.getError()));

            javafx.beans.value.ChangeListener<Number> volListener = (obs, o, n) -> {
                try {
                    double b = AudioSettings.getMasterVolume() * AudioSettings.getMusicVolume();
                    double s = (volumeFactor != null) ? Math.max(0.0, Math.min(1.0, b * volumeFactor)) : b;
                    mp.setVolume(s);
                } catch (Exception ignored) {}
            };
            AudioSettings.masterProperty().addListener(volListener);
            AudioSettings.musicProperty().addListener(volListener);
            // remember the listener so dispose can remove it later
            try { mpVolumeListeners.put(mp, volListener); } catch (Exception ignored) {}
            return mp;
        } catch (Throwable ex) {
            System.err.println("[SoundManager] Failed to create MediaPlayer for " + resourcePath + ": " + ex);
            return null;
        }
    }

    public void disposeMediaPlayer(MediaPlayer mp) {
        if (mp == null) return;
        try {
            try {
                javafx.beans.value.ChangeListener<Number> l = mpVolumeListeners.remove(mp);
                if (l != null) {
                    AudioSettings.masterProperty().removeListener(l);
                    AudioSettings.musicProperty().removeListener(l);
                }
            } catch (Exception ignored) {}
            try { mp.stop(); } catch (Exception ignored) {}
            try { mp.dispose(); } catch (Exception ignored) {}
        } catch (Throwable ignored) {}
    }
}
