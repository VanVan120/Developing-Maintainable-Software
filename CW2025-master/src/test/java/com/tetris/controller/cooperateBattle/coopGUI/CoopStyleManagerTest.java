package com.tetris.controller.cooperateBattle.coopGUI;

import org.junit.jupiter.api.*;

import com.comp2042.controller.cooperateBattle.coopGUI.CoopStyleManager;

import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.layout.Region;

/**
 * Tests for CoopStyleManager helpers.
 */
public class CoopStyleManagerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
    }

    @Test
    void addAndRemoveStyleClass_onNode() {
        Region r = new Region();
        CoopStyleManager.addStyleClass(r, "foo-bar");
        assertTrue(r.getStyleClass().contains("foo-bar"));
        CoopStyleManager.removeStyleClass(r, "foo-bar");
        assertFalse(r.getStyleClass().contains("foo-bar"));
    }

    @Test
    void ensureStylesheet_nullScene_doesNotThrow() {
        assertDoesNotThrow(() -> CoopStyleManager.ensureStylesheet(null));
    }
}
