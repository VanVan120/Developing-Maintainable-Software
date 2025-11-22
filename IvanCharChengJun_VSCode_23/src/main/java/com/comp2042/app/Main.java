package com.comp2042.app;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX application entry point. The {@link #start} method delegates UI
 * initialization to {@link AppInitializer} so that resource lookup and
 * scene/stage configuration can be tested separately from the JavaFX lifecycle.
 */
public class Main extends Application {

    /**
     * Start the JavaFX application by initializing the primary stage.
     *
     * @param primaryStage the primary stage supplied by the JavaFX runtime
     * @throws Exception if initialization fails (for example if loading the
     *                   main FXML fails). This method runs on the JavaFX
     *                   Application Thread.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        AppInitializer initializer = new AppInitializer(null);
        initializer.initialize(primaryStage);
    }

    /**
     * Program entry point. Launches the JavaFX runtime which will call
     * {@link #start} on the JavaFX Application Thread.
     */
    public static void main(String[] args) {
        launch(args);
    }
}
