package com.tetris.audio.soundManagerTest;

import com.comp2042.audio.soundManager.SoundManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SoundManagerTest {

    @Test
    void init_and_playMethods_doNotThrow_whenResourcesMissing() {
        SoundManager s = new SoundManager(SoundManagerTest.class);
        // init should handle missing resources gracefully
        assertDoesNotThrow(s::init);

        // playing when no clips are loaded should not throw (falls back to beep)
        assertDoesNotThrow(s::playHoverSound);
        assertDoesNotThrow(s::playClickSound);
        assertDoesNotThrow(s::playHardDropSound);
    }

    @Test
    void musicControls_doNotThrow_whenNoMedia() {
        SoundManager s = new SoundManager(SoundManagerTest.class);
        assertDoesNotThrow(s::startSingleplayerMusic);
        assertDoesNotThrow(s::stopSingleplayerMusic);
        assertDoesNotThrow(s::playGameOverMusic);
        assertDoesNotThrow(s::stopGameOverMusic);
        assertDoesNotThrow(s::playCountdownMusic);
        assertDoesNotThrow(s::stopCountdownMusic);
    }
}
