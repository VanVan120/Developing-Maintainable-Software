package com.tetris.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.comp2042.model.ClearRow;
import com.comp2042.model.DownData;
import com.comp2042.model.ViewData;

class DownDataTest {

    @Test
    void gettersReturnValuesProvided() {
        ClearRow cr = new ClearRow(1, new int[][] {{1}}, 50, new int[] {0});
        int[][] brick = new int[][] {{1}};
        int[][] next = new int[][] {{0}};
        ViewData vd = new ViewData(brick, 0, 0, next);
        DownData dd = new DownData(cr, vd);

        assertSame(cr, dd.getClearRow());
        assertSame(vd, dd.getViewData());
    }

    @Test
    void equalsHashCodeAndToString() {
        ClearRow cr1 = new ClearRow(1, new int[][] {{1}}, 50, new int[] {0});
    int[][] b1 = new int[][] {{1}};
    int[][] n1 = new int[][] {{0}};
    ViewData vd1 = new ViewData(b1, 0, 0, n1);
        DownData a = new DownData(cr1, vd1);
        DownData b = new DownData(cr1, vd1);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertTrue(a.toString().contains("DownData"));
    }

    @Test
    void equalsDifferentWhenFieldsDifferent() {
        ClearRow cr1 = new ClearRow(1, new int[][] {{1}}, 50, new int[] {0});
        ClearRow cr2 = new ClearRow(2, new int[][] {{2}}, 100, new int[] {0});
    int[][] b1 = new int[][] {{1}};
    int[][] n1 = new int[][] {{0}};
    int[][] b2 = new int[][] {{2,2},{2,2}};
    int[][] n2 = new int[][] {{0}};
    ViewData vd1 = new ViewData(b1, 0, 0, n1);
    ViewData vd2 = new ViewData(b2, 0, 0, n2);

        DownData d1 = new DownData(cr1, vd1);
        DownData d2 = new DownData(cr2, vd1);
        DownData d3 = new DownData(cr1, vd2);

        assertNotEquals(d1, d2);
        assertNotEquals(d1, d3);
    }
}
