package com.comp2042.controller.mainMenu;

import com.comp2042.controller.guiControl.GuiController;
import com.comp2042.controller.cooperateBattle.coopGUI.CoopGuiController;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.fxml.FXMLLoader;
import javafx.application.Platform;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;

/**
 * Helper to initialize the main menu UI. The long initialization logic was
 * extracted from `MainMenuController.initialize()` to keep the controller file
 * smaller. The controller should call the static `initialize(...)` method and
 * apply returned helpers/state as needed.
 */
public class MainMenuInitializer {

    public static class Result {
        public MainMenuOverlayHelper overlayHelper;
        public MainMenuButtonEffectsHelper buttonEffectsHelper;
        public MainMenuMediaHelper mediaHelper;
        public String menuMediaUrl;
    }

    /**
     * Initialize the main menu view and behaviour.
     *
     * <p>The method centralizes loading resources, wiring button behaviour,
     * creating helpers (overlay, media, effects) and registering audio
     * listeners. It returns a {@link Result} object containing helpers and
     * metadata the caller should retain and apply to the controller.
     *
     * @return a {@link Result} instance with prepared helpers and menu state
     */
    public static Result initialize(
            Runnable loadHandlingSettings,
            Runnable loadControlSettings,
            ImageView bgImage,
            Button settingsBtn,
            StackPane settingsOptions,
            MediaView menuMediaView,
            StackPane mediaContainer,
            StackPane rootStack,
            VBox mainButtons,
            javafx.scene.text.Text titleText,
            StackPane singleOptions,
            Button singlePlayerBtn, Button multiPlayerBtn, Button backBtn,
            StackPane multiOptions, Button multiBackBtn,
            StackPane controlsOptions,
            Button controlsBtn, Button handlingBtn, Button audioBtn, Button settingsBackBtn, Button singlePlayerConfigBtn, Button multiPlayerConfigBtn, Button controlsBackBtn,
            Button scoreBattleBtn, Button classicBattleBtn, Button cooperateBattleBtn,
            Button normalBtn, Button easyBtn, Button hardBtn,
            MainMenuAudioManager audioManager,
            Consumer<String> loadGame,
            Runnable stopMenuMusic,
            Runnable loadHandlingControls,
            Runnable loadAudioSettings,
            Runnable loadSinglePlayerControls,
            Runnable loadMultiplayerControls,
            Consumer<StackPane> transitionTo,
            Consumer<javafx.scene.Scene> ensureMainMenuStylesheet
    ) {
        Result res = new Result();
        loadInitialResources(loadHandlingSettings, loadControlSettings, bgImage);

        initSettingsButton(res, settingsBtn, settingsOptions);

        initMediaPreview(res, menuMediaView, mediaContainer);

        initializeHelpersAndAudio(res, mainButtons, titleText, audioManager);

        initMenuMediaPreviewClick(res, menuMediaView);

        setupButtonsBehavior(res,
                singleOptions, multiOptions, controlsOptions,
                singlePlayerBtn, multiPlayerBtn, controlsBtn, handlingBtn, audioBtn, backBtn, multiBackBtn,
                settingsOptions, settingsBackBtn,
                singlePlayerConfigBtn, multiPlayerConfigBtn, controlsBackBtn,
                scoreBattleBtn, classicBattleBtn, cooperateBattleBtn,
                normalBtn, easyBtn, hardBtn,
                loadHandlingControls, loadAudioSettings, loadSinglePlayerControls, loadMultiplayerControls,
                stopMenuMusic, loadGame,
                mainButtons, titleText
        );

        attachEffectsAndSounds(res, audioManager,
                multiPlayerBtn, singlePlayerBtn, settingsBtn, scoreBattleBtn, classicBattleBtn, cooperateBattleBtn,
                multiBackBtn, easyBtn, normalBtn, hardBtn, backBtn, controlsBtn, handlingBtn, audioBtn,
                settingsBackBtn, singlePlayerConfigBtn, multiPlayerConfigBtn, controlsBackBtn
        );

        normalizePrimaryButtonWidths(mainButtons, multiPlayerBtn, singlePlayerBtn, settingsBtn);
        ensureStylesheetOnScene(rootStack, ensureMainMenuStylesheet);
        return res;
    }

