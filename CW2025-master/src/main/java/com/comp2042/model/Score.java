package com.comp2042.model;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/**
 * Simple score container backed by a JavaFX {@link IntegerProperty} for UI
 * binding.
 *
 * Note: the property is a JavaFX observable and should be updated on the
 * JavaFX Application Thread when possible. Callers that update the score from
 * background threads should use {@code Platform.runLater(...)} to avoid
 * threading issues.
 */
public final class Score {

    private final IntegerProperty score = new SimpleIntegerProperty(0);

    /**
     * @return the JavaFX {@link IntegerProperty} used for UI binding.
     */
    public IntegerProperty scoreProperty() {
        return score;
    }

    /**
     * @return the current score value.
     */
    public int getScore() { return score.get(); }

    /**
     * Add the given amount to the current score.
     *
     * @param i amount to add (may be negative to subtract).
     */
    public void add(int i){
        score.setValue(score.getValue() + i);
    }

    /**
     * Reset the score to zero.
     */
    public void reset() {
        score.setValue(0);
    }

    @Override
    public String toString() {
        return "Score{" + "score=" + score.get() + '}';
    }
}
