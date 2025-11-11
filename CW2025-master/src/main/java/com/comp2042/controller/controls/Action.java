package com.comp2042.controller.controls;

/**
 * Represents a logical control action for key bindings.
 */
public enum Action {
    LEFT("Left"),
    RIGHT("Right"),
    SOFT_DROP("Soft Drop"),
    HARD_DROP("Hard Drop"),
    ROTATE("Rotate"),
    SWITCH("Switch");

    private final String displayName;

    Action(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
