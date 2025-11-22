package com.comp2042.input;

/**
 * Immutable value object describing a requested input action.
 *
 * <p>Contains the {@link EventType} and the {@link EventSource}. This class
 * is safe to use as a map key (implements {@code equals}/{@code hashCode}).
 */
public final class MoveEvent {
    private final EventType eventType;
    private final EventSource eventSource;

    /**
     * Create a new MoveEvent.
     *
     * @param eventType type of the event (may be null in some code paths)
     * @param eventSource origin of the event (may be null)
     */
    public MoveEvent(EventType eventType, EventSource eventSource) {
        this.eventType = eventType;
        this.eventSource = eventSource;
    }

    /**
     * Convenience factory.
     */
    public static MoveEvent of(EventType type, EventSource source) {
        return new MoveEvent(type, source);
    }

    /** @return the event type (may be {@code null}). */
    public EventType getEventType() {
        return eventType;
    }

    /** @return the event source (may be {@code null}). */
    public EventSource getEventSource() {
        return eventSource;
    }

    /** @return {@code true} when the event originated from a user interaction. */
    public boolean isFromUser() {
        return eventSource != null && eventSource.isUser();
    }

    /** @return {@code true} when the event originated from a background thread. */
    public boolean isFromThread() {
        return eventSource != null && eventSource.isThread();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MoveEvent moveEvent = (MoveEvent) o;
        return eventType == moveEvent.eventType && eventSource == moveEvent.eventSource;
    }

    @Override
    public int hashCode() {
        int result = eventType != null ? eventType.hashCode() : 0;
        result = 31 * result + (eventSource != null ? eventSource.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MoveEvent{" +
                "eventType=" + eventType +
                ", eventSource=" + eventSource +
                '}';
    }
}
