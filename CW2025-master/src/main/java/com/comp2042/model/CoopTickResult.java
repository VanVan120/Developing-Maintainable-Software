package com.comp2042.model;

public final class CoopTickResult {
    private final boolean merged;
    private final DownData leftData; 
    private final DownData rightData;
    private final ClearRow clearRow; 

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
