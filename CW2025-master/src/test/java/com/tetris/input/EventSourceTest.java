package com.tetris.input;

import org.junit.jupiter.api.Test;

import com.comp2042.input.EventSource;

import static org.junit.jupiter.api.Assertions.*;

class EventSourceTest {

    @Test
    void testIsUser() {
        assertTrue(EventSource.USER.isUser());
        assertFalse(EventSource.THREAD.isUser());
    }

    @Test
    void testIsThread() {
        assertTrue(EventSource.THREAD.isThread());
        assertFalse(EventSource.USER.isThread());
    }

    @Test
    void testFromName() {
        assertEquals(EventSource.USER, EventSource.fromName("USER"));
        assertEquals(EventSource.THREAD, EventSource.fromName("THREAD"));
        assertEquals(EventSource.USER, EventSource.fromName(" user "));
        assertEquals(EventSource.THREAD, EventSource.fromName(" thread "));
        assertNull(EventSource.fromName("INVALID"));
        assertNull(EventSource.fromName(null));
    }

    @Test
    void testEnumValues() {
        assertEquals(2, EventSource.values().length);
        assertEquals(EventSource.USER, EventSource.valueOf("USER"));
        assertEquals(EventSource.THREAD, EventSource.valueOf("THREAD"));
    }
}
