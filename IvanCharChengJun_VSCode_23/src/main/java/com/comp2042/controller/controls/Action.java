package com.comp2042.controller.controls;

/**
 * Enumeration of logical control actions used for key bindings.
 *
 * <p>Each enum constant carries a human-friendly display name used by the
 * UI when presenting the action to users.
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

    /**
     * Return a human-friendly display name for this action.
     *
     * @return display name (never {@code null})
     */
    public String getDisplayName() {
        return displayName;
    }
}
