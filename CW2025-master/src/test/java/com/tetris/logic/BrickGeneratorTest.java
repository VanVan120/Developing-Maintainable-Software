package com.tetris.logic;

import org.junit.jupiter.api.Test;

import com.comp2042.logic.Brick;
import com.comp2042.logic.BrickGenerator;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BrickGeneratorTest {

    // An anonymous implementation of BrickGenerator to test default methods
    private final BrickGenerator brickGenerator = new BrickGenerator() {
        @Override
        public Brick getBrick() {
            return null; // Not needed for testing default methods
        }

        @Override
        public Brick getNextBrick() {
            return null; // Not needed for testing default methods
        }
    };

    @Test
    void testDefaultGetUpcomingBricks() {
        List<Brick> upcoming = brickGenerator.getUpcomingBricks(5);
        assertNotNull(upcoming, "Upcoming bricks list should not be null");
        assertTrue(upcoming.isEmpty(), "Default implementation should return an empty list");
    }

    @Test
    void testDefaultReplaceNext() {
        assertFalse(brickGenerator.replaceNext(new com.comp2042.logic.bricks.IBrick()), "Default implementation should return false");
    }
}
