package com.comp2042.logic;

import com.comp2042.logic.bricks.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BrickTest {

    private final Brick[] bricks = {
        new IBrick(),
        new JBrick(),
        new LBrick(),
        new OBrick(),
        new SBrick(),
        new TBrick(),
        new ZBrick()
    };

    @Test
    void testGetShapeMatrix() {
        for (Brick brick : bricks) {
            List<int[][]> shapeMatrix = brick.getShapeMatrix();
            assertNotNull(shapeMatrix, "Shape matrix should not be null for " + brick.getClass().getSimpleName());
            assertFalse(shapeMatrix.isEmpty(), "Shape matrix should not be empty for " + brick.getClass().getSimpleName());
            for (int[][] matrix : shapeMatrix) {
                assertNotNull(matrix, "Rotation matrix should not be null for " + brick.getClass().getSimpleName());
            }
        }
    }
}
