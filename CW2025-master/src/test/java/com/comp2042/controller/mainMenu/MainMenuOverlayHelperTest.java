package com.comp2042.controller.mainMenu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class MainMenuOverlayHelperTest {

    @BeforeAll
    public static void init() throws Exception { JfxInitializer.init(); }

    @Test
    public void transition_show_close_noExceptions() throws Exception {
        VBox v = new VBox();
        Text t = new Text("Title");
        MainMenuOverlayHelper helper = new MainMenuOverlayHelper(v, t);

        StackPane overlay = new StackPane();
        overlay.setVisible(false);
        helper.transitionTo(overlay);
        // transitionTo should make overlay visible
        assertTrue(overlay.isVisible());

        helper.transitionFrom(overlay);
        helper.closeOverlayWithAnimation(overlay, () -> {});
        helper.showOverlay(overlay);
        assertTrue(overlay.isVisible());
    }
}
