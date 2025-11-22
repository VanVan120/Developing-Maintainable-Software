package com.comp2042.app;

import java.net.URL;

/**
 * Default {@link ResourceProvider} that resolves resources from the
 * application's classpath using the current class loader.
 *
 * <p>This implementation attempts to be tolerant of common resource path
 * formats: it first tries the provided name as-is using the class loader,
 * then retries without a leading slash, and finally falls back to
 * {@code Class.getResource} which understands leading slashes relative to
 * the classpath root.
 */
public class ClasspathResourceProvider implements ResourceProvider {
    private final ClassLoader loader;

    /**
     * Create a provider that uses this class's class loader to resolve
     * resources from the runtime classpath.
     */
    public ClasspathResourceProvider() {
        this.loader = getClass().getClassLoader();
    }

    /**
     * Resolve a resource path to a {@link URL} using multiple strategies.
     *
     * <ol>
     *   <li>Try {@link ClassLoader#getResource(String)} with the provided name.</li>
     *   <li>If not found, remove a leading '/' and retry.</li>
     *   <li>As a last resort, call {@link Class#getResource(String)} on this
     *       class which accepts names with a leading '/'.</li>
     * </ol>
     *
     * @param resourcePath the classpath resource path to resolve, or {@code null}
     *                     to indicate no resource
     * @return a {@link URL} to the resource, or {@code null} if not found
     */
    @Override
    public URL getResource(String resourcePath) {
        if (resourcePath == null) return null;
        URL u = loader.getResource(resourcePath);
        if (u == null) {
            String withoutLeading = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
            u = loader.getResource(withoutLeading);
        }
        if (u == null) {
            try { u = getClass().getResource(resourcePath); } catch (Exception ignored) {}
        }
        return u;
    }
}
