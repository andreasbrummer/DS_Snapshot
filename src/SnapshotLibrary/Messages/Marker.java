package SnapshotLibrary.Messages;

import java.util.Objects;
import java.util.UUID;

public class Marker implements SnapMsg {
    private final UUID snapshotId;
    public Marker(UUID snapshotId) {
        this.snapshotId = snapshotId;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Marker marker)) return false;
        return Objects.equals(getSnapshotId(), marker.getSnapshotId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSnapshotId());
    }

    @Override
    public String toString() {
        return "Marker{" +
                "snapshotId=" + snapshotId +
                '}';
    }

    @Override
    public String getMsgType() {
        return SnapEnum.MARKER.name();
    }
}
