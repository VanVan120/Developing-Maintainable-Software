package com.comp2042.app;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Delegate startup to AppInitializer which encapsulates resource lookup and stage scene configuration and is easier to unit-test.
        AppInitializer initializer = new AppInitializer(null);
        initializer.initialize(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
