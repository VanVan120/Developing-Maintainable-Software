package com.comp2042.app;

import java.net.URL;

/**
 * Abstraction for resolving resources used by the application (FXML, CSS, assets).
 * This allows tests to inject a fake provider that returns controlled URLs.
 */
public interface ResourceProvider {
    /**
     * Return a URL for the given classpath resource, or null if not found.
     */
    URL getResource(String resourcePath);
}
