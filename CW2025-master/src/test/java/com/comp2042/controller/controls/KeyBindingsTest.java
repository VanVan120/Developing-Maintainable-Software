package com.comp2042.controller.controls;

import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link KeyBindings} model.
 *
 * These tests cover the pure-Java behavior (no JavaFX toolkit required).
 */
class KeyBindingsTest {

    private KeyBindings kb;

    @BeforeEach
    void setUp() {
        kb = new KeyBindings();
    }

    @Test
    void defaultsAreSet() {
        assertEquals(KeyCode.LEFT, kb.get(Action.LEFT));
        assertEquals(KeyCode.RIGHT, kb.get(Action.RIGHT));
        assertEquals(KeyCode.DOWN, kb.get(Action.SOFT_DROP));
        assertEquals(KeyCode.SPACE, kb.get(Action.HARD_DROP));
        assertEquals(KeyCode.UP, kb.get(Action.ROTATE));
        assertEquals(KeyCode.C, kb.get(Action.SWITCH));
    }

    @Test
    void setAndGetWorks() {
        kb.set(Action.LEFT, KeyCode.A);
        assertEquals(KeyCode.A, kb.get(Action.LEFT));
        kb.set(Action.SWITCH, KeyCode.Z);
        assertEquals(KeyCode.Z, kb.get(Action.SWITCH));
    }

    @Test
    void findActionForKeyReturnsAssignedAction() {
        kb.set(Action.LEFT, KeyCode.A);
        kb.set(Action.RIGHT, KeyCode.D);
        assertEquals(Action.LEFT, kb.findActionForKey(KeyCode.A));
        assertEquals(Action.RIGHT, kb.findActionForKey(KeyCode.D));
        assertNull(kb.findActionForKey(KeyCode.S));
    }

    @Test
    void duplicateKeyAssignmentAllowedAndFindReturnsFirstMatch() {
        // KeyBindings does not prevent duplicate keys; findActionForKey will return
        // the first matching mapping in EnumMap iteration order (the enum declaration order).
        kb.set(Action.LEFT, KeyCode.A);
        kb.set(Action.RIGHT, KeyCode.A);

        Action found = kb.findActionForKey(KeyCode.A);
        assertNotNull(found);
        // enum order places LEFT before RIGHT, so LEFT should be returned
        assertEquals(Action.LEFT, found);
    }

    @Test
    void resetToDefaultsRestoresOriginalValues() {
        kb.set(Action.LEFT, KeyCode.A);
        kb.set(Action.ROTATE, KeyCode.B);
        kb.resetToDefaults();

        assertEquals(KeyCode.LEFT, kb.get(Action.LEFT));
        assertEquals(KeyCode.UP, kb.get(Action.ROTATE));
    }

    @Test
    void panelDefaultsAndResetToPanelDefaults() {
        EnumMap<Action, KeyCode> map = new EnumMap<>(Action.class);
    map.put(Action.LEFT, KeyCode.DIGIT1);
    map.put(Action.RIGHT, KeyCode.DIGIT2);
        kb.setPanelDefaults(map);

        // change current mappings
        kb.set(Action.LEFT, KeyCode.A);
        kb.set(Action.RIGHT, KeyCode.B);

        kb.resetToPanelDefaults();

    assertEquals(KeyCode.DIGIT1, kb.get(Action.LEFT));
    assertEquals(KeyCode.DIGIT2, kb.get(Action.RIGHT));
    }

    @Test
    void nullHandlingDoesNotThrow() {
        // operations with nulls should be safe
        kb.set(null, KeyCode.A);
        kb.set(Action.LEFT, null);
        assertNull(kb.get(null));
        assertNull(kb.findActionForKey(null));
        kb.setPanelDefaults(null);
        kb.setPanelDefault(null, null);
    }
}
