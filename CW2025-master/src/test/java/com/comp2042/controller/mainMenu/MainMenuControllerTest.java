package com.comp2042.controller.mainMenu;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MainMenuControllerTest {

    @BeforeAll
    public static void init() throws Exception { JfxInitializer.init(); }

    @Test
    public void initialize_and_cleanup_noExceptions() {
        MainMenuController c = new MainMenuController();
        // fields are null; initialize should be resilient and not throw
        c.initialize();
        c.cleanup();
        assertTrue(true);
    }
}
