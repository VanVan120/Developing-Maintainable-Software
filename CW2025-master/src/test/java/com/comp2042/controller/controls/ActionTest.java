package com.comp2042.controller.controls;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ActionTest {

    @Test
    public void displayNames() {
        assertEquals("Left", Action.LEFT.getDisplayName());
        assertEquals("Right", Action.RIGHT.getDisplayName());
        assertEquals("Soft Drop", Action.SOFT_DROP.getDisplayName());
        assertEquals("Hard Drop", Action.HARD_DROP.getDisplayName());
        assertEquals("Rotate", Action.ROTATE.getDisplayName());
        assertEquals("Switch", Action.SWITCH.getDisplayName());
    }
}
