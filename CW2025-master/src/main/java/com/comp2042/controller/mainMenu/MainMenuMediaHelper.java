package com.comp2042.controller.mainMenu;

import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import java.net.URL;

/**
 * Helper responsible for loading and managing the preview video shown in
 * the main menu. The helper hides the media area when no suitable resource
 * is available or when initialization fails.
 */
public class MainMenuMediaHelper {
    private String menuMediaUrl;

    public void initMenuMedia(MediaView menuMediaView, StackPane mediaContainer) {
        /**
         * Initialize the menu preview media. Looks for several candidate files
         * in resources and starts a muted looping player when found.
         *
         * @param menuMediaView the MediaView to attach a MediaPlayer to
         * @param mediaContainer parent container used for sizing bindings
         */
        if (menuMediaView == null || mediaContainer == null) return;
        // Allow tests to force 'no media' behaviour by setting this system property
        try {
            if ("true".equals(System.getProperty("com.comp2042.test.noMenuMedia"))) {
                disableMenuMedia(mediaContainer, menuMediaView, "forced no media (test)");
                return;
            }
        } catch (Exception ignored) {}
        try {
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
                        disableMenuMedia(mediaContainer, menuMediaView, "MediaPlayer error: " + (mp.getError() != null ? mp.getError().getMessage() : "unknown"));
                    });
                    mp.setOnReady(() -> { try { System.out.println("Menu media ready: " + menuMediaUrl); } catch (Exception ignored) {} });
                    mp.setCycleCount(MediaPlayer.INDEFINITE);
                    mp.setAutoPlay(true);
                    mp.setMute(true);
                    menuMediaView.setMediaPlayer(mp);

                    Platform.runLater(() -> {
                        try {
                            menuMediaView.setPreserveRatio(false);
                            menuMediaView.fitHeightProperty().bind(mediaContainer.heightProperty().multiply(0.90));
                            menuMediaView.fitWidthProperty().bind(mediaContainer.widthProperty().multiply(0.98));
                        } catch (Exception ignored) {}
                    });
                } catch (Exception me) {
                    System.err.println("Failed to initialize menu media: " + me.getMessage());
                    disableMenuMedia(mediaContainer, menuMediaView, "Media init exception: " + me.getMessage());
                }
            } else {
                System.out.println("No preview video found in resources (tried menu.mp4, tetris_preview.mp4, preview.mp4)");
                disableMenuMedia(mediaContainer, menuMediaView, "no resource");
            }
        } catch (Exception e) {
            System.out.println("Failed to load preview media: " + e.getMessage());
        }
    }

    public void disableMenuMedia(StackPane mediaContainer, MediaView menuMediaView, String reason) {
        /**
         * Disable and clear menu preview media. This disposes any player and
         * hides the media container.
         *
         * @param mediaContainer container to hide
         * @param menuMediaView view to clear and detach player from
         * @param reason diagnostic reason printed to stderr
         */
        try { System.err.println("Menu media disabled: " + reason); } catch (Exception ignored) {}
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
        try { if (mediaContainer != null) mediaContainer.setVisible(false); } catch (Exception ignored) {}
    }

    public String getMenuMediaUrl() {
        /**
         * Return the selected menu media URL (external form) or {@code null}
         * if no media was found or initialization failed.
         */
        return menuMediaUrl;
    }
}
