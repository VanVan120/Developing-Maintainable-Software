package com.comp2042.logic;

import javafx.scene.paint.Color;

/**
 * Enum of brick identity and their display color.
 *
 * <p>Used by rendering and UI code to map a brick type to a display colour.
 */
public enum BrickShape {
    I(Color.CYAN),
    J(Color.BLUE),
    L(Color.ORANGE),
    O(Color.YELLOW),
    S(Color.GREEN),
    T(Color.PURPLE),
    Z(Color.RED);

    private final Color color;

    BrickShape(Color color) {
        this.color = color;
    }

    /**
     * Return the display colour associated with this shape.
     *
     * @return non-null {@link Color} used for rendering
     */
    public Color getColor() {
        return color;
    }
}
