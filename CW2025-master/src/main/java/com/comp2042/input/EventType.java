package com.comp2042.input;

public enum EventType {
    DOWN,
    LEFT,
    RIGHT,
    ROTATE;

    public boolean isMove() {
        return this == LEFT || this == RIGHT || this == DOWN;
    }

    public boolean isRotation() {
        return this == ROTATE;
    }

    public static EventType fromName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
