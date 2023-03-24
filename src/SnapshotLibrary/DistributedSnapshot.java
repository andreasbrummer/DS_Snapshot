package SnapshotLibrary;

import SnapshotLibrary.Messages.Marker;

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

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;





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
    * @param: input_nodes: list of SocketAddress of input nodes
    * @param: snapshots: map of all snapshots currently in progress
    * @param: output_nodes: map of all output nodes, using UUID as key and Socket object as value
    * @param: output_stream: map of all output streams, using UUID as key and ObjectOutputStream object as value
    * @param: SNAPSHOT_START_DELAY_MS: only for testing
    * */

public class DistributedSnapshot{
    private MessageListener listener;
    private State status;
    private Server server;
    private final Path path;
    private final List<SocketAddress> inputNodes = new ArrayList<>();
    private final Map<UUID,Snapshot> snapshots = new HashMap<>();
    private final Map<UUID,Socket> outputNodes = new HashMap<>();
    private final Map<UUID,ObjectOutputStream> outputStream = new HashMap<>();
    private static final Log LOGGER = LogFactory.getLog(DistributedSnapshot.class);

    private final Object messageLock = new Object();


    /*  only for testing
        delay (in milliseconds) before a snapshot is started */
    static final int SNAPSHOT_START_DELAY_MS = 5000;

    /*  only for testing */
    static final boolean TEST_MODE = true;
    //TODO capire il discorso delle cartella e dei file (es. se cartella è gia esistente)
    public DistributedSnapshot(Path path) {
        this.path = path;
    }
    public DistributedSnapshot(String folderName, MessageListener listener, State status) {
        this.path = Storage.createFolder(folderName);
        this.listener = listener;
        this.status = status;
    }
    public DistributedSnapshot() {
        this.path = Storage.createFolder("Snapshots");
    }
    //TODO: pensare se mettere come argomento del costruttore anche lo status


    public boolean init(int serverPortNumber) {
            server = new Server();
            server.start(serverPortNumber);
            LOGGER.info("Server started.");
            return true;
    }


    public void end() throws IOException { // close server
        server.stop();
        LOGGER.info("Server stopped.");
    }

    public String installNewConnectionToNode(InetAddress ip, int port) throws IOException {
        Socket socket = new Socket(ip, port);
        UUID id = UUID.randomUUID();
        outputNodes.put(id, socket);
        ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
        outputStream.put(id, objectOutput);
        return id.toString();
    }

    public void sendMessage(String node_id, Object msg) throws IOException {
        outputStream.get(UUID.fromString(node_id)).writeObject(msg);
    }

    public UUID startSnapshot() throws IOException {
        UUID snapshotId = UUID.randomUUID();
        Marker marker = new Marker(snapshotId);
        State stateToStore = status.copy(); // Copia lo stato di status in stateToStore (altrimenti finche non faccio lo store, se ricevo i messaggi viene modificato)
        snapshots.put(marker.getSnapshotId(), new Snapshot(marker.getSnapshotId(), stateToStore ,new ArrayList<>(inputNodes)));
        //LOGGER.info("Starting snapshot " + input_nodes); //only for test

        // send marker to all nodes
        for (ObjectOutputStream objectOutput : outputStream.values()) {
            objectOutput.writeObject(marker);
        }
        return snapshotId;
    }

    public void endSnapshot(Snapshot snapshot)  {
        LOGGER.info("Snapshot " + snapshot.getSnapshotId() + " ended.");
        Storage.storeSnapshot(snapshot, path);
        snapshots.remove(snapshot.getSnapshotId());
        // test
        if(TEST_MODE) {
            LOGGER.debug("Printing snapshot... " );
            LOGGER.debug(snapshot.toString());
        }


    }

