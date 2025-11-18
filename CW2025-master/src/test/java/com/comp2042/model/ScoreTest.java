package com.comp2042.model;

import static org.junit.jupiter.api.Assertions.*;

import javafx.beans.property.IntegerProperty;
import org.junit.jupiter.api.Test;

class ScoreTest {

    @Test
    void addAndResetAndGet() {
        Score s = new Score();
        assertEquals(0, s.getScore());
        s.add(10);
        assertEquals(10, s.getScore());
        s.add(-3);
        assertEquals(7, s.getScore());
        s.reset();
        assertEquals(0, s.getScore());
    }

    @Test
    void propertyExposesValue() {
        Score s = new Score();
        IntegerProperty p = s.scoreProperty();
        assertEquals(s.getScore(), p.get());
        s.add(5);
        assertEquals(p.get(), s.getScore());
    }
}
