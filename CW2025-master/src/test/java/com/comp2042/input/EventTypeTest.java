package com.comp2042.input;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventTypeTest {

    @Test
    void testIsMove() {
        assertTrue(EventType.DOWN.isMove());
        assertTrue(EventType.LEFT.isMove());
        assertTrue(EventType.RIGHT.isMove());
        assertFalse(EventType.ROTATE.isMove());
    }

    @Test
    void testIsRotation() {
        assertTrue(EventType.ROTATE.isRotation());
        assertFalse(EventType.DOWN.isRotation());
        assertFalse(EventType.LEFT.isRotation());
        assertFalse(EventType.RIGHT.isRotation());
    }

    @Test
    void testFromName() {
        assertEquals(EventType.DOWN, EventType.fromName("DOWN"));
        assertEquals(EventType.LEFT, EventType.fromName("LEFT"));
        assertEquals(EventType.RIGHT, EventType.fromName("RIGHT"));
        assertEquals(EventType.ROTATE, EventType.fromName("ROTATE"));
        assertEquals(EventType.DOWN, EventType.fromName(" down "));
        assertNull(EventType.fromName("INVALID"));
        assertNull(EventType.fromName(null));
    }

    @Test
    void testEnumValues() {
        assertEquals(4, EventType.values().length);
        assertEquals(EventType.DOWN, EventType.valueOf("DOWN"));
        assertEquals(EventType.LEFT, EventType.valueOf("LEFT"));
        assertEquals(EventType.RIGHT, EventType.valueOf("RIGHT"));
        assertEquals(EventType.ROTATE, EventType.valueOf("ROTATE"));
    }
}
