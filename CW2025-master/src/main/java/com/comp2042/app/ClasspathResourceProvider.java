package com.comp2042.app;

import java.net.URL;

/**
 * Default ResourceProvider implementation that resolves resources from the
 * application's classpath using the current class loader.
 */
public class ClasspathResourceProvider implements ResourceProvider {
    private final ClassLoader loader;

    public ClasspathResourceProvider() {
        this.loader = getClass().getClassLoader();
    }

    @Override
    public URL getResource(String resourcePath) {
        if (resourcePath == null) return null;
        // Try direct resource first, then without leading slash
        URL u = loader.getResource(resourcePath);
        if (u == null && resourcePath.startsWith("/")) {
            u = loader.getResource(resourcePath.substring(1));
        }
        return u;
    }
}
