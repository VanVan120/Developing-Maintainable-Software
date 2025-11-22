package com.comp2042.controller.classicBattle;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

/**
 * Helper that centralises loading and lifecycle management for music and
 * short sound effects used by the Classic Battle controller.
 *
 * <p>The helper tries to create JavaFX {@link javafx.scene.media.MediaPlayer}
 * instances for music assets (.wav or .mp3) and falls back to
 * {@link javax.sound.sampled.Clip} for shorter sounds or when the JavaFX
 * media subsystem is unavailable. It also exposes small lifecycle helpers
 * that stop and dispose players/clips and return {@code null} to make
 * caller-side replacement convenient (e.g. {@code p = stopAndDispose(p)}).
 */
public final class ClassicBattleAudioHelper {

    private final Class<?> ctx;

    private MediaPlayer matchGameOverPlayer = null;
    private Clip matchGameOverClipFallback = null;
    private MediaPlayer matchCountdownPlayer = null;
    private Clip matchCountdownClipFallback = null;

    /**
     * Create a new helper that resolves resources using the provided class's
     * class loader. Passing {@code null} will cause the helper to use its own
     * class for resource resolution.
     *
     * @param ctx class used as the resource owner; may be {@code null}
     */
    public ClassicBattleAudioHelper(Class<?> ctx) {
        this.ctx = ctx == null ? ClassicBattleAudioHelper.class : ctx;
    }

    /**
     * Stop and dispose the provided {@link MediaPlayer} if non-null.
     *
     * <p>The method intentionally swallows exceptions to keep callers simple;
     * it returns {@code null} so callers can assign the result back into
     * their variable (convenience pattern used in this codebase).
     *
     * @param p player to stop and dispose
     * @return always {@code null}
     */
    public static MediaPlayer stopAndDispose(MediaPlayer p) {
        try {
            if (p != null) {
                try { p.stop(); } catch (Exception ignored) {}
                try { p.dispose(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Stop and close the provided {@link Clip} if non-null and return
     * {@code null} for convenient reassignment.
     *
     * @param c clip to stop and close
     * @return always {@code null}
     */
    public static Clip stopAndClose(Clip c) {
        try {
            if (c != null) {
                try { c.stop(); } catch (Exception ignored) {}
                try { c.close(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Attempt to load a music asset using the helper's resource owner. The
     * method tries {@code baseName}.wav first then {@code baseName}.mp3. When
     * a resource is found a {@link MediaPlayer} is created, configured (cycle
     * count and optional volume) and returned. The method returns {@code
     * null} if no resource is found or creation fails.
     *
     * @param baseName resource base name without extension
     * @param cycleCount cycle count for the player (use {@code MediaPlayer.INDEFINITE}
     *                   for looping)
     * @param volume optional initial volume (0.0-1.0) or {@code null}
     * @param onEnd optional runnable executed when playback ends
     * @return configured MediaPlayer or {@code null} if creation failed
     */
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

    /**
     * Load a short sound fallback as a {@link Clip}. The method attempts to
     * locate a WAV resource and open it as a Clip which is then configured to
     * loop the specified number of times.
     *
     * @param baseName resource base name without extension
     * @param loopCount number of times to loop (use {@code Clip.LOOP_CONTINUOUSLY}
     *                  for continuous looping)
     * @return opened Clip or {@code null} on failure
     */
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

    /**
     * Stop any currently playing match "game over" sounds and release
     * associated resources.
     */
    public void stopMatchGameOverSound() {
        matchGameOverPlayer = stopAndDispose(matchGameOverPlayer);
        matchGameOverClipFallback = stopAndClose(matchGameOverClipFallback);
    }

    /**
     * Play the match "game over" sound. The helper first attempts to create
     * and play a {@link MediaPlayer}. If that fails it falls back to a
     * {@link Clip} playback. Exceptions during playback are swallowed to keep
     * the call-site simple.
     */
    public void playMatchGameOverSound() {
        try {
            matchGameOverPlayer = stopAndDispose(matchGameOverPlayer);
            matchGameOverClipFallback = stopAndClose(matchGameOverClipFallback);

            matchGameOverPlayer = loadMediaPlayer("GameOver", 1, null, () -> {
                matchGameOverPlayer = stopAndDispose(matchGameOverPlayer);
            });
            if (matchGameOverPlayer != null) return;
        } catch (Exception ignored) {}

        try {
            matchGameOverClipFallback = loadClip("GameOver", 1);
        } catch (Exception ignored) {}
    }

    /**
     * Start the looping match countdown sound. Uses a MediaPlayer when
     * available and falls back to a Clip.
     */
    public void playMatchCountdownSound() {
        try {
            matchCountdownPlayer = stopAndDispose(matchCountdownPlayer);
            matchCountdownClipFallback = stopAndClose(matchCountdownClipFallback);

            matchCountdownPlayer = loadMediaPlayer("Countdown", MediaPlayer.INDEFINITE, 0.75, null);
            if (matchCountdownPlayer != null) return;
        } catch (Exception ignored) {}

        try {
            matchCountdownClipFallback = loadClip("Countdown", Clip.LOOP_CONTINUOUSLY);
        } catch (Exception ignored) {}
    }

    /**
     * Stop any countdown sounds and release resources.
     */
    public void stopMatchCountdownSound() {
        matchCountdownPlayer = stopAndDispose(matchCountdownPlayer);
        matchCountdownClipFallback = stopAndClose(matchCountdownClipFallback);
    }
}
