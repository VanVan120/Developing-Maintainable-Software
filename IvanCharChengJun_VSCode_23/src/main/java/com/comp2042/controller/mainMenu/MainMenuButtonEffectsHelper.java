package com.comp2042.controller.mainMenu;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

/**
 * Provides visual hover animations and sound wiring for menu buttons.
 *
 * <p>Small utility used by {@link MainMenuInitializer} to keep UI polish
 * behaviour centrally defined.
 */
public class MainMenuButtonEffectsHelper {
    /**
     * Attach a subtle translation and drop-shadow animation to the given
     * button when the mouse enters/exits.
     *
     * @param b the button to enhance; ignored if {@code null}
     * @param expansion horizontal translate distance (px) applied on hover
     */
    public void attachHoverEffects(Button b, double expansion) {
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
                DropShadow ds = new DropShadow(6, 0, 4, javafx.scene.paint.Color.rgb(0,0,0,0.28));
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

    /**
     * Wire mouse-enter/press/action events to the provided
     * {@link MainMenuAudioManager} so hover and click sounds are played.
     *
     * @param btn target button (ignored if {@code null})
     * @param audioManager audio manager used to play hover/click sounds
     */
    public void attachButtonSoundHandlers(Button btn, MainMenuAudioManager audioManager) {
        if (btn == null) return;
        try {
            btn.addEventHandler(MouseEvent.MOUSE_ENTERED, e -> { try { audioManager.playHover(); } catch (Exception ignored) {} });
            btn.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> { try { audioManager.playClick(); } catch (Exception ignored) {} });
            btn.addEventHandler(javafx.event.ActionEvent.ACTION, e -> { try { audioManager.playClick(); } catch (Exception ignored) {} });
        } catch (Exception ignored) {}
    }
}
