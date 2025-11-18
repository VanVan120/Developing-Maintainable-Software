package com.comp2042.app;

import java.net.URL;

/**
 * Abstraction for resolving application resources such as FXML and CSS.
 *
 * <p>Implementations resolve a resource path to a {@link java.net.URL} or
 * return {@code null} when the resource cannot be located. Tests can provide
 * alternate implementations that return controlled URLs (for example, to test
 * FXML loading without relying on production classpath contents).
 */
public interface ResourceProvider {
    /**
     * Resolve the given resource path to a {@link URL} or return {@code null}
     * if the resource is not available.
     *
     * @param resourcePath classpath-like resource path (may be {@code null})
     * @return a {@link URL} that can be used to read the resource, or
     *         {@code null} when the resource cannot be resolved
     */
    URL getResource(String resourcePath);
}
