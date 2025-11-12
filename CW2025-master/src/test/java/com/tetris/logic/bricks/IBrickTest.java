package com.tetris.logic.bricks;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.comp2042.logic.bricks.IBrick;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IBrickTest {

    private IBrick brick;

    @BeforeEach
    void setUp() {
        brick = new IBrick();
    }

    @Test
    void testGetShapeMatrix_returnsCorrectNumberOfRotations() {
        assertEquals(2, brick.getShapeMatrix().size());
    }

    @Test
    void testGetShapeMatrix_returnsCorrectShape() {
        List<int[][]> shapeMatrix = brick.getShapeMatrix();
        int[][] rotation1 = {
                {0, 0, 0, 0},
                {1, 1, 1, 1},
                {0, 0, 0, 0},
                {0, 0, 0, 0}
        };
        int[][] rotation2 = {
                {0, 1, 0, 0},
                {0, 1, 0, 0},
                {0, 1, 0, 0},
                {0, 1, 0, 0}
        };
        assertArrayEquals(rotation1, shapeMatrix.get(0));
        assertArrayEquals(rotation2, shapeMatrix.get(1));
    }

    @Test
    void testGetShapeMatrix_returnsDeepCopy() {
        List<int[][]> shapeMatrix1 = brick.getShapeMatrix();
        List<int[][]> shapeMatrix2 = brick.getShapeMatrix();
        assertNotSame(shapeMatrix1, shapeMatrix2);
        assertNotSame(shapeMatrix1.get(0), shapeMatrix2.get(0));
        
        // Modify the copy and check if the original is unchanged
        shapeMatrix1.get(0)[1][1] = 99;
        assertNotEquals(99, brick.getShapeMatrix().get(0)[1][1]);
    }
}
