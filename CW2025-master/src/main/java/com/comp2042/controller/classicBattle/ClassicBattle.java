package com.comp2042.controller.classicBattle;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.media.MediaPlayer;
import com.comp2042.controller.GameController;
import com.comp2042.controller.GuiController;

import java.util.logging.Logger;
import java.util.logging.Level;

public class ClassicBattle implements Initializable {

    @FXML StackPane leftHolder;
    @FXML StackPane rightHolder;
    @FXML javafx.scene.layout.VBox leftNextBox;
    @FXML javafx.scene.layout.VBox leftNextContent;
    @FXML javafx.scene.text.Text leftNextLabel;
    @FXML javafx.scene.layout.VBox rightNextBox;
    @FXML javafx.scene.layout.VBox rightNextContent;
    @FXML javafx.scene.text.Text rightNextLabel;
    @FXML Button backBtn;

    GuiController leftGui;
    GuiController rightGui;
    GameController leftController;
    GameController rightController;
    javafx.scene.layout.StackPane centerOverlay;
    ClassicBattlePreviewService previewService;
    javafx.scene.media.MediaPlayer classicBattleMusicPlayer = null;
    ClassicBattleAudioHelper audioHelper;

    private ClassicBattleMatchManager matchManager;
    private javafx.scene.layout.StackPane activeOverlay = null;
    private javafx.animation.Animation activePulse = null;
    private static final Logger LOGGER = Logger.getLogger(ClassicBattle.class.getName());

    private static void safeRun(Runnable r, String ctx) {
        try {
            r.run();
        } catch (Exception ex) {
            LOGGER.log(Level.FINER, ctx, ex);
        }
    }

    private void safeRunLater(Runnable r, String ctx) {
        javafx.application.Platform.runLater(() -> safeRun(r, ctx));
    }

    private MediaPlayer stopAndDispose(MediaPlayer p) {
        return ClassicBattleAudioHelper.stopAndDispose(p);
    }

    void ensureClassicStylesheet(Scene scene) {
        if (scene == null) return;
        try {
            String css = getClass().getClassLoader().getResource("css/classic-battle.css").toExternalForm();
            if (!scene.getStylesheets().contains(css)) scene.getStylesheets().add(css);
        } catch (Exception ignored) {}
    }

