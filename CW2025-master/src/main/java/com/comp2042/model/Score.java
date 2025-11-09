package com.comp2042.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Simple score container backed by a JavaFX IntegerProperty for UI binding.
 */
public final class Score {

    private final IntegerProperty score = new SimpleIntegerProperty(0);

    public IntegerProperty scoreProperty() {
        return score;
    }

    /** Return the current score value. */
    public int getScore() { return score.get(); }

    public void add(int i){
        score.setValue(score.getValue() + i);
    }

    public void reset() {
        score.setValue(0);
    }

    @Override
    public String toString() {
        return "Score{" + "score=" + score.get() + '}';
    }
}
