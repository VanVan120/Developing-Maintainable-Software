package com.tetris.input;

import com.comp2042.input.InputEventListener;
import com.comp2042.input.MoveEvent;
import com.comp2042.model.DownData;
import com.comp2042.model.ViewData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class InputEventListenerTest {

    private final InputEventListener listener = new InputEventListener() {
        @Override
        public DownData onDownEvent(MoveEvent event) {
            return null;
        }

        @Override
        public ViewData onLeftEvent(MoveEvent event) {
            return null;
        }

        @Override
        public ViewData onRightEvent(MoveEvent event) {
            return null;
        }

        @Override
        public ViewData onRotateEvent(MoveEvent event) {
            return null;
        }

        @Override
        public void createNewGame() {
            // Do nothing
        }
    };

    @Test
    void onSwapEvent_defaultMethod_doesNotThrow() {
        assertDoesNotThrow(() -> listener.onSwapEvent());
    }
}
