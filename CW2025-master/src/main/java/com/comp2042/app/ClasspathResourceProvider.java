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
        // Try direct resource first using the class loader
        URL u = loader.getResource(resourcePath);
        // If not found, try the name without a leading slash
        if (u == null) {
            String withoutLeading = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            u = loader.getResource(withoutLeading);
        }
        // As a last resort, try Class.getResource which understands leading '/'
        if (u == null) {
            try { u = getClass().getResource(resourcePath); } catch (Exception ignored) {}
        }
        return u;
    }
}
