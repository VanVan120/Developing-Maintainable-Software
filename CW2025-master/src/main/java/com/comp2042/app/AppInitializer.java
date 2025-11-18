package com.comp2042.app;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Initializes the application's primary UI and stage.
 *
 * <p>This class encapsulates the steps required to bring up the main JavaFX
 * window: locating and loading the primary FXML, creating a sized
 * {@link javafx.scene.Scene}, applying stylesheets and configuring the
 * {@link javafx.stage.Stage} (title, maximized/full-screen behavior and show).
 *
 * <p>A {@link ResourceProvider} is injected to allow test code to supply
 * controlled resource URLs (for example, test FXML/CSS) instead of relying on
 * classpath resolution. All public methods are intended to be executed on the
 * JavaFX Application Thread.
 */
public class AppInitializer {
    private final ResourceProvider resourceProvider;

    public AppInitializer(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider == null ? new ClasspathResourceProvider() : resourceProvider;
    }

    /**
     * Configure and show the application's primary stage.
     *
     * <p>This method performs the following steps:
     * <ol>
     *   <li>Locate and load the main FXML (see {@code MAIN_FXML}).</li>
     *   <li>Create a {@link javafx.scene.Scene} sized to the primary screen's
     *       visual bounds.</li>
     *   <li>Apply the menu stylesheet if present (see {@code MENU_CSS}).</li>
     *   <li>Configure the stage (title, maximize, attempt full-screen) and
     *       show it.</li>
     * </ol>
     *
     * @param primaryStage the primary {@link Stage} provided by the JavaFX
     *                     runtime; must be non-null and is modified and shown
     *                     by this method
     * @throws Exception if loading the FXML fails; callers should run this on
     *                   the JavaFX Application Thread (this method does not
     *                   perform thread switching)
     */
    public void initialize(Stage primaryStage) throws Exception {
        final String MAIN_FXML = "mainMenu.fxml";
        final String MENU_CSS = "css/menu.css";

        Parent root = loadRoot(MAIN_FXML);

        primaryStage.setTitle("Tetris Nexus");
        Scene scene = createScene(root);
        applyStyles(scene, MENU_CSS);

        primaryStage.setScene(scene);
        primaryStage.setMaximized(true);
        try {
            primaryStage.setFullScreen(true);
            primaryStage.setFullScreenExitHint("");
        } catch (Exception ignored) {}
        primaryStage.show();
    }

    /**
     * Load and return the root node from the named FXML resource.
     *
     * @param fxmlResource path to the FXML resource (relative to classpath)
     * @return the loaded {@link Parent} root
     * @throws Exception if the resource cannot be loaded by the {@link FXMLLoader}
     * @throws IllegalStateException if the resource cannot be found
     */
    private Parent loadRoot(String fxmlResource) throws Exception {
        URL location = resourceProvider.getResource(fxmlResource);
        if (location == null) {
            throw new IllegalStateException("FXML resource '" + fxmlResource + "' not found on classpath");
        }
        FXMLLoader fxmlLoader = new FXMLLoader(location);
        return fxmlLoader.load();
    }

    /**
     * Create a scene sized to the primary screen visual bounds.
     *
     * @param root the scene root node
     * @return a new {@link Scene} sized to the screen's visual bounds
     */
    private Scene createScene(Parent root) {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        return new Scene(root, visualBounds.getWidth(), visualBounds.getHeight());
    }

    /**
     * Resolve the given stylesheet resource and add it to the scene's
     * stylesheets if available.
     *
     * <p>Failures to locate or apply the stylesheet are ignored so that the UI
     * can still start without optional styling; callers may prefer to log
     * failures in production code.
     *
     * @param scene the scene to which the stylesheet should be applied; if
     *              {@code null} the method returns immediately
     * @param cssResource classpath path to the css resource
     */
    private void applyStyles(Scene scene, String cssResource) {
        if (scene == null) return;
        try {
            URL css = resourceProvider.getResource(cssResource);
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}
    }
}
