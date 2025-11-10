package com.tetris.app;

import com.comp2042.app.ClasspathResourceProvider;
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

        assertNotNull(u1, "Resource should be found without leading slash");
        assertNotNull(u2, "Resource should be found with leading slash");
        assertEquals(u1.toExternalForm(), u2.toExternalForm(), "Both lookups should return the same URL");
    }

    @Test
    void getResource_nonexistent_returnsNull() {
        ClasspathResourceProvider p = new ClasspathResourceProvider();
        assertNull(p.getResource("this/resource/does-not-exist.txt"));
    }
}
