package com.comp2042.controller.cooperateBattle.coopGUI;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;

/**
 * Small utility to manage attaching and detaching the coop key event filters.
 * Keeps handlers centralized so the controller can remain concise.
 */
public class CoopInputHandler {

    private EventHandler<KeyEvent> pressedHandler;
    private EventHandler<KeyEvent> releasedHandler;
    private Scene attachedScene;

    public CoopInputHandler(CoopGuiController controller) {
        this.pressedHandler = e -> controller.onKeyPressed(e);
        this.releasedHandler = e -> controller.onKeyReleased(e);
    }

    public void attachToScene(Scene s) {
        if (s == null) return;
        try {
            detach();
            s.addEventFilter(KeyEvent.KEY_PRESSED, pressedHandler);
            s.addEventFilter(KeyEvent.KEY_RELEASED, releasedHandler);
            attachedScene = s;
        } catch (Exception ignored) {}
    }

    public void detach() {
        try {
            if (attachedScene != null) {
                try { attachedScene.removeEventFilter(KeyEvent.KEY_PRESSED, pressedHandler); } catch (Exception ignored) {}
                try { attachedScene.removeEventFilter(KeyEvent.KEY_RELEASED, releasedHandler); } catch (Exception ignored) {}
                attachedScene = null;
            }
        } catch (Exception ignored) {}
    }
}
