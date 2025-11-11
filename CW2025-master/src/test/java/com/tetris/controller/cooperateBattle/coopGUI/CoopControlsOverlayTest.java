package com.tetris.controller.cooperateBattle.coopGUI;

import org.junit.jupiter.api.*;

import com.comp2042.controller.cooperateBattle.coopGUI.CoopControlsOverlay;

import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.input.KeyCode;
import javafx.application.Platform;

public class CoopControlsOverlayTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void keySetStoresValues() {
        CoopControlsOverlay.KeySet ks = new CoopControlsOverlay.KeySet(KeyCode.A, KeyCode.B, KeyCode.C, KeyCode.D, KeyCode.E, KeyCode.F);
        assertEquals(KeyCode.A, ks.left);
        assertEquals(KeyCode.B, ks.right);
        assertEquals(KeyCode.C, ks.rotate);
        assertEquals(KeyCode.D, ks.down);
        assertEquals(KeyCode.E, ks.hard);
        assertEquals(KeyCode.F, ks.swap);
    }

    @Test
    void showWithNullSceneDoesNotThrow() {
        // Passing null scene should be handled gracefully (no exception thrown synchronously)
        assertDoesNotThrow(() -> {
            CoopControlsOverlay.show(null,
                new CoopControlsOverlay.KeySet(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.SHIFT),
                new CoopControlsOverlay.KeySet(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.SHIFT),
                new CoopControlsOverlay.KeySet(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.SHIFT),
                new CoopControlsOverlay.KeySet(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.SHIFT),
                (l,r) -> {},
                () -> {},
                () -> {}
            );
        });
    }
}
