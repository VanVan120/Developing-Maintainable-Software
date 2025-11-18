package com.comp2042.controller.mainMenu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;

public class MainMenuMediaHelperTest {

    @BeforeAll
    public static void init() throws Exception { JfxInitializer.init(); }

    @Test
    public void initMenuMedia_missingResource_disablesContainer() {
        MainMenuMediaHelper h = new MainMenuMediaHelper();
        MediaView mv = new MediaView();
        StackPane container = new StackPane();
        container.setVisible(true);
        // force helper to behave as if resources are missing in this test
        System.setProperty("com.comp2042.test.noMenuMedia", "true");
        try {
            h.initMenuMedia(mv, container);
        } finally {
            System.clearProperty("com.comp2042.test.noMenuMedia");
        }
        // when resources are not present, helper will hide the container
        assertFalse(container.isVisible());
    }
}
