package com.comp2042.input;

/**
 * Type of input event representing a requested piece action.
 *
 * <p>Used by input dispatch code and listeners to decide how to update game
 * state or the view. Includes helpers to identify move vs rotation events.
 */
public enum EventType {
    /** Soft/hard/repeated down movement. */
    DOWN,

    /** Move the piece left. */
    LEFT,

    /** Move the piece right. */
    RIGHT,

    /** Rotate the piece. */
    ROTATE;

    /**
     * @return {@code true} when the type is a lateral or down movement.
     */
    public boolean isMove() {
        return this == LEFT || this == RIGHT || this == DOWN;
    }

    /**
     * @return {@code true} when the type represents a rotation request.
     */
    public boolean isRotation() {
        return this == ROTATE;
    }

    /**
     * Parse an {@code EventType} from a case-insensitive name. Returns
     * {@code null} for unknown or null inputs.
     */
    public static EventType fromName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