    public void restoreSnapshot(UUID snapshotId) throws IOException, ClassNotFoundException, InterruptedException {
        Snapshot snapshot = Storage.loadSnapshot(snapshotId, path);
        if(snapshot != null) {
            LOGGER.info("Restoring snapshot " + snapshotId + " ...");
            State new_state = snapshot.getStatus();
            status.setState(new_state);
            LOGGER.info("Restoring: State of the saved snapshot: " + status.getState());
            List<Pair<SocketAddress,Object>> nodeMessages = snapshot.getNodeMessages();
            while(!snapshot.getNodeMessages().isEmpty()) {
                Pair<SocketAddress, Object> pair = nodeMessages.remove(0);
                listener.onMessageReceived(pair.getRight());
            }
            LOGGER.info("Restoring: State after the incoming messages in the snapshot: " + status.getState());
        }
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
                LOGGER.info("Server started on port" + port);
                running = true;
                serverThread = new Thread(() -> {
                    while (running) {
                        try {
                            Socket socket = serverSocket.accept();
                            inputNodes.add(socket.getRemoteSocketAddress());
                            // Gestisci la connessione del client qui:
                            LOGGER.info("New connection established with:" + socket.getInetAddress() + " port:" + socket.getPort());
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

        public void stop() throws IOException {
            try {
                running = false;
                serverSocket.close();
                serverThread.join();
                LOGGER.info("Server stopped.");
            } catch (IOException e) {
                if(!serverSocket.isClosed())
                    serverSocket.close();
                LOGGER.error("Error closing server socket", e);
            } catch (InterruptedException e) {
                LOGGER.error("Interrupted while waiting for server thread to join", e);
                // Restore interrupted status
                Thread.currentThread().interrupt();
            }
        }

    }





    private class NodeHandler implements Runnable {
        private final Socket clientSocket;
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
            } else {
                // Case: starting snapshot
                LOGGER.info("Starting snapshot " + snapshotId);
                State stateToStore = status.copy(); // Copia lo stato di status in stateToStore (altrimenti finche non faccio lo store, se ricevo i messaggi viene modificato)
                snapshot = new Snapshot(snapshotId, stateToStore, new ArrayList<>(inputNodes));

                snapshots.put(snapshotId, snapshot);

                // Forward marker to all other nodes in the network
                if (TEST_MODE) {
                    // Wait before starting the snapshot
                    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
                    executor.schedule(() -> {
                        // Forward marker to all other nodes in the network
                        outputNodes.forEach((k, v) -> {
                            try {
                                outputStream.get(k).writeObject(marker);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }, SNAPSHOT_START_DELAY_MS, TimeUnit.MILLISECONDS);
                }
                else {
                    outputNodes.forEach((k, v) -> {
                        try {
                            outputStream.get(k).writeObject(marker);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            }
            // If it was the last marker, end the snapshot
            if (snapshot.removeFromNodeAddressList(clientSocket.getRemoteSocketAddress()))
                endSnapshot(snapshot);
        }




        /* @description: handleMessage method
         * @note: This method is used to handle a message.
         */
        private void handleMessage(Object message) {
            if (snapshots.isEmpty()) {
                // No snapshot in progress: do not save received messages
                LOGGER.debug("Not saving received messages");
            } else {
                // Snapshot in progress: save received messages
                for (Snapshot snapshot : snapshots.values()) {
                    LOGGER.debug("Saving received messages: "+ message);
                    //LOGGER.debug(snapshot.getConnectedNodes()); //only to test
                    if (snapshot.getConnectedNodes().contains(clientSocket.getRemoteSocketAddress())) {
                        snapshot.addNodeMessage(clientSocket.getRemoteSocketAddress(), message);
                        LOGGER.debug(snapshot.getConnectedNodes().toString() + clientSocket.toString()+ message);
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
                    synchronized(messageLock) {
                        //listener.onMessageReceived(inputObject);
                        /*Controllo se ho ricevuto un marker*/
                        if (inputObject instanceof Marker) {
                            LOGGER.debug("Received a new marker:\n Id: " + ((Marker) inputObject).getSnapshotId());
                            handleMarker((Marker) inputObject);
                        } else {
                            LOGGER.debug("Received a new message: " + inputObject);
                            handleMessage(inputObject); //Perche lo avevi tolto?

                            //testato che aspetta che la chiamata termini prima di andare avanti
                            //in realta aspetta anche se non è synchronize
                            listener.onMessageReceived(inputObject);
                            //LOGGER.debug("Eccomi sono fuori"); //for test
                        }
                    }
                }
            } catch (IOException e) {
                if (clientSocket.isClosed()) {
                    // Socket closed by remote host
                    LOGGER.error("Socket chiuso dal lato remoto.");

                    //TODO: togliere da input_nodes e chiudere questo thread.
                } else {
                    // Other I/O errors
                    e.printStackTrace();
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                // Socket closing
                try {
                    clientSocket.close(); //IN TEORIA NON DOBBIAMO CHIUDERE NOI IL SOCKET DEL CLIENT. DEVE CHIUDERLO IL CLIENT
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            }
        }
    }


