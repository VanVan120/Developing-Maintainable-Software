package com.comp2042;

/**
 * Result returned by CoopGameController.tick() describing merges and clear rows.
 */
public final class CoopTickResult {
    private final boolean merged;
    private final DownData leftData; // may be null if left did not merge this tick
    private final DownData rightData; // may be null if right did not merge this tick
    private final ClearRow clearRow; // may be null if no rows removed

    public CoopTickResult(boolean merged, DownData leftData, DownData rightData, ClearRow clearRow) {
        this.merged = merged;
        this.leftData = leftData;
        this.rightData = rightData;
        this.clearRow = clearRow;
    }

    public boolean isMerged() { return merged; }
    public DownData getLeftData() { return leftData; }
    public DownData getRightData() { return rightData; }
    public ClearRow getClearRow() { return clearRow; }
}