    void hideEmbeddedUi(Parent root) {
        try {
            if (root == null) return;
            javafx.scene.Node n = root.lookup("#pauseBtn");
            if (n != null) { n.setVisible(false); n.setManaged(false); }
            n = root.lookup("#nextBoxFrame");
            if (n != null) { n.setVisible(false); n.setManaged(false); }
            javafx.scene.Node nb = root.lookup("#nextBox");
            if (nb != null) { nb.setVisible(false); nb.setManaged(false); }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (backBtn != null) {
            backBtn.setOnAction(this::onBack);
        }
        // create audio helper for music/sfx management
        audioHelper = new ClassicBattleAudioHelper(getClass());
        try {
            java.net.URL fontUrl = getClass().getClassLoader().getResource("digital.ttf");
            if (fontUrl != null) {
                javafx.scene.text.Font.loadFont(fontUrl.toExternalForm(), 38);
            }
        } catch (Exception ignored) {}
        try {
            if (leftNextLabel != null) leftNextLabel.getStyleClass().add("nextBrickLabel");
            if (rightNextLabel != null) rightNextLabel.getStyleClass().add("nextBrickLabel");
            try {
                javafx.scene.text.Font f = javafx.scene.text.Font.font("Let's go Digital", 26);
                if (leftNextLabel != null) {
                    leftNextLabel.setFont(f);
                    leftNextLabel.setFill(javafx.scene.paint.Color.YELLOW);
                }
                if (rightNextLabel != null) {
                    rightNextLabel.setFont(f);
                    rightNextLabel.setFill(javafx.scene.paint.Color.YELLOW);
                }
            } catch (Exception ignored2) {}
        } catch (Exception ignored) {}
    }

    public void restartMatch() {
        safeRunLater(() -> {
            try {
                classicBattleMusicPlayer = stopAndDispose(classicBattleMusicPlayer);
                try { audioHelper.stopMatchCountdownSound(); } catch (Exception ignored) {}
                try { audioHelper.stopMatchGameOverSound(); } catch (Exception ignored) {}
                try { if (previewService != null) previewService.stop(); } catch (Exception ignored) {}
                try { if (leftController != null) leftController.createNewGame(); } catch (Exception ignored) {}
                try { if (rightController != null) rightController.createNewGame(); } catch (Exception ignored) {}
                try { if (leftGui != null) leftGui.isGameOverProperty().set(false); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.isGameOverProperty().set(false); } catch (Exception ignored) {}
                try { if (leftGui != null) leftGui.startCountdown(3); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.startCountdown(3); } catch (Exception ignored) {}
                try { if (previewService != null) previewService.play(); } catch (Exception ignored) {}
                    try { if (matchManager != null) matchManager.clearMatchEnded(); } catch (Exception ignored) {}
                try {
                    if (activeOverlay != null) {
                        Scene s = leftHolder.getScene();
                        if (s != null && s.getRoot() instanceof javafx.scene.layout.Pane) {
                            javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) s.getRoot();
                            root.getChildren().remove(activeOverlay);
                        }
                        activeOverlay = null;
                        try { if (activePulse != null) { activePulse.stop(); activePulse = null; } } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        }, "restartMatch");
    }

    public void initBothGames(javafx.scene.input.KeyCode leftSwap, javafx.scene.input.KeyCode rightSwap) throws IOException {
        new ClassicBattleGameInitializer().initialize(this, leftSwap, rightSwap);
        try {
            if (leftGui != null) {
                leftGui.setMultiplayerRequestControlsHandler(requester -> {
                    try {
                        Scene scene = leftHolder.getScene();
                        if (scene == null) return;
                        ClassicBattleOverlayFactory factory = new ClassicBattleOverlayFactory(getClass());
                        factory.attachControlsOverlayToScene(scene, leftGui, rightGui);
                    } catch (Exception ignored) {}
                });
            }
        } catch (Exception ignored) {}
        try {
            if (rightGui != null) {
                rightGui.setMultiplayerRequestControlsHandler(requester -> {
                    try {
                        Scene scene = leftHolder.getScene();
                        if (scene == null) return;
                        ClassicBattleOverlayFactory factory = new ClassicBattleOverlayFactory(getClass());
                        factory.attachControlsOverlayToScene(scene, leftGui, rightGui);
                    } catch (Exception ignored) {}
                });
            }
        } catch (Exception ignored) {}
        try {
            final Runnable stopMusic = () -> {
                try {
                    if (classicBattleMusicPlayer != null) { classicBattleMusicPlayer.stop(); classicBattleMusicPlayer.dispose(); classicBattleMusicPlayer = null; }
                } catch (Exception ignored) {}
            };

            final java.util.function.BiConsumer<String, String> showWinner = (title, reason) -> {
                try {
                    Scene scene = leftHolder.getScene();
                    if (scene == null) return;
                    ClassicBattleOverlayFactory factory = new ClassicBattleOverlayFactory(getClass());
                    StackPane overlay = factory.attachWinnerOverlayToScene(scene, title, reason,
                        () -> {
                            try {
                                if (activeOverlay != null && activeOverlay.getParent() instanceof javafx.scene.layout.Pane) ((javafx.scene.layout.Pane) activeOverlay.getParent()).getChildren().remove(activeOverlay);
                            } catch (Exception ignored) {}
                            try { restartMatch(); } catch (Exception ignored) {}
                        },
                        () -> {
                            try {
                                if (activeOverlay != null && activeOverlay.getParent() instanceof javafx.scene.layout.Pane) ((javafx.scene.layout.Pane) activeOverlay.getParent()).getChildren().remove(activeOverlay);
                            } catch (Exception ignored) {}
                            try {
                                URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
                                if (loc == null) return;
                                FXMLLoader loader = new FXMLLoader(loc);
                                Parent menuRoot = loader.load();
                                Stage stage = (Stage) scene.getWindow();
                                if (stage.getScene() != null) {
                                    try { audioHelper.stopMatchGameOverSound(); } catch (Exception ignored) {}
                                    try { if (leftGui != null) leftGui.cleanup(); } catch (Exception ignored) {}
                                    try { if (rightGui != null) rightGui.cleanup(); } catch (Exception ignored) {}
                                    stage.getScene().setRoot(menuRoot);
                                } else {
                                    Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                                    stage.setScene(s2);
                                }
                            } catch (Exception ex) { ex.printStackTrace(); }
                        }
                    );
                    if (overlay == null) return;
                    try { if (activePulse != null) { activePulse.stop(); activePulse = null; } } catch (Exception ignored) {}
                    activeOverlay = overlay;
                    try {
                        Object ap = overlay.getProperties().get("activePulse");
                        if (ap instanceof javafx.animation.Animation) {
                            activePulse = (javafx.animation.Animation) ap;
                        }
                    } catch (Exception ignored) {}
                } catch (Exception ignored) {}
            };

            matchManager = new ClassicBattleMatchManager(leftGui, rightGui, previewService, audioHelper, stopMusic, this::restartMatch, showWinner);
            try { if (matchManager != null) matchManager.registerListeners(); } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private void onBack(ActionEvent ev) {
        try {
            URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
            if (loc == null) return;
            FXMLLoader loader = new FXMLLoader(loc);
            Parent menuRoot = loader.load();
            Stage stage = (Stage) backBtn.getScene().getWindow();
            if (stage.getScene() != null) {
                try { if (leftGui != null) leftGui.cleanup(); } catch (Exception ignored) {}
                try { if (rightGui != null) rightGui.cleanup(); } catch (Exception ignored) {}
                stage.getScene().setRoot(menuRoot);
                try {
                    String css = getClass().getClassLoader().getResource("css/menu.css").toExternalForm();
                    if (!stage.getScene().getStylesheets().contains(css)) stage.getScene().getStylesheets().add(css);
                } catch (Exception ignored) {}
            } else {
                Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                try {
                    String css = getClass().getClassLoader().getResource("css/menu.css").toExternalForm();
                    s2.getStylesheets().add(css);
                } catch (Exception ignored) {}
                stage.setScene(s2);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try { if (previewService != null) previewService.stop(); } catch (Exception ignored) {}
            try { if (classicBattleMusicPlayer != null) { classicBattleMusicPlayer.stop(); classicBattleMusicPlayer.dispose(); classicBattleMusicPlayer = null; } } catch (Exception ignored) {}
        try { audioHelper.stopMatchGameOverSound(); } catch (Exception ignored) {}
        try { audioHelper.stopMatchCountdownSound(); } catch (Exception ignored) {}
        }    
}