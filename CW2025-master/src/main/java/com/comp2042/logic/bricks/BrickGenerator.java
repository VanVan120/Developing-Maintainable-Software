package com.comp2042.logic.bricks;

public interface BrickGenerator {

    Brick getBrick();

    Brick getNextBrick();

    /**
     * Optional: peek upcoming bricks without consuming. Default implementation returns empty list.
     */
    default java.util.List<Brick> getUpcomingBricks(int count) {
        return java.util.Collections.emptyList();
    }
}
