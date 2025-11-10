package com.comp2042.audio.soundManager;

import javafx.scene.media.AudioClip;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

/**
 * Utility responsible for loading short sound effects. Tries JavaFX AudioClip
 * first (simpler API), and falls back to javax.sound.sampled.Clip when the
 * AudioClip cannot be created (or the platform doesn't support it).
 */
public final class ClipLoader {

    private final Class<?> resourceOwner;

    public static final class AudioLoadResult {
        public final AudioClip audioClip;
        public final Clip fallbackClip;

        public AudioLoadResult(AudioClip audioClip, Clip fallbackClip) {
            this.audioClip = audioClip;
            this.fallbackClip = fallbackClip;
        }
    }

    public ClipLoader(Class<?> resourceOwner) {
        this.resourceOwner = resourceOwner == null ? getClass() : resourceOwner;
    }

    /**
     * Attempts to load the given resource path as an AudioClip, falling back
     * to a javax.sound.sampled.Clip for .wav resources if needed.
     * Returns an AudioLoadResult containing either an AudioClip or a fallback Clip (or both null).
     */
    public AudioLoadResult load(String resourcePath) {
        if (resourcePath == null) return new AudioLoadResult(null, null);

        // Prefer Class.getResource (allows absolute paths starting with '/')
        URL url = resourceOwner.getResource(resourcePath);

        // If not found, try the classloader (expects no leading '/').
        if (url == null) {
            String loaderPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            ClassLoader cl = resourceOwner.getClassLoader();
            if (cl != null) url = cl.getResource(loaderPath);
        }

        if (url == null) return new AudioLoadResult(null, null);

        // First try JavaFX AudioClip (convenient API for short sounds)
        try {
            AudioClip ac = new AudioClip(url.toExternalForm());
            return new AudioLoadResult(ac, null);
        } catch (Throwable ignored) {
            // Fall through to javax.sound fallback below
        }

        // Fallback: try loading via AudioSystem (works well for .wav on many platforms)
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip c = AudioSystem.getClip();
            c.open(ais);
            return new AudioLoadResult(null, c);
        } catch (Throwable ignored) {
            // give up and return empty result
        }

        return new AudioLoadResult(null, null);
    }
}
