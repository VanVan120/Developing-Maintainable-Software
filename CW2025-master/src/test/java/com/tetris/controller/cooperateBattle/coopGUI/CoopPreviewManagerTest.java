package com.tetris.controller.cooperateBattle.coopGUI;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

import java.util.ArrayList;

import com.comp2042.logic.Brick;
import com.comp2042.controller.cooperateBattle.coopController.CoopGameController;
import com.comp2042.controller.cooperateBattle.coopGUI.CoopGuiController;
import com.comp2042.controller.cooperateBattle.coopGUI.CoopPreviewManager;

/**
 * Tests for CoopPreviewManager that avoid external libs by using small stubs.
 */
public class CoopPreviewManagerTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
    }

    @Test
    void refreshPreviews_updatesLeftAndRightContainers() throws Exception {
        // create a controller stub that returns a VBox with a Text child for any preview list
        CoopGuiController controller = new CoopGuiController() {
            @Override
            public javafx.scene.layout.VBox buildNextPreview(java.util.List<Brick> bricks) {
                javafx.scene.layout.VBox v = new javafx.scene.layout.VBox();
                v.getChildren().add(new Text("preview"));
                return v;
            }
        };

        // create a CoopGameController stub providing upcoming bricks lists
        CoopGameController coop = new CoopGameController(10, 20) {
            @Override public java.util.List<Brick> getUpcomingLeft(int count) { return new ArrayList<>(); }
            @Override public java.util.List<Brick> getUpcomingRight(int count) { return new ArrayList<>(); }
        };

        VBox leftBox = new VBox();
        Pane nextContent = new Pane();

        CoopPreviewManager mgr = new CoopPreviewManager(controller, coop, leftBox, nextContent);

        // call refresh
        mgr.refreshPreviews();

        // leftBox should have at least the Text child created by buildNextPreview
        boolean leftHasPreview = leftBox.getChildren().stream().anyMatch(n -> n instanceof Text && "preview".equals(((Text)n).getText()));
        assertTrue(leftHasPreview, "leftBox should contain preview Text after refresh");

        // nextContent should also contain the preview content added
        boolean rightHasPreview = nextContent.getChildren().stream().anyMatch(n -> n instanceof Text && "preview".equals(((Text)n).getText()));
        assertTrue(rightHasPreview, "nextContent should contain preview Text after refresh");
    }
}
