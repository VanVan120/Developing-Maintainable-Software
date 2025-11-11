package com.tetris.controller.cooperateBattle.coopGUI;

import org.junit.jupiter.api.*;

import com.comp2042.controller.cooperateBattle.coopGUI.CoopGuiController;
import com.comp2042.controller.cooperateBattle.coopGUI.CoopInputHandler;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
// no direct KeyCode/KeyEvent imports needed for this test

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Tests for CoopInputHandler without external mocking libraries.
 */
public class CoopInputHandlerTest {

    @BeforeAll
    static void initToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // already initialized
        }
    }

    @Test
    void attachToScene_and_detach_behavior() throws Exception {
        Pane root = new Pane();
        Scene scene = new Scene(root, 400, 300);

        // use a plain controller instance (we just need an object reference)
        CoopGuiController stub = new CoopGuiController();

        CoopInputHandler handler = new CoopInputHandler(stub);

        // attach on FX thread and wait for attach to complete
        final CountDownLatch attachLatch = new CountDownLatch(1);
        Platform.runLater(() -> { handler.attachToScene(scene); attachLatch.countDown(); });
        Assertions.assertTrue(attachLatch.await(500, TimeUnit.MILLISECONDS), "attachToScene should complete quickly");

        // verify via reflection that the handler recorded the attached scene
        java.lang.reflect.Field f = CoopInputHandler.class.getDeclaredField("attachedScene");
        f.setAccessible(true);
        Object attached = f.get(handler);
        Assertions.assertSame(scene, attached, "attachedScene should reference the scene after attachToScene");

        // now detach on FX thread and verify attachedScene cleared
        final CountDownLatch detachLatch = new CountDownLatch(1);
        Platform.runLater(() -> { handler.detach(); detachLatch.countDown(); });
        Assertions.assertTrue(detachLatch.await(500, TimeUnit.MILLISECONDS), "detach should complete quickly");
        Object attachedAfter = f.get(handler);
        Assertions.assertNull(attachedAfter, "attachedScene should be null after detach");
    }
}
