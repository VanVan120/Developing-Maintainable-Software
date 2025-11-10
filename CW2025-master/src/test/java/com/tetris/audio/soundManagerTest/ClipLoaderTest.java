package com.tetris.audio.soundManagerTest;

import com.comp2042.audio.soundManager.ClipLoader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClipLoaderTest {

    @Test
    void load_nullResourcePath_returnsEmptyResult() {
        ClipLoader loader = new ClipLoader(ClipLoaderTest.class);
        ClipLoader.AudioLoadResult r = loader.load(null);
        assertNotNull(r);
        assertNull(r.audioClip);
        assertNull(r.fallbackClip);
    }

    @Test
    void load_nonexistentResource_returnsEmptyResult() {
        ClipLoader loader = new ClipLoader(ClipLoaderTest.class);
        ClipLoader.AudioLoadResult r = loader.load("/sounds/this-does-not-exist.wav");
        assertNotNull(r);
        assertNull(r.audioClip);
        assertNull(r.fallbackClip);
    }
}
