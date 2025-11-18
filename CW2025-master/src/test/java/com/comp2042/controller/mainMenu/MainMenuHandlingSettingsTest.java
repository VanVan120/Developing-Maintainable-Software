package com.comp2042.controller.mainMenu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javafx.scene.layout.StackPane;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainMenuHandlingSettingsTest {

    @BeforeAll
    public static void init() throws Exception { JfxInitializer.init(); }

    @Test
    public void showHandlingControls_whenFxmlMissing_noSaveCalled() {
        MainMenuHandlingSettings h = new MainMenuHandlingSettings();
        StackPane root = new StackPane();
        StackPane settings = new StackPane();
        AtomicBoolean saved = new AtomicBoolean(false);
        h.showHandlingControls(getClass().getClassLoader(), 10, 20, 30, 1.0, true, root, settings, (result) -> saved.set(true), (p, r) -> { if (r != null) r.run(); }, (p) -> {});
        assertFalse(saved.get());
    }
}
