package com.tetris.logic.bricks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp2042.logic.bricks.OBrick;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OBrickTest {

    private OBrick brick;

    @BeforeEach
    void setUp() {
        brick = new OBrick();
    }

    @Test
    void testGetShapeMatrix_returnsCorrectNumberOfRotations() {
        assertEquals(1, brick.getShapeMatrix().size());
    }

    @Test
    void testGetShapeMatrix_returnsCorrectShape() {
        List<int[][]> shapeMatrix = brick.getShapeMatrix();
        int[][] rotation1 = {
                {0, 0, 0, 0},
                {0, 4, 4, 0},
                {0, 4, 4, 0},
                {0, 0, 0, 0}
        };
        assertArrayEquals(rotation1, shapeMatrix.get(0));
    }

    @Test
    void testGetShapeMatrix_returnsDeepCopy() {
        List<int[][]> shapeMatrix1 = brick.getShapeMatrix();
        shapeMatrix1.get(0)[1][1] = 99;
        assertNotEquals(99, brick.getShapeMatrix().get(0)[1][1]);
    }
}
