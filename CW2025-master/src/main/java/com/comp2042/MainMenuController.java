package com.comp2042;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

import java.io.IOException;
import java.net.URL;
import javafx.application.Platform;

public class MainMenuController {

    @FXML private ImageView bgImage;
    @FXML private VBox mainButtons;
    @FXML private javafx.scene.text.Text titleText;
    @FXML private javafx.scene.layout.StackPane singleOptions;
    @FXML private Button singlePlayerBtn;
    @FXML private Button multiPlayerBtn;
    @FXML private Button settingsBtn;
    @FXML private Button easyBtn;
    @FXML private Button normalBtn;
    @FXML private Button hardBtn;
    @FXML private Button backBtn;
    @FXML private javafx.scene.layout.StackPane multiOptions;
    @FXML private Button scoreBattleBtn;
    @FXML private Button classicBattleBtn;
    @FXML private Button cooperateBattleBtn;
    @FXML private Button multiBackBtn;

    @FXML
    public void initialize() {
        // set a background if available
        try {
            URL bg = getClass().getClassLoader().getResource("GUI.jpg");
            if (bg != null && bgImage != null) bgImage.setImage(new Image(bg.toExternalForm()));
        } catch (Exception ignored) {}

        singlePlayerBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                showOverlay(singleOptions);
            }
        });

        // Multiplayer button toggles multiplayer options
        multiPlayerBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                showOverlay(multiOptions);
            }
        });

        backBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                hideOverlay(singleOptions);
            }
        });

        // Multiplayer back button returns to main buttons
        if (multiBackBtn != null) {
            multiBackBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    hideOverlay(multiOptions);
                }
            });
        }

        // Multiplayer mode placeholders: currently just log and return to main menu
            if (scoreBattleBtn != null) {
                scoreBattleBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                        try {
                            URL location = getClass().getClassLoader().getResource("scoreBattleLayout.fxml");
                            if (location == null) return;
                            FXMLLoader fxmlLoader = new FXMLLoader(location);
                            Parent root = fxmlLoader.load();
                            ScoreBattleController controller = fxmlLoader.getController();

                            // determine the current Stage so we can preserve window state
                            Stage stage = (Stage) scoreBattleBtn.getScene().getWindow();

                            // apply full scenic background for the whole score-battle scene (not per-board)
                            try {
                                URL normalBg = getClass().getClassLoader().getResource("Normal.jpg");
                                if (normalBg != null) root.setStyle("-fx-background-image: url('" + normalBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
                                // Do not clear the Scene's stylesheets here - clearing the Scene stylesheets
                                // removes the shared `menu.css` which causes unstyled controls after
                                // returning from Score Battle. Embedded roots already clear their own
                                // stylesheets in the multiplayer loader so leave the Scene alone.
                            } catch (Exception ignored) {}

                            double w = stage.getWidth();
                            double h = stage.getHeight();
                            boolean full = stage.isFullScreen();
                            boolean max = stage.isMaximized();

                            if (stage.getScene() != null) {
                                stage.getScene().setRoot(root);
                                stage.setMaximized(max);
                                if (full) Platform.runLater(() -> stage.setFullScreen(true));
                            } else {
                                Scene scene = new Scene(root, Math.max(420, w), Math.max(700, h));
                                stage.setScene(scene);
                                stage.setMaximized(max);
                                if (full) Platform.runLater(() -> stage.setFullScreen(true));
                                stage.show();
                            }

                            // initialize both games inside ScoreBattleController
                            controller.initBothGames();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
        if (classicBattleBtn != null) {
            classicBattleBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    try {
                        URL location = getClass().getClassLoader().getResource("classicBattleLayout.fxml");
                        if (location == null) return;
                        FXMLLoader fxmlLoader = new FXMLLoader(location);
                        Parent root = fxmlLoader.load();
                        ClassicBattle controller = fxmlLoader.getController();

                        Stage stage = (Stage) classicBattleBtn.getScene().getWindow();

                        try {
                            URL normalBg = getClass().getClassLoader().getResource("Normal.jpg");
                            if (normalBg != null) root.setStyle("-fx-background-image: url('" + normalBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
                        } catch (Exception ignored) {}

                        double w = stage.getWidth();
                        double h = stage.getHeight();
                        boolean full = stage.isFullScreen();
                        boolean max = stage.isMaximized();

                        if (stage.getScene() != null) {
                            stage.getScene().setRoot(root);
                            stage.setMaximized(max);
                            if (full) Platform.runLater(() -> stage.setFullScreen(true));
                        } else {
                            Scene scene = new Scene(root, Math.max(420, w), Math.max(700, h));
                            stage.setScene(scene);
                            stage.setMaximized(max);
                            if (full) Platform.runLater(() -> stage.setFullScreen(true));
                            stage.show();
                        }

                        // initialize both games inside ClassicBattle
                        controller.initBothGames();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }
        if (cooperateBattleBtn != null) {
            cooperateBattleBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    System.out.println("Cooperate Battle selected - not implemented yet");
                }
            });
        }

        // Normal button: load the game layout and start the game (normal mode)
        normalBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            try {
                URL location = getClass().getClassLoader().getResource("gameLayout.fxml");
                FXMLLoader fxmlLoader = new FXMLLoader(location);
                Parent root = fxmlLoader.load();
                GuiController controller = fxmlLoader.getController();

                Stage stage = (Stage) normalBtn.getScene().getWindow();
                // preserve current stage size and full-screen/maximized state and reuse existing Scene root
                double w = stage.getWidth();
                double h = stage.getHeight();
                boolean full = stage.isFullScreen();
                boolean max = stage.isMaximized();

                if (stage.getScene() != null) {
                    // set Normal background specifically for Normal mode
                    try {
                        URL normalBg = getClass().getClassLoader().getResource("Normal.jpg");
                        if (normalBg != null) root.setStyle("-fx-background-image: url('" + normalBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
                    } catch (Exception ignored) {}

                    // reuse existing scene to keep size and window state unchanged
                    stage.getScene().setRoot(root);
                    stage.setMaximized(max);
                    if (full) Platform.runLater(() -> stage.setFullScreen(true));
                } else {
                    try {
                        URL normalBg = getClass().getClassLoader().getResource("Normal.jpg");
                        if (normalBg != null) root.setStyle("-fx-background-image: url('" + normalBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
                    } catch (Exception ignored) {}

                    Scene scene = new Scene(root, Math.max(420, w), Math.max(700, h));
                    stage.setScene(scene);
                    stage.setMaximized(max);
                    if (full) Platform.runLater(() -> stage.setFullScreen(true));
                    stage.show();
                }

                // initialize game controller and start countdown
                new GameController(controller);
                controller.startCountdown(3);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            }
        });

        // Easy and Hard: pass mode to loadGame so we can set background and level
        easyBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) { loadGame("Easy"); }
        });
        hardBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) { loadGame("Hard"); }
        });
    }

    private void loadGame(String mode) {
        try {
            URL location = getClass().getClassLoader().getResource("gameLayout.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(location);
            Parent root = fxmlLoader.load();
            GuiController controller = fxmlLoader.getController();

            Stage stage = (Stage) backBtn.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            boolean full = stage.isFullScreen();
            boolean max = stage.isMaximized();

            // Apply mode-specific background and level text
            try {
                String bgName = null;
                if ("Easy".equalsIgnoreCase(mode)) bgName = "Easy.jpg";
                else if ("Hard".equalsIgnoreCase(mode)) bgName = "Hard.jpg";
                else bgName = "Normal.jpg";

                if (bgName != null) {
                    URL bgUrl = getClass().getClassLoader().getResource(bgName);
                    if (bgUrl != null) root.setStyle("-fx-background-image: url('" + bgUrl.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
                }
            } catch (Exception ignored) {}

            if (stage.getScene() != null) {
                stage.getScene().setRoot(root);
                stage.setMaximized(max);
                if (full) Platform.runLater(() -> stage.setFullScreen(true));
            } else {
                Scene scene = new Scene(root, Math.max(420, w), Math.max(700, h));
                stage.setScene(scene);
                stage.setMaximized(max);
                if (full) Platform.runLater(() -> stage.setFullScreen(true));
                stage.show();
            }

            // initialize game controller and set level text
            new GameController(controller);
            controller.setLevelText(mode);
            // adjust drop interval per mode: Easy slower, Normal default, Hard faster
            try {
                if ("Easy".equalsIgnoreCase(mode)) {
                    controller.setDropIntervalMs(1300); // slower
                } else if ("Hard".equalsIgnoreCase(mode)) {
                    controller.setDropIntervalMs(800); // faster
                } else {
                    controller.setDropIntervalMs(1000); // default (Normal)
                }
            } catch (Exception ignored) {}
            controller.startCountdown(3);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Helper to show an overlay StackPane and hide main title/buttons
    private void showOverlay(javafx.scene.layout.StackPane overlay) {
        if (overlay == null) return;
        mainButtons.setVisible(false);
        overlay.setVisible(true);
        if (titleText != null) titleText.setVisible(false);
    }

    // Helper to hide an overlay and restore main title/buttons
    private void hideOverlay(javafx.scene.layout.StackPane overlay) {
        if (overlay == null) return;
        overlay.setVisible(false);
        mainButtons.setVisible(true);
        if (titleText != null) titleText.setVisible(true);
    }
}
