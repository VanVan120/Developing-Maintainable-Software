package com.comp2042.controller.mainMenu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javafx.scene.layout.StackPane;
import javafx.scene.input.KeyCode;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainMenuControlsHelperTest {

    @BeforeAll
    public static void init() throws Exception { JfxInitializer.init(); }

    @Test
    public void show_whenFxmlMissing_noSaveCalled() {
        MainMenuControlsHelper h = new MainMenuControlsHelper();
        StackPane root = new StackPane();
        StackPane controls = new StackPane();
        AtomicBoolean saved = new AtomicBoolean(false);
        h.show(getClass().getClassLoader(),
                KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.Q,
                KeyCode.LEFT, KeyCode.RIGHT, KeyCode.UP, KeyCode.DOWN, KeyCode.SPACE, KeyCode.C,
                root, controls,
                (r) -> saved.set(true),
                (pane, onFinished) -> { if (onFinished != null) onFinished.run(); },
                (pane) -> {}
        );
        // resource likely missing in test classpath; save should not be called
        assertFalse(saved.get());
    }
}
