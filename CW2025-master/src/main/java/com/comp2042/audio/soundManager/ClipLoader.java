package com.comp2042.audio.soundManager;

import javafx.scene.media.AudioClip;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.net.URL;

/**
 * Loader utility for short sound effects.
 *
 * <p>The loader first tries to construct a JavaFX {@link AudioClip} from the
 * resource URL (convenient for short sounds). If creating an {@code AudioClip}
 * fails (for example on platforms where JavaFX media is not available) the
 * loader falls back to {@link javax.sound.sampled.Clip} which works well for
 * many WAV files.
 */
public final class ClipLoader {

    private final Class<?> resourceOwner;

    /**
     * Result returned by {@link #load(String)}. Either {@code audioClip} or
     * {@code fallbackClip} may be non-null depending on what could be created.
     */
    public static final class AudioLoadResult {
        public final AudioClip audioClip;
        public final Clip fallbackClip;

        public AudioLoadResult(AudioClip audioClip, Clip fallbackClip) {
            this.audioClip = audioClip;
            this.fallbackClip = fallbackClip;
        }
    }

    /**
     * Create a loader that resolves resources using the supplied class as the
     * resource owner. When {@code null} the loader class itself is used.
     */
    public ClipLoader(Class<?> resourceOwner) {
        this.resourceOwner = resourceOwner == null ? getClass() : resourceOwner;
    }

    /**
     * Attempt to load the resource at {@code resourcePath} as an audio asset.
     *
     * <p>The method resolves the resource using the configured {@code
     * resourceOwner}, tries to build a JavaFX {@link AudioClip} and, if that
     * fails, attempts to open a {@link Clip} using {@link AudioSystem}.
     *
     * @param resourcePath classpath-like resource path (may start with '/')
     * @return an {@link AudioLoadResult} containing either an {@link AudioClip}
     *         or a {@link Clip} (or both {@code null} if loading failed)
     */
    public AudioLoadResult load(String resourcePath) {
        if (resourcePath == null) return new AudioLoadResult(null, null);

        URL url = resourceOwner.getResource(resourcePath);

        if (url == null) {
            String loaderPath = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            ClassLoader cl = resourceOwner.getClassLoader();
            if (cl != null) url = cl.getResource(loaderPath);
        }

        if (url == null) return new AudioLoadResult(null, null);

        try {
            AudioClip ac = new AudioClip(url.toExternalForm());
            return new AudioLoadResult(ac, null);
        } catch (Throwable ignored) {
        }

        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(url);
            Clip c = AudioSystem.getClip();
            c.open(ais);
            return new AudioLoadResult(null, c);
        } catch (Throwable ignored) {
        }

        return new AudioLoadResult(null, null);
    }
}
