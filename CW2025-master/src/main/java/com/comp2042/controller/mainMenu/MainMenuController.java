package com.comp2042.controller.mainMenu;

import com.comp2042.controller.gameControl.GameController;
import com.comp2042.controller.guiControl.GuiController;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
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
    @FXML private javafx.scene.layout.StackPane settingsOptions;
    @FXML private Button controlsBtn;
    @FXML private Button handlingBtn;
    @FXML private Button audioBtn;
    @FXML private Button settingsBackBtn;
    @FXML private StackPane controlsOptions;
    @FXML private Button singlePlayerConfigBtn;
    @FXML private Button multiPlayerConfigBtn;
    @FXML private Button controlsBackBtn;

    private MainMenuAudioManager audioManager = new MainMenuAudioManager();
    private MainMenuOverlayHelper overlayHelper;
    private KeyCode spLeft = null;
    private KeyCode spRight = null;
    private KeyCode spRotate = null;
    private KeyCode spDown = null;
    private KeyCode spHard = null;
    private KeyCode spSwitch = null;
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
    
    public void initialize() {
        MainMenuInitializer.Result r = MainMenuInitializer.initialize(
                this::loadHandlingSettings,
                this::loadControlSettings,
                bgImage,
                settingsBtn,
                settingsOptions,
                menuMediaView,
                mediaContainer,
                rootStack,
                mainButtons,
                titleText,
                singleOptions,
                singlePlayerBtn, multiPlayerBtn, backBtn,
                multiOptions, multiBackBtn,
                controlsOptions,
                controlsBtn, handlingBtn, audioBtn, settingsBackBtn, singlePlayerConfigBtn, multiPlayerConfigBtn, controlsBackBtn,
                scoreBattleBtn, classicBattleBtn, cooperateBattleBtn,
                normalBtn, easyBtn, hardBtn,
                audioManager,
                (mode) -> loadGame(mode),
                () -> stopMenuMusic(),
                () -> loadHandlingControls(),
                () -> loadAudioSettings(),
                () -> loadSinglePlayerControls(),
                () -> loadMultiplayerControls(),
                (pane) -> transitionTo(pane),
                (scene) -> ensureMainMenuStylesheet(scene)
        );
            try { this.overlayHelper = r.overlayHelper; } catch (Exception ignored) {}
    }

    private void loadAudioSettings() {
        try {
            audioManager.showAudioSettings(getClass().getClassLoader(), rootStack, settingsOptions,
                    (overlay, onFinished) -> closeOverlayWithAnimation(overlay, onFinished),
                    (overlay) -> transitionTo(overlay)
            );
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadHandlingControls() {
        try {
            MainMenuHandlingSettings helper = new MainMenuHandlingSettings();
            helper.showHandlingControls(getClass().getClassLoader(),
                    settingArrMs, settingDasMs, settingDcdMs, settingSdf, settingHardDropEnabled,
                    rootStack, settingsOptions,
                    (result) -> {
                        settingArrMs = result.settingArrMs; settingDasMs = result.settingDasMs; settingDcdMs = result.settingDcdMs; settingSdf = result.settingSdf; settingHardDropEnabled = result.settingHardDropEnabled;
                        try { saveHandlingSettings(); } catch (Exception ignored) {}
                    },
                    (overlay, onFinished) -> closeOverlayWithAnimation(overlay, onFinished),
                    (overlay) -> transitionTo(overlay)
            );
        } catch (Exception ex) { ex.printStackTrace(); }
    }

    private void loadSinglePlayerControls() {
        try {
            MainMenuControlsHelper helper = new MainMenuControlsHelper();
            helper.showSinglePlayerControls(getClass().getClassLoader(),
                    spLeft, spRight, spRotate, spDown, spHard, spSwitch,
                    rootStack, controlsOptions,
                    (result) -> {
                        spLeft = result.mpLeft_left; spRight = result.mpLeft_right; spRotate = result.mpLeft_rotate; spDown = result.mpLeft_down; spHard = result.mpLeft_hard; spSwitch = result.mpLeft_switch;
                        try { saveControlSettings(); } catch (Exception ignored) {}
                    },
                    (overlay, onFinished) -> closeOverlayWithAnimation(overlay, onFinished),
                    (overlay) -> transitionTo(overlay)
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void loadMultiplayerControls() {
        try {
            MainMenuControlsHelper helper = new MainMenuControlsHelper();
            helper.show(getClass().getClassLoader(),
                    mpLeft_left, mpLeft_right, mpLeft_rotate, mpLeft_down, mpLeft_hard, mpLeft_switch,
                    mpRight_left, mpRight_right, mpRight_rotate, mpRight_down, mpRight_hard, mpRight_switch,
                    rootStack, controlsOptions,
                    (result) -> {
                        mpLeft_left = result.mpLeft_left; mpLeft_right = result.mpLeft_right; mpLeft_rotate = result.mpLeft_rotate; mpLeft_down = result.mpLeft_down; mpLeft_hard = result.mpLeft_hard; mpLeft_switch = result.mpLeft_switch;
                        mpRight_left = result.mpRight_left; mpRight_right = result.mpRight_right; mpRight_rotate = result.mpRight_rotate; mpRight_down = result.mpRight_down; mpRight_hard = result.mpRight_hard; mpRight_switch = result.mpRight_switch;
                        try { saveControlSettings(); } catch (Exception ignored) {}
                    },
                    (overlay, onFinished) -> closeOverlayWithAnimation(overlay, onFinished),
                    (overlay) -> transitionTo(overlay)
            );
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
                    controller.setDropIntervalMs(800); 
                } else if ("Hard".equalsIgnoreCase(mode)) {
                    controller.setDropIntervalMs(200); 
                } else {
                    controller.setDropIntervalMs(500); 
                }
            } catch (Exception ignored) {}
            controller.startCountdown(3);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void transitionTo(StackPane toPane) {
        try { if (overlayHelper != null) overlayHelper.transitionTo(toPane); } catch (Exception ignored) {}
    }

    private void closeOverlayWithAnimation(StackPane fromPane, Runnable onFinished) {
        try { if (overlayHelper != null) overlayHelper.closeOverlayWithAnimation(fromPane, onFinished); else if (onFinished != null) onFinished.run(); } catch (Exception ignored) {}
    }

    private void stopMenuMusic() {
        try { audioManager.stopAndDisposeMusic(); } catch (Exception ignored) {}
    }

    public void cleanup() {
        try { audioManager.cleanup(); } catch (Exception ignored) {}
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

    private void saveControlSettings() {
        try {
            MainMenuPreferences mmp = new MainMenuPreferences();
            MainMenuControlSettings cs = new MainMenuControlSettings();
            cs.spLeft = spLeft; cs.spRight = spRight; cs.spRotate = spRotate; cs.spDown = spDown; cs.spHard = spHard; cs.spSwitch = spSwitch;
            cs.mpLeft_left = mpLeft_left; cs.mpLeft_right = mpLeft_right; cs.mpLeft_rotate = mpLeft_rotate; cs.mpLeft_down = mpLeft_down; cs.mpLeft_hard = mpLeft_hard; cs.mpLeft_switch = mpLeft_switch;
            cs.mpRight_left = mpRight_left; cs.mpRight_right = mpRight_right; cs.mpRight_rotate = mpRight_rotate; cs.mpRight_down = mpRight_down; cs.mpRight_hard = mpRight_hard; cs.mpRight_switch = mpRight_switch;
            mmp.saveControlSettings(cs);
        } catch (Exception ex) {
            try { System.err.println("Failed to save control settings: " + ex.getMessage()); } catch (Exception ignored) {}
        }
    }

    /** Load control key settings (single-player and multiplayer) from Preferences. */
    private void loadControlSettings() {
        try {
            MainMenuPreferences mmp = new MainMenuPreferences();
            MainMenuControlSettings cs = mmp.loadControlSettings();
            spLeft = cs.spLeft; spRight = cs.spRight; spRotate = cs.spRotate; spDown = cs.spDown; spHard = cs.spHard; spSwitch = cs.spSwitch;
            mpLeft_left = cs.mpLeft_left; mpLeft_right = cs.mpLeft_right; mpLeft_rotate = cs.mpLeft_rotate; mpLeft_down = cs.mpLeft_down; mpLeft_hard = cs.mpLeft_hard; mpLeft_switch = cs.mpLeft_switch;
            mpRight_left = cs.mpRight_left; mpRight_right = cs.mpRight_right; mpRight_rotate = cs.mpRight_rotate; mpRight_down = cs.mpRight_down; mpRight_hard = cs.mpRight_hard; mpRight_switch = cs.mpRight_switch;
        } catch (Exception ex) {
            try { System.err.println("Failed to load control settings: " + ex.getMessage()); } catch (Exception ignored) {}
        }
    }

    private void saveHandlingSettings() {
        try {
            MainMenuPreferences mmp = new MainMenuPreferences();
            MainMenuHandlingSettings hs = new MainMenuHandlingSettings();
            hs.settingArrMs = settingArrMs; hs.settingDasMs = settingDasMs; hs.settingDcdMs = settingDcdMs; hs.settingSdf = settingSdf; hs.settingHardDropEnabled = settingHardDropEnabled;
            mmp.saveHandlingSettings(hs);
        } catch (Exception ex) {
            try { System.err.println("Failed to save handling settings: " + ex.getMessage()); } catch (Exception ignored) {}
        }
    }

    private void loadHandlingSettings() {
        try {
            MainMenuPreferences mmp = new MainMenuPreferences();
            MainMenuHandlingSettings hs = mmp.loadHandlingSettings();
            settingArrMs = hs.settingArrMs; settingDasMs = hs.settingDasMs; settingDcdMs = hs.settingDcdMs; settingSdf = hs.settingSdf; settingHardDropEnabled = hs.settingHardDropEnabled;
        } catch (Exception ex) {
            try { System.err.println("Failed to load handling settings: " + ex.getMessage()); } catch (Exception ignored) {}
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