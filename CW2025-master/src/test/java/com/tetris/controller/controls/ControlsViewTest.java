package com.tetris.controller.controls;

import com.comp2042.controller.controls.ControlsView;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

public class ControlsViewTest {

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

    @Test
    public void setButtonKey_and_info_and_header_and_hide() throws Exception {
        runAndWait(() -> {
            Label lbl = new Label();
            Label header = new Label();
            Button reset = new Button();
            Button save = new Button();
            Button cancel = new Button();
            Button interactive = new Button();

            ControlsView view = new ControlsView(lbl, header, reset, save, cancel);

            view.setButtonKey(interactive, KeyCode.A, true);
            assertEquals("A", interactive.getText());
            assertFalse(interactive.isDisabled());

            view.setInfoText("hello");
            assertEquals("hello", lbl.getText());

            view.setHeaderText("HeaderX");
            assertEquals("HeaderX", header.getText());

            view.hideActionButtons();
            assertFalse(reset.isVisible());
            assertFalse(save.isVisible());
            assertFalse(cancel.isVisible());
            assertFalse(lbl.isVisible());
        });
    }

    @Test
    public void addHoverEffect_adds_and_removes_style() throws Exception {
        runAndWait(() -> {
            Button b = new Button();
            ControlsView view = new ControlsView(new Label(), new Label(), new Button(), new Button(), new Button());
            view.addHoverEffect(b);
            // simulate enter
            if (b.getOnMouseEntered() != null) b.getOnMouseEntered().handle(null);
            assertTrue(b.getStyleClass().contains("keybind-hover"));
            // simulate exit
            if (b.getOnMouseExited() != null) b.getOnMouseExited().handle(null);
            assertFalse(b.getStyleClass().contains("keybind-hover"));
        });
    }
}
