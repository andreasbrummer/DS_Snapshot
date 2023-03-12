package SnapshotLibrary;

import SnapshotLibrary.Messages.SnapMsg;

import java.io.Serializable;
import java.net.InetAddress;
import java.util.*;

public class Snapshot implements Serializable {

    private final UUID snapshotId;

    private final Serializable status;
    private final  Map<InetAddress, List<SnapMsg>> messages;



    public Snapshot( UUID snapshotId,
                     Serializable status,
                     Map<InetAddress, List<SnapMsg>> messages) {
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
