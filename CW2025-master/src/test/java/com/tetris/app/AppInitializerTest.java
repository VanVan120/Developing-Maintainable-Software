package com.tetris.app;

import com.comp2042.app.AppInitializer;
import com.comp2042.app.ResourceProvider;
import com.comp2042.app.ClasspathResourceProvider;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class AppInitializerTest {

    @Test
    void constructor_nullProvider_usesClasspathResourceProvider() throws Exception {
        AppInitializer init = new AppInitializer(null);

        // access private field via reflection
        Field f = AppInitializer.class.getDeclaredField("resourceProvider");
        f.setAccessible(true);
        Object rp = f.get(init);
        assertNotNull(rp);
        assertTrue(rp instanceof ClasspathResourceProvider, "Default provider should be ClasspathResourceProvider");
    }

    @Test
    void initialize_missingMainFxml_throwsIllegalStateException() {
        // Create a ResourceProvider that returns null for everything
        ResourceProvider missing = new ResourceProvider() {
            @Override
            public java.net.URL getResource(String resourcePath) { return null; }
        };

        AppInitializer init = new AppInitializer(missing);

        // We pass null for the Stage because loadRoot should fail before stage usage
        assertThrows(IllegalStateException.class, () -> init.initialize(null));
    }
}
