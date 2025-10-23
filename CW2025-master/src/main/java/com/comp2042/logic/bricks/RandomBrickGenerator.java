package com.comp2042.logic.bricks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class RandomBrickGenerator implements BrickGenerator {

    private final List<Brick> brickList;

    private final Deque<Brick> nextBricks = new ArrayDeque<>();
    // maintain a small buffer of upcoming bricks to stabilize preview behaviour
    private static final int BUFFER_SIZE = 4;

    public RandomBrickGenerator() {
        brickList = new ArrayList<>();
        brickList.add(new IBrick());
        brickList.add(new JBrick());
        brickList.add(new LBrick());
        brickList.add(new OBrick());
        brickList.add(new SBrick());
        brickList.add(new TBrick());
        brickList.add(new ZBrick());
        // seed the deque with a small buffer
        while (nextBricks.size() < BUFFER_SIZE) {
            nextBricks.add(brickList.get(ThreadLocalRandom.current().nextInt(brickList.size())));
        }
    }

    @Override
    public Brick getBrick() {
        // ensure buffer before returning the head so previews remain stable
        while (nextBricks.size() < BUFFER_SIZE) {
            nextBricks.add(brickList.get(ThreadLocalRandom.current().nextInt(brickList.size())));
        }
        return nextBricks.poll();
    }

    @Override
    public Brick getNextBrick() {
        return nextBricks.peek();
    }

    @Override
    public java.util.List<Brick> getUpcomingBricks(int count) {
        // ensure the internal buffer has at least `count` elements so the returned list reflects the actual queue
        while (nextBricks.size() < Math.max(count, BUFFER_SIZE)) {
            nextBricks.add(brickList.get(ThreadLocalRandom.current().nextInt(brickList.size())));
        }
        java.util.List<Brick> out = new java.util.ArrayList<>();
        int i = 0;
        for (Brick b : nextBricks) {
            if (i++ >= count) break;
            out.add(b);
        }
        return out;
    }

    @Override
    public boolean replaceNext(Brick replacement) {
        if (replacement == null) return false;
        if (nextBricks.isEmpty()) return false;
        // Replace the head while preserving order: poll head and add replacement at front
        nextBricks.poll();
        // Put replacement at head by creating a new deque with replacement first
        Deque<Brick> newDeque = new ArrayDeque<>();
        newDeque.add(replacement);
        for (Brick b : nextBricks) newDeque.add(b);
        nextBricks.clear();
        nextBricks.addAll(newDeque);
        return true;
    }
}
