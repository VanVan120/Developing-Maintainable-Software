package com.tetris.logic.bricks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp2042.logic.bricks.JBrick;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class JBrickTest {

    private JBrick brick;

    @BeforeEach
    void setUp() {
        brick = new JBrick();
    }

    @Test
    void testGetShapeMatrix_returnsCorrectNumberOfRotations() {
        assertEquals(4, brick.getShapeMatrix().size());
    }

    @Test
    void testGetShapeMatrix_returnsCorrectShape() {
        List<int[][]> shapeMatrix = brick.getShapeMatrix();
        int[][] rotation1 = {
                {0, 0, 0, 0},
                {2, 2, 2, 0},
                {0, 0, 2, 0},
                {0, 0, 0, 0}
        };
        int[][] rotation2 = {
                {0, 0, 0, 0},
                {0, 2, 2, 0},
                {0, 2, 0, 0},
                {0, 2, 0, 0}
        };
        int[][] rotation3 = {
                {0, 0, 0, 0},
                {0, 2, 0, 0},
                {0, 2, 2, 2},
                {0, 0, 0, 0}
        };
        int[][] rotation4 = {
                {0, 0, 2, 0},
                {0, 0, 2, 0},
                {0, 2, 2, 0},
                {0, 0, 0, 0}
        };
        assertArrayEquals(rotation1, shapeMatrix.get(0));
        assertArrayEquals(rotation2, shapeMatrix.get(1));
        assertArrayEquals(rotation3, shapeMatrix.get(2));
        assertArrayEquals(rotation4, shapeMatrix.get(3));
    }

    @Test
    void testGetShapeMatrix_returnsDeepCopy() {
        List<int[][]> shapeMatrix1 = brick.getShapeMatrix();
        shapeMatrix1.get(0)[1][0] = 99;
        assertNotEquals(99, brick.getShapeMatrix().get(0)[1][0]);
    }
}
