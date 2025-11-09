package com.comp2042.input;

public final class MoveEvent {
    private final EventType eventType;
    private final EventSource eventSource;

    public MoveEvent(EventType eventType, EventSource eventSource) {
        this.eventType = eventType;
        this.eventSource = eventSource;
    }

    public static MoveEvent of(EventType type, EventSource source) {
        return new MoveEvent(type, source);
    }

    public EventType getEventType() {
        return eventType;
    }

    public EventSource getEventSource() {
        return eventSource;
    }

    public boolean isFromUser() {
        return eventSource != null && eventSource.isUser();
    }

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
