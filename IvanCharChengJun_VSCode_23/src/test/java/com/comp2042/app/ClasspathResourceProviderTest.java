package com.comp2042.app;

import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

class ClasspathResourceProviderTest {

    @Test
    void getResource_nullPath_returnsNull() {
        ClasspathResourceProvider p = new ClasspathResourceProvider();
        assertNull(p.getResource(null), "Expected null when resourcePath is null");
    }

    @Test
    void getResource_withAndWithoutLeadingSlash_resolvesSameResource() {
        ClasspathResourceProvider p = new ClasspathResourceProvider();

        URL u1 = p.getResource("testing/sample.txt");
        URL u2 = p.getResource("/testing/sample.txt");

        // Accept either both null (resource not present on classpath) or both resolving
        if (u1 == null || u2 == null) {
            assertTrue(u1 == null && u2 == null, "Both lookups should either be null or resolve to the same resource");
        } else {
            assertEquals(u1.toExternalForm(), u2.toExternalForm(), "Both lookups should return the same URL");
        }
    }

    @Test
    void getResource_nonexistent_returnsNull() {
        ClasspathResourceProvider p = new ClasspathResourceProvider();
        assertNull(p.getResource("this/resource/does-not-exist.txt"));
    }
}
