package SnapshotLibrary;

import SnapshotLibrary.Messages.SnapMsg;
import org.apache.commons.lang3.tuple.Pair;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.*;

public class Snapshot implements Serializable {

    private  UUID snapshotId = null;

    private  Serializable status = null;
    private List<Pair<SocketAddress,Object>> node_messages_List = new ArrayList<Pair<SocketAddress,Object>>();
    private List<SocketAddress> connected_nodes = new ArrayList<SocketAddress>(); //contiene gli incoming nodes quando faccio partire lo snapshot



    public Snapshot(UUID snapshotId,
                    Serializable status, List<SocketAddress> connected_nodes) {
        this.snapshotId = snapshotId;
        this.status = status;
        this.connected_nodes = connected_nodes;
    }

    public boolean remove_from_node_address_list(SocketAddress node_address){
        connected_nodes.remove(node_address);
        return connected_nodes.isEmpty();
    }
    public List<SocketAddress> getConnected_nodes() {
        return connected_nodes;
    }
    public Serializable getStatus() {
        return status;
    }



    public UUID getSnapshotId() {
        return snapshotId;
    }

    public List<Pair<SocketAddress,Object>> getNode_messages_List() {
        return node_messages_List;
    }

    public void addNode_messages_List(SocketAddress nodeID, Object msg) {
        this.node_messages_List.add(Pair.of(nodeID, msg));
    }

    public void print_snapshot(){
        System.out.println("Snapshot ID: " + snapshotId);
        System.out.println("Status: " + status);
        System.out.println("Connected nodes: " + connected_nodes);
        System.out.println("Node messages: " + node_messages_List);
    }
}
