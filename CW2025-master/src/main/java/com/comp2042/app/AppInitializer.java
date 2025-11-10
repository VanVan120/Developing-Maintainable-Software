package com.comp2042.app;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Encapsulates application initialization: loading main FXML, creating the
 * primary Scene, applying stylesheets and configuring the primary Stage.
 * This class is designed for easy unit testing by injecting a ResourceProvider.
 */
public class AppInitializer {
    private final ResourceProvider resourceProvider;

    public AppInitializer(ResourceProvider resourceProvider) {
        this.resourceProvider = resourceProvider == null ? new ClasspathResourceProvider() : resourceProvider;
    }

    /**
     * Initialize the given primary stage. Throws an IllegalStateException when
     * required resources (like the main FXML) are missing.
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

    private Parent loadRoot(String fxmlResource) throws Exception {
        URL location = resourceProvider.getResource(fxmlResource);
        if (location == null) {
            throw new IllegalStateException("FXML resource '" + fxmlResource + "' not found on classpath");
        }
        FXMLLoader fxmlLoader = new FXMLLoader(location);
        return fxmlLoader.load();
    }

    private Scene createScene(Parent root) {
        Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
        return new Scene(root, visualBounds.getWidth(), visualBounds.getHeight());
    }

    private void applyStyles(Scene scene, String cssResource) {
        if (scene == null) return;
        try {
            URL css = resourceProvider.getResource(cssResource);
            if (css != null) scene.getStylesheets().add(css.toExternalForm());
        } catch (Exception ignored) {}
    }
}
