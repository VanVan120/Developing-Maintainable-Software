package com.comp2042.app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.net.URL;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        URL location = getClass().getClassLoader().getResource("mainMenu.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(location);
        Parent root = fxmlLoader.load();

        primaryStage.setTitle("Tetris Nexus");
    // Make the application open fullscreen on startup (maximize + full screen)
    Rectangle2D visualBounds = Screen.getPrimary().getVisualBounds();
    Scene scene = new Scene(root, visualBounds.getWidth(), visualBounds.getHeight());
        // apply menu stylesheet if available
        try {
            String css = getClass().getClassLoader().getResource("menu.css").toExternalForm();
            scene.getStylesheets().add(css);
        } catch (Exception ignored) {}
        primaryStage.setScene(scene);
        // maximize window and request full-screen so the app fills the entire display
        primaryStage.setMaximized(true);
        try {
            // request full screen; on some platforms this will hide window chrome and taskbar
            primaryStage.setFullScreen(true);
            // hide default full-screen exit hint to avoid overlay (optional)
            primaryStage.setFullScreenExitHint("");
        } catch (Exception ignored) {}
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
