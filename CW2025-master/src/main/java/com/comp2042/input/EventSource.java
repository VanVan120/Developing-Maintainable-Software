package com.comp2042.input;

public enum EventSource {
    USER,
    THREAD;

    public boolean isUser() {
        return this == USER;
    }

    public boolean isThread() {
        return this == THREAD;
    }

    public static EventSource fromName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
