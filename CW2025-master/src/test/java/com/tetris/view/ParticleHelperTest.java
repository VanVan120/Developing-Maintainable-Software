package com.tetris.view;

import com.comp2042.model.ClearRow;
import com.comp2042.model.ViewData;
import com.comp2042.view.ParticleHelper;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.function.BooleanSupplier;

import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ParticleHelperTest {

    @BeforeAll
    public static void initToolkit() {
        // Ensure JavaFX toolkit is initialised for tests that use scene graph operations
        new JFXPanel();
    }

    private static boolean waitForCondition(long timeoutMs, BooleanSupplier condition) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            try { Thread.sleep(25); } catch (InterruptedException ignored) {}
            if (condition.getAsBoolean()) return true;
        }
        return false;
    }

    @Test
    public void testFlashRowAtAddsAndRemoves() throws Exception {
        Pane p = new Pane();

        Platform.runLater(() -> ParticleHelper.flashRowAt(p, 10, 12, 30, 14));

        // wait for node to be added
        assertTrue(waitForCondition(500, () -> !p.getChildren().isEmpty()), "flash node should be added");

        // the fade-out sequence completes ~300ms after start; give some slack
        assertTrue(waitForCondition(3000, () -> p.getChildren().isEmpty()), "flash node should be removed after animation");
    }

    @Test
    public void testFlashRowAddsAndRemoves() throws Exception {
        Pane p = new Pane();

        Platform.runLater(() -> ParticleHelper.flashRow(p, 5, 6, 40, 18));

        assertTrue(waitForCondition(500, () -> !p.getChildren().isEmpty()), "flash row node should be added");
        assertTrue(waitForCondition(3000, () -> p.getChildren().isEmpty()), "flash row node should be removed after animation");
    }

    @Test
    public void testSpawnParticlesAtAddsAndRemoves() throws Exception {
        Pane p = new Pane();

        Platform.runLater(() -> ParticleHelper.spawnParticlesAt(p, 50, 50, new int[][]{{1}}));

        // particles should be added fairly quickly
        assertTrue(waitForCondition(500, () -> !p.getChildren().isEmpty()), "particles should be added");

        // particles life is ~600ms; wait up to 3s for cleanup
        assertTrue(waitForCondition(4000, () -> p.getChildren().isEmpty()), "particles should be removed after animation");
    }

    @Test
    public void testSpawnRowClearParticlesAddsAndRemoves() throws Exception {
        Pane particlePane = new Pane();

        // build a small display matrix with a filled rectangle at row 0
        int rows = 4, cols = 4;
        Rectangle[][] display = new Rectangle[rows][cols];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Rectangle rect = new Rectangle(10, 10);
                rect.setFill((r == 0) ? Color.RED : Color.TRANSPARENT);
                display[r][c] = rect;
            }
        }

        ClearRow cr = new ClearRow(1, new int[rows][cols], 0, new int[]{0});

        Platform.runLater(() -> ParticleHelper.spawnRowClearParticles(particlePane, cr, display, null, 0.0, 0.0, 10.0, 10.0, null));

        assertTrue(waitForCondition(1000, () -> !particlePane.getChildren().isEmpty()), "row-clear particles should be added");
        assertTrue(waitForCondition(4000, () -> particlePane.getChildren().isEmpty()), "row-clear particles should be removed after animation");
    }

    @Test
    public void testPlayLockEffectAddsAndRemoves() throws Exception {
        Pane particlePane = new Pane();
        Pane brickPanel = new Pane();
        Pane root = new Pane();
        root.getChildren().addAll(brickPanel, particlePane);

        // attach to a scene so coordinate conversions succeed (create scene on FX thread)
        Platform.runLater(() -> new Scene(root, 400, 400));

        ViewData start = new ViewData(new int[][]{{1}}, 1, 3, new int[][]{{0}});
        ViewData end = new ViewData(new int[][]{{1}}, 1, 6, new int[][]{{0}});

        Platform.runLater(() -> ParticleHelper.playLockEffect(particlePane, start, end, false, brickPanel, null, 20.0, 20.0));

        // lock effect creates rectangles which are then animated & removed
        assertTrue(waitForCondition(1000, () -> !particlePane.getChildren().isEmpty()), "lock effect should add rectangles");
        // transitions may take up to ~1s; give extra slack for cleanup
        assertTrue(waitForCondition(4000, () -> particlePane.getChildren().isEmpty()), "lock effect rectangles should be removed after animation");
    }
}
