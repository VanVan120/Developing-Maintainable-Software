package com.comp2042.logic;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.comp2042.logic.bricks.IBrick;
import com.comp2042.logic.bricks.JBrick;
import com.comp2042.logic.bricks.LBrick;
import com.comp2042.logic.bricks.OBrick;
import com.comp2042.logic.bricks.SBrick;
import com.comp2042.logic.bricks.TBrick;
import com.comp2042.logic.bricks.ZBrick;

/**
 * A simple random (bag) brick generator.
 *
 * <p>Maintains an internal buffer of upcoming bricks and exposes a peek API so
 * GUI code can render previews. Uses a shuffled bag approach for fairness.
 */
public class RandomBrickGenerator implements BrickGenerator {

    private final List<Brick> brickList;

    private final Deque<Brick> nextBricks = new ArrayDeque<>();
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
        refillBagIfNeeded();
    }

    private void refillBagIfNeeded() {
        // keep adding full shuffled bags until we reach at least BUFFER_SIZE
        while (nextBricks.size() < BUFFER_SIZE) {
            List<Brick> bag = new ArrayList<>(brickList);
            java.util.Collections.shuffle(bag, ThreadLocalRandom.current());
            for (Brick b : bag) nextBricks.add(b);
        }
    }

    @Override
    public Brick getBrick() {
        // ensure buffer before returning the head so previews remain stable
        refillBagIfNeeded();
        return nextBricks.poll();
    }

    @Override
    public Brick getNextBrick() {
        return nextBricks.peek();
    }

    @Override
    public java.util.List<Brick> getUpcomingBricks(int count) {
        // ensure the internal buffer has at least `count` elements so the returned list reflects the actual queue
        while (nextBricks.size() < Math.max(count, BUFFER_SIZE)) refillBagIfNeeded();
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
