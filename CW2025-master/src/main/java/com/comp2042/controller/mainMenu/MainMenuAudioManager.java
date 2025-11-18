package com.comp2042.controller.mainMenu;

import com.comp2042.audio.audioSettings.AudioSettings;
import javafx.beans.value.ChangeListener;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import java.net.URL;
import java.awt.Toolkit;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Manages menu audio: background music and UI SFX for the main menu.
 *
 * <p>Responsibilities:
 * - Load menu music and short audio clips (hover/click).
 * - Expose simple play/stop controls and apply mixed volumes from
 *   {@link com.comp2042.audio.audioSettings.AudioSettings}.
 * - Provide an audio-settings overlay UI integration method.
 *
 * Notes:
 * - The implementation prefers JavaFX {@link MediaPlayer}/{@link AudioClip}
 *   and falls back to a system beep when clips are unavailable.
 * - Most methods swallow exceptions to avoid breaking the UI thread.
 */
public class MainMenuAudioManager {
    private MediaPlayer menuMusicPlayer = null;
    private AudioClip hoverClip = null;
    private AudioClip clickClip = null;
    private boolean fallbackBeep = true;

    private ChangeListener<Number> masterVolListener = null;
    private ChangeListener<Number> musicVolListener = null;
    private ChangeListener<Number> sfxVolListener = null;

    /**
     * Load menu music and SFX resources using the provided class loader.
     *
     * @param loader class loader used to locate bundled audio resources.
     */
    public void loadAll(ClassLoader loader) {
        loadMenuMusic(loader);
        loadClips(loader);
        applyVolumes();
    }

    private void loadMenuMusic(ClassLoader loader) {
        try {
            URL musicUrl = loader.getResource("sounds/MainMenu.wav");
            if (musicUrl == null) musicUrl = MainMenuAudioManager.class.getResource("/sounds/MainMenu.wav");
            if (musicUrl != null) {
                try {
                    Media music = new Media(musicUrl.toExternalForm());
                    menuMusicPlayer = new MediaPlayer(music);
                    menuMusicPlayer.setCycleCount(MediaPlayer.INDEFINITE);
                    menuMusicPlayer.setAutoPlay(true);
                    menuMusicPlayer.setVolume(0.6);
                    menuMusicPlayer.setOnError(() -> System.err.println("Menu music error: " + menuMusicPlayer.getError()));
                } catch (Exception ex) {
                    System.err.println("[AudioManager] Failed to initialize menu music: " + ex);
                    ex.printStackTrace();
                }
            } else {
                System.out.println("[AudioManager] MainMenu.wav not found in resources (expected sounds/MainMenu.wav)");
            }
        } catch (Exception ex) {
            System.err.println("[AudioManager] Exception while loading menu music: " + ex);
            ex.printStackTrace();
        }
    }

    private void loadClips(ClassLoader loader) {
        try {
            URL h = loader.getResource("sounds/hover.wav");
            if (h != null) hoverClip = new AudioClip(h.toExternalForm());
        } catch (Exception ignored) {}
        try {
            URL c = loader.getResource("sounds/click.wav");
            if (c != null) clickClip = new AudioClip(c.toExternalForm());
        } catch (Exception ignored) {}
    }

