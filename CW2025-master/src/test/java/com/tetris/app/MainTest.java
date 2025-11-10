package com.tetris.app;

import com.comp2042.app.Main;
import javafx.application.Application;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Lightweight unit tests for {@link Main} that do not start the JavaFX runtime.
 *
 * These tests use reflection to verify the presence of the {@code main(String[])}
 * entrypoint and the {@code start(Stage)} lifecycle method, and that the class
 * extends {@link Application}. They are safe to run on CI without launching
 * the JavaFX toolkit.
 */
public class MainTest {

    @Test
    public void hasMainMethod() throws Exception {
        Method main = Main.class.getMethod("main", String[].class);
        assertNotNull(main, "Main should declare a main(String[]) method");
        assertTrue(Modifier.isStatic(main.getModifiers()), "main should be static");
        assertEquals(void.class, main.getReturnType(), "main should return void");
    }

    @Test
    public void extendsApplicationAndHasStart() throws Exception {
        // Ensure Main extends JavaFX Application class
        assertTrue(Application.class.isAssignableFrom(Main.class), "Main should extend javafx.application.Application");

        // Verify the start(Stage) method is present with the correct signature
        Method start = Main.class.getMethod("start", Stage.class);
        assertNotNull(start, "start(Stage) should be declared");
        assertFalse(Modifier.isStatic(start.getModifiers()), "start should be an instance method");
    }
}
