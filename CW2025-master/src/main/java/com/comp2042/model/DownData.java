package com.comp2042.model;

/**
 * Immutable result of a "down" operation for a player.
 *
 * This object groups any {@link ClearRow} information produced when rows are
 * cleared with the {@link ViewData} snapshot that renderers use to update the
 * display. Instances are immutable and safe to share between game logic and
 * rendering code.
 */
public final class DownData {
    private final ClearRow clearRow;
    private final ViewData viewData;

    public DownData(ClearRow clearRow, ViewData viewData) {
        this.clearRow = clearRow;
        this.viewData = viewData;
    }

    /**
     * @return information about rows cleared by the down operation, or
     *         {@code null} if no rows were cleared.
     */
    public ClearRow getClearRow() {
        return clearRow;
    }

    /**
     * @return a snapshot describing the board and active piece position after
     *         the down operation. The returned value should be treated as an
     *         immutable snapshot.
     */
    public ViewData getViewData() {
        return viewData;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DownData downData = (DownData) o;
        return java.util.Objects.equals(clearRow, downData.clearRow) &&
                java.util.Objects.equals(viewData, downData.viewData);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(clearRow, viewData);
    }

    @Override
    public String toString() {
        return "DownData{" +
                "clearRow=" + clearRow +
                ", viewData=" + viewData +
                '}';
    }
}
