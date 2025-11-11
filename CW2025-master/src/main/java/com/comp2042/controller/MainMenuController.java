package com.comp2042.controller;

import com.comp2042.audio.audioSettings.AudioSettings;
import com.comp2042.controller.classicBattle.ClassicBattle;
import com.comp2042.controller.controls.ControlsController;
import com.comp2042.controller.cooperateBattle.coopController.CoopGameController;
import com.comp2042.controller.cooperateBattle.coopGUI.CoopGuiController;

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
import java.util.prefs.Preferences;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;
import javafx.animation.TranslateTransition;
import javafx.scene.effect.DropShadow;
import javafx.scene.paint.Color;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.media.AudioClip;
import javafx.scene.input.MouseEvent;
import java.awt.Toolkit;
import javafx.beans.value.ChangeListener;

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
    @FXML private javafx.scene.layout.StackPane settingsOptions;
    @FXML private Button controlsBtn;
    @FXML private Button handlingBtn;
    @FXML private Button audioBtn;
    @FXML private Button settingsBackBtn;
    @FXML private StackPane controlsOptions;
    @FXML private Button singlePlayerConfigBtn;
    @FXML private Button multiPlayerConfigBtn;
    @FXML private Button controlsBackBtn;

    private String menuMediaUrl;
    private MediaPlayer menuMusicPlayer = null;
    private AudioClip menuHoverClip = null;
    private AudioClip menuClickClip = null;
    private boolean menuFallbackBeep = true;
    private ChangeListener<Number> masterVolListener = null;
    private ChangeListener<Number> musicVolListener = null;
    private ChangeListener<Number> sfxVolListener = null;
    private KeyCode spLeft = null;
    private KeyCode spRight = null;
    private KeyCode spRotate = null;
    private KeyCode spDown = null;
    private KeyCode spHard = null;
    private KeyCode spSwitch = null;
    private final Preferences prefs = Preferences.userNodeForPackage(MainMenuController.class);
    private int settingArrMs = 50; 
    private int settingDasMs = 120;
    private int settingDcdMs = 20; 
    private double settingSdf = 1.0; 
    private boolean settingHardDropEnabled = true;
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
        try { loadHandlingSettings(); } catch (Exception ignored) {}
        try { loadControlSettings(); } catch (Exception ignored) {}
        try {
            URL bg = getClass().getClassLoader().getResource("GUI.gif");
                if (bg != null && bgImage != null) bgImage.setImage(new Image(bg.toExternalForm(), true));
        } catch (Exception ignored) {}

        if (settingsBtn != null) {
            settingsBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    showOverlay(settingsOptions);
                }
            });
        }

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

        // Load and play looping main menu background music (MainMenu.wav)
        try {
            URL musicUrl = getClass().getClassLoader().getResource("sounds/MainMenu.wav");
            if (musicUrl == null) musicUrl = getClass().getResource("/sounds/MainMenu.wav");
            if (musicUrl != null) {
                try {
                    Media music = new Media(musicUrl.toExternalForm());
                    menuMusicPlayer = new MediaPlayer(music);
                    menuMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    menuMusicPlayer.setAutoPlay(true);
                    // default pleasant volume
                    menuMusicPlayer.setVolume(0.6);
                    menuMusicPlayer.setOnError(() -> System.err.println("Menu music error: " + menuMusicPlayer.getError()));
                    System.out.println("[MainMenuController] MainMenu.wav loaded and playing: " + musicUrl);
                } catch (Exception ex) {
                    System.err.println("[MainMenuController] Failed to initialize menu music: " + ex);
                    ex.printStackTrace();
                }
            } else {
                System.out.println("[MainMenuController] MainMenu.wav not found in resources (expected sounds/MainMenu.wav)");
            }
        } catch (Exception ex) {
            System.err.println("[MainMenuController] Exception while loading menu music: " + ex);
            ex.printStackTrace();
        }

        // Load menu UI sound clips (optional)
        try {
            URL h = getClass().getClassLoader().getResource("sounds/hover.wav");
            if (h != null) menuHoverClip = new AudioClip(h.toExternalForm());
        } catch (Exception ignored) {}
        try {
            URL c = getClass().getClassLoader().getResource("sounds/click.wav");
            if (c != null) menuClickClip = new AudioClip(c.toExternalForm());
        } catch (Exception ignored) {}

        try {
            applyAudioVolumesToMenu();
            try {
                masterVolListener = (obs, o, n) -> applyAudioVolumesToMenu();
                AudioSettings.masterProperty().addListener(masterVolListener);
            } catch (Exception ignored) {}
            try {
                musicVolListener = (obs, o, n) -> applyAudioVolumesToMenu();
                AudioSettings.musicProperty().addListener(musicVolListener);
            } catch (Exception ignored) {}
            try {
                sfxVolListener = (obs, o, n) -> applyAudioVolumesToMenu();
                AudioSettings.sfxProperty().addListener(sfxVolListener);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}

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
                
        if (settingsBackBtn != null) {
            settingsBackBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    hideOverlay(settingsOptions);
                }
            });
        }
        
        if (controlsBtn != null) {
            controlsBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    transitionFrom(settingsOptions);
                    transitionTo(controlsOptions);
                }
            });
        }
        
        if (handlingBtn != null) {
            handlingBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    loadHandlingControls();
                }
            });
        }
        if (audioBtn != null) {
            audioBtn.setOnAction(new EventHandler<ActionEvent>() {
                @Override
                public void handle(ActionEvent event) {
                    loadAudioSettings();
                }
            });
        }

        if (controlsBackBtn != null) {
            controlsBackBtn.setOnAction(e -> {
                transitionFrom(controlsOptions);
                transitionTo(settingsOptions);
            });
        }

        if (singlePlayerConfigBtn != null) {
            singlePlayerConfigBtn.setOnAction(e -> {
                loadSinglePlayerControls();
            });
        }

        if (multiPlayerConfigBtn != null) {
            multiPlayerConfigBtn.setOnAction(e -> {
                loadMultiplayerControls();
            });
        }


            if (scoreBattleBtn != null) {
                scoreBattleBtn.setOnAction(new EventHandler<ActionEvent>() {
                    @Override
                    public void handle(ActionEvent event) {
                                stopMenuMusic();
                        try {
                            URL location = getClass().getClassLoader().getResource("scoreBattleLayout.fxml");
                            if (location == null) return;
                            FXMLLoader fxmlLoader = new FXMLLoader(location);
                            Parent root = fxmlLoader.load();
                            ScoreBattleController controller = fxmlLoader.getController();
                            Stage stage = (Stage) scoreBattleBtn.getScene().getWindow();
                            try {
                                URL mpBg = getClass().getClassLoader().getResource("Multiplayer.gif");
                                if (mpBg != null) root.setStyle("-fx-background-image: url('" + mpBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
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
                            stopMenuMusic();
                    try {
                        URL location = getClass().getClassLoader().getResource("classicBattleLayout.fxml");
                        if (location == null) return;
                        FXMLLoader fxmlLoader = new FXMLLoader(location);
                        Parent root = fxmlLoader.load();
                        ClassicBattle controller = fxmlLoader.getController();
                        Stage stage = (Stage) classicBattleBtn.getScene().getWindow();
                        try {
                            URL mpBg = getClass().getClassLoader().getResource("Multiplayer.gif");
                            if (mpBg != null) root.setStyle("-fx-background-image: url('" + mpBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
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
                            stopMenuMusic();
                    try {
                        URL location = getClass().getClassLoader().getResource("gameLayout.fxml");
                        if (location == null) return;
                        FXMLLoader fxmlLoader = new FXMLLoader(location);
                        CoopGuiController coopGui = new CoopGuiController();
                        fxmlLoader.setControllerFactory((Class<?> c) -> {
                            if (c == GuiController.class) return coopGui;
                            try {
                                return c.getDeclaredConstructor().newInstance();
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
                        Parent root = fxmlLoader.load();

                        Stage stage = (Stage) cooperateBattleBtn.getScene().getWindow();
                        double w = stage.getWidth();
                        double h = stage.getHeight();
                        boolean full = stage.isFullScreen();
                        boolean max = stage.isMaximized();
                        try {
                            URL mpBg = getClass().getClassLoader().getResource("Multiplayer.gif");
                            if (mpBg != null) root.setStyle("-fx-background-image: url('" + mpBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
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

                        CoopGameController coopModel = new CoopGameController(10, 25);
                        coopModel.createNewGame();
                        // initialize coop GUI with model
                        try { coopGui.setHardDropEnabled(settingHardDropEnabled); } catch (Exception ignored) {}
                        coopGui.initCoop(coopModel);
                        try { coopGui.setLevelText("Cooperate"); } catch (Exception ignored) {}
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }

        normalBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                stopMenuMusic();
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
                        URL normalBg = getClass().getClassLoader().getResource("Normal.gif");
                        if (normalBg != null) root.setStyle("-fx-background-image: url('" + normalBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
                    } catch (Exception ignored) {}
                    stage.getScene().setRoot(root);
                    stage.setMaximized(max);
                    if (full) Platform.runLater(() -> stage.setFullScreen(true));
                } else {
                    try {
                        URL normalBg = getClass().getClassLoader().getResource("Normal.gif");
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

        Platform.runLater(() -> {
            try {
                final double expansion = 30; 
                final double[] baseWidth = new double[1];
                Runnable recompute = () -> {
                    if (multiPlayerBtn == null || singlePlayerBtn == null || settingsBtn == null) return;
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
                // attach simple menu UI sounds (hover + click) to major buttons
                try { attachButtonSoundHandlers(multiPlayerBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(singlePlayerBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(settingsBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(scoreBattleBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(classicBattleBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(cooperateBattleBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(easyBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(normalBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(hardBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(backBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(controlsBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(handlingBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(audioBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(settingsBackBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(singlePlayerConfigBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(multiPlayerConfigBtn); } catch (Exception ignored) {}
                try { attachButtonSoundHandlers(controlsBackBtn); } catch (Exception ignored) {}
                
            } catch (Exception ignored) {}
        });
        // Ensure the menu styles are loaded into the active scene when it becomes available
        Platform.runLater(() -> {
            try {
                javafx.scene.Scene s = null;
                if (rootStack != null) s = rootStack.getScene();
                if (s == null && backBtn != null) s = backBtn.getScene();
                ensureMainMenuStylesheet(s);
            } catch (Exception ignored) {}
        });
    }

    private void applyAudioVolumesToMenu() {
        try {
            double masterVol = AudioSettings.getMasterVolume();
            double musicVol = AudioSettings.getMusicVolume();
            double sfxVol = AudioSettings.getSfxVolume();
            double combinedMusic = masterVol * musicVol;
            double combinedSfx = masterVol * sfxVol;
            try {
                if (menuMusicPlayer != null) menuMusicPlayer.setVolume(combinedMusic);
            } catch (Exception ignored) {}
            try {
                if (menuHoverClip != null) menuHoverClip.setVolume(combinedSfx);
            } catch (Exception ignored) {}
            try {
                if (menuClickClip != null) menuClickClip.setVolume(combinedSfx);
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private void loadAudioSettings() {
        try {
            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("menu-overlay");
            Rectangle dark = new Rectangle();
            dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
            dark.getStyleClass().add("menu-overlay-dark");
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
            container.getStyleClass().add("menu-overlay-container");

            javafx.scene.text.Text header = new javafx.scene.text.Text("Audio");
            header.getStyleClass().add("menu-overlay-header");

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
            topBar.getStyleClass().add("menu-overlay-topbar");
            container.setTop(topBar);

            // sliders
            javafx.scene.layout.VBox center = new javafx.scene.layout.VBox(14);
            center.getStyleClass().add("menu-overlay-center");
            center.setAlignment(javafx.geometry.Pos.TOP_LEFT);

            javafx.scene.control.Label lMaster = new javafx.scene.control.Label("Master Volume");
            // make the label brighter and larger for better readability
            lMaster.getStyleClass().add("menu-label");
            javafx.scene.control.Slider sMaster = new javafx.scene.control.Slider(0, 1, AudioSettings.getMasterVolume());
            // hide built-in slider tick labels (we provide clearer custom labels below)
            sMaster.setShowTickLabels(false); sMaster.setShowTickMarks(true); sMaster.setBlockIncrement(0.05);

            javafx.scene.control.Label lMusic = new javafx.scene.control.Label("Music Volume");
            // make the label brighter and larger for better readability
            lMusic.getStyleClass().add("menu-label");
            javafx.scene.control.Slider sMusic = new javafx.scene.control.Slider(0, 1, AudioSettings.getMusicVolume());
            // hide built-in slider tick labels (we provide clearer custom labels below)
            sMusic.setShowTickLabels(false); sMusic.setShowTickMarks(true); sMusic.setBlockIncrement(0.05);

            javafx.scene.control.Label lSfx = new javafx.scene.control.Label("SFX Volume");
            // make the label brighter and larger for better readability
            lSfx.getStyleClass().add("menu-label");
            javafx.scene.control.Slider sSfx = new javafx.scene.control.Slider(0, 1, AudioSettings.getSfxVolume());
            // hide built-in slider tick labels (we provide clearer custom labels below)
            sSfx.setShowTickLabels(false); sSfx.setShowTickMarks(true); sSfx.setBlockIncrement(0.05);

            sMaster.valueProperty().addListener((obs, o, n) -> {
                try {
                    double combinedMusic = n.doubleValue() * sMusic.getValue();
                    double combinedSfx = n.doubleValue() * sSfx.getValue();
                    if (menuMusicPlayer != null) try { menuMusicPlayer.setVolume(combinedMusic); } catch (Exception ignored) {}
                    if (menuHoverClip != null) try { menuHoverClip.setVolume(combinedSfx); } catch (Exception ignored) {}
                    if (menuClickClip != null) try { menuClickClip.setVolume(combinedSfx); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            });
            sMusic.valueProperty().addListener((obs, o, n) -> {
                try {
                    double combinedMusic = sMaster.getValue() * n.doubleValue();
                    if (menuMusicPlayer != null) try { menuMusicPlayer.setVolume(combinedMusic); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            });
            sSfx.valueProperty().addListener((obs, o, n) -> {
                try {
                    double combinedSfx = sMaster.getValue() * n.doubleValue();
                    if (menuHoverClip != null) try { menuHoverClip.setVolume(combinedSfx); } catch (Exception ignored) {}
                    if (menuClickClip != null) try { menuClickClip.setVolume(combinedSfx); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            });

            javafx.scene.layout.HBox ticksMaster = new javafx.scene.layout.HBox();
            javafx.scene.control.Label tMaster0 = new javafx.scene.control.Label("0");
            javafx.scene.control.Label tMaster05 = new javafx.scene.control.Label("0.5");
            javafx.scene.control.Label tMaster1 = new javafx.scene.control.Label("1");
            // make the main tick labels larger and higher contrast and add subtle drop shadow
            tMaster0.getStyleClass().add("menu-tick-large");
            tMaster05.getStyleClass().add("menu-tick-medium");
            tMaster1.getStyleClass().add("menu-tick-large");
            javafx.scene.layout.Region spacerMasterLeft = new javafx.scene.layout.Region();
            javafx.scene.layout.Region spacerMasterRight = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacerMasterLeft, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox.setHgrow(spacerMasterRight, javafx.scene.layout.Priority.ALWAYS);
            ticksMaster.getChildren().addAll(tMaster0, spacerMasterLeft, tMaster05, spacerMasterRight, tMaster1);

            javafx.scene.layout.HBox ticksMusic = new javafx.scene.layout.HBox();
            javafx.scene.control.Label tMusic0 = new javafx.scene.control.Label("0");
            javafx.scene.control.Label tMusic05 = new javafx.scene.control.Label("0.5");
            javafx.scene.control.Label tMusic1 = new javafx.scene.control.Label("1");
            tMusic0.getStyleClass().add("menu-tick-large");
            tMusic05.getStyleClass().add("menu-tick-medium");
            tMusic1.getStyleClass().add("menu-tick-large");
            javafx.scene.layout.Region spacerMusicLeft = new javafx.scene.layout.Region();
            javafx.scene.layout.Region spacerMusicRight = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacerMusicLeft, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox.setHgrow(spacerMusicRight, javafx.scene.layout.Priority.ALWAYS);
            ticksMusic.getChildren().addAll(tMusic0, spacerMusicLeft, tMusic05, spacerMusicRight, tMusic1);

            javafx.scene.layout.HBox ticksSfx = new javafx.scene.layout.HBox();
            javafx.scene.control.Label tSfx0 = new javafx.scene.control.Label("0");
            javafx.scene.control.Label tSfx05 = new javafx.scene.control.Label("0.5");
            javafx.scene.control.Label tSfx1 = new javafx.scene.control.Label("1");
            tSfx0.getStyleClass().add("menu-tick-large");
            tSfx05.getStyleClass().add("menu-tick-medium");
            tSfx1.getStyleClass().add("menu-tick-large");
            javafx.scene.layout.Region spacerSfxLeft = new javafx.scene.layout.Region();
            javafx.scene.layout.Region spacerSfxRight = new javafx.scene.layout.Region();
            javafx.scene.layout.HBox.setHgrow(spacerSfxLeft, javafx.scene.layout.Priority.ALWAYS);
            javafx.scene.layout.HBox.setHgrow(spacerSfxRight, javafx.scene.layout.Priority.ALWAYS);
            ticksSfx.getChildren().addAll(tSfx0, spacerSfxLeft, tSfx05, spacerSfxRight, tSfx1);

            center.getChildren().addAll(lMaster, sMaster, ticksMaster, lMusic, sMusic, ticksMusic, lSfx, sSfx, ticksSfx);
            container.setCenter(center);

            // Reset
            btnReset.setOnAction(ev -> {
                sMaster.setValue(1.0);
                sMusic.setValue(0.6);
                sSfx.setValue(0.9);
            });

            // Cancel
            btnCancel.setOnAction(ev -> {
                ev.consume();
                // restore persisted audio settings (undo live preview) before closing
                try { applyAudioVolumesToMenu(); } catch (Exception ignored) {}
                closeOverlayWithAnimation(overlay, () -> {
                    try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                    try { if (settingsOptions != null) settingsOptions.setVisible(true); } catch (Exception ignored) {}
                });
            });

            // Save
            btnSave.setOnAction(ev -> {
                ev.consume();
                try {
                    AudioSettings.setMasterVolume(sMaster.getValue());
                    AudioSettings.setMusicVolume(sMusic.getValue());
                    AudioSettings.setSfxVolume(sSfx.getValue());
                } catch (Exception ignored) {}
                closeOverlayWithAnimation(overlay, () -> {
                    try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                    try { if (settingsOptions != null) settingsOptions.setVisible(true); } catch (Exception ignored) {}
                });
            });

            overlay.getChildren().addAll(dark, container);
            try { if (settingsOptions != null) settingsOptions.setVisible(false); rootStack.getChildren().add(overlay); } catch (Exception ignored) {}
            transitionTo(overlay);

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadHandlingControls() {
        try {
            URL loc = getClass().getClassLoader().getResource("handling.fxml");
            if (loc == null) {
                System.err.println("Cannot find handling.fxml");
                return;
            }

            FXMLLoader fx = new FXMLLoader(loc);
            javafx.scene.layout.StackPane pane = fx.load();
            HandlingController hc = fx.getController();

            // initialize with stored values
            hc.init(settingArrMs, settingDasMs, settingDcdMs, settingSdf, settingHardDropEnabled);
            try { hc.setHeaderText("Handling"); } catch (Exception ignored) {}
            try { hc.hideActionButtons(); } catch (Exception ignored) {}

            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("menu-overlay");
            Rectangle dark = new Rectangle();
            dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
            dark.getStyleClass().add("menu-overlay-dark");
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
            container.getStyleClass().add("menu-overlay-container");

            javafx.scene.text.Text header = new javafx.scene.text.Text("Handling");
            header.getStyleClass().add("menu-overlay-header");
            BorderPane.setAlignment(header, javafx.geometry.Pos.CENTER_LEFT);

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
            topBar.getStyleClass().add("menu-overlay-topbar");
            container.setTop(topBar);

            javafx.scene.layout.VBox center = new javafx.scene.layout.VBox(18);
            center.getStyleClass().add("menu-overlay-center");
            center.getChildren().add(pane);
            container.setCenter(center);

            // Reset
            btnReset.setOnAction(ev -> {
                try { hc.resetToDefaults(); } catch (Exception ignored) {}
            });

            // Cancel
            btnCancel.setOnAction(ev -> {
                ev.consume();
                closeOverlayWithAnimation(overlay, () -> {
                    try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                    try {
                        if (settingsOptions != null) {
                            settingsOptions.setVisible(true);
                            settingsOptions.setTranslateX(0);
                            settingsOptions.setOpacity(1.0);
                        }
                    } catch (Exception ignored) {}
                });
            });

            // Save persist settings
            btnSave.setOnAction(ev -> {
                ev.consume();
                try {
                    settingArrMs = hc.getArrMs();
                    settingDasMs = hc.getDasMs();
                    settingDcdMs = hc.getDcdMs();
                    settingSdf = hc.getSdf();
                    settingHardDropEnabled = hc.isHardDropEnabled();
                    // persist handling settings
                    try { saveHandlingSettings(); } catch (Exception ignored) {}
                    closeOverlayWithAnimation(overlay, () -> {
                        try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                        try { if (settingsOptions != null) settingsOptions.setVisible(true); } catch (Exception ignored) {}
                    });
                } catch (Exception ex) { ex.printStackTrace(); }
            });

            overlay.getChildren().addAll(dark, container);
            try { if (settingsOptions != null) settingsOptions.setVisible(false); rootStack.getChildren().add(overlay); } catch (Exception ignored) {}
            transitionTo(overlay);

        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadSinglePlayerControls() {
        try {
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
            overlay.getStyleClass().add("menu-overlay");

            Rectangle dark = new Rectangle();
            dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
            dark.getStyleClass().add("menu-overlay-dark");
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
            container.getStyleClass().add("menu-overlay-container");

            javafx.scene.text.Text header = new javafx.scene.text.Text("Single Player");
            header.getStyleClass().add("menu-overlay-header");
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
            topBar.getStyleClass().add("menu-overlay-topbar");
            container.setTop(topBar);

            // Center: the single controls pane
            javafx.scene.layout.VBox center = new javafx.scene.layout.VBox(18);
            center.getStyleClass().add("menu-overlay-center");
            center.getChildren().add(pane);
            container.setCenter(center);

            btnReset.setOnAction(ev -> {
                try {
                    cc.resetToDefaults();
                } catch (Exception ignored) {}
            });

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

            btnSave.setOnAction(ev -> {
                ev.consume();
                try {
                    spLeft = cc.getLeft(); spRight = cc.getRight(); spRotate = cc.getRotate(); spDown = cc.getDown(); spHard = cc.getHard(); spSwitch = cc.getSwitch();
                    // persist control settings
                    try { saveControlSettings(); } catch (Exception ignored) {}
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

    private void loadMultiplayerControls() {
        try {
            URL loc = getClass().getClassLoader().getResource("controls.fxml");
            if (loc == null) {
                System.err.println("Cannot find controls.fxml");
                return;
            }

            FXMLLoader fx1 = new FXMLLoader(loc);
            javafx.scene.layout.StackPane pane1 = fx1.load();
            ControlsController cc1 = fx1.getController();
            KeyCode defaultLeft_left = (mpLeft_left != null) ? mpLeft_left : KeyCode.A;
            KeyCode defaultLeft_right = (mpLeft_right != null) ? mpLeft_right : KeyCode.D;
            KeyCode defaultLeft_rotate = (mpLeft_rotate != null) ? mpLeft_rotate : KeyCode.W;
            KeyCode defaultLeft_down = (mpLeft_down != null) ? mpLeft_down : KeyCode.S;
            KeyCode defaultLeft_hard = (mpLeft_hard != null) ? mpLeft_hard : KeyCode.SHIFT;
            KeyCode defaultLeft_switch = (mpLeft_switch != null) ? mpLeft_switch : KeyCode.Q;
            cc1.init(defaultLeft_left, defaultLeft_right, defaultLeft_rotate, defaultLeft_down, defaultLeft_hard, defaultLeft_switch);
            try { cc1.setDefaultKeys(defaultLeft_left, defaultLeft_right, defaultLeft_rotate, defaultLeft_down, defaultLeft_hard, defaultLeft_switch); } catch (Exception ignored) {}
            try { cc1.hideActionButtons(); } catch (Exception ignored) {}
            try { cc1.setHeaderText("Left Player Controls"); } catch (Exception ignored) {}

            FXMLLoader fx2 = new FXMLLoader(loc);
            javafx.scene.layout.StackPane pane2 = fx2.load();
            ControlsController cc2 = fx2.getController();
            KeyCode defaultRight_switch = (mpRight_switch != null) ? mpRight_switch : KeyCode.C;
            cc2.init(mpRight_left, mpRight_right, mpRight_rotate, mpRight_down, mpRight_hard, defaultRight_switch);
            try { cc2.setDefaultKeys(mpRight_left != null ? mpRight_left : KeyCode.LEFT, mpRight_right != null ? mpRight_right : KeyCode.RIGHT, mpRight_rotate != null ? mpRight_rotate : KeyCode.UP, mpRight_down != null ? mpRight_down : KeyCode.DOWN, mpRight_hard != null ? mpRight_hard : KeyCode.SPACE, defaultRight_switch); } catch (Exception ignored) {}
            try { cc2.hideActionButtons(); } catch (Exception ignored) {}
            try { cc2.setHeaderText("Right Player Controls"); } catch (Exception ignored) {}

            try {
                cc1.setKeyAvailabilityChecker((code, btn) -> {
                    if (code == null) return true;
                    try {
                        return !(code.equals(cc2.getLeft()) || code.equals(cc2.getRight()) || code.equals(cc2.getRotate()) || code.equals(cc2.getDown()) || code.equals(cc2.getHard()) || code.equals(cc2.getSwitch()));
                    } catch (Exception ignored) { return true; }
                });

                cc2.setKeyAvailabilityChecker((code, btn) -> {
                    if (code == null) return true;
                    try {
                        return !(code.equals(cc1.getLeft()) || code.equals(cc1.getRight()) || code.equals(cc1.getRotate()) || code.equals(cc1.getDown()) || code.equals(cc1.getHard()) || code.equals(cc1.getSwitch()));
                    } catch (Exception ignored) { return true; }
                });
            } catch (Exception ignored) {}

            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("menu-overlay");

            Rectangle dark = new Rectangle();
            dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
            dark.getStyleClass().add("menu-overlay-dark");
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
            container.getStyleClass().add("menu-overlay-container");

            // Title (left)
            javafx.scene.text.Text header = new javafx.scene.text.Text("Multiplayer");
            header.getStyleClass().add("menu-overlay-header");
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
            topBar.getStyleClass().add("menu-overlay-topbar");
            container.setTop(topBar);

            javafx.scene.layout.VBox center = new javafx.scene.layout.VBox(18);
            center.getStyleClass().add("menu-overlay-center");

            center.getChildren().addAll( pane1, pane2);

            javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane(center);
            sp.setFitToWidth(true);
            sp.getStyleClass().add("menu-overlay-scroll");
            container.setCenter(sp);

            btnReset.setOnAction(ev -> {
                try {
                    cc1.init(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.Q);
                    try { cc1.setDefaultKeys(KeyCode.A, KeyCode.D, KeyCode.W, KeyCode.S, KeyCode.SHIFT, KeyCode.C); } catch (Exception ignored) {}
                    cc2.init(null, null, null, null, null);
                } catch (Exception ignored) {}
            });

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

                    mpLeft_left = cc1.getLeft(); mpLeft_right = cc1.getRight(); mpLeft_rotate = cc1.getRotate(); mpLeft_down = cc1.getDown(); mpLeft_hard = cc1.getHard(); mpLeft_switch = cc1.getSwitch();
                    mpRight_left = cc2.getLeft(); mpRight_right = cc2.getRight(); mpRight_rotate = cc2.getRotate(); mpRight_down = cc2.getDown(); mpRight_hard = cc2.getHard(); mpRight_switch = cc2.getSwitch();

                    try { saveControlSettings(); } catch (Exception ignored) {}

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

    private void attachButtonSoundHandlers(Button btn) {
        if (btn == null) return;
        try {
            btn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { e.getSource().toString(); playMenuHover(); });
            btn.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> { e.getSource().toString(); playMenuClick(); });
            btn.addEventHandler(javafx.event.ActionEvent.ACTION, e -> { e.getSource().toString(); playMenuClick(); });
        } catch (Exception ignored) {}
    }

    private void playMenuHover() {
        try {
            if (menuHoverClip != null) menuHoverClip.play();
            else if (menuFallbackBeep) Toolkit.getDefaultToolkit().beep();
        } catch (Exception ex) { System.err.println("Menu hover play failed: " + ex); }
    }

    private void playMenuClick() {
        try {
            if (menuClickClip != null) menuClickClip.play();
            else if (menuFallbackBeep) Toolkit.getDefaultToolkit().beep();
        } catch (Exception ex) { System.err.println("Menu click play failed: " + ex); }
    }

    private void loadGame(String mode) {
        stopMenuMusic();
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
                if ("Easy".equalsIgnoreCase(mode)) bgName = "Easy.gif";
                else if ("Hard".equalsIgnoreCase(mode)) bgName = "Hard.gif";
                else bgName = "Normal.gif";
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
            try { controller.setHardDropEnabled(settingHardDropEnabled); } catch (Exception ignored) {}
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

    private void stopMenuMusic() {
        try {
            if (menuMusicPlayer != null) {
                try { menuMusicPlayer.stop(); } catch (Exception ignored) {}
                try { menuMusicPlayer.dispose(); } catch (Exception ignored) {}
                menuMusicPlayer = null;
            }
        } catch (Exception ignored) {}
    }

    public void cleanup() {
        try { stopMenuMusic(); } catch (Exception ignored) {}
        try {
            if (menuHoverClip != null) { try { menuHoverClip.stop(); } catch (Exception ignored) {} menuHoverClip = null; }
        } catch (Exception ignored) {}
        try {
            if (menuClickClip != null) { try { menuClickClip.stop(); } catch (Exception ignored) {} menuClickClip = null; }
        } catch (Exception ignored) {}
        try {
            if (menuMediaView != null) {
                MediaPlayer mp = menuMediaView.getMediaPlayer();
                if (mp != null) {
                    try { mp.stop(); } catch (Exception ignored) {}
                    try { mp.dispose(); } catch (Exception ignored) {}
                }
                try { menuMediaView.setMediaPlayer(null); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try { if (masterVolListener != null) AudioSettings.masterProperty().removeListener(masterVolListener); } catch (Exception ignored) {}
        try { if (musicVolListener != null) AudioSettings.musicProperty().removeListener(musicVolListener); } catch (Exception ignored) {}
        try { if (sfxVolListener != null) AudioSettings.sfxProperty().removeListener(sfxVolListener); } catch (Exception ignored) {}
        masterVolListener = null; musicVolListener = null; sfxVolListener = null;

        try {
            Button[] btns = new Button[] { singlePlayerBtn, multiPlayerBtn, settingsBtn, easyBtn, normalBtn, hardBtn, backBtn, scoreBattleBtn, classicBattleBtn, cooperateBattleBtn, multiBackBtn, controlsBtn, handlingBtn, audioBtn, settingsBackBtn, singlePlayerConfigBtn, multiPlayerConfigBtn, controlsBackBtn };
            for (Button b : btns) {
                if (b == null) continue;
                try { b.setOnAction(null); } catch (Exception ignored) {}
                try { b.setOnMouseEntered(null); } catch (Exception ignored) {}
                try { b.setOnMouseExited(null); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        try { if (bgImage != null) bgImage.setImage(null); } catch (Exception ignored) {}
    }

    private void hideOverlay(javafx.scene.layout.StackPane overlay) {
        if (overlay == null) return;
        try {
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

    private void saveControlSettings() {
        try {
            prefs.put("spLeft", spLeft != null ? spLeft.name() : "");
            prefs.put("spRight", spRight != null ? spRight.name() : "");
            prefs.put("spRotate", spRotate != null ? spRotate.name() : "");
            prefs.put("spDown", spDown != null ? spDown.name() : "");
            prefs.put("spHard", spHard != null ? spHard.name() : "");
            prefs.put("spSwitch", spSwitch != null ? spSwitch.name() : "");

            prefs.put("mpLeft_left", mpLeft_left != null ? mpLeft_left.name() : "");
            prefs.put("mpLeft_right", mpLeft_right != null ? mpLeft_right.name() : "");
            prefs.put("mpLeft_rotate", mpLeft_rotate != null ? mpLeft_rotate.name() : "");
            prefs.put("mpLeft_down", mpLeft_down != null ? mpLeft_down.name() : "");
            prefs.put("mpLeft_hard", mpLeft_hard != null ? mpLeft_hard.name() : "");
            prefs.put("mpLeft_switch", mpLeft_switch != null ? mpLeft_switch.name() : "");

            prefs.put("mpRight_left", mpRight_left != null ? mpRight_left.name() : "");
            prefs.put("mpRight_right", mpRight_right != null ? mpRight_right.name() : "");
            prefs.put("mpRight_rotate", mpRight_rotate != null ? mpRight_rotate.name() : "");
            prefs.put("mpRight_down", mpRight_down != null ? mpRight_down.name() : "");
            prefs.put("mpRight_hard", mpRight_hard != null ? mpRight_hard.name() : "");
            prefs.put("mpRight_switch", mpRight_switch != null ? mpRight_switch.name() : "");
        } catch (Exception ex) {
            // don't fail the UI on persistence error
            try { System.err.println("Failed to save control settings: " + ex.getMessage()); } catch (Exception ignored) {}
        }
    }

    /** Load control key settings (single-player and multiplayer) from Preferences. */
    private void loadControlSettings() {
        try {
            String s;
            s = prefs.get("spLeft", ""); spLeft = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("spRight", ""); spRight = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("spRotate", ""); spRotate = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("spDown", ""); spDown = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("spHard", ""); spHard = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("spSwitch", ""); spSwitch = s.isEmpty() ? null : safeKeyCodeOf(s);

            s = prefs.get("mpLeft_left", ""); mpLeft_left = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpLeft_right", ""); mpLeft_right = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpLeft_rotate", ""); mpLeft_rotate = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpLeft_down", ""); mpLeft_down = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpLeft_hard", ""); mpLeft_hard = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpLeft_switch", ""); mpLeft_switch = s.isEmpty() ? null : safeKeyCodeOf(s);

            s = prefs.get("mpRight_left", ""); mpRight_left = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpRight_right", ""); mpRight_right = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpRight_rotate", ""); mpRight_rotate = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpRight_down", ""); mpRight_down = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpRight_hard", ""); mpRight_hard = s.isEmpty() ? null : safeKeyCodeOf(s);
            s = prefs.get("mpRight_switch", ""); mpRight_switch = s.isEmpty() ? null : safeKeyCodeOf(s);
        } catch (Exception ex) {
            try { System.err.println("Failed to load control settings: " + ex.getMessage()); } catch (Exception ignored) {}
        }
    }

    private void saveHandlingSettings() {
        try {
            prefs.putInt("settingArrMs", settingArrMs);
            prefs.putInt("settingDasMs", settingDasMs);
            prefs.putInt("settingDcdMs", settingDcdMs);
            prefs.putDouble("settingSdf", settingSdf);
            prefs.putBoolean("settingHardDropEnabled", settingHardDropEnabled);
        } catch (Exception ex) {
            try { System.err.println("Failed to save handling settings: " + ex.getMessage()); } catch (Exception ignored) {}
        }
    }

    private void loadHandlingSettings() {
        try {
            settingArrMs = prefs.getInt("settingArrMs", settingArrMs);
            settingDasMs = prefs.getInt("settingDasMs", settingDasMs);
            settingDcdMs = prefs.getInt("settingDcdMs", settingDcdMs);
            settingSdf = prefs.getDouble("settingSdf", settingSdf);
            settingHardDropEnabled = prefs.getBoolean("settingHardDropEnabled", settingHardDropEnabled);
        } catch (Exception ex) {
            try { System.err.println("Failed to load handling settings: " + ex.getMessage()); } catch (Exception ignored) {}
        }
    }

    private KeyCode safeKeyCodeOf(String name) {
        if (name == null || name.isEmpty()) return null;
        try {
            return KeyCode.valueOf(name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /** Ensure the main menu stylesheet(s) are loaded into the given Scene. */
    private void ensureMainMenuStylesheet(javafx.scene.Scene scene) {
        if (scene == null) return;
        try {
            // menu.css is the existing shared stylesheet; our file will be css/main-menu.css
            java.net.URL menuCss = getClass().getClassLoader().getResource("css/menu.css");
            java.net.URL myCss = getClass().getClassLoader().getResource("css/main-menu.css");
            if (menuCss != null) {
                String s = menuCss.toExternalForm();
                if (!scene.getStylesheets().contains(s)) scene.getStylesheets().add(s);
            }
            if (myCss != null) {
                String s2 = myCss.toExternalForm();
                if (!scene.getStylesheets().contains(s2)) scene.getStylesheets().add(s2);
            }
        } catch (Exception ignored) {}
    }
}