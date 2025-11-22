package com.comp2042.logic;

import com.comp2042.logic.bricks.IBrick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RandomBrickGeneratorTest {

    private RandomBrickGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new RandomBrickGenerator();
    }

    @Test
    void testGetBrick() {
        Brick firstBrick = generator.getBrick();
        assertNotNull(firstBrick, "First brick should not be null");

        Brick secondBrick = generator.getBrick();
        assertNotNull(secondBrick, "Second brick should not be null");

        assertNotSame(firstBrick, secondBrick, "Should get different bricks on subsequent calls");
    }

    @Test
    void testGetNextBrick() {
        Brick nextBrick = generator.getNextBrick();
        assertNotNull(nextBrick, "Next brick should not be null");

        Brick sameNextBrick = generator.getNextBrick();
        assertSame(nextBrick, sameNextBrick, "Should get the same brick if not consumed");
    }

    @Test
    void testGetUpcomingBricks() {
        List<Brick> upcoming = generator.getUpcomingBricks(3);
        assertEquals(3, upcoming.size(), "Should get the requested number of upcoming bricks");
        assertNotNull(upcoming.get(0));
        assertNotNull(upcoming.get(1));
        assertNotNull(upcoming.get(2));

        // Check that getUpcomingBricks doesn't consume the bricks
        Brick nextBrick = generator.getNextBrick();
        assertSame(upcoming.get(0), nextBrick, "First upcoming should be the same as next brick");
    }

    @Test
    void testReplaceNext() {
        Brick originalNext = generator.getNextBrick();
        Brick newBrick = new IBrick();

        assertTrue(generator.replaceNext(newBrick), "Replacing next brick should succeed");

        Brick replacedNext = generator.getNextBrick();
        assertNotSame(originalNext, replacedNext, "Next brick should be different after replacement");
        assertSame(newBrick, replacedNext, "Next brick should be the new brick");
    }

    @Test
    void testReplaceNextWithNull() {
        assertFalse(generator.replaceNext(null), "Replacing with null should fail");
    }

    @Test
    void testBagRefill() {
        // Consume more bricks than one bag contains (7) to test refill
        for (int i = 0; i < 10; i++) {
            assertNotNull(generator.getBrick(), "Brick should not be null, even after bag refill");
        }
    }
}