    /**
     * Apply current volume settings from {@code AudioSettings} to loaded
     * players/clips.
     */
    public void applyVolumes() {
        try {
            double masterVol = AudioSettings.getMasterVolume();
            double musicVol = AudioSettings.getMusicVolume();
            double sfxVol = AudioSettings.getSfxVolume();
            double combinedMusic = masterVol * musicVol;
            double combinedSfx = masterVol * sfxVol;
            try { if (menuMusicPlayer != null) menuMusicPlayer.setVolume(combinedMusic); } catch (Exception ignored) {}
            try { if (hoverClip != null) hoverClip.setVolume(combinedSfx); } catch (Exception ignored) {}
            try { if (clickClip != null) clickClip.setVolume(combinedSfx); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    public void setMusicVolume(double vol) {
        try { if (menuMusicPlayer != null) menuMusicPlayer.setVolume(vol); } catch (Exception ignored) {}
    }

    public void setSfxVolume(double vol) {
        try { if (hoverClip != null) hoverClip.setVolume(vol); } catch (Exception ignored) {}
        try { if (clickClip != null) clickClip.setVolume(vol); } catch (Exception ignored) {}
    }

    /**
     * Register listeners on {@code AudioSettings} so changes are reflected
     * live in this manager.
     */
    public void registerListeners() {
        try {
            masterVolListener = (obs, o, n) -> applyVolumes();
            AudioSettings.masterProperty().addListener(masterVolListener);
        } catch (Exception ignored) {}
        try {
            musicVolListener = (obs, o, n) -> applyVolumes();
            AudioSettings.musicProperty().addListener(musicVolListener);
        } catch (Exception ignored) {}
        try {
            sfxVolListener = (obs, o, n) -> applyVolumes();
            AudioSettings.sfxProperty().addListener(sfxVolListener);
        } catch (Exception ignored) {}
    }

    /**
     * Remove listeners previously registered with {@code registerListeners}.
     */
    public void unregisterListeners() {
        try { if (masterVolListener != null) AudioSettings.masterProperty().removeListener(masterVolListener); } catch (Exception ignored) {}
        try { if (musicVolListener != null) AudioSettings.musicProperty().removeListener(musicVolListener); } catch (Exception ignored) {}
        try { if (sfxVolListener != null) AudioSettings.sfxProperty().removeListener(sfxVolListener); } catch (Exception ignored) {}
        masterVolListener = null; musicVolListener = null; sfxVolListener = null;
    }

    /**
     * Play the hover sound. If an {@code AudioClip} is unavailable a
     * platform beep may be used as a fallback.
     */
    public void playHover() {
        try {
            if (hoverClip != null) hoverClip.play();
            else if (fallbackBeep) Toolkit.getDefaultToolkit().beep();
        } catch (Exception ex) { System.err.println("Menu hover play failed: " + ex); }
    }

    /**
     * Play the click sound. If an {@code AudioClip} is unavailable a
     * platform beep may be used as a fallback.
     */
    public void playClick() {
        try {
            if (clickClip != null) clickClip.play();
            else if (fallbackBeep) Toolkit.getDefaultToolkit().beep();
        } catch (Exception ex) { System.err.println("Menu click play failed: " + ex); }
    }

    /**
     * Stop and dispose the menu music player if present.
     * After this call {@link #getMenuMusicPlayer()} will return {@code null}.
     */
    public void stopAndDisposeMusic() {
        try {
            if (menuMusicPlayer != null) {
                try { menuMusicPlayer.stop(); } catch (Exception ignored) {}
                try { menuMusicPlayer.dispose(); } catch (Exception ignored) {}
                menuMusicPlayer = null;
            }
        } catch (Exception ignored) {}
    }

    /**
     * Return the currently-loaded {@link MediaPlayer} for menu music, or
     * {@code null} when none is loaded.
     */
    public MediaPlayer getMenuMusicPlayer() { return menuMusicPlayer; }

    /**
     * Release all audio resources (music player and clips) and unregister
     * listeners. Safe to call multiple times.
     */
    public void cleanup() {
        stopAndDisposeMusic();
        try { if (hoverClip != null) { try { hoverClip.stop(); } catch (Exception ignored) {} hoverClip = null; } } catch (Exception ignored) {}
        try { if (clickClip != null) { try { clickClip.stop(); } catch (Exception ignored) {} clickClip = null; } } catch (Exception ignored) {}
        unregisterListeners();
    }

    /**
     * Show an audio-settings overlay allowing the user to preview and persist
     * master/music/sfx volume values. The provided callers are used to close
     * the overlay with the same animation helpers used elsewhere in the UI.
     *
     * @param loaderClassLoader used to locate any resources
     * @param rootStack root stack where the overlay will be added
     * @param settingsOptions the settings pane to hide/restore
     * @param closeOverlayCaller function that accepts (overlay, onFinished)
     * @param transitionCaller function that accepts (overlay) to play the
     *                         opening transition
     */
    public void showAudioSettings(ClassLoader loaderClassLoader,
                                  StackPane rootStack, StackPane settingsOptions,
                                  BiConsumer<StackPane, Runnable> closeOverlayCaller,
                                  Consumer<StackPane> transitionCaller) {
        try {
            StackPane overlay = new StackPane();
            overlay.getStyleClass().add("menu-overlay");
            Rectangle dark = new Rectangle();
            dark.setFill(javafx.scene.paint.Color.rgb(8,8,10,0.82));
            dark.getStyleClass().add("menu-overlay-dark");
            Platform.runLater(() -> {
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

            Text header = new Text("Audio");
            header.getStyleClass().add("menu-overlay-header");

            HBox actionBox = new HBox(10);
            actionBox.setAlignment(Pos.CENTER_RIGHT);
            Button btnReset = new Button("Reset");
            Button btnCancel = new Button("Cancel");
            Button btnSave = new Button("Save");
            btnReset.getStyleClass().add("menu-button"); btnCancel.getStyleClass().add("menu-button"); btnSave.getStyleClass().add("menu-button");
            actionBox.getChildren().addAll(btnReset, btnCancel, btnSave);

            BorderPane topBar = new BorderPane();
            topBar.setLeft(header);
            topBar.setRight(actionBox);
            topBar.getStyleClass().add("menu-overlay-topbar");
            container.setTop(topBar);

            VBox center = new VBox(14);
            center.getStyleClass().add("menu-overlay-center");
            center.setAlignment(Pos.TOP_LEFT);

            Label lMaster = new Label("Master Volume");
            lMaster.getStyleClass().add("menu-label");
            Slider sMaster = new Slider(0, 1, AudioSettings.getMasterVolume());
            sMaster.setShowTickLabels(false); sMaster.setShowTickMarks(true); sMaster.setBlockIncrement(0.05);

            Label lMusic = new Label("Music Volume");
            lMusic.getStyleClass().add("menu-label");
            Slider sMusic = new Slider(0, 1, AudioSettings.getMusicVolume());
            sMusic.setShowTickLabels(false); sMusic.setShowTickMarks(true); sMusic.setBlockIncrement(0.05);

            Label lSfx = new Label("SFX Volume");
            lSfx.getStyleClass().add("menu-label");
            Slider sSfx = new Slider(0, 1, AudioSettings.getSfxVolume());
            sSfx.setShowTickLabels(false); sSfx.setShowTickMarks(true); sSfx.setBlockIncrement(0.05);

            sMaster.valueProperty().addListener((obs, o, n) -> {
                try {
                    double combinedMusic = n.doubleValue() * sMusic.getValue();
                    double combinedSfx = n.doubleValue() * sSfx.getValue();
                    try { this.setMusicVolume(combinedMusic); } catch (Exception ignored) {}
                    try { this.setSfxVolume(combinedSfx); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            });
            sMusic.valueProperty().addListener((obs, o, n) -> {
                try {
                    double combinedMusic = sMaster.getValue() * n.doubleValue();
                    try { this.setMusicVolume(combinedMusic); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            });
            sSfx.valueProperty().addListener((obs, o, n) -> {
                try {
                    double combinedSfx = sMaster.getValue() * n.doubleValue();
                    try { this.setSfxVolume(combinedSfx); } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            });

            HBox ticksMaster = new HBox();
            Label tMaster0 = new Label("0");
            Label tMaster05 = new Label("0.5");
            Label tMaster1 = new Label("1");
            tMaster0.getStyleClass().add("menu-tick-large");
            tMaster05.getStyleClass().add("menu-tick-medium");
            tMaster1.getStyleClass().add("menu-tick-large");
            Region spacerMasterLeft = new Region();
            Region spacerMasterRight = new Region();
            HBox.setHgrow(spacerMasterLeft, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(spacerMasterRight, javafx.scene.layout.Priority.ALWAYS);
            ticksMaster.getChildren().addAll(tMaster0, spacerMasterLeft, tMaster05, spacerMasterRight, tMaster1);

            HBox ticksMusic = new HBox();
            Label tMusic0 = new Label("0");
            Label tMusic05 = new Label("0.5");
            Label tMusic1 = new Label("1");
            tMusic0.getStyleClass().add("menu-tick-large");
            tMusic05.getStyleClass().add("menu-tick-medium");
            tMusic1.getStyleClass().add("menu-tick-large");
            Region spacerMusicLeft = new Region();
            Region spacerMusicRight = new Region();
            HBox.setHgrow(spacerMusicLeft, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(spacerMusicRight, javafx.scene.layout.Priority.ALWAYS);
            ticksMusic.getChildren().addAll(tMusic0, spacerMusicLeft, tMusic05, spacerMusicRight, tMusic1);

            HBox ticksSfx = new HBox();
            Label tSfx0 = new Label("0");
            Label tSfx05 = new Label("0.5");
            Label tSfx1 = new Label("1");
            tSfx0.getStyleClass().add("menu-tick-large");
            tSfx05.getStyleClass().add("menu-tick-medium");
            tSfx1.getStyleClass().add("menu-tick-large");
            Region spacerSfxLeft = new Region();
            Region spacerSfxRight = new Region();
            HBox.setHgrow(spacerSfxLeft, javafx.scene.layout.Priority.ALWAYS);
            HBox.setHgrow(spacerSfxRight, javafx.scene.layout.Priority.ALWAYS);
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
                try { this.applyVolumes(); } catch (Exception ignored) {}
                closeOverlayCaller.accept(overlay, () -> {
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
                closeOverlayCaller.accept(overlay, () -> {
                    try { rootStack.getChildren().remove(overlay); } catch (Exception ignored) {}
                    try { if (settingsOptions != null) settingsOptions.setVisible(true); } catch (Exception ignored) {}
                });
            });

            overlay.getChildren().addAll(dark, container);
            try { if (settingsOptions != null) settingsOptions.setVisible(false); rootStack.getChildren().add(overlay); } catch (Exception ignored) {}
            transitionCaller.accept(overlay);

        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