    private static void loadInitialResources(Runnable loadHandlingSettings, Runnable loadControlSettings, ImageView bgImage) {
        try { try { if (loadHandlingSettings != null) loadHandlingSettings.run(); } catch (Exception ignored) {} } catch (Exception ignored) {}
        try { try { if (loadControlSettings != null) loadControlSettings.run(); } catch (Exception ignored) {} } catch (Exception ignored) {}
        try {
            URL bg = MainMenuInitializer.class.getResource("/GUI.gif");
            if (bg != null && bgImage != null) bgImage.setImage(new Image(bg.toExternalForm(), true));
        } catch (Exception ignored) {}
    }

    private static void initSettingsButton(Result res, Button settingsBtn, StackPane settingsOptions) {
        if (settingsBtn != null) {
            settingsBtn.setOnAction(ev -> {
                try {
                    if (res.overlayHelper != null) res.overlayHelper.showOverlay(settingsOptions);
                } catch (Exception ignored) {}
            });
        }
    }

    private static void initMediaPreview(Result res, MediaView menuMediaView, StackPane mediaContainer) {
        try {
            res.mediaHelper = new MainMenuMediaHelper();
            try { res.mediaHelper.initMenuMedia(menuMediaView, mediaContainer); } catch (Exception ignored) {}
            try { res.menuMediaUrl = res.mediaHelper.getMenuMediaUrl(); } catch (Exception ignored) {}
        } catch (Exception e) {
            System.out.println("Failed to load preview media: " + e.getMessage());
        }
    }

