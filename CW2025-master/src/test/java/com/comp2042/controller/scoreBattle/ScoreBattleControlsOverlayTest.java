package com.comp2042.controller.scoreBattle;

import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ScoreBattleControlsOverlayTest {

    @BeforeAll
    public static void initToolkit() {
        new JFXPanel();
    }

    @Test
    public void show_addsOverlayToSceneRoot() throws Exception {
        Pane root = new Pane();
        Scene scene = new Scene(root, 800, 600);

        // use real GuiController instances (safe for basic API usage)
        com.comp2042.controller.guiControl.GuiController left = new com.comp2042.controller.guiControl.GuiController();
        com.comp2042.controller.guiControl.GuiController right = new com.comp2042.controller.guiControl.GuiController();

        ScoreBattleControlsOverlay overlay = new ScoreBattleControlsOverlay(scene, left, right);

        // call show() and ensure it does not throw and eventually registers nodes
        overlay.show(left);

        // wait for up to 2s for the FX thread to add the overlay
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline && root.getChildren().isEmpty()) {
            Thread.sleep(50);
        }

        // root should have at least one child (the overlay) after show
        assertFalse(root.getChildren().isEmpty(), "Overlay should add nodes to scene root");
    }
}
