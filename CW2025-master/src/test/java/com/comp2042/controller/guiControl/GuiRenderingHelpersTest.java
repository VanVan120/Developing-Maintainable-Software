package com.comp2042.controller.guiControl;

import com.comp2042.model.ViewData;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GuiRenderingHelpersTest {
    @Test
    void doRefreshAndSetRectangleData() {
        GuiController c = new GuiController(){};
        // prepare matrices and rectangles sized to match the test board (5 rows x 2 cols)
        int rows = 5;
        int cols = 2;
        c.displayMatrix = new Rectangle[rows][cols];
        c.rectangles = new Rectangle[rows][cols];
        c.ghostRectangles = new Rectangle[rows][cols];
        // ensure panels exist so helper methods do not dereference null
        c.brickPanel = new javafx.scene.layout.Pane();
        c.ghostPanel = new javafx.scene.layout.Pane();
        c.gamePanel = new javafx.scene.layout.GridPane();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                c.displayMatrix[i][j] = new Rectangle();
                c.rectangles[i][j] = new Rectangle();
                c.ghostRectangles[i][j] = new Rectangle();
            }
        }

        int[][] shape = new int[][]{{1,0},{1,1}};
        ViewData v = new ViewData(shape, 0, 2, new int[][]{{0}});
        // call helpers — should not throw
        GuiRenderingHelpers.doRefreshBrick(c, v);
        int[][] board = new int[rows][cols];
        GuiRenderingHelpers.updateGhost(c, v, board);
        GuiRenderingHelpers.refreshGameBackground(c, board);
        GuiRenderingHelpers.setRectangleData(c, 1, new Rectangle());
        assertNotNull(GuiRenderingHelpers.boardToPixel(c, 0, 0));
    }
}
