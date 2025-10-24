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
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.util.Duration;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;

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
    @FXML private MediaView menuMediaView;
    @FXML private javafx.scene.layout.StackPane mediaContainer;
    private String menuMediaUrl;

    @FXML
    public void initialize() {
        // set a background if available
        try {
            URL bg = getClass().getClassLoader().getResource("GUI.jpg");
            if (bg != null && bgImage != null) bgImage.setImage(new Image(bg.toExternalForm()));
        } catch (Exception ignored) {}

        // Attempt to load a preview video into the right-side MediaView (if present in resources)
        try {
            if (menuMediaView != null && mediaContainer != null) {
                String[] candidates = {"menu.mp4", "tetris_preview.mp4", "preview.mp4", "Tetris.mp4"};
                URL mediaUrl = null;
                for (String name : candidates) {
                    mediaUrl = getClass().getClassLoader().getResource(name);
                    if (mediaUrl != null) break;
                }
                if (mediaUrl != null) {
                    menuMediaUrl = mediaUrl.toExternalForm();
                    Media media = new Media(menuMediaUrl);
                    MediaPlayer mp = new MediaPlayer(media);
                    mp.setCycleCount(MediaPlayer.INDEFINITE);
                    mp.setAutoPlay(true);
                    mp.setMute(true); // mute preview by default
                    menuMediaView.setMediaPlayer(mp);
                    // Defer binding until after initial layout so mainButtons height is valid
                    Platform.runLater(() -> {
                        try {
                            if (mainButtons != null && mediaContainer != null) {
                                mediaContainer.prefHeightProperty().bind(mainButtons.heightProperty().multiply(0.92));
                            }

                            // Do NOT preserve aspect ratio here so the video can fill the container height
                            menuMediaView.setPreserveRatio(false);

                            // Bind MediaView to the container size (slightly inset)
                            // use a slightly smaller multiplier so the visible video is a touch shorter than the buttons
                            menuMediaView.fitHeightProperty().bind(mediaContainer.heightProperty().multiply(0.90));
                            menuMediaView.fitWidthProperty().bind(mediaContainer.widthProperty().multiply(0.98));
                        } catch (Exception ignored) {}
                    });
                } else {
                    // no media found; leave placeholder empty
                    System.out.println("No preview video found in resources (tried menu.mp4, tetris_preview.mp4, preview.mp4)");
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to load preview media: " + e.getMessage());
        }

        // Make the preview clickable: open a larger preview window when clicked
        try {
            if (menuMediaView != null) {
                menuMediaView.setOnMouseClicked(new javafx.event.EventHandler<javafx.scene.input.MouseEvent>() {
                    @Override
                    public void handle(javafx.scene.input.MouseEvent ev) {
                        try {
                            if (menuMediaUrl == null) return;
                            Media media2 = new Media(menuMediaUrl);
                            MediaPlayer mp2 = new MediaPlayer(media2);
                            mp2.setAutoPlay(true);
                            mp2.setCycleCount(MediaPlayer.INDEFINITE);
                            Stage popup = new Stage();
                            MediaView mv = new MediaView(mp2);
                            mv.setPreserveRatio(true);
                            StackPane root = new StackPane(mv);
                            Scene scene = new Scene(root, 900, 600);
                            popup.setScene(scene);
                            popup.initOwner(menuMediaView.getScene() != null ? (Stage)menuMediaView.getScene().getWindow() : null);
                            popup.setTitle("Preview");
                            // bind media view to popup size
                            mv.fitWidthProperty().bind(popup.widthProperty());
                            mv.fitHeightProperty().bind(popup.heightProperty());
                            popup.setOnCloseRequest(new javafx.event.EventHandler<javafx.stage.WindowEvent>() {
                                @Override
                                public void handle(javafx.stage.WindowEvent e2) {
                                    mp2.stop();
                                    mp2.dispose();
                                }
                            });
                            popup.show();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                });
            }
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

        // Ensure all main buttons share the same width (match the longest) after layout
        Platform.runLater(() -> {
            try {
                final double expansion = 30; // px to expand on hover
                final double[] baseWidth = new double[1];

                Runnable recompute = () -> {
                    // don't recompute while a hover animation is active (prevents feedback loops)
                    if ((multiPlayerBtn != null && multiPlayerBtn.isHover()) || (singlePlayerBtn != null && singlePlayerBtn.isHover()) || (settingsBtn != null && settingsBtn.isHover())) return;
                    double w1 = multiPlayerBtn.getWidth();
                    double w2 = singlePlayerBtn.getWidth();
                    double w3 = settingsBtn.getWidth();
                    double max = Math.max(w1, Math.max(w2, w3));
                    if (max <= 0) return; // layout not ready
                    baseWidth[0] = max;
                    // apply computed base width (not bound so we can animate)
                    multiPlayerBtn.setPrefWidth(max);
                    singlePlayerBtn.setPrefWidth(max);
                    settingsBtn.setPrefWidth(max);
                };

                // initial compute (if layout not ready yet, listeners below will update when sizes change)
                recompute.run();

                // Listen for intrinsic width changes and recompute when not hovered
                multiPlayerBtn.widthProperty().addListener(new javafx.beans.InvalidationListener() {
                    @Override
                    public void invalidated(javafx.beans.Observable observable) { recompute.run(); }
                });
                singlePlayerBtn.widthProperty().addListener(new javafx.beans.InvalidationListener() {
                    @Override
                    public void invalidated(javafx.beans.Observable observable) { recompute.run(); }
                });
                settingsBtn.widthProperty().addListener(new javafx.beans.InvalidationListener() {
                    @Override
                    public void invalidated(javafx.beans.Observable observable) { recompute.run(); }
                });

                // Hover animations: translate the whole button to the left on hover and return on exit.
                // Animating translateX avoids changing widths (prevents recompute feedback/accumulation).
                Duration dur = Duration.millis(140);

                java.util.function.BiConsumer<Button, Double> animateTranslate = (b, to) -> {
                    // Stop any existing timeline stored in properties
                    Object existing = b.getProperties().get("hoverTimeline");
                    if (existing instanceof Timeline) ((Timeline) existing).stop();
                    Timeline t = new Timeline(new KeyFrame(dur, new KeyValue(b.translateXProperty(), to)));
                    b.getProperties().put("hoverTimeline", t);
                    t.play();
                };

                javafx.event.EventHandler<javafx.scene.input.MouseEvent> enterHandler = e -> {
                    Button b = (Button) e.getSource();
                    // move left by expansion px
                    animateTranslate.accept(b, -expansion);
                    try {
                        // create a subtle drop shadow and animate its radius for a glow effect
                        DropShadow ds = new DropShadow(6, 0, 4, Color.rgb(0,0,0,0.28));
                        b.setEffect(ds);
                        // stop previous shadow timeline if any
                        Object existing = b.getProperties().get("shadowTimeline");
                        if (existing instanceof Timeline) ((Timeline) existing).stop();
                        Timeline s = new Timeline(new KeyFrame(dur, new KeyValue(ds.radiusProperty(), 18)));
                        b.getProperties().put("shadowTimeline", s);
                        b.getProperties().put("hoverDropShadow", ds);
                        s.play();
                    } catch (Exception ignored) {}
                };
                javafx.event.EventHandler<javafx.scene.input.MouseEvent> exitHandler = e -> {
                    Button b = (Button) e.getSource();
                    // return to original position
                    animateTranslate.accept(b, 0.0);
                    try {
                        Object existing = b.getProperties().get("shadowTimeline");
                        if (existing instanceof Timeline) ((Timeline) existing).stop();
                        Object dsObj = b.getProperties().get("hoverDropShadow");
                        if (dsObj instanceof DropShadow) {
                            DropShadow ds = (DropShadow) dsObj;
                            Timeline s = new Timeline(new KeyFrame(dur, new KeyValue(ds.radiusProperty(), 6)));
                            s.setOnFinished(new javafx.event.EventHandler<javafx.event.ActionEvent>() {
                                @Override
                                public void handle(javafx.event.ActionEvent event) {
                                    b.setEffect(null);
                                    b.getProperties().remove("hoverDropShadow");
                                    b.getProperties().remove("shadowTimeline");
                                }
                            });
                            b.getProperties().put("shadowTimeline", s);
                            s.play();
                        } else {
                            b.setEffect(null);
                        }
                    } catch (Exception ignored) {}
                };

                multiPlayerBtn.setOnMouseEntered(enterHandler);
                multiPlayerBtn.setOnMouseExited(exitHandler);
                singlePlayerBtn.setOnMouseEntered(enterHandler);
                singlePlayerBtn.setOnMouseExited(exitHandler);
                settingsBtn.setOnMouseEntered(enterHandler);
                settingsBtn.setOnMouseExited(exitHandler);
            } catch (Exception ignored) {}
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
