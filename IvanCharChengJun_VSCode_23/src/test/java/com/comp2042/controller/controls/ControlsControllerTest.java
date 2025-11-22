package com.comp2042.controller.controls;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ControlsControllerTest {

    @BeforeAll
    public static void initToolkit() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ex) {
            // already started
        }
    }

    private interface ThrowingRunnable { void run() throws Exception; }

    private static void runAndWait(ThrowingRunnable r) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> err = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                r.run();
            } catch (Throwable t) {
                err.set(t);
            } finally {
                latch.countDown();
            }
        });
        if (!latch.await(5, TimeUnit.SECONDS)) throw new AssertionError("Timeout waiting for FX runLater");
        if (err.get() != null) throw new AssertionError(err.get());
    }

    private static void setPrivateField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    public void init_getters_reset_and_hide() throws Exception {
        runAndWait(() -> {
            ControlsController ctrl = new ControlsController();

            // create FXML-like controls
            Button btnLeftDefault = new Button();
            Button btnRightDefault = new Button();
            Button btnSoftDefault = new Button();
            Button btnHardDefault = new Button();
            Button btnRotateDefault = new Button();
            Button btnSwitchDefault = new Button();

            Button btnLeftCurrent = new Button();
            Button btnRightCurrent = new Button();
            Button btnSoftCurrent = new Button();
            Button btnHardCurrent = new Button();
            Button btnRotateCurrent = new Button();
            Button btnSwitchCurrent = new Button();

            Button btnReset = new Button();
            Button btnSave = new Button();
            Button btnCancel = new Button();

            Label lblInfo = new Label();
            Label lblHeader = new Label();

            // inject into controller
            setPrivateField(ctrl, "btnLeftDefault", btnLeftDefault);
            setPrivateField(ctrl, "btnRightDefault", btnRightDefault);
            setPrivateField(ctrl, "btnSoftDefault", btnSoftDefault);
            setPrivateField(ctrl, "btnHardDefault", btnHardDefault);
            setPrivateField(ctrl, "btnRotateDefault", btnRotateDefault);
            setPrivateField(ctrl, "btnSwitchDefault", btnSwitchDefault);

            setPrivateField(ctrl, "btnLeftCurrent", btnLeftCurrent);
            setPrivateField(ctrl, "btnRightCurrent", btnRightCurrent);
            setPrivateField(ctrl, "btnSoftCurrent", btnSoftCurrent);
            setPrivateField(ctrl, "btnHardCurrent", btnHardCurrent);
            setPrivateField(ctrl, "btnRotateCurrent", btnRotateCurrent);
            setPrivateField(ctrl, "btnSwitchCurrent", btnSwitchCurrent);

            setPrivateField(ctrl, "btnReset", btnReset);
            setPrivateField(ctrl, "btnSave", btnSave);
            setPrivateField(ctrl, "btnCancel", btnCancel);

            setPrivateField(ctrl, "lblInfo", lblInfo);
            setPrivateField(ctrl, "lblHeader", lblHeader);

            // now initialize controller (will create a ControlsView internally and wire handlers)
            ctrl.initialize();

            // set known defaults and panel defaults and verify getters and resets
            ctrl.init(KeyCode.A, KeyCode.S, KeyCode.W, KeyCode.DOWN, KeyCode.SPACE, KeyCode.C);
            assertEquals(KeyCode.A, ctrl.getLeft());
            assertEquals(KeyCode.S, ctrl.getRight());
            assertEquals(KeyCode.C, ctrl.getSwitch());

            // set panel defaults then reset to them
            ctrl.setDefaultKeys(KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.C);
            ctrl.resetToPanelDefaults();
            assertEquals(KeyCode.LEFT, ctrl.getLeft());

            // hide action buttons -> fields/labels should be hidden
            ctrl.hideActionButtons();
            assertFalse(btnReset.isVisible());
            assertFalse(lblInfo.isVisible());

            ctrl.setHeaderText("HeaderTest");
            assertEquals("HeaderTest", lblHeader.getText());
        });
    }
}
