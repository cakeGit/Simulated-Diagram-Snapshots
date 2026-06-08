package com.cake.sds.diagram;

public enum SnapshotResolution {
    PIXELATED(1),
    BLOCK_X4(4),
    BLOCK_X8(8),
    BLOCK_X16(16),
    BLOCK_X32(32),
    BLOCK_X64(64);

    private final int scale;

    SnapshotResolution(final int scale) {
        this.scale = scale;
    }

    public int scale() {
        return this.scale;
    }
}
