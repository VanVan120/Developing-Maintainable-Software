package com.comp2042.controller.cooperateBattle.coopGUI;

import org.junit.jupiter.api.*;

import javafx.scene.input.KeyCode;

import com.comp2042.controller.cooperateBattle.coopController.CoopGameController;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Simple JUnit5 tests for CoopGuiController that avoid external test libraries.
 * These tests use reflection to call private methods and to inject a small
 * CoopGameController subclass to observe interactions.
 */
public class CoopGuiControllerTest {

    private CoopGuiController controller;

    @BeforeEach
    void setUp() {
        controller = new CoopGuiController();
    }

    @Test
    void testHandleKeyCode_leftMove_invokesCoopMoveLeft() throws Exception {
        // arrange: provide a CoopGameController stub by subclassing to record calls
        class StubCoop extends CoopGameController {
            int leftMoveCalls = 0;
            StubCoop() { super(10, 20); createNewGame(); }
            @Override public boolean moveLeftPlayerLeft() { leftMoveCalls++; return true; }
        }

        StubCoop stub = new StubCoop();
        // inject stub into controller
        setField(controller, "coop", stub);

        // set a known key mapping for left-move-left
        controller.setLeftKeys(KeyCode.A, KeyCode.B, KeyCode.C, KeyCode.D, KeyCode.E, KeyCode.F);

        // call private handleKeyCode via reflection
        Method m = CoopGuiController.class.getDeclaredMethod("handleKeyCode", KeyCode.class);
        m.setAccessible(true);
        Object result = m.invoke(controller, KeyCode.A);

        Assertions.assertTrue(result instanceof Boolean);
        Assertions.assertTrue((Boolean) result, "handleKeyCode should return true when the stub returns true");
        Assertions.assertEquals(1, stub.leftMoveCalls, "stub.moveLeftPlayerLeft should have been invoked once");
    }

    @Test
    void testHandleKeyCode_unmappedKey_returnsFalse() throws Exception {
        // ensure no coop is present (unmapped should return false)
        setField(controller, "coop", null);
        Method m = CoopGuiController.class.getDeclaredMethod("handleKeyCode", KeyCode.class);
        m.setAccessible(true);
        Object result = m.invoke(controller, KeyCode.Z);
        Assertions.assertTrue(result instanceof Boolean);
        Assertions.assertFalse((Boolean) result, "unmapped key should return false");
    }

    @Test
    void testIsInputBlocked_coopPaused_true() throws Exception {
        // when coopPaused is true, isInputBlocked should return true
        setField(controller, "coopPaused", Boolean.TRUE);
        Method m = CoopGuiController.class.getDeclaredMethod("isInputBlocked");
        m.setAccessible(true);
        Object res = m.invoke(controller);
        Assertions.assertTrue(res instanceof Boolean);
        Assertions.assertTrue((Boolean) res, "isInputBlocked should be true when coopPaused is true");
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ex) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException(name);
    }
}
