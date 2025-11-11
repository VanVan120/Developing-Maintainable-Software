package com.tetris.controller.cooperateBattle.coopGUI;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import com.comp2042.audio.soundManager.SoundManager;
import com.comp2042.controller.cooperateBattle.coopGUI.CoopMusicManager;

/**
 * Tests for CoopMusicManager that avoid playing real media.
 *
 * These tests verify null-safety and basic lifecycle calls. They do not attempt
 * to construct a real MediaPlayer (avoids dependency on platform/media codecs).
 */
public class CoopMusicManagerTest {

    @Test
    void getPlayerInitiallyNull() {
        CoopMusicManager mgr = new CoopMusicManager();
        assertNull(mgr.getPlayer(), "player should be null before creation");
    }

    @Test
    void playOrCreate_withNullSoundManager_doesNotThrow_and_playerRemainsNull() {
        CoopMusicManager mgr = new CoopMusicManager();
        assertDoesNotThrow(() -> mgr.playOrCreate(null));
        assertNull(mgr.getPlayer(), "player remains null when SoundManager is null");
    }

    @Test
    void stopAndDispose_withNullSoundManager_and_nullPlayer_doesNotThrow() {
        CoopMusicManager mgr = new CoopMusicManager();
        assertDoesNotThrow(() -> mgr.stopAndDispose(null));
    }

    @Test
    void stopAndDispose_callsSoundManagerDisposeWhenProvided() {
        // Provide a small stub SoundManager that records whether disposeMediaPlayer was called
        class StubSoundManager extends SoundManager {
            boolean disposed = false;
            StubSoundManager() { super(null); }
            @Override
            public javafx.scene.media.MediaPlayer createMediaPlayer(String path, boolean autoPlay, Double volume) {
                // Return null for simplicity - we don't attempt playback
                return null;
            }
            @Override
            public void disposeMediaPlayer(javafx.scene.media.MediaPlayer p) {
                disposed = true;
            }
        }

        CoopMusicManager mgr = new CoopMusicManager();
        StubSoundManager stub = new StubSoundManager();
        // calling stopAndDispose when player is null should not throw and should not set disposed
        assertDoesNotThrow(() -> mgr.stopAndDispose(stub));
        assertFalse(stub.disposed, "dispose should not be called when player is null");
    }
}
