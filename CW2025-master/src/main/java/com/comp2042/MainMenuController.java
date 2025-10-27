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
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;

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
    private KeyCode spSwitch = null;
    
    // stored multiplayer control overrides (left player = upper, right player = bottom)
    private KeyCode mpLeft_left = null;
    private KeyCode mpLeft_right = null;
    private KeyCode mpLeft_rotate = null;
    private KeyCode mpLeft_down = null;
    private KeyCode mpLeft_hard = null;
    private KeyCode mpLeft_switch = null;

    private KeyCode mpRight_left = null;
    private KeyCode mpRight_right = null;
    private KeyCode mpRight_rotate = null;
    private KeyCode mpRight_down = null;
    private KeyCode mpRight_hard = null;
    private KeyCode mpRight_switch = null;
    
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
                // Open multiplayer controls overlay (two player configurator)
                loadMultiplayerControls();
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
                            controller.initBothGames(mpLeft_switch, mpRight_switch);
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
                        controller.initBothGames(mpLeft_switch, mpRight_switch);
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
            // Build a top-bar overlay that matches the multiplayer design but with a single Controls pane.
            URL loc = getClass().getClassLoader().getResource("controls.fxml");
            if (loc == null) {
                System.err.println("Cannot find controls.fxml");
                return;
            }

            FXMLLoader fx = new FXMLLoader(loc);
            javafx.scene.layout.StackPane pane = fx.load();
            ControlsController cc = fx.getController();

            // Initialize with stored values including switch (if previously set)
            cc.init(spLeft, spRight, spRotate, spDown, spHard, spSwitch);
            try { cc.setHeaderText("Single Player Configuration"); } catch (Exception ignored) {}

            // Hide the embedded controller's action buttons - we'll provide top-bar actions instead
            try { cc.hideActionButtons(); } catch (Exception ignored) {}

            // Create overlay shell similar to multiplayer overlay but for single pane
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-padding:0;");

            Rectangle dark = new Rectangle();
            dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
            javafx.application.Platform.runLater(() -> {
                try {
                    if (overlay.getScene() != null) {
                        dark.widthProperty().bind(overlay.getScene().widthProperty());
                        dark.heightProperty().bind(overlay.getScene().heightProperty());
                    }
                } catch (Exception ignored) {}
            });

            BorderPane container = new BorderPane();
            container.setMaxWidth(Double.MAX_VALUE);
            container.setMaxHeight(Double.MAX_VALUE);
            container.setStyle("-fx-padding:18;");

            javafx.scene.text.Text header = new javafx.scene.text.Text("Single Player Configuration");
            header.setStyle("-fx-font-size:34px; -fx-fill: #9fb0ff; -fx-font-weight:700;");
            BorderPane.setAlignment(header, javafx.geometry.Pos.CENTER_LEFT);

            // Top-right action buttons
            javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(10);
            actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            javafx.scene.control.Button btnReset = new javafx.scene.control.Button("Reset");
            javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Cancel");
            javafx.scene.control.Button btnSave = new javafx.scene.control.Button("Save");
            btnReset.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
            actionBox.getChildren().addAll(btnReset, btnCancel, btnSave);

            BorderPane topBar = new BorderPane();
            topBar.setLeft(header);
            topBar.setRight(actionBox);
            topBar.setStyle("-fx-padding:8 18 18 18;");
            container.setTop(topBar);

            // Center: the single controls pane
            javafx.scene.layout.VBox center = new javafx.scene.layout.VBox(18);
            center.setStyle("-fx-padding:12; -fx-background-color: transparent;");
            center.getChildren().add(pane);
            container.setCenter(center);

            // Wire Reset to restore defaults in the embedded controller
            btnReset.setOnAction(ev -> {
                try {
                    // Use the public API to reset internal controls to defaults
                    cc.resetToDefaults();
                } catch (Exception ignored) {}
            });

            // Cancel should remove overlay and ensure controlsOptions is visible again
            btnCancel.setOnAction(ev -> {
                ev.consume();
                closeOverlayWithAnimation(overlay, () -> {
                    try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                    try {
                        if (controlsOptions != null) {
                            controlsOptions.setVisible(true);
                            controlsOptions.setTranslateX(0);
                            controlsOptions.setOpacity(1.0);
                        }
                    } catch (Exception ignored) {}
                });
            });

            // Save should persist values into the MainMenuController fields and close
            btnSave.setOnAction(ev -> {
                ev.consume();
                try {
                    spLeft = cc.getLeft(); spRight = cc.getRight(); spRotate = cc.getRotate(); spDown = cc.getDown(); spHard = cc.getHard(); spSwitch = cc.getSwitch();
                    closeOverlayWithAnimation(overlay, () -> {
                        try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                        try { if (controlsOptions != null) controlsOptions.setVisible(true); } catch (Exception ignored) {}
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            overlay.getChildren().addAll(dark, container);
            try {
                if (controlsOptions != null) controlsOptions.setVisible(false);
                rootStack.getChildren().add(overlay);
            } catch (Exception ignored) {}
            transitionTo(overlay);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Load multiplayer controls overlay: two instances of controls.fxml stacked vertically.
     * Upper panel configures the left player, lower panel configures the right player.
     * On Save we validate that no KeyCode is reused between the two players.
     */
    private void loadMultiplayerControls() {
        try {
            URL loc = getClass().getClassLoader().getResource("controls.fxml");
            if (loc == null) {
                System.err.println("Cannot find controls.fxml");
                return;
            }

            // First controls pane (left / upper player)
            FXMLLoader fx1 = new FXMLLoader(loc);
            javafx.scene.layout.StackPane pane1 = fx1.load();
            ControlsController cc1 = fx1.getController();
            // initialize with stored values (may be null) — default to WASD + SHIFT for left player
            KeyCode defaultLeft_left = (mpLeft_left != null) ? mpLeft_left : KeyCode.A;
            KeyCode defaultLeft_right = (mpLeft_right != null) ? mpLeft_right : KeyCode.D;
            KeyCode defaultLeft_rotate = (mpLeft_rotate != null) ? mpLeft_rotate : KeyCode.W;
            KeyCode defaultLeft_down = (mpLeft_down != null) ? mpLeft_down : KeyCode.S;
            KeyCode defaultLeft_hard = (mpLeft_hard != null) ? mpLeft_hard : KeyCode.SHIFT;
            // Left-player default switch should be 'Q' (WASD player uses Q to swap)
            KeyCode defaultLeft_switch = (mpLeft_switch != null) ? mpLeft_switch : KeyCode.Q;
            cc1.init(defaultLeft_left, defaultLeft_right, defaultLeft_rotate, defaultLeft_down, defaultLeft_hard, defaultLeft_switch);
            // set the per-panel defaults so the 'Default' column shows WASD for left player (include Switch)
            try { cc1.setDefaultKeys(defaultLeft_left, defaultLeft_right, defaultLeft_rotate, defaultLeft_down, defaultLeft_hard, defaultLeft_switch); } catch (Exception ignored) {}
            // hide per-panel action buttons — we use the top bar's Reset/Cancel/Save instead
            try { cc1.hideActionButtons(); } catch (Exception ignored) {}
            try { cc1.setHeaderText("Left Player Controls"); } catch (Exception ignored) {}

            // Second controls pane (right / bottom player)
            FXMLLoader fx2 = new FXMLLoader(loc);
            javafx.scene.layout.StackPane pane2 = fx2.load();
            ControlsController cc2 = fx2.getController();
            KeyCode defaultRight_switch = (mpRight_switch != null) ? mpRight_switch : KeyCode.C;
            cc2.init(mpRight_left, mpRight_right, mpRight_rotate, mpRight_down, mpRight_hard, defaultRight_switch);
            try { cc2.setDefaultKeys(mpRight_left != null ? mpRight_left : KeyCode.LEFT, mpRight_right != null ? mpRight_right : KeyCode.RIGHT, mpRight_rotate != null ? mpRight_rotate : KeyCode.UP, mpRight_down != null ? mpRight_down : KeyCode.DOWN, mpRight_hard != null ? mpRight_hard : KeyCode.SPACE, defaultRight_switch); } catch (Exception ignored) {}
            try { cc2.hideActionButtons(); } catch (Exception ignored) {}
            try { cc2.setHeaderText("Right Player Controls"); } catch (Exception ignored) {}

            // Build a full-screen overlay with a dark background and a top bar for title + buttons
            StackPane overlay = new StackPane();
            overlay.setStyle("-fx-padding:0;");

            Rectangle dark = new Rectangle();
            dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
            // bind size to scene when available
            javafx.application.Platform.runLater(() -> {
                try {
                    if (overlay.getScene() != null) {
                        dark.widthProperty().bind(overlay.getScene().widthProperty());
                        dark.heightProperty().bind(overlay.getScene().heightProperty());
                    }
                } catch (Exception ignored) {}
            });

            // Top bar with title and action buttons on the right (Reset, Cancel, Save)
            BorderPane container = new BorderPane();
            container.setMaxWidth(Double.MAX_VALUE);
            container.setMaxHeight(Double.MAX_VALUE);
            container.setStyle("-fx-padding:18;");

            // Title (left)
            javafx.scene.text.Text header = new javafx.scene.text.Text("Multiplayer Configuration");
            header.setStyle("-fx-font-size:34px; -fx-fill: #9fb0ff; -fx-font-weight:700;");
            BorderPane.setAlignment(header, javafx.geometry.Pos.CENTER_LEFT);
            container.setTop(new StackPane(header));

            // action buttons (right)
            javafx.scene.layout.HBox actionBox = new javafx.scene.layout.HBox(10);
            actionBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
            javafx.scene.control.Button btnReset = new javafx.scene.control.Button("Reset");
            javafx.scene.control.Button btnCancel = new javafx.scene.control.Button("Cancel");
            javafx.scene.control.Button btnSave = new javafx.scene.control.Button("Save");
            btnReset.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
            actionBox.getChildren().addAll(btnReset, btnCancel, btnSave);

            // place actionBox into a right-aligned container on the top bar
            StackPane topRight = new StackPane(actionBox);
            BorderPane.setAlignment(topRight, javafx.geometry.Pos.CENTER_RIGHT);
            BorderPane topBar = new BorderPane();
            topBar.setLeft(header);
            topBar.setRight(actionBox);
            topBar.setStyle("-fx-padding:8 18 18 18;");
            container.setTop(topBar);

            // Center area: stacked left (upper) and right (bottom) player control panes, each with a small label
            javafx.scene.layout.VBox center = new javafx.scene.layout.VBox(18);
            center.setStyle("-fx-padding:12; -fx-background-color: transparent;");

            center.getChildren().addAll( pane1, pane2);

            // Put center inside a scroll pane in case window is small
            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(center);
            sp.setFitToWidth(true);
            sp.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding:12;");
            container.setCenter(sp);

            // Wire Reset to restore defaults in both embedded controllers
            btnReset.setOnAction(ev -> {
                try {
                    // left player: WASD + SHIFT (use Q as default Switch for left player)
                    cc1.init(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.Q);
                    try { cc1.setDefaultKeys(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.C); } catch (Exception ignored) {}
                    // right player: use ControlsController defaults (pass nulls)
                    cc2.init(null, null, null, null, null);
                } catch (Exception ignored) {}
            });

            // Cancel should remove overlay and ensure controlsOptions is visible again
            btnCancel.setOnAction(ev -> {
                ev.consume();
                closeOverlayWithAnimation(overlay, () -> {
                    try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                    try {
                        if (controlsOptions != null) {
                            controlsOptions.setVisible(true);
                            controlsOptions.setTranslateX(0);
                            controlsOptions.setOpacity(1.0);
                        }
                    } catch (Exception ignored) {}
                });
            });

            // Save validates uniqueness and stores values
            btnSave.setOnAction(ev -> {
                ev.consume();
                try {
                    KeyCode[] keys = new KeyCode[] {
                        cc1.getLeft(), cc1.getRight(), cc1.getRotate(), cc1.getDown(), cc1.getHard(), cc1.getSwitch(),
                        cc2.getLeft(), cc2.getRight(), cc2.getRotate(), cc2.getDown(), cc2.getHard(), cc2.getSwitch()
                    };
                    java.util.Set<KeyCode> set = new java.util.HashSet<>();
                    String dupFound = null;
                    for (KeyCode k : keys) {
                        if (k == null) continue;
                        if (set.contains(k)) { dupFound = k.getName(); break; }
                        set.add(k);
                    }
                    if (dupFound != null) {
                        javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                        a.setTitle("Duplicate Key");
                        a.setHeaderText("Key already assigned");
                        a.setContentText("The key '" + dupFound + "' is used multiple times across players. Please ensure each key is unique.");
                        a.showAndWait();
                        return;
                    }

                    // persist into fields
                    mpLeft_left = cc1.getLeft(); mpLeft_right = cc1.getRight(); mpLeft_rotate = cc1.getRotate(); mpLeft_down = cc1.getDown(); mpLeft_hard = cc1.getHard(); mpLeft_switch = cc1.getSwitch();
                    mpRight_left = cc2.getLeft(); mpRight_right = cc2.getRight(); mpRight_rotate = cc2.getRotate(); mpRight_down = cc2.getDown(); mpRight_hard = cc2.getHard(); mpRight_switch = cc2.getSwitch();

                    // close overlay with animation and then restore controls pane
                    closeOverlayWithAnimation(overlay, () -> {
                        try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                        try { if (controlsOptions != null) controlsOptions.setVisible(true); } catch (Exception ignored) {}
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            overlay.getChildren().addAll(dark, container);
            try { 
                // hide the underlying controls pane while the overlay is active to avoid duplicate animations
                if (controlsOptions != null) controlsOptions.setVisible(false);
                rootStack.getChildren().add(overlay); 
            } catch (Exception ignored) {}
            transitionTo(overlay);

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

    /**
     * Animates an overlay pane OUT to the right and then runs the provided callback.
     * The callback typically removes the overlay from the stack and restores the previous menu pane.
     */
    private void closeOverlayWithAnimation(StackPane fromPane, Runnable onFinished) {
        if (fromPane == null) {
            if (onFinished != null) Platform.runLater(onFinished);
            return;
        }
        Platform.runLater(() -> {
            try {
                double endX = fromPane.getScene() != null ? fromPane.getScene().getWidth() : fromPane.getWidth();
                TranslateTransition tt = new TranslateTransition(Duration.millis(220), fromPane);
                tt.setFromX(fromPane.getTranslateX());
                tt.setToX(endX);
                tt.setInterpolator(Interpolator.EASE_BOTH);
                tt.setOnFinished(ev -> {
                    try { fromPane.setVisible(false); fromPane.setTranslateX(0); } catch (Exception ex1) {}
                    try { if (onFinished != null) onFinished.run(); } catch (Exception ex2) {}
                });
                tt.play();
            } catch (Exception ex3) {
                try { fromPane.setVisible(false); } catch (Exception ex4) {}
                try { if (onFinished != null) onFinished.run(); } catch (Exception ex5) {}
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