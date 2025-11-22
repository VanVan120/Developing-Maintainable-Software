package com.comp2042.audio.soundManager;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import com.comp2042.audio.audioSettings.AudioSettings;

import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Factory responsible for creating and disposing {@link MediaPlayer}
 * instances while attaching volume listeners that respond to
 * {@link com.comp2042.audio.audioSettings.AudioSettings} changes.
 *
 * <p>Using a factory keeps {@link javafx.scene.media.MediaPlayer} lifecycle
 * concerns isolated from {@link com.comp2042.audio.soundManager.SoundManager}.
 */
public class MediaPlayerFactory {
    private final Class<?> resourceOwner;
    private final Map<MediaPlayer, javafx.beans.value.ChangeListener<Number>> mpVolumeListeners = new WeakHashMap<>();

    /**
     * Create a factory that resolves media resources relative to the provided
     * {@code resourceOwner} class. When {@code null} the factory's class is
     * used.
     */
    public MediaPlayerFactory(Class<?> resourceOwner) {
        this.resourceOwner = resourceOwner == null ? getClass() : resourceOwner;
    }

    /**
     * Create a {@link MediaPlayer} for the given resource path.
     *
     * <p>The created player will not auto-play and will have a volume bound to
     * the current audio settings. A change listener is attached so the player
     * responds to future volume changes. If the resource cannot be resolved
     * or player creation fails the method returns {@code null}.
     *
     * @param resourcePath classpath-like path to the media resource
     * @param loop whether the player should loop indefinitely
     * @param volumeFactor optional multiplier applied on top of master/music
     *                     volumes (may be {@code null})
     * @return a configured {@link MediaPlayer} or {@code null} if creation fails
     */
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
            mp.setOnError(() -> System.err.println("[MediaPlayerFactory] MediaPlayer error for " + resourcePath + ": " + mp.getError()));

            javafx.beans.value.ChangeListener<Number> volListener = (obs, o, n) -> {
                try {
                    double b = AudioSettings.getMasterVolume() * AudioSettings.getMusicVolume();
                    double s = (volumeFactor != null) ? Math.max(0.0, Math.min(1.0, b * volumeFactor)) : b;
                    mp.setVolume(s);
                } catch (Exception ignored) {}
            };
            AudioSettings.masterProperty().addListener(volListener);
            AudioSettings.musicProperty().addListener(volListener);
            try { mpVolumeListeners.put(mp, volListener); } catch (Exception ignored) {}
            return mp;
        } catch (Throwable ex) {
            System.err.println("[MediaPlayerFactory] Failed to create MediaPlayer for " + resourcePath + ": " + ex);
            return null;
        }
    }

    /**
     * Dispose a single {@link MediaPlayer} previously created by this factory.
     * Attached listeners are removed and the player is stopped/disposed.
     *
     * @param mp the player to dispose; method is tolerant of {@code null}
     */
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

    /**
     * Dispose all tracked {@link MediaPlayer} instances and detach listeners.
     */
    public void disposeAll() {
        try {
            for (MediaPlayer mp : new java.util.ArrayList<>(mpVolumeListeners.keySet())) {
                try { disposeMediaPlayer(mp); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }
}
