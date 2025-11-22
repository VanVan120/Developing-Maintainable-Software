package com.comp2042.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ViewDataTest {

    @Test
    void defensiveCopyBehavior() {
        int[][] brick = new int[][] {{1,2},{3,4}};
        int[][] next = new int[][] {{0}};
        ViewData v = new ViewData(brick, 2, 3, next);

        // mutate originals
        brick[0][0] = 9;
        next[0][0] = 8;

        int[][] b1 = v.getBrickData();
        int[][] n1 = v.getNextBrickData();
        assertNotEquals(9, b1[0][0]);
        assertNotEquals(8, n1[0][0]);

        // modifying returned should not change internal state
        b1[0][0] = 7;
        int[][] b2 = v.getBrickData();
        assertNotEquals(7, b2[0][0]);
    }

    @Test
    void gettersAndEquality() {
        int[][] b = new int[][] {{1}};
        int[][] n = new int[][] {{2}};
        ViewData a = new ViewData(b, 1, 2, n);
        ViewData c = new ViewData(new int[][] {{1}}, 1, 2, new int[][] {{2}});
        assertEquals(a, c);
        assertEquals(a.hashCode(), c.hashCode());
        assertTrue(a.toString().contains("ViewData"));
        assertEquals(1, a.getXPosition());
        assertEquals(2, a.getYPosition());
    }
}
