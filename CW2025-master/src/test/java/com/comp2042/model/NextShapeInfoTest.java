package com.comp2042.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NextShapeInfoTest {

    @Test
    void gettersAndDefensiveCopy() {
        int[][] shape = new int[][] {{1,0},{0,1}};
        NextShapeInfo n = new NextShapeInfo(shape, 3);

        assertEquals(3, n.getPosition());

        // changing original should not affect stored
        shape[0][0] = 9;
        int[][] ret = n.getShape();
        assertNotEquals(9, ret[0][0]);

        // modifying returned copy should not affect subsequent calls
        ret[0][0] = 7;
        int[][] ret2 = n.getShape();
        assertNotEquals(7, ret2[0][0]);
    }

    @Test
    void equalsHashCodeAndToString() {
        int[][] s1 = new int[][] {{1}};
        int[][] s2 = new int[][] {{1}};
        NextShapeInfo a = new NextShapeInfo(s1, 1);
        NextShapeInfo b = new NextShapeInfo(s2, 1);
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.toString().contains("NextShapeInfo"));
    }
}
