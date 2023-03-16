package SnapshotLibrary;

import SnapshotLibrary.Messages.Marker;
import SnapshotLibrary.Messages.SnapMsg;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;


    /*
    * @author: Andreas Brummer
    * @author: Francesco Caserta
    * @date: 2023-03-16
    * @version: 1.0
    * @description: DistributedSnapshot class
    * @note: This class is used to create a distributed snapshot of a distributed system.
    * @note: The class is used to create a snapshot of the distributed system, and to store it in a file.
    * @note: The class is also used to retrieve a snapshot from a file.
    * @note: The class is also used to delete a snapshot from a file.
    * @param: serverPortNumber: port number for the server
    * @param: status: current status of the distributed system
    * @param: server: server object
    * @param: path: path to the directory where snapshots will be stored
    * @param: input_nodes: list of input nodes
    * @param: snapshots: map of all snapshots currently in progress
    * @param: output_nodes: map of all output nodes, using UUID as key and Socket object as value
    * @param: output_stream: map of all output streams, using UUID as key and ObjectOutputStream object as value
    * @param: SNAPSHOT_START_DELAY_MS: only for testing
    * */

public class DistributedSnapshot{
    private int serverPortNumber;
    private Serializable status;
    private Server server;
    private Path path;
    private List<SocketAddress> input_nodes = new ArrayList<SocketAddress>();
    private Map<UUID,Snapshot> snapshots = new HashMap<>();
    Map<UUID,Socket> output_nodes = new HashMap<>();
    Map<UUID,ObjectOutputStream> output_stream = new HashMap<>();


    /*  only for testing
        delay (in milliseconds) before a snapshot is started */
    private static final int SNAPSHOT_START_DELAY_MS = 10000;

    /*  only for testing */
    private static boolean TEST_MODE = true;

    public DistributedSnapshot(Path path) {
        this.path = path;
    }
    public DistributedSnapshot(String folderName) {
        this.path = Storage.createFolder(folderName);
    }
    public DistributedSnapshot() {
        this.path = Storage.createFolder("Snapshots");
    }

    public void init(int serverPortNumber) throws IOException {
        server = new Server();
        server.start(serverPortNumber);
        System.out.println("Server started.");
    }

    public void end() { // close server
        server.stop();
        System.out.println("Server stopped.");
    }

    public String installNewConnectionToNode(InetAddress ip, int port) throws IOException {
        Socket socket = new Socket(ip, port);
        UUID id = UUID.randomUUID();
        output_nodes.put(id, socket);
        ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
        output_stream.put(id, objectOutput);
        return id.toString();
    }

    public void sendMessage(String node_id, Object msg) throws IOException {
        Socket destination = output_nodes.get(UUID.fromString(node_id));
        output_stream.get(UUID.fromString(node_id)).writeObject(msg);
    }

    public void startSnapshot() throws IOException {
        Marker marker = new Marker(UUID.randomUUID());
        snapshots.put(marker.getSnapshotId(), new Snapshot(marker.getSnapshotId(), status, input_nodes));

        // send marker to all nodes
        for (ObjectOutputStream objectOutput : output_stream.values()) {
            objectOutput.writeObject(marker);
        }
    }

    public void endSnapshot(Snapshot snapshot) throws IOException {
        System.out.println("Snapshot " + snapshot.getSnapshotId() + " ended.");
        Storage.storeSnapshot(snapshot, path);
        snapshots.remove(snapshot.getSnapshotId());
        // test
        snapshot.print_snapshot();

    }








    private class Server {
        private ServerSocket serverSocket;
        private Thread serverThread;
        private boolean running;

        //controllare se public giusto
        public void start(int portNumber) { //passare 0 per trovarne una libera
            try {
                serverSocket = new ServerSocket(portNumber);
                int port = serverSocket.getLocalPort();
                serverPortNumber = port;
                System.out.println("Server started on port" + port);
                running = true;
                serverThread = new Thread(() -> {
                    while (running) {
                        try {
                            Socket socket = serverSocket.accept();
                            input_nodes.add(socket.getRemoteSocketAddress());
                            // Gestisci la connessione del client qui:
                            System.out.println("New connection established with:" + socket.getInetAddress() + " port:" + socket.getPort());
                            new Thread(new NodeHandler(socket)).start();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
                serverThread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stop() {
            try {
                running = false;
                serverSocket.close();
                serverThread.join();
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }





    private class NodeHandler implements Runnable {
        private Socket clientSocket;

        public NodeHandler(Socket socket) {
            this.clientSocket = socket;
        }
        /* @description: handleMarker method
         * @note: This method is used to handle a marker message.
         */

        private void handleMarker(Marker marker) throws IOException, InterruptedException {
            UUID snapshotId = marker.getSnapshotId();
            Snapshot snapshot;

            if (snapshots.containsKey(snapshotId)) {
                // Case: snapshot in progress
                snapshot = snapshots.get(snapshotId);
                if (snapshot.remove_from_node_address_list(clientSocket.getRemoteSocketAddress()))
                    // If it was the last marker, end the snapshot
                    endSnapshot(snapshot);
            } else {
                // Case: starting snapshot
                System.out.println("Starting snapshot " + snapshotId);
                snapshot = new Snapshot(snapshotId, status, input_nodes);

                snapshots.put(snapshotId, snapshot);

                // Forward marker to all other nodes in the network
                if (TEST_MODE) {
                    // Wait before starting the snapshot
                    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                    executor.schedule(() -> {
                        // Forward marker to all other nodes in the network
                        output_nodes.forEach((k, v) -> {
                            try {
                                output_stream.get(k).writeObject(marker);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }, SNAPSHOT_START_DELAY_MS, TimeUnit.MILLISECONDS);
                }
                else {
                    output_nodes.forEach((k, v) -> {
                        try {
                            output_stream.get(k).writeObject(marker);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                if (snapshot.remove_from_node_address_list(clientSocket.getRemoteSocketAddress()))
                    // If it was the last marker, end the snapshot
                    endSnapshot(snapshot);
            }
        }




        /* @description: handleMessage method
         * @note: This method is used to handle a message.
         */
        private void handleMessage(Object message) {
            if (snapshots.isEmpty()) {
                // No snapshot in progress: do not save received messages
                System.out.println("Not saving received messages");
            } else {
                // Snapshot in progress: save received messages
                for (Snapshot snapshot : snapshots.values()) {
                    System.out.println("Saving received messages: "+ message);
                    if (snapshot.getConnected_nodes().contains(clientSocket.getRemoteSocketAddress())) {
                        snapshot.addNode_messages_List(clientSocket.getRemoteSocketAddress(), message);
                    }
                }
            }
        }

        @Override
        public void run() {
            try {
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                //ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
                Object inputObject;
                while ((inputObject = in.readObject()) != null) {
                    /*Controllo se ho ricevuto un marker*/
                    if (inputObject instanceof Marker ) {
                        System.out.println("Received a new marker:\n Id: " + ((Marker) inputObject).getSnapshotId());
                        handleMarker((Marker) inputObject);
                    } else {
                        System.out.println("Received a new message: " + inputObject);
                        handleMessage(inputObject);
                    }
                }
            } catch (IOException e) {
                System.out.println("chiuso il socket");
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e ) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }
}




