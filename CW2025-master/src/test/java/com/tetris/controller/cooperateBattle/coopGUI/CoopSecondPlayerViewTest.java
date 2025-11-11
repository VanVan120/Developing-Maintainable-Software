package com.tetris.controller.cooperateBattle.coopGUI;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import javafx.application.Platform;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Rectangle;

import com.comp2042.controller.cooperateBattle.coopGUI.CoopSecondPlayerView;
import com.comp2042.model.ViewData;

/**
 * Tests for CoopSecondPlayerView: building rectangles and simple refresh logic.
 */
public class CoopSecondPlayerViewTest {

    @BeforeAll
    static void initToolkit() {
        try { Platform.startup(() -> {}); } catch (IllegalStateException ignored) {}
    }

    @Test
    void build_createsRectangles_forNonEmptyViewData() {
        Pane brickPanel = new Pane();
        Pane ghostPanel = new Pane();
        CoopSecondPlayerView view = new CoopSecondPlayerView(brickPanel, ghostPanel);

        // create minimal ViewData with a 2x2 shape
        int[][] shape = new int[][] { {1, 0}, {1, 1} };
        ViewData vd = new ViewData(shape, 0, 2, new int[][]{{0}});

        view.build(vd, 12.0, 12.0, 1.0, 1.0);

        // expect rectangles added to panels (some number > 0)
        assertTrue(brickPanel.getChildren().stream().anyMatch(n -> n instanceof Rectangle), "brickPanel should contain rectangles after build");
        assertTrue(ghostPanel.getChildren().stream().anyMatch(n -> n instanceof Rectangle), "ghostPanel should contain ghost rectangles after build");
    }

    @Test
    void refresh_positionsAndVisibility_doNotThrow() {
        Pane brickPanel = new Pane();
        Pane ghostPanel = new Pane();
        CoopSecondPlayerView view = new CoopSecondPlayerView(brickPanel, ghostPanel);

        int[][] shape = new int[][] { {1, 0}, {1, 1} };
        ViewData vd = new ViewData(shape, 1, 3, new int[][]{{0}});

        // first build to create rectangles
        view.build(vd, 10.0, 10.0, 1.0, 1.0);

        // create a small board matrix with empty cells
        int[][] board = new int[20][10];

        // call refresh (boardView null) — ensure no exceptions and panels translated
        assertDoesNotThrow(() -> view.refresh(vd, board, null, vd, 10.0, 10.0, 0.0, 0.0));
        assertNotNull(view.getSecondBrickPanel());
        assertNotNull(view.getSecondGhostPanel());
    }
}
