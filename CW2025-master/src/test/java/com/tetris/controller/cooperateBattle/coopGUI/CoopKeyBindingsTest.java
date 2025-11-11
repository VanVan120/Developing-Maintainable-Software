package com.tetris.controller.cooperateBattle.coopGUI;

import org.junit.jupiter.api.*;

import com.comp2042.controller.cooperateBattle.coopGUI.CoopKeyBindings;

import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.input.KeyCode;

import java.util.prefs.Preferences;
import java.util.UUID;

public class CoopKeyBindingsTest {

    @Test
    void defaultsMatchConstants() {
        CoopKeyBindings kb = new CoopKeyBindings();
        assertEquals(CoopKeyBindings.DEFAULT_LEFT_LEFT, kb.getLeftMoveLeft());
        assertEquals(CoopKeyBindings.DEFAULT_RIGHT_RIGHT, kb.getRightMoveRight());
        assertEquals(CoopKeyBindings.DEFAULT_LEFT_ROTATE, kb.getLeftRotate());
        assertEquals(CoopKeyBindings.DEFAULT_RIGHT_ROTATE, kb.getRightRotate());
    }

    @Test
    void setLeftKeys_updatesGetters() {
        CoopKeyBindings kb = new CoopKeyBindings();
        kb.setLeftKeys(KeyCode.Z, KeyCode.X, KeyCode.C, KeyCode.V, KeyCode.B, KeyCode.N);
        assertEquals(KeyCode.Z, kb.getLeftMoveLeft());
        assertEquals(KeyCode.X, kb.getLeftMoveRight());
        assertEquals(KeyCode.C, kb.getLeftRotate());
        assertEquals(KeyCode.V, kb.getLeftDown());
        assertEquals(KeyCode.B, kb.getLeftHard());
        assertEquals(KeyCode.N, kb.getLeftSwap());
    }

    @Test
    void saveAndLoadPreferences_roundTripsValues() throws Exception {
        String nodeName = "test/CoopKeyBindingsTest/" + UUID.randomUUID();
        Preferences prefs = Preferences.userRoot().node(nodeName);
        try {
            // prepare bindings and save
            CoopKeyBindings kb = new CoopKeyBindings();
            kb.setLeftKeys(KeyCode.Z, KeyCode.X, KeyCode.C, KeyCode.V, KeyCode.B, KeyCode.N);
            kb.setRightKeys(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.C);
            kb.saveToPreferences(prefs);

            // new instance loads from the same prefs
            CoopKeyBindings loaded = new CoopKeyBindings();
            loaded.loadFromPreferences(prefs);

            assertEquals(KeyCode.Z, loaded.getLeftMoveLeft());
            assertEquals(KeyCode.X, loaded.getLeftMoveRight());
            assertEquals(KeyCode.C, loaded.getLeftRotate());
            assertEquals(KeyCode.V, loaded.getLeftDown());
            assertEquals(KeyCode.B, loaded.getLeftHard());
            assertEquals(KeyCode.N, loaded.getLeftSwap());

            assertEquals(KeyCode.LEFT, loaded.getRightMoveLeft());
            assertEquals(KeyCode.RIGHT, loaded.getRightMoveRight());
            assertEquals(KeyCode.UP, loaded.getRightRotate());
            assertEquals(KeyCode.DOWN, loaded.getRightDown());
            assertEquals(KeyCode.SPACE, loaded.getRightHard());
            assertEquals(KeyCode.C, loaded.getRightSwap());

        } finally {
            try { prefs.removeNode(); } catch (Exception ignored) {}
        }
    }
}
