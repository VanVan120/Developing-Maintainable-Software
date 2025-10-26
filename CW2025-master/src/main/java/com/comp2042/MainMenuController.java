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
import javafx.scene.input.KeyCode;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;
import javafx.animation.TranslateTransition;
import javafx.scene.control.Accordion;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TitledPane;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
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
    @FXML private javafx.scene.layout.StackPane rootStack;
    private String menuMediaUrl;
    
    // --- SETTINGS VARIABLES ---
    @FXML private javafx.scene.layout.StackPane settingsOptions;
    @FXML private Button controlsBtn;
    @FXML private Button handlingBtn;
    @FXML private Button audioBtn;
    @FXML private Button settingsBackBtn;
    
    // --- NEW CONTROLS SUB-MENU VARIABLES ---
    @FXML private StackPane controlsOptions;
    @FXML private Button singlePlayerConfigBtn;
    @FXML private Button multiPlayerConfigBtn;
    @FXML private Button controlsBackBtn;

    // stored singleplayer control overrides
    private KeyCode spLeft = null;
    private KeyCode spRight = null;
    private KeyCode spRotate = null;
    private KeyCode spDown = null;
    private KeyCode spHard = null;
    
    @FXML
    public void initialize() {
        // set a background if available
        try {
            URL bg = getClass().getClassLoader().getResource("GUI.jpg");
            if (bg != null && bgImage != null) bgImage.setImage(new Image(bg.toExternalForm()));
        } catch (Exception ignored) {}

        // Settings button: shows the 'settingsOptions' overlay
        if (settingsBtn != null) {
            settingsBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    showOverlay(settingsOptions);
                }
            });
        }

        // --- (MediaView logic remains unchanged) ---
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
                    try {
                        Media media = new Media(menuMediaUrl);
                        MediaPlayer mp = new MediaPlayer(media);
                        mp.setOnError(() -> {
                            try { System.err.println("Menu media player error: " + mp.getError()); } catch (Exception ignored) {}
                            disableMenuMedia("MediaPlayer error: " + (mp.getError() != null ? mp.getError().getMessage() : "unknown"));
                        });
                        mp.setOnReady(() -> {
                            try { System.out.println("Menu media ready: " + menuMediaUrl); } catch (Exception ignored) {}
                        });
                        mp.setCycleCount(MediaPlayer.INDEFINITE);
                        mp.setAutoPlay(true);
                        mp.setMute(true); 
                        menuMediaView.setMediaPlayer(mp);

                        Platform.runLater(() -> {
                            try {
                                if (mainButtons != null && mediaContainer != null) {
                                    mediaContainer.prefHeightProperty().bind(mainButtons.heightProperty().multiply(0.92));
                                }
                                menuMediaView.setPreserveRatio(false);
                                menuMediaView.fitHeightProperty().bind(mediaContainer.heightProperty().multiply(0.90));
                                menuMediaView.fitWidthProperty().bind(mediaContainer.widthProperty().multiply(0.98));
                            } catch (Exception ignored) {}
                        });
                    } catch (Exception me) {
                        System.err.println("Failed to initialize menu media: " + me.getMessage());
                        disableMenuMedia("Media init exception: " + me.getMessage());
                    }
                } else {
                    System.out.println("No preview video found in resources (tried menu.mp4, tetris_preview.mp4, preview.mp4)");
                    disableMenuMedia("no resource");
                }
            }
        } catch (Exception e) {
            System.out.println("Failed to load preview media: " + e.getMessage());
        }

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
                            mv.fitWidthProperty().bind(popup.widthProperty());
                            mv.fitHeightProperty().bind(popup.heightProperty());
                            popup.setOnCloseRequest(new javafx.event.EventHandler<javafx.stage.WindowEvent>() {
                                @Override
                                public void handle(javafx.stage.WindowEvent e2) {
                                    try { mp2.stop(); } catch (Exception ignored) {}
                                    try { mp2.dispose(); } catch (Exception ignored) {}
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

        if (multiBackBtn != null) {
            multiBackBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    hideOverlay(multiOptions);
                }
            });
        }
        
        // --- SETTINGS MENU BUTTON HANDLERS ---
        
        // "Back" button on Settings menu (goes to Main Menu)
        if (settingsBackBtn != null) {
            settingsBackBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    hideOverlay(settingsOptions);
                }
            });
        }
        
        // "Controls" button (goes to Controls Sub-Menu)
        if (controlsBtn != null) {
            controlsBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    // Transition from Settings -> Controls sub-menu
                    transitionFrom(settingsOptions);
                    transitionTo(controlsOptions);
                }
            });
        }
        
        if (handlingBtn != null) {
            handlingBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    System.out.println("Handling button clicked - not implemented yet");
                    // TODO: Create handlingOptions StackPane and transition to it
                }
            });
        }
        if (audioBtn != null) {
            audioBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    System.out.println("Audio button clicked - not implemented yet");
                    // TODO: Create audioOptions StackPane and transition to it
                }
            });
        }

        // --- NEW CONTROLS SUB-MENU HANDLERS ---

        // "Back" button on Controls sub-menu (goes to Settings)
        if (controlsBackBtn != null) {
            controlsBackBtn.setOnAction(e -> {
                // Transition from Controls sub-menu -> Settings
                transitionFrom(controlsOptions);
                transitionTo(settingsOptions);
            });
        }

        // "Single Player" button (loads the final controls.fxml overlay)
        if (singlePlayerConfigBtn != null) {
            singlePlayerConfigBtn.setOnAction(e -> {
                loadSinglePlayerControls();
            });
        }

        // "Multiplayer" button (placeholder)
        if (multiPlayerConfigBtn != null) {
            multiPlayerConfigBtn.setOnAction(e -> {
                System.out.println("Multiplayer controls not implemented yet.");
                // TODO: Load multiplayer controls FXML
            });
        }


        // --- (Multiplayer game buttons remain unchanged) ---
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
                            Stage stage = (Stage) scoreBattleBtn.getScene().getWindow();
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

        // --- (Solo game buttons remain unchanged) ---
        normalBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
            try {
                URL location = getClass().getClassLoader().getResource("gameLayout.fxml");
                FXMLLoader fxmlLoader = new FXMLLoader(location);
                Parent root = fxmlLoader.load();
                GuiController controller = fxmlLoader.getController();
                Stage stage = (Stage) normalBtn.getScene().getWindow();
                double w = stage.getWidth();
                double h = stage.getHeight();
                boolean full = stage.isFullScreen();
                boolean max = stage.isMaximized();
                if (stage.getScene() != null) {
                    try {
                        URL normalBg = getClass().getClassLoader().getResource("Normal.jpg");
                        if (normalBg != null) root.setStyle("-fx-background-image: url('" + normalBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
                    } catch (Exception ignored) {}
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
                new GameController(controller);
                controller.startCountdown(3);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            }
        });
        easyBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) { loadGame("Easy"); }
        });
        hardBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) { loadGame("Hard"); }
        });

        // --- (Hover effects remain unchanged, but with new buttons added) ---
        Platform.runLater(() -> {
            try {
                final double expansion = 30; 
                final double[] baseWidth = new double[1];
                Runnable recompute = () -> {
                    if ((multiPlayerBtn != null && multiPlayerBtn.isHover()) || (singlePlayerBtn != null && singlePlayerBtn.isHover()) || (settingsBtn != null && settingsBtn.isHover())) return;
                    double w1 = multiPlayerBtn.getWidth();
                    double w2 = singlePlayerBtn.getWidth();
                    double w3 = settingsBtn.getWidth();
                    double max = Math.max(w1, Math.max(w2, w3));
                    if (max <= 0) return; 
                    baseWidth[0] = max;
                    multiPlayerBtn.setPrefWidth(max);
                    singlePlayerBtn.setPrefWidth(max);
                    settingsBtn.setPrefWidth(max);
                };
                recompute.run();
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

                attachHoverEffects(multiPlayerBtn, expansion);
                attachHoverEffects(singlePlayerBtn, expansion);
                attachHoverEffects(settingsBtn, expansion);
                attachHoverEffects(scoreBattleBtn, expansion);
                attachHoverEffects(classicBattleBtn, expansion);
                attachHoverEffects(cooperateBattleBtn, expansion);
                attachHoverEffects(multiBackBtn, expansion);
                attachHoverEffects(easyBtn, expansion);
                attachHoverEffects(normalBtn, expansion);
                attachHoverEffects(hardBtn, expansion);
                attachHoverEffects(backBtn, expansion);
                attachHoverEffects(controlsBtn, expansion);
                attachHoverEffects(handlingBtn, expansion);
                attachHoverEffects(audioBtn, expansion);
                attachHoverEffects(settingsBackBtn, expansion);
                
                // ADDED NEW BUTTONS TO HOVER EFFECTS
                attachHoverEffects(singlePlayerConfigBtn, expansion);
                attachHoverEffects(multiPlayerConfigBtn, expansion);
                attachHoverEffects(controlsBackBtn, expansion);
                
            } catch (Exception ignored) {}
        });
    }
    
    /**
     * This method now loads the final controls.fxml overlay.
     * It is called by the 'singlePlayerConfigBtn'.
     */
    private void loadSinglePlayerControls() {
        try {
            URL loc = getClass().getClassLoader().getResource("controls.fxml");
            if (loc == null) {
                System.err.println("Cannot find controls.fxml");
                return;
            }
            FXMLLoader fx = new FXMLLoader(loc);
            javafx.scene.layout.StackPane overlay = fx.load();
            ControlsController cc = fx.getController();
            
            // initialize with current mappings
            cc.init(spLeft, spRight, spRotate, spDown, spHard);
            
            // set close handler so we can hide/remove this overlay
            cc.setCloseHandler(saved -> {
                try {
                    if (saved) {
                        KeyCode l = cc.getLeft();
                        KeyCode r = cc.getRight();
                        KeyCode rot = cc.getRotate();
                        KeyCode d = cc.getDown();
                        KeyCode h = cc.getHard();
                        if (l != null) spLeft = l;
                        if (r != null) spRight = r;
                        if (rot != null) spRotate = rot;
                        if (d != null) spDown = d;
                        if (h != null) spHard = h;
                    }
                } catch (Exception ignored) {}
                // hide and remove the overlay
                transitionFrom(overlay); // Use new transition
                try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
            });

            // add to scene overlay stack and show with same slide animation
            try { rootStack.getChildren().add(overlay); } catch (Exception ignored) {}
            transitionTo(overlay); // Use new transition
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    // Reusable helper to attach hover effects
    private void attachHoverEffects(Button b, double expansion) {
        if (b == null) return;
        Duration dur = Duration.millis(140);
        java.util.function.BiConsumer<Button, Double> animateTranslate = (btn, to) -> {
            Object existing = btn.getProperties().get("hoverTimeline");
            if (existing instanceof Timeline) ((Timeline) existing).stop();
            Timeline t = new Timeline(new KeyFrame(dur, new KeyValue(btn.translateXProperty(), to)));
            btn.getProperties().put("hoverTimeline", t);
            t.play();
        };

        b.setOnMouseEntered(e -> {
            animateTranslate.accept(b, -expansion);
            try {
                DropShadow ds = new DropShadow(6, 0, 4, Color.rgb(0,0,0,0.28));
                b.setEffect(ds);
                Object existing = b.getProperties().get("shadowTimeline");
                if (existing instanceof Timeline) ((Timeline) existing).stop();
                Timeline s = new Timeline(new KeyFrame(dur, new KeyValue(ds.radiusProperty(), 18)));
                b.getProperties().put("shadowTimeline", s);
                b.getProperties().put("hoverDropShadow", ds);
                s.play();
            } catch (Exception ignored) {}
        });

        b.setOnMouseExited(e -> {
            animateTranslate.accept(b, 0.0);
            try {
                Object existing = b.getProperties().get("shadowTimeline");
                if (existing instanceof Timeline) ((Timeline) existing).stop();
                Object dsObj = b.getProperties().get("hoverDropShadow");
                if (dsObj instanceof DropShadow) {
                    DropShadow ds = (DropShadow) dsObj;
                    Timeline s = new Timeline(new KeyFrame(dur, new KeyValue(ds.radiusProperty(), 6)));
                    s.setOnFinished(ev -> {
                        b.setEffect(null);
                        b.getProperties().remove("hoverDropShadow");
                        b.getProperties().remove("shadowTimeline");
                    });
                    b.getProperties().put("shadowTimeline", s);
                    s.play();
                } else {
                    b.setEffect(null);
                }
            } catch (Exception ignored) {}
        });
    }

    // Load game (remains unchanged)
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
            try {
                controller.setControlKeys(spLeft, spRight, spRotate, spDown, spHard);
            } catch (Exception ignored) {}
            new GameController(controller);
            controller.setLevelText(mode);
            try {
                if ("Easy".equalsIgnoreCase(mode)) {
                    controller.setDropIntervalMs(1300); 
                } else if ("Hard".equalsIgnoreCase(mode)) {
                    controller.setDropIntervalMs(800); 
                } else {
                    controller.setDropIntervalMs(1000); 
                }
            } catch (Exception ignored) {}
            controller.startCountdown(3);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void transitionTo(StackPane toPane) {
        if (toPane == null) return;
        toPane.setVisible(true);
        toPane.setOpacity(1.0);
        Platform.runLater(() -> {
            try {
                double startX = toPane.getScene() != null ? toPane.getScene().getWidth() : toPane.getWidth();
                toPane.setTranslateX(startX);
                TranslateTransition tt = new TranslateTransition(Duration.millis(300), toPane);
                tt.setFromX(startX);
                tt.setToX(0);
                tt.setInterpolator(Interpolator.EASE_BOTH);
                tt.play();
            } catch (Exception ignored) {}
        });
    }

    /**
     * Animates an overlay pane OUT to the right, without affecting main menu.
     */
    private void transitionFrom(StackPane fromPane) {
        if (fromPane == null) return;
        Platform.runLater(() -> {
            try {
                double endX = fromPane.getScene() != null ? fromPane.getScene().getWidth() : fromPane.getWidth();
                TranslateTransition tt = new TranslateTransition(Duration.millis(220), fromPane);
                tt.setFromX(fromPane.getTranslateX());
                tt.setToX(endX);
                tt.setInterpolator(Interpolator.EASE_BOTH);
                tt.setOnFinished(ev -> {
                    try { fromPane.setVisible(false); fromPane.setTranslateX(0); } catch (Exception ignored) {}
                });
                tt.play();
            } catch (Exception ignored) {
                try { fromPane.setVisible(false); } catch (Exception ignored2) {}
            }
        });
    }

    // --- ORIGINAL show/hideOverlay METHODS ---
    // (These interact with the main menu buttons)

    private void showOverlay(javafx.scene.layout.StackPane overlay) {
        if (overlay == null) return;
        try {
            if (mainButtons != null) {
                Timeline t = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(mainButtons.translateXProperty(), 0), new KeyValue(mainButtons.opacityProperty(), 1.0)),
                        new KeyFrame(Duration.millis(260), new KeyValue(mainButtons.translateXProperty(), -40, Interpolator.EASE_BOTH), new KeyValue(mainButtons.opacityProperty(), 0.08, Interpolator.EASE_BOTH))
                );
                t.play();
            }
            if (titleText != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(200), titleText);
                ft.setFromValue(1.0);
                ft.setToValue(0.0);
                ft.play();
            }
            // Use the new transitionTo helper for the animation
            transitionTo(overlay);
            
        } catch (Exception ex) {
            try { mainButtons.setVisible(false); } catch (Exception ignored) {}
            try { overlay.setVisible(true); } catch (Exception ignored) {}
            try { if (titleText != null) titleText.setVisible(false); } catch (Exception ignored) {}
        }
    }

    private void disableMenuMedia(String reason) {
        try {
            System.err.println("Menu media disabled: " + reason);
        } catch (Exception ignored) {}
        try {
            if (menuMediaView != null) {
                javafx.scene.media.MediaPlayer mp = menuMediaView.getMediaPlayer();
                if (mp != null) {
                    try { mp.stop(); } catch (Exception ignored) {}
                    try { mp.dispose(); } catch (Exception ignored) {}
                }
                try { menuMediaView.setMediaPlayer(null); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try { if (mediaContainer != null) mediaContainer.setVisible(false); } catch (Exception ignored) {}
    }

    private void hideOverlay(javafx.scene.layout.StackPane overlay) {
        if (overlay == null) return;
        try {
            // Use the new transitionFrom helper for the animation
            transitionFrom(overlay);
            
            // Restore main buttons
            if (mainButtons != null) {
                Timeline t = new Timeline(
                        new KeyFrame(Duration.ZERO, new KeyValue(mainButtons.translateXProperty(), mainButtons.getTranslateX()), new KeyValue(mainButtons.opacityProperty(), mainButtons.getOpacity())),
                        new KeyFrame(Duration.millis(260), new KeyValue(mainButtons.translateXProperty(), 0, Interpolator.EASE_BOTH), new KeyValue(mainButtons.opacityProperty(), 1.0, Interpolator.EASE_BOTH))
                );
                t.play();
            }
            if (titleText != null) {
                FadeTransition ft = new FadeTransition(Duration.millis(220), titleText);
                ft.setFromValue(titleText.getOpacity());
                ft.setToValue(1.0);
                ft.play();
            }
        } catch (Exception ex) {
            try { overlay.setVisible(false); } catch (Exception ignored) {}
            try { mainButtons.setVisible(true); } catch (Exception ignored) {}
            try { if (titleText != null) titleText.setVisible(true); } catch (Exception ignored) {}
        }
    }
}