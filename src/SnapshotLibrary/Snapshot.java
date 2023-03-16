package SnapshotLibrary;

import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.net.SocketAddress;
import java.util.*;

public class Snapshot implements Serializable {

    private UUID snapshotId;
    private Serializable snapshotStatus;
    private List<Pair<SocketAddress, Object>> nodeMessages = new ArrayList<>();
    private List<SocketAddress> connectedNodes = new ArrayList<>();

    public Snapshot(UUID snapshotId, Serializable snapshotStatus, List<SocketAddress> connectedNodes) {
        this.snapshotId = snapshotId;
        this.snapshotStatus = snapshotStatus;
        this.connectedNodes = connectedNodes;
    }

    public boolean removeFromNodeAddressList(SocketAddress nodeAddress) {
        connectedNodes.remove(nodeAddress);
        return connectedNodes.isEmpty();
    }

    public List<SocketAddress> getConnectedNodes() {
        return connectedNodes;
    }

    public Serializable getSnapshotStatus() {
        return snapshotStatus;
    }

    public UUID getSnapshotId() {
        return snapshotId;
    }

    public List<Pair<SocketAddress, Object>> getNodeMessages() {
        return nodeMessages;
    }

    public void addNodeMessage(SocketAddress nodeAddress, Object message) {
        nodeMessages.add(Pair.of(nodeAddress, message));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Snapshot ID: ").append(snapshotId)
                .append(" Snapshot status: ").append(snapshotStatus)
                .append(" Node messages:\n");
        nodeMessages.forEach(message -> sb.append("Sender: ").append(message.getLeft().toString())
                .append(" Content: ").append(message.getRight().toString())
                .append("\n"));
        return sb.toString();
    }

}
