package com.cake.sds.diagram.snapshot;

import com.cake.sds.diagram.CameraMode;
import com.cake.sds.diagram.SnapshotFrame;
import com.cake.sds.diagram.SnapshotResolution;
import com.cake.sds.diagram.SnapshotStyle;

public class SnapshotSettings {
    public CameraMode cameraMode = CameraMode.NORMAL;
    public SnapshotStyle snapshotStyle = SnapshotStyle.DIAGRAM;
    public SnapshotFrame frame = SnapshotFrame.NORMAL;
    public SnapshotResolution resolution = SnapshotResolution.PIXELATED;
}
