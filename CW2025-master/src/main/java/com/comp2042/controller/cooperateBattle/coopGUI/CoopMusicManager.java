package com.comp2042.controller.cooperateBattle.coopGUI;

import java.util.logging.Level;
import java.util.logging.Logger;
import com.comp2042.audio.soundManager.SoundManager;

/**
 * Small helper to manage the lifecycle of the cooperative mode music player.
 * Centralises creation, play and disposal so the controller is cleaner.
 */
public class CoopMusicManager {
    private static final Logger LOGGER = Logger.getLogger(CoopMusicManager.class.getName());

    private javafx.scene.media.MediaPlayer player = null;

    public CoopMusicManager() {}

    /**
     * Return the internal media player instance, or {@code null} if none was created.
     */
    public javafx.scene.media.MediaPlayer getPlayer() { return player; }

    /**
     * Create the media player via SoundManager if available and play it.
     */
    public void playOrCreate(SoundManager sm) {
        try {
            if (player == null) {
                if (sm != null) {
                    player = sm.createMediaPlayer("/sounds/CorporateBattle.wav", true, 0.6);
                }
                // If SoundManager not available, player remains null and caller should handle it
            }
            if (player != null) {
                try { player.play(); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to create/play coop music", e);
        }
    }

    /**
     * Stop and dispose player using SoundManager when possible, otherwise use direct API.
     */
    public void stopAndDispose(SoundManager sm) {
        try {
            if (player != null) {
                try {
                    if (sm != null) sm.disposeMediaPlayer(player);
                    else { try { player.stop(); } catch (Exception ignored) {} try { player.dispose(); } catch (Exception ignored) {} }
                } catch (Exception e) {
                    LOGGER.log(Level.FINER, "Failed to dispose coop music player via SoundManager", e);
                }
                player = null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.FINER, "Failed to stop/dispose coop music", e);
        }
    }
}
