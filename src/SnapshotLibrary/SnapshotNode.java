package SnapshotLibrary;

import SnapshotLibrary.Messages.Marker;
import SnapshotLibrary.Messages.SnapMsg;
import org.apache.commons.lang3.SerializationUtils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Saves the state of a node and its messages.
 */
public abstract class SnapshotNode {

     protected UUID lastSnapshotID;
     protected Serializable status;
     protected Set<InetAddress> connectedNodes;
     protected Map<UUID, Map<InetAddress, List<SnapMsg>>> receivedMessages;
     protected Map<UUID, Map<InetAddress, Boolean>> receivedMarkers;

    public SnapshotNode() {
        connectedNodes = new HashSet<>();
        receivedMessages = new HashMap<>();
        receivedMarkers = new HashMap<>();
    }

    public SnapshotNode(Snapshot Snapshot) {

    }

    protected void startSnapshot(Marker msg, InetAddress receiver) throws IOException {
        /* send msg to receiver */
        Socket socket = new Socket(receiver, 30181);
        OutputStream output = socket.getOutputStream();
        byte[] data = SerializationUtils.serialize(msg);
        output.write(data);
        socket.close();
    }
    protected void receive(SnapMsg msg, InetAddress sender) {
        if(msg instanceof Marker) {
            UUID snapshotId = ((Marker) msg).getSnapshotId();
            if (!receivedMarkers.containsKey(snapshotId)) {
                receivedMarkers.put(snapshotId, connectedNodes.stream().collect(Collectors.toMap(node -> node, node -> false)));
                receivedMarkers.get(snapshotId).put(sender, true);
                receivedMessages.put(snapshotId, connectedNodes.stream().collect(Collectors.toMap(node -> node, node -> new ArrayList<>())));
            } else {
                receivedMarkers.get(snapshotId).put(sender, true);
                if (receivedMarkers.get(snapshotId).entrySet().stream().allMatch(Map.Entry::getValue)) {
                    Serializable localSnapshot  = new Snapshot(snapshotId, status, receivedMessages.get(snapshotId));
                    try (FileOutputStream out = new FileOutputStream("snapshot_" + snapshotId.toString())) {
                        out.write(SerializationUtils.serialize(localSnapshot));
                    } catch (IOException e) {
                        throw new RuntimeException(e);

                    }
                    receivedMarkers.remove(snapshotId);
                    receivedMessages.remove(snapshotId);
                }
            }
        } else {
            receivedMarkers.forEach((snapshotId, markers) -> {
                if(!receivedMarkers.get(snapshotId).get(sender)) {
                    receivedMessages.get(snapshotId).get(sender).add(msg);
                }
            });
        }
    }

    private void evaluate(SnapMsg msg, InetAddress sender) {
    }


    public void addNewNode(InetAddress node) {
        connectedNodes.add(node);
    }
    public void removeNode(InetAddress node) {
        connectedNodes.remove(node);
    }
}