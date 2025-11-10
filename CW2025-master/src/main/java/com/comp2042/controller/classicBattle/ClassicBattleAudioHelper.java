package com.comp2042.controller.classicBattle;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

/**
 * Audio helper extracted from ClassicBattle to centralise music/sfx loading
 * and lifecycle management. This class owns the players used for the
 * countdown and game-over sounds so callers can simply ask it to play/stop.
 */
public final class ClassicBattleAudioHelper {

    private final Class<?> ctx;

    private MediaPlayer matchGameOverPlayer = null;
    private Clip matchGameOverClipFallback = null;
    private MediaPlayer matchCountdownPlayer = null;
    private Clip matchCountdownClipFallback = null;

    public ClassicBattleAudioHelper(Class<?> ctx) {
        this.ctx = ctx == null ? ClassicBattleAudioHelper.class : ctx;
    }

    // Keep small static lifecycle helpers for convenience
    public static MediaPlayer stopAndDispose(MediaPlayer p) {
        try {
            if (p != null) {
                try { p.stop(); } catch (Exception ignored) {}
                try { p.dispose(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    public static Clip stopAndClose(Clip c) {
        try {
            if (c != null) {
                try { c.stop(); } catch (Exception ignored) {}
                try { c.close(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    private MediaPlayer loadMediaPlayer(String baseName, int cycleCount, Double volume, Runnable onEnd) {
        try {
            URL musicUrl = ctx.getClassLoader().getResource("sounds/" + baseName + ".wav");
            if (musicUrl == null) musicUrl = ctx.getClassLoader().getResource("sounds/" + baseName + ".mp3");
            if (musicUrl != null) {
                Media m = new Media(musicUrl.toExternalForm());
                MediaPlayer mp = new MediaPlayer(m);
                mp.setCycleCount(cycleCount);
                mp.setAutoPlay(true);
                if (volume != null) mp.setVolume(volume.doubleValue());
                if (onEnd != null) mp.setOnEndOfMedia(onEnd);
                return mp;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private Clip loadClip(String baseName, int loopCount) {
        try {
            URL u = ctx.getClassLoader().getResource("sounds/" + baseName + ".wav");
            if (u != null) {
                AudioInputStream ais = AudioSystem.getAudioInputStream(u);
                Clip c = AudioSystem.getClip();
                c.open(ais);
                c.loop(loopCount);
                return c;
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void stopMatchGameOverSound() {
        matchGameOverPlayer = stopAndDispose(matchGameOverPlayer);
        matchGameOverClipFallback = stopAndClose(matchGameOverClipFallback);
    }

    public void playMatchGameOverSound() {
        try {
            // stop any existing players/clips first
            matchGameOverPlayer = stopAndDispose(matchGameOverPlayer);
            matchGameOverClipFallback = stopAndClose(matchGameOverClipFallback);

            // Try JavaFX MediaPlayer first (tries .wav then .mp3)
            matchGameOverPlayer = loadMediaPlayer("GameOver", 1, null, () -> {
                matchGameOverPlayer = stopAndDispose(matchGameOverPlayer);
            });
            if (matchGameOverPlayer != null) return;
        } catch (Exception ignored) {}

        try {
            // Fallback to Clip (.wav)
            matchGameOverClipFallback = loadClip("GameOver", 1);
        } catch (Exception ignored) {}
    }

    public void playMatchCountdownSound() {
        try {
            matchCountdownPlayer = stopAndDispose(matchCountdownPlayer);
            matchCountdownClipFallback = stopAndClose(matchCountdownClipFallback);

            // Try JavaFX MediaPlayer first (tries .wav then .mp3)
            matchCountdownPlayer = loadMediaPlayer("Countdown", MediaPlayer.INDEFINITE, 0.75, null);
            if (matchCountdownPlayer != null) return;
        } catch (Exception ignored) {}

        try {
            // Fallback to Clip (.wav)
            matchCountdownClipFallback = loadClip("Countdown", Clip.LOOP_CONTINUOUSLY);
        } catch (Exception ignored) {}
    }

    public void stopMatchCountdownSound() {
        matchCountdownPlayer = stopAndDispose(matchCountdownPlayer);
        matchCountdownClipFallback = stopAndClose(matchCountdownClipFallback);
    }
}