    private static void initializeHelpersAndAudio(Result res, VBox mainButtons, javafx.scene.text.Text titleText, MainMenuAudioManager audioManager) {
        try {
            // initialize helpers
            res.overlayHelper = new MainMenuOverlayHelper(mainButtons, titleText);
            res.buttonEffectsHelper = new MainMenuButtonEffectsHelper();
            res.mediaHelper = res.mediaHelper == null ? new MainMenuMediaHelper() : res.mediaHelper;

            try { if (audioManager != null) audioManager.loadAll(MainMenuInitializer.class.getClassLoader()); } catch (Exception ignored) {}
            try { if (audioManager != null) audioManager.registerListeners(); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private static void initMenuMediaPreviewClick(Result res, MediaView menuMediaView) {
        try {
            if (menuMediaView != null) {
                menuMediaView.setOnMouseClicked(ev -> {
                    try {
                        if (res.menuMediaUrl == null) return;
                        Media media2 = new Media(res.menuMediaUrl);
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
                        popup.setOnCloseRequest(e2 -> {
                            try { mp2.stop(); } catch (Exception ignored) {}
                            try { mp2.dispose(); } catch (Exception ignored) {}
                        });
                        popup.show();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    private static void setupButtonsBehavior(Result res,
                                             StackPane singleOptions, StackPane multiOptions, StackPane controlsOptions,
                                             Button singlePlayerBtn, Button multiPlayerBtn, Button controlsBtn, Button handlingBtn, Button audioBtn, Button backBtn, Button multiBackBtn,
                                             StackPane settingsOptions, Button settingsBackBtn,
                                             Button singlePlayerConfigBtn, Button multiPlayerConfigBtn, Button controlsBackBtn,
                                             Button scoreBattleBtn, Button classicBattleBtn, Button cooperateBattleBtn,
                                             Button normalBtn, Button easyBtn, Button hardBtn,
                                             Runnable loadHandlingControls, Runnable loadAudioSettings, Runnable loadSinglePlayerControls, Runnable loadMultiplayerControls,
                                             Runnable stopMenuMusic, Consumer<String> loadGame,
                                             VBox mainButtons, javafx.scene.text.Text titleText) {
        initOverlayButtons(res, singleOptions, multiOptions, controlsOptions,
                singlePlayerBtn, multiPlayerBtn, controlsBtn, settingsOptions);

        initLoaderButtons(handlingBtn, audioBtn, loadHandlingControls, loadAudioSettings);

        initBackRestoreButtons(res, backBtn, multiBackBtn, settingsBackBtn,
                singleOptions, multiOptions, settingsOptions, controlsOptions,
                mainButtons, titleText);

        initConfigButtons(res, singlePlayerConfigBtn, multiPlayerConfigBtn, controlsBackBtn,
            controlsOptions, settingsOptions, loadSinglePlayerControls, loadMultiplayerControls);

        initBattleButtons(scoreBattleBtn, classicBattleBtn, cooperateBattleBtn, stopMenuMusic);

        initGameDifficultyButtons(normalBtn, easyBtn, hardBtn, stopMenuMusic, loadGame);
    }

    private static void initOverlayButtons(Result res, StackPane singleOptions, StackPane multiOptions, StackPane controlsOptions,
                                           Button singlePlayerBtn, Button multiPlayerBtn, Button controlsBtn, StackPane settingsOptions) {
        if (singlePlayerBtn != null) singlePlayerBtn.setOnAction(ev -> { if (res.overlayHelper != null) res.overlayHelper.showOverlay(singleOptions); });
        if (multiPlayerBtn != null) multiPlayerBtn.setOnAction(ev -> { if (res.overlayHelper != null) res.overlayHelper.showOverlay(multiOptions); });
        if (controlsBtn != null) controlsBtn.setOnAction(ev -> {
            try {
                if (res.overlayHelper != null) {
                    try { if (settingsOptions != null && settingsOptions.isVisible()) settingsOptions.setVisible(false); } catch (Exception ignored) {}
                    res.overlayHelper.showOverlay(controlsOptions);
                }
            } catch (Exception ignored) {}
        });
    }

    private static void initLoaderButtons(Button handlingBtn, Button audioBtn, Runnable loadHandlingControls, Runnable loadAudioSettings) {
        if (handlingBtn != null) handlingBtn.setOnAction(ev -> { try { if (loadHandlingControls != null) loadHandlingControls.run(); } catch (Exception ignored) {} });
        if (audioBtn != null) audioBtn.setOnAction(ev -> { try { if (loadAudioSettings != null) loadAudioSettings.run(); } catch (Exception ignored) {} });
    }

    private static void initBackRestoreButtons(Result res, Button backBtn, Button multiBackBtn, Button settingsBackBtn,
                                               StackPane singleOptions, StackPane multiOptions, StackPane settingsOptions, StackPane controlsOptions,
                                               VBox mainButtons, javafx.scene.text.Text titleText) {
        if (backBtn != null) backBtn.setOnAction(ev -> {
            try {
                if (res.overlayHelper != null) {
                    res.overlayHelper.closeOverlayWithAnimation(singleOptions, () -> restoreMainAndTitle(mainButtons, titleText));
                }
            } catch (Exception ignored) {}
        });
        if (multiBackBtn != null) multiBackBtn.setOnAction(ev -> {
            try {
                if (res.overlayHelper != null) {
                    res.overlayHelper.closeOverlayWithAnimation(multiOptions, () -> restoreMainAndTitle(mainButtons, titleText));
                }
            } catch (Exception ignored) {}
        });
        if (settingsBackBtn != null) settingsBackBtn.setOnAction(ev -> {
            try {
                if (res.overlayHelper != null) {
                    res.overlayHelper.closeOverlayWithAnimation(settingsOptions, () -> restoreMainAndTitle(mainButtons, titleText));
                }
            } catch (Exception ignored) {}
        });
    }

    private static void restoreMainAndTitle(VBox mainButtons, javafx.scene.text.Text titleText) {
        try {
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
        } catch (Exception ignored) {}
    }

    private static void initConfigButtons(Result res, Button singlePlayerConfigBtn, Button multiPlayerConfigBtn, Button controlsBackBtn,
                                          StackPane controlsOptions, StackPane settingsOptions, Runnable loadSinglePlayerControls, Runnable loadMultiplayerControls) {
        if (singlePlayerConfigBtn != null) singlePlayerConfigBtn.setOnAction(ev -> {
            try {
                if (res.overlayHelper != null) {
                    try { if (settingsOptions != null && settingsOptions.isVisible()) res.overlayHelper.closeOverlayWithAnimation(settingsOptions, () -> {}); } catch (Exception ignored) {}
                    try { if (loadSinglePlayerControls != null) loadSinglePlayerControls.run(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        });
        if (multiPlayerConfigBtn != null) multiPlayerConfigBtn.setOnAction(ev -> {
            try {
                if (res.overlayHelper != null) {
                    try { if (settingsOptions != null && settingsOptions.isVisible()) res.overlayHelper.closeOverlayWithAnimation(settingsOptions, () -> {}); } catch (Exception ignored) {}
                    try { if (loadMultiplayerControls != null) loadMultiplayerControls.run(); } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        });
        if (controlsBackBtn != null) controlsBackBtn.setOnAction(ev -> {
            try {
                if (res.overlayHelper != null) {
                    res.overlayHelper.closeOverlayWithAnimation(controlsOptions, () -> {
                        try { if (settingsOptions != null) res.overlayHelper.showOverlay(settingsOptions); } catch (Exception ignored) {}
                    });
                }
            } catch (Exception ignored) {}
        });
    }

    private static void initBattleButtons(Button scoreBattleBtn, Button classicBattleBtn, Button cooperateBattleBtn, Runnable stopMenuMusic) {
        if (scoreBattleBtn != null) {
            scoreBattleBtn.setOnAction(ev -> {
                try {
                    if (stopMenuMusic != null) stopMenuMusic.run();
                    URL location = MainMenuInitializer.class.getResource("/scoreBattleLayout.fxml");
                    if (location == null) return;
                    FXMLLoader fxmlLoader = new FXMLLoader(location);
                    Parent root = fxmlLoader.load();
                    com.comp2042.controller.scoreBattle.ScoreBattleController controller = fxmlLoader.getController();
                    Stage stage = (Stage) scoreBattleBtn.getScene().getWindow();
                    try {
                        URL mpBg = MainMenuInitializer.class.getResource("/Multiplayer.gif");
                        if (mpBg != null) root.setStyle("-fx-background-image: url('" + mpBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
                    } catch (Exception ignored) {}
                    double w = stage.getWidth(); double h = stage.getHeight(); boolean full = stage.isFullScreen(); boolean max = stage.isMaximized();
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
                    try { controller.initBothGames(null, null); } catch (Exception ignored) {}
                } catch (IOException ex) { ex.printStackTrace(); }
            });
        }

        if (classicBattleBtn != null) {
            classicBattleBtn.setOnAction(ev -> {
                try {
                    if (stopMenuMusic != null) stopMenuMusic.run();
                    URL location = MainMenuInitializer.class.getResource("/classicBattleLayout.fxml");
                    if (location == null) return;
                    FXMLLoader fxmlLoader = new FXMLLoader(location);
                    Parent root = fxmlLoader.load();
                    com.comp2042.controller.classicBattle.ClassicBattle controller = fxmlLoader.getController();
                    Stage stage = (Stage) classicBattleBtn.getScene().getWindow();
                    try {
                        URL mpBg = MainMenuInitializer.class.getResource("/Multiplayer.gif");
                        if (mpBg != null) root.setStyle("-fx-background-image: url('" + mpBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;");
                    } catch (Exception ignored) {}
                    double w = stage.getWidth(); double h = stage.getHeight(); boolean full = stage.isFullScreen(); boolean max = stage.isMaximized();
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
                    try { controller.initBothGames(null, null); } catch (Exception ignored) {}
                } catch (IOException ex) { ex.printStackTrace(); }
            });
        }

        if (cooperateBattleBtn != null) {
            cooperateBattleBtn.setOnAction(ev -> {
                try {
                    if (stopMenuMusic != null) stopMenuMusic.run();
                    URL location = MainMenuInitializer.class.getResource("/gameLayout.fxml");
                    if (location == null) return;
                    FXMLLoader fxmlLoader = new FXMLLoader(location);
                    CoopGuiController coopGui = new CoopGuiController();
                    fxmlLoader.setControllerFactory((Class<?> c) -> {
                        if (c == GuiController.class) return coopGui;
                        try { return c.getDeclaredConstructor().newInstance(); } catch (Exception e) { throw new RuntimeException(e); }
                    });
                    Parent root = fxmlLoader.load();
                    Stage stage = (Stage) cooperateBattleBtn.getScene().getWindow();
                    double w = stage.getWidth(); double h = stage.getHeight(); boolean full = stage.isFullScreen(); boolean max = stage.isMaximized();
                    try { URL mpBg = MainMenuInitializer.class.getResource("/Multiplayer.gif"); if (mpBg != null) root.setStyle("-fx-background-image: url('" + mpBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;"); } catch (Exception ignored) {}
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
                    com.comp2042.controller.cooperateBattle.coopController.CoopGameController coopModel = new com.comp2042.controller.cooperateBattle.coopController.CoopGameController(10, 25);
                    coopModel.createNewGame();
                    try { coopGui.setHardDropEnabled(true); } catch (Exception ignored) {}
                    coopGui.initCoop(coopModel);
                    try { coopGui.setLevelText("Cooperate"); } catch (Exception ignored) {}
                } catch (IOException ex) { ex.printStackTrace(); }
            });
        }
    }

    private static void initGameDifficultyButtons(Button normalBtn, Button easyBtn, Button hardBtn, Runnable stopMenuMusic, Consumer<String> loadGame) {
        if (normalBtn != null) {
            normalBtn.setOnAction(ev -> {
                try {
                    if (stopMenuMusic != null) stopMenuMusic.run();
                    URL location = MainMenuInitializer.class.getResource("/gameLayout.fxml");
                    FXMLLoader fxmlLoader = new FXMLLoader(location);
                    Parent root = fxmlLoader.load();
                    com.comp2042.controller.guiControl.GuiController controller = fxmlLoader.getController();
                    Stage stage = (Stage) normalBtn.getScene().getWindow();
                    double w = stage.getWidth(); double h = stage.getHeight(); boolean full = stage.isFullScreen(); boolean max = stage.isMaximized();
                    try { URL normalBg = MainMenuInitializer.class.getResource("/Normal.gif"); if (normalBg != null) root.setStyle("-fx-background-image: url('" + normalBg.toExternalForm() + "'); -fx-background-size: cover; -fx-background-position: center center;"); } catch (Exception ignored) {}
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
                    try { controller.setControlKeys(null, null, null, null, null); } catch (Exception ignored) {}
                    try { controller.setHardDropEnabled(true); } catch (Exception ignored) {}
                    new com.comp2042.controller.gameControl.GameController(controller);
                    controller.setLevelText("Normal");
                    try { controller.setDropIntervalMs(1000); } catch (Exception ignored) {}
                    controller.startCountdown(3);
                } catch (IOException ex) { ex.printStackTrace(); }
            });
        }

        if (easyBtn != null) easyBtn.setOnAction(ev -> { if (loadGame != null) loadGame.accept("Easy"); });
        if (hardBtn != null) hardBtn.setOnAction(ev -> { if (loadGame != null) loadGame.accept("Hard"); });
    }

    private static void attachEffectsAndSounds(Result res, MainMenuAudioManager audioManager, Button... buttons) {
        try {
            final double expansion = 30;
            try {
                if (res.buttonEffectsHelper != null) {
                    for (Button b : buttons) {
                        try { if (b != null) res.buttonEffectsHelper.attachHoverEffects(b, expansion); } catch (Exception ignored) {}
                    }
                }

                for (Button b : buttons) {
                    try { if (res.buttonEffectsHelper != null) res.buttonEffectsHelper.attachButtonSoundHandlers(b, audioManager); } catch (Exception ignored) {}
                }

            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private static void normalizePrimaryButtonWidths(VBox mainButtons, Button multiPlayerBtn, Button singlePlayerBtn, Button settingsBtn) {
        try {
            Platform.runLater(() -> {
                try {
                    Button[] primary = new Button[] { multiPlayerBtn, singlePlayerBtn, settingsBtn };
                    double max = 0;
                    for (Button b : primary) {
                        if (b == null) continue;
                        double w = b.getWidth();
                        if (w <= 0) w = b.prefWidth(-1); // fallback
                        if (w > max) max = w;
                    }
                    if (max <= 0 && mainButtons != null) {
                        // fallback to container width minus some padding
                        double containerW = mainButtons.getWidth();
                        if (containerW > 0) max = Math.max(max, containerW * 0.7);
                    }
                    if (max > 0) {
                        for (Button b : primary) {
                            if (b == null) continue;
                            try { b.setPrefWidth(max); } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {}
    }

    private static void ensureStylesheetOnScene(StackPane rootStack, Consumer<javafx.scene.Scene> ensureMainMenuStylesheet) {
        Platform.runLater(() -> {
            try {
                javafx.scene.Scene s = null;
                if (rootStack != null) s = rootStack.getScene();
                if (s == null) s = null;
                if (ensureMainMenuStylesheet != null) ensureMainMenuStylesheet.accept(s);
            } catch (Exception ignored) {}
        });
    }
}
