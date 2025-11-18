package com.comp2042.audio.soundManagerTest;

import com.comp2042.audio.soundManager.MediaPlayerFactory;
import javafx.scene.media.MediaPlayer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MediaPlayerFactoryTest {

    @Test
    void createMediaPlayer_whenResourceMissing_returnsNull() {
        MediaPlayerFactory f = new MediaPlayerFactory(MediaPlayerFactoryTest.class);
        MediaPlayer mp = f.createMediaPlayer("/sounds/does-not-exist.mp3", false, null);
        assertNull(mp);
    }

    @Test
    void disposeMediaPlayer_null_doesNotThrow() {
        MediaPlayerFactory f = new MediaPlayerFactory(MediaPlayerFactoryTest.class);
        // Should not throw
        f.disposeMediaPlayer(null);
    }

    @Test
    void disposeAll_onEmpty_doesNotThrow() {
        MediaPlayerFactory f = new MediaPlayerFactory(MediaPlayerFactoryTest.class);
        f.disposeAll();
    }
}
