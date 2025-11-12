package com.tetris.utils;

import com.comp2042.logic.Brick;
import com.comp2042.model.NextShapeInfo;
import com.comp2042.utils.BrickRotator;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BrickRotatorTest {

    static class TestBrick implements Brick {
        private final List<int[][]> shapes;

        TestBrick(List<int[][]> shapes) { this.shapes = shapes; }

        @Override
        public List<int[][]> getShapeMatrix() {
            // return copies to mimic real implementations
            return shapes;
        }
    }

    @Test
    void getCurrentAndNextShape_andWrapping() {
        BrickRotator r = new BrickRotator();
        int[][] s0 = new int[][]{{1}};
        int[][] s1 = new int[][]{{2}};
        java.util.List<int[][]> shapes = new java.util.ArrayList<>();
        shapes.add(s0);
        shapes.add(s1);
        TestBrick b = new TestBrick(shapes);
        r.setBrick(b);

        int[][] current = r.getCurrentShape();
        assertArrayEquals(new int[]{1}, current[0]);

        NextShapeInfo next = r.getNextShape();
        assertEquals(1, next.getPosition());

        // setCurrentShape wraps values
        r.setCurrentShape(5); // 5 % 2 == 1
        int[][] curr2 = r.getCurrentShape();
        assertArrayEquals(new int[]{2}, curr2[0]);

        r.setCurrentShape(-1); // normalized to ( -1 % 2 +2) %2 ==1
        int[][] curr3 = r.getCurrentShape();
        assertArrayEquals(new int[]{2}, curr3[0]);
    }

    @Test
    void defensiveCopyReturnedByGetCurrentShape() {
        BrickRotator r = new BrickRotator();
        int[][] s0 = new int[][]{{9}};
        java.util.List<int[][]> shapes2 = new java.util.ArrayList<>();
        shapes2.add(s0);
        TestBrick b = new TestBrick(shapes2);
        r.setBrick(b);

        int[][] returned = r.getCurrentShape();
        returned[0][0] = 123;
        int[][] returned2 = r.getCurrentShape();
        assertEquals(9, returned2[0][0], "getCurrentShape should return a defensive copy");
    }

    @Test
    void operationsThrowWhenNoBrickSet() {
        BrickRotator r = new BrickRotator();
        assertThrows(IllegalStateException.class, r::getCurrentShape);
        assertThrows(IllegalStateException.class, r::getNextShape);
    }

    @Test
    void emptyShapesListCausesNextAndCurrentToFailAppropriately() {
        BrickRotator r = new BrickRotator();
        TestBrick empty = new TestBrick(Collections.emptyList());
        r.setBrick(empty);
        // getCurrentShape and getNextShape should throw due to 0 shapes
        assertThrows(IllegalStateException.class, r::getCurrentShape);
        assertThrows(IllegalStateException.class, r::getNextShape);
        // setCurrentShape should not throw; it will log and set index to 0
        r.setCurrentShape(5);
    }
}
