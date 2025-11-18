package com.comp2042.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MoveEventTest {

    @Test
    void testConstructorAndGetters() {
        MoveEvent event = new MoveEvent(EventType.DOWN, EventSource.USER);
        assertEquals(EventType.DOWN, event.getEventType());
        assertEquals(EventSource.USER, event.getEventSource());
    }

    @Test
    void testOfFactoryMethod() {
        MoveEvent event = MoveEvent.of(EventType.LEFT, EventSource.THREAD);
        assertEquals(EventType.LEFT, event.getEventType());
        assertEquals(EventSource.THREAD, event.getEventSource());
    }

    @Test
    void testIsFromUser() {
        MoveEvent userEvent = new MoveEvent(EventType.RIGHT, EventSource.USER);
        assertTrue(userEvent.isFromUser());

        MoveEvent threadEvent = new MoveEvent(EventType.RIGHT, EventSource.THREAD);
        assertFalse(threadEvent.isFromUser());
    }

    @Test
    void testIsFromThread() {
        MoveEvent threadEvent = new MoveEvent(EventType.ROTATE, EventSource.THREAD);
        assertTrue(threadEvent.isFromThread());

        MoveEvent userEvent = new MoveEvent(EventType.ROTATE, EventSource.USER);
        assertFalse(userEvent.isFromThread());
    }

    @Test
    void testEquals() {
        MoveEvent event1 = new MoveEvent(EventType.DOWN, EventSource.USER);
        MoveEvent event2 = new MoveEvent(EventType.DOWN, EventSource.USER);
        MoveEvent event3 = new MoveEvent(EventType.LEFT, EventSource.USER);
        MoveEvent event4 = new MoveEvent(EventType.DOWN, EventSource.THREAD);

        assertEquals(event1, event2);
        assertNotEquals(event1, event3);
        assertNotEquals(event1, event4);
        assertNotEquals(event1, null);
        assertNotEquals(event1, new Object());
    }

    @Test
    void testHashCode() {
        MoveEvent event1 = new MoveEvent(EventType.DOWN, EventSource.USER);
        MoveEvent event2 = new MoveEvent(EventType.DOWN, EventSource.USER);
        assertEquals(event1.hashCode(), event2.hashCode());

        MoveEvent event3 = new MoveEvent(EventType.LEFT, EventSource.USER);
        assertNotEquals(event1.hashCode(), event3.hashCode());
    }

    @Test
    void testToString() {
        MoveEvent event = new MoveEvent(EventType.DOWN, EventSource.USER);
        String expected = "MoveEvent{eventType=DOWN, eventSource=USER}";
        assertEquals(expected, event.toString());
    }
}
