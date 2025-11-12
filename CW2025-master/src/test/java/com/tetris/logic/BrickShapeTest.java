package com.tetris.logic;

import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

import com.comp2042.logic.BrickShape;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BrickShapeTest {

    @Test
    void testGetColor() {
        assertEquals(Color.CYAN, BrickShape.I.getColor());
        assertEquals(Color.BLUE, BrickShape.J.getColor());
        assertEquals(Color.ORANGE, BrickShape.L.getColor());
        assertEquals(Color.YELLOW, BrickShape.O.getColor());
        assertEquals(Color.GREEN, BrickShape.S.getColor());
        assertEquals(Color.PURPLE, BrickShape.T.getColor());
        assertEquals(Color.RED, BrickShape.Z.getColor());
    }
}
