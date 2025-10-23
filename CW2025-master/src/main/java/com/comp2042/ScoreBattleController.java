package com.comp2042;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.geometry.Pos;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ScoreBattleController implements Initializable {

    @FXML
    private StackPane leftHolder;

    @FXML
    private StackPane rightHolder;

    // optional external preview containers (added to scoreBattleLayout.fxml)
    @FXML
    private javafx.scene.layout.VBox leftNextBox;
    @FXML
    private javafx.scene.layout.VBox leftNextContent;
    @FXML
    private javafx.scene.text.Text leftNextLabel;

    @FXML
    private javafx.scene.layout.VBox rightNextBox;
    @FXML
    private javafx.scene.layout.VBox rightNextContent;
    @FXML
    private javafx.scene.text.Text rightNextLabel;

    @FXML
    private Button backBtn;

    private GuiController leftGui;
    private GuiController rightGui;
    // keep controller handles so we can query/stop games
    private GameController leftController;
    private GameController rightController;

    // match UI elements
    private javafx.scene.layout.StackPane centerOverlay;
    private javafx.scene.text.Text matchTimerText;
    private javafx.scene.text.Text matchScoreText;
    private javafx.animation.Timeline matchTimer;
    private int remainingSeconds = 5 * 60; // 5 minutes

    // timeline to poll and update per-player next previews
    private javafx.animation.Timeline previewPoller;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // nothing here; will load children when scene is shown
        if (backBtn != null) {
            backBtn.setOnAction(this::onBack);
        }
        // Load the digital font used by single-player so the Next: label matches exactly
        try {
            java.net.URL fontUrl = getClass().getClassLoader().getResource("digital.ttf");
            if (fontUrl != null) {
                javafx.scene.text.Font.loadFont(fontUrl.toExternalForm(), 38);
            }
        } catch (Exception ignored) {}
        // ensure the external labels use the same CSS class as single-player
        try {
            if (leftNextLabel != null) leftNextLabel.getStyleClass().add("nextBrickLabel");
            if (rightNextLabel != null) rightNextLabel.getStyleClass().add("nextBrickLabel");
            // Also apply inline styling to guarantee parity even if stylesheet wasn't applied
            try {
                javafx.scene.text.Font f = javafx.scene.text.Font.font("Let's go Digital", 26);
                if (leftNextLabel != null) {
                    leftNextLabel.setFont(f);
                    leftNextLabel.setFill(javafx.scene.paint.Color.YELLOW);
                    leftNextLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 6, 0.0, 0, 2); -fx-font-weight: bold;");
                }
                if (rightNextLabel != null) {
                    rightNextLabel.setFont(f);
                    rightNextLabel.setFill(javafx.scene.paint.Color.YELLOW);
                    rightNextLabel.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.75), 6, 0.0, 0, 2); -fx-font-weight: bold;");
                }
            } catch (Exception ignored2) {}
        } catch (Exception ignored) {}
    }

    // Called by Menu to initialize and start both games
    public void initBothGames() throws IOException {
        // load left game layout
        URL gameLayout = getClass().getClassLoader().getResource("gameLayout.fxml");
        FXMLLoader leftLoader = new FXMLLoader(gameLayout);
        Parent leftRoot = leftLoader.load();
        leftGui = leftLoader.getController();

        // load right game layout
        FXMLLoader rightLoader = new FXMLLoader(gameLayout);
        Parent rightRoot = rightLoader.load();
        rightGui = rightLoader.getController();

        // Ensure embedded roots do NOT apply the global '.root' background or load their own stylesheets
        try {
            // remove any stylesheet that would apply the scenic background per-root
            leftRoot.getStylesheets().clear();
            rightRoot.getStylesheets().clear();
            leftRoot.getStyleClass().remove("root");
            rightRoot.getStyleClass().remove("root");
            // force transparent background so the scene root's scenic image is visible once behind both boards
            String transparent = "-fx-background-color: transparent;";
            leftRoot.setStyle(transparent);
            rightRoot.setStyle(transparent);
        } catch (Exception ignored) {}

        // Hide UI chrome that shouldn't appear in multiplayer: Back button (scene-level) and
        // per-board Pause / Next-box elements that create thin vertical separators.
        try {
            if (backBtn != null) {
                backBtn.setVisible(false);
                backBtn.setManaged(false);
            }

            // helper to hide several nodes by id inside an embedded root
            java.util.function.Consumer<Parent> hideEmbeddedUi = (root) -> {
                // hide pause button, decorative frame and embedded next box if present
                try {
                    javafx.scene.Node n = root.lookup("#pauseBtn");
                    if (n != null) { n.setVisible(false); n.setManaged(false); }
                    n = root.lookup("#nextBoxFrame");
                    if (n != null) { n.setVisible(false); n.setManaged(false); }
                    javafx.scene.Node nb = root.lookup("#nextBox");
                    if (nb != null) { nb.setVisible(false); nb.setManaged(false); }
                } catch (Exception e) {
                    // log at least to stderr during maintenance to surface unexpected UI lookup issues
                    e.printStackTrace();
                }
            };

            hideEmbeddedUi.accept(leftRoot);
            hideEmbeddedUi.accept(rightRoot);
        } catch (Exception ignored) {}

        // attempt to measure the internal gameBoard node so we can size the SubScene to match single-player
        double measuredW = -1, measuredH = -1;
        try {
            leftRoot.applyCss();
            leftRoot.layout();
            // try lookup by fx:id first, then by style class
            javafx.scene.Node gb = leftRoot.lookup("#gameBoard");
            if (gb == null) gb = leftRoot.lookup(".gameBoard");
            if (gb != null) {
                measuredW = gb.prefWidth(-1);
                measuredH = gb.prefHeight(-1);
                if (measuredW <= 0) measuredW = gb.getLayoutBounds().getWidth();
                if (measuredH <= 0) measuredH = gb.getLayoutBounds().getHeight();
            }
        } catch (Exception ignored) {}

        double initialW = (measuredW > 0) ? measuredW : 400;
        double initialH = (measuredH > 0) ? measuredH : 640;

        // create SubScenes sized to the measured single-player board area and center them in the holders
        // Do NOT apply the scenic background to each embedded root. Instead, the Score Battle scene's root
        // should hold the single background image so it appears once behind both playfields. We ensure the
        // embedded roots remain transparent and have no stylesheets so they don't draw or re-apply backgrounds.

        SubScene leftSub = new SubScene(leftRoot, initialW, initialH);
        leftSub.setFill(Color.TRANSPARENT);
        SubScene rightSub = new SubScene(rightRoot, initialW, initialH);
        rightSub.setFill(Color.TRANSPARENT);

        // center subscenes in their holders
        leftHolder.setAlignment(Pos.CENTER);
        rightHolder.setAlignment(Pos.CENTER);

        // add to holders
        leftHolder.getChildren().add(leftSub);
        rightHolder.getChildren().add(rightSub);

        // ensure the loaded root Pane matches the subscene size so GuiController's layout measurements work
        if (leftRoot instanceof Pane) {
            Pane lp = (Pane) leftRoot;
            lp.prefWidthProperty().bind(leftSub.widthProperty());
            lp.prefHeightProperty().bind(leftSub.heightProperty());
        }
        if (rightRoot instanceof Pane) {
            Pane rp = (Pane) rightRoot;
            rp.prefWidthProperty().bind(rightSub.widthProperty());
            rp.prefHeightProperty().bind(rightSub.heightProperty());
        }

    // start controllers and keep references
    leftController = new GameController(leftGui);
    rightController = new GameController(rightGui);

        // set level text or drop intervals if desired
        leftGui.setLevelText("Score Battle");
        rightGui.setLevelText("Score Battle");

        leftGui.setDropIntervalMs(1000);
        rightGui.setDropIntervalMs(1000);

        // start countdowns for both
        leftGui.startCountdown(3);
        rightGui.startCountdown(3);

        // configure per-player controls so each board only responds to its assigned keys
        try {
            // left player: W=rotate, A=left, S=soft-drop, D=right, SHIFT=hard-drop
            leftGui.setControlKeys(javafx.scene.input.KeyCode.A, javafx.scene.input.KeyCode.D, javafx.scene.input.KeyCode.W, javafx.scene.input.KeyCode.S, javafx.scene.input.KeyCode.SHIFT);
            // left player swap key = Q
            leftGui.setSwapKey(javafx.scene.input.KeyCode.Q);
            // right player: Left=left, Right=right, Up=rotate, Down=soft-drop, Space=hard-drop
            rightGui.setControlKeys(javafx.scene.input.KeyCode.LEFT, javafx.scene.input.KeyCode.RIGHT, javafx.scene.input.KeyCode.UP, javafx.scene.input.KeyCode.DOWN, javafx.scene.input.KeyCode.SPACE);
            // right player swap key = C
            rightGui.setSwapKey(javafx.scene.input.KeyCode.C);
        } catch (Exception ignored) {}

        // create a centered overlay showing remaining time and combined scores
        try {
            javafx.application.Platform.runLater(() -> {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;
                centerOverlay = new javafx.scene.layout.StackPane();
                centerOverlay.setPickOnBounds(false);
                // overlay should not block gameplay input while match is running
                centerOverlay.setMouseTransparent(true);

                javafx.scene.layout.VBox v = new javafx.scene.layout.VBox(6); // tighter vertical spacing for top layout
                v.setAlignment(javafx.geometry.Pos.TOP_CENTER);

                matchTimerText = new javafx.scene.text.Text(formatTime(remainingSeconds));
                // slightly smaller font to avoid overlap on typical resolutions
                matchTimerText.setStyle("-fx-font-size: 56px; -fx-fill: yellow; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 6, 0.0, 0, 2);");

                matchScoreText = new javafx.scene.text.Text("0  —  0");
                matchScoreText.setStyle("-fx-font-size: 36px; -fx-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.85), 6, 0.0, 0, 2);");

                v.getChildren().addAll(matchTimerText, matchScoreText);
                centerOverlay.getChildren().add(v);

                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    root.getChildren().add(centerOverlay);
                    // make overlay fill the scene width so StackPane alignment centers children correctly
                    centerOverlay.prefWidthProperty().bind(scene.widthProperty());
                    centerOverlay.prefHeightProperty().bind(scene.heightProperty());
                    // position at top-center with a small top margin (6% down the window)
                    StackPane.setAlignment(v, javafx.geometry.Pos.TOP_CENTER);
                    v.translateYProperty().bind(scene.heightProperty().multiply(0.06));
                }
            });
        } catch (Exception ignored) {}

        // start match timer which updates every second (but only after both countdowns complete)
        matchTimer = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.seconds(1), _e -> {
            // reference the event to avoid unused-parameter warnings in some compilers
            java.util.Objects.requireNonNull(_e);
            remainingSeconds--;
            if (matchTimerText != null) matchTimerText.setText(formatTime(remainingSeconds));
            updateMatchScoreText();
            if (remainingSeconds <= 0) {
                matchTimer.stop();
                endMatchAndAnnounceWinner();
            }
        }));
        matchTimer.setCycleCount(javafx.animation.Animation.INDEFINITE);

        // Only play the match timer once both embedded UIs have finished their start countdowns
        try {
            final javafx.beans.property.BooleanProperty leftDone = leftGui.countdownFinishedProperty();
            final javafx.beans.property.BooleanProperty rightDone = rightGui.countdownFinishedProperty();
            javafx.beans.InvalidationListener startWhenReady = _obs -> {
                // reference parameter so linters/compiler don't complain about unused lambda args
                java.util.Objects.requireNonNull(_obs);
                try {
                    if (leftDone.get() && rightDone.get()) {
                        matchTimer.play();
                    }
                } catch (Exception ignored) {}
            };
            leftDone.addListener(startWhenReady);
            rightDone.addListener(startWhenReady);
            // also check immediately in case both already finished
            if (leftDone.get() && rightDone.get()) matchTimer.play();
        } catch (Exception ignored) {
            // fallback: start immediately
            matchTimer.play();
        }

        // start a small poller to refresh each player's next-three previews from their GameController
        try {
            // Timeline handlers already run on the JavaFX Application Thread, so we can safely
            // update UI elements directly from the handler. Avoid extra Platform.runLater calls
            // which can cause unnecessary queuing under load.
            previewPoller = new javafx.animation.Timeline(new javafx.animation.KeyFrame(javafx.util.Duration.millis(300), _ev -> {
                try {
                    // reference the event to satisfy compilers that flag unused lambda parameters
                    java.util.Objects.requireNonNull(_ev);
                    if (leftController != null && leftGui != null) {
                        java.util.List<com.comp2042.logic.bricks.Brick> up = leftController.getUpcomingBricks(3);
                        if (leftNextContent != null) {
                            javafx.scene.layout.VBox built = leftGui.buildNextPreview(up);
                            leftNextContent.getChildren().clear();
                            if (built != null) leftNextContent.getChildren().addAll(built.getChildren());
                        } else {
                            if (up != null) leftGui.showNextBricks(up);
                        }
                    }
                    if (rightController != null && rightGui != null) {
                        java.util.List<com.comp2042.logic.bricks.Brick> up2 = rightController.getUpcomingBricks(3);
                        if (rightNextContent != null) {
                            javafx.scene.layout.VBox built2 = rightGui.buildNextPreview(up2);
                            rightNextContent.getChildren().clear();
                            if (built2 != null) rightNextContent.getChildren().addAll(built2.getChildren());
                        } else {
                            if (up2 != null) rightGui.showNextBricks(up2);
                        }
                    }
                } catch (Exception e) {
                    // log unexpected exceptions during preview update to aid debugging
                    e.printStackTrace();
                }
            }));
            previewPoller.setCycleCount(javafx.animation.Animation.INDEFINITE);
            previewPoller.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onBack(ActionEvent ev) {
        // return to main menu by replacing the current scene root with mainMenu.fxml
        try {
            URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
            if (loc == null) return;
            FXMLLoader loader = new FXMLLoader(loc);
            Parent menuRoot = loader.load();
            Stage stage = (Stage) backBtn.getScene().getWindow();
            if (stage.getScene() != null) {
                stage.getScene().setRoot(menuRoot);
                // restore shared menu stylesheet (if available) so buttons keep their look
                try {
                    String css = getClass().getClassLoader().getResource("menu.css").toExternalForm();
                    if (!stage.getScene().getStylesheets().contains(css)) stage.getScene().getStylesheets().add(css);
                } catch (Exception ignored) {}
            } else {
                Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                try {
                    String css = getClass().getClassLoader().getResource("menu.css").toExternalForm();
                    s2.getStylesheets().add(css);
                } catch (Exception ignored) {}
                stage.setScene(s2);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}
    }

    private String formatTime(int seconds) {
        int mins = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", mins, secs);
    }

    private void updateMatchScoreText() {
        try {
            int ls = (leftController != null) ? leftController.getScoreProperty().get() : 0;
            int rs = (rightController != null) ? rightController.getScoreProperty().get() : 0;
            if (matchScoreText != null) matchScoreText.setText(String.format("%d  —  %d", ls, rs));
        } catch (Exception ignored) {}
    }

    private void endMatchAndAnnounceWinner() {
        // stop both games
        try { if (leftGui != null) leftGui.gameOver(); } catch (Exception ignored) {}
        try { if (rightGui != null) rightGui.gameOver(); } catch (Exception ignored) {}
        try { if (previewPoller != null) previewPoller.stop(); } catch (Exception ignored) {}

        // determine winner
        int ls = (leftController != null) ? leftController.getScoreProperty().get() : 0;
        int rs = (rightController != null) ? rightController.getScoreProperty().get() : 0;
        String title;
        if (ls > rs) title = "Left Player Wins!";
        else if (rs > ls) title = "Right Player Wins!";
        else title = "Draw!";

        // create an impressive animated announcement overlay
        javafx.application.Platform.runLater(() -> {
            try {
                Scene scene = leftHolder.getScene();
                if (scene == null) return;
                StackPane overlay = new StackPane();
                overlay.setPickOnBounds(true);
                overlay.setStyle("-fx-background-color: rgba(0,0,0,0.6);");

                VBox dialog = new VBox(12);
                dialog.setAlignment(Pos.CENTER);

                javafx.scene.text.Text titleText = new javafx.scene.text.Text(title);
                titleText.setStyle("-fx-font-size: 72px; -fx-fill: gold; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 12, 0.0, 0, 4);");

                javafx.scene.text.Text scoreText = new javafx.scene.text.Text(String.format("%d : %d", ls, rs));
                scoreText.setStyle("-fx-font-size: 48px; -fx-fill: white; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.9), 8, 0.0, 0, 3);");

                Button btnMenu = new Button("Main Menu");
                btnMenu.getStyleClass().add("menu-button");
                btnMenu.setOnAction(ev -> {
                    ev.consume();
                    try {
                        URL loc = getClass().getClassLoader().getResource("mainMenu.fxml");
                        if (loc == null) return;
                        FXMLLoader loader = new FXMLLoader(loc);
                        Parent menuRoot = loader.load();
                        Stage stage = (Stage) scene.getWindow();
                        if (stage.getScene() != null) {
                            stage.getScene().setRoot(menuRoot);
                        } else {
                            Scene s2 = new Scene(menuRoot, Math.max(420, stage.getWidth()), Math.max(700, stage.getHeight()));
                            stage.setScene(s2);
                        }
                    } catch (Exception ex) { ex.printStackTrace(); }
                });

                dialog.getChildren().addAll(titleText, scoreText, btnMenu);
                overlay.getChildren().add(dialog);

                if (scene.getRoot() instanceof javafx.scene.layout.Pane) {
                    javafx.scene.layout.Pane root = (javafx.scene.layout.Pane) scene.getRoot();
                    root.getChildren().add(overlay);
                }

                // play pop animations
                try {
                    titleText.setScaleX(0.2); titleText.setScaleY(0.2); titleText.setOpacity(0);
                    scoreText.setOpacity(0);
                    javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(700), titleText);
                    st.setToX(1.0); st.setToY(1.0);
                    javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(700), titleText);
                    ft.setToValue(1.0);
                    st.play(); ft.play();
                    javafx.animation.FadeTransition ft2 = new javafx.animation.FadeTransition(javafx.util.Duration.millis(600), scoreText);
                    ft2.setDelay(javafx.util.Duration.millis(500)); ft2.setToValue(1.0); ft2.play();
                } catch (Exception ignored) {}
            } catch (Exception ignored) {}
        });
    }
}
