package SnapshotLibrary;

import SnapshotLibrary.Messages.SnapMsg;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.*;

public class Snapshot implements Serializable {
    @NonNull
    private final UUID snapshotId;
    @NonNull
    private final Serializable status;
    private final @NonNull Map<InetAddress, List<SnapMsg>> messages;



    public Snapshot(@NonNull UUID snapshotId,
                    @NonNull Serializable status,
                    @NonNull Map<InetAddress, List<SnapMsg>> messages) {
        this.snapshotId = snapshotId;
        this.status = status;
        this.messages = messages;
    }

    public Serializable getStatus() {
        return status;
    }

    public Map<InetAddress, List<SnapMsg>> getMessages() {
        return new HashMap<>(messages);
    }

    public Set<InetAddress> getConnectedNodes() {
        return messages.keySet();
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Snapshot snapshot)) return false;
        return getSnapshotId().equals(snapshot.getSnapshotId()) && getStatus().equals(snapshot.getStatus()) && getMessages().equals(snapshot.getMessages());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSnapshotId(), getStatus(), getMessages());
    }

    @Override
    public String toString() {
        return "SavedLocalSnapshot{\n" +
                "\tsnapshotId=" + snapshotId +
                ",\n\tstatus=" + status +
                ",\n\tmessages=" + messages +
                "\n}";
    }
}
