package SnapshotLibrary;

import SnapshotLibrary.Messages.Marker;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;



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
    private static final UUID UUID_NULL = new UUID(0L, 0L);
    private MessageListener listener;
    private State status;
    private Server server;
    private final Path path;
    private final List<SocketAddress> inputNodes = new ArrayList<>();
    private final Map<UUID,Snapshot> snapshots = new HashMap<>();
    private final Map<UUID,Socket> outputNodes = new HashMap<>();
    //private final List<NodeConnection> nodeConnections = new ArrayList<>();

    private final Map<UUID,ObjectOutputStream> outputStream = new HashMap<>();
    private static final Log LOGGER = LogFactory.getLog(DistributedSnapshot.class);

    private final Object messageLock = new Object();



    /*  only for testing
        delay (in milliseconds) before a snapshot is started */
    static final int SNAPSHOT_START_DELAY_MS = 7000;

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


    public boolean init(int serverPortNumber) throws IOException {
            server = new Server();
            server.start(serverPortNumber);
            LOGGER.info("Server started.");
            return true;
    }

    // close server
        public void end() throws IOException {
            closeAllConnections();
            server.stop();
            LOGGER.info("Server stopped.");
        }

    public  synchronized String installNewConnectionToNode(InetAddress ip, int port) throws IOException {

        Socket socket = new Socket(ip, port);
        UUID id = UUID.randomUUID();
        for(Socket s : outputNodes.values()){
            if(s.getInetAddress().equals(ip) && s.getPort() == port){
                LOGGER.error("Connection already exists with: " + ip + " port: " + port);
                return null;
            }
        }
        outputNodes.put(id, socket);
        LOGGER.debug("added node with id: " + id);
        ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
        outputStream.put(id, objectOutput);
        return id.toString();
    }

    public synchronized void reconnectToNode(String nodeId,InetAddress ip, int port) throws IOException {
        Socket socket = new Socket(ip, port);
        UUID id = UUID.fromString(nodeId);
        for(Socket s : outputNodes.values()){
            if(s.getInetAddress().equals(ip) && s.getPort() == port){
                LOGGER.error("Connection already exists with: " + ip + " port: " + port);
                return;
            }
        }
        outputNodes.put(id, socket);
        LOGGER.debug("added node with id: " + id);
        ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
        outputStream.put(id, objectOutput);
    }

    public synchronized void closeConnection(String node_id) throws IOException {
        UUID id = UUID.fromString(node_id);
        Socket socket = outputNodes.get(id);
        ObjectOutputStream objectOutput = outputStream.get(id);
        try {
            objectOutput.close();
            socket.close();
            outputNodes.remove(id);
            outputStream.remove(id);
        } catch (NullPointerException e) {
            LOGGER.error("No connection with : " + socket);
        }

        LOGGER.info("Connection closed with: " + socket.getInetAddress() + " port: " + socket.getPort());
    }


    private void closeAllConnections() {
        ExecutorService executor = Executors.newFixedThreadPool(outputNodes.size());

        for (UUID id : outputNodes.keySet()) {
            executor.execute(() -> {
                try {
                    Socket socket = outputNodes.get(id);
                    ObjectOutputStream objectOutput = outputStream.get(id);

                    objectOutput.close();
                    socket.close();
                    outputNodes.remove(id);
                    outputStream.remove(id);

                    LOGGER.info("Connection closed with: " + socket.getInetAddress() + " port: " + socket.getPort());
                } catch (IOException e) {
                    LOGGER.error("Error closing connection with id: " + id, e);
                }
            });
        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            LOGGER.info("All connections closed.");
        } catch (InterruptedException e) {
            LOGGER.error("Interrupted while waiting for connection closing threads to finish", e);
            // Restore interrupted status
            Thread.currentThread().interrupt();
        }
    }


    public void sendMessage(String node_id, Object msg) throws IOException {
        ObjectOutputStream objectOutput = outputStream.get(UUID.fromString(node_id));
        try {
            LOGGER.debug("Sending message to " + node_id);
            objectOutput.writeObject(msg);
        } catch (IOException e) {
            if (e instanceof java.io.EOFException || e instanceof java.net.SocketException) {
                // Handle the case where the server closed the connection remotely
                //outputNodes.remove(UUID.fromString(node_id));
                LOGGER.error("Server closed the connection remotely.");
            } else {
                // Handle other IOExceptions
                LOGGER.error("Error sending message to " + node_id, e);
            }
        } catch (NullPointerException e) {
            LOGGER.error("Error sending message to " + node_id+ ", invalid address.");
        }
    }

    public UUID startSnapshot() throws IOException {
        UUID snapshotId = UUID.randomUUID();
        Marker marker = new Marker(snapshotId);
        // Copia lo stato di status in stateToStore (altrimenti finche non faccio lo store, se ricevo i messaggi viene modificato)
        State stateToStore = status.copy();
        snapshots.put(marker.getSnapshotId(), new Snapshot(marker.getSnapshotId(), stateToStore ,new ArrayList<>(inputNodes)));
        LOGGER.debug("Starting snapshot " + inputNodes);
        LOGGER.debug("Snapshot id: " + snapshotId);

        // send marker to all nodes
        for (ObjectOutputStream objectOutput : outputStream.values()) {
            LOGGER.debug("Sending marker to " + objectOutput);
            objectOutput.writeObject(marker);
        }
        if(outputStream.size() == 0) {
            LOGGER.debug("No nodes connected, ending snapshot.");
            endSnapshot(snapshots.get(snapshotId));
        }
        return snapshotId;
    }

    public void endSnapshot(Snapshot snapshot)  {
        LOGGER.info("Snapshot " + snapshot.getSnapshotId() + " ended.");
        Storage.storeSnapshot(snapshot, path);
        snapshots.remove(snapshot.getSnapshotId());
        if(TEST_MODE) {
            LOGGER.debug("Printing snapshot... " );
            LOGGER.debug(snapshot.toString());
        }


    }

    public static UUID getUuidNull(){
        return UUID_NULL;
    }

    public void restoreSnapshot(UUID snapshotId) throws IOException, ClassNotFoundException, InterruptedException {
        try {
            //No snapshot found
            if (snapshotId.equals(UUID_NULL)) {
                LOGGER.info("Resetting to initial state (No snapshots found)");
                status.resetState();
                Storage.deleteAllSnapshots(path); //svuota la cartella snapshot
            }
            //Snapshot already exists
            else {
                Snapshot snapshot = Storage.loadSnapshot(snapshotId, path);
                Storage.deleteSnapshotsAfter(snapshotId, path); //cancella tutti gli snapshot successivi a quello restored
                LOGGER.info("Restoring snapshot " + snapshotId + " ...");
                State new_state = snapshot.getStatus();
                status.setState(new_state);
                LOGGER.info("Restoring: State of the saved snapshot: " + status.getState());
                List<Pair<SocketAddress, Object>> nodeMessages = snapshot.getNodeMessages();
                while (!snapshot.getNodeMessages().isEmpty()) {
                    Pair<SocketAddress, Object> pair = nodeMessages.remove(0);
                    listener.onMessageReceived(pair.getRight());
                }
                LOGGER.info("Restoring: State after the incoming messages in the snapshot: " + status.getState());
            }
        }catch (NullPointerException e){
            LOGGER.error("Error restoring snapshot " + snapshotId + " ...", e);
        }
    }







 private class Server {
     private ServerSocket serverSocket;
     private Thread serverThread;
     private AtomicBoolean running = new AtomicBoolean(false);
     List<Thread> nodeHandlerThreads = new ArrayList<>();
     public void start(int portNumber) throws IOException {
         if (running.get()) {
             LOGGER.warn("Server is already running.");
             return;
         }
         serverSocket = new ServerSocket(portNumber);
         int port = serverSocket.getLocalPort();
         LOGGER.info("Server started on port " + port);
         running.set(true);
         serverThread = new Thread(() -> {
             while (running.get()) {
                 try {
                     Socket socket = serverSocket.accept();
                     inputNodes.add(socket.getRemoteSocketAddress());
                     LOGGER.info("New connection established with: " + socket.getInetAddress() + " port:" + socket.getPort());
                     Thread nodeHandlerThread = new Thread(new NodeHandler(socket), "NodeHandler-"+Thread.activeCount());
                     nodeHandlerThreads.add(nodeHandlerThread);
                     nodeHandlerThread.start();
                 } catch (SocketException e) {
                     if (running.get()) {
                         LOGGER.error("Error accepting client connection.", e);
                     }
                 } catch (IOException e) {
                     LOGGER.error("Error accepting client connection.", e);
                 }
             }
         });
         serverThread.start();
     }

     public void stop() {
         if (!running.get()) {
             LOGGER.warn("Server is not running.");
             return;
         }
         running.set(false);
         try {
             serverSocket.close();
             serverThread.interrupt();
             for (Thread nodeHandlerThread : nodeHandlerThreads) {
                 LOGGER.debug("Interrupting node handler thread: " + nodeHandlerThread.getName());
                 nodeHandlerThread.interrupt();
                 LOGGER.debug("Interrupted node handler thread: " + nodeHandlerThread.getName());
                 nodeHandlerThread.join();
                 LOGGER.debug("Joined node handler thread."+ nodeHandlerThread.getName()+ "out of " + nodeHandlerThreads.size() + "threads");
             }
             serverThread.join();
             LOGGER.info("Server stopped.");
         } catch (IOException | InterruptedException e) {
             LOGGER.error("Error stopping server.", e);
         }

     }

     public boolean isRunning() {
         return running.get();
     }

     public int getPort() {
         return serverSocket.getLocalPort();
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
                LOGGER.debug("Snapshot " + snapshotId + " in progress.");
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
        private void handleEOFException(EOFException e) {
            // Client closed connection
            LOGGER.info("Client closed connection: " + clientSocket.getRemoteSocketAddress());
            // Remove client from input nodes
            inputNodes.remove(clientSocket.getRemoteSocketAddress());
        }
        private void handleInterruptedException(InterruptedException e) {
            // Thread interrupted
            LOGGER.debug("Node handler thread interrupted.");
            Thread.currentThread().interrupt();
        }
        @Override
        public void run() {
            try {
                clientSocket.setSoTimeout(5000);
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                Object inputObject;
                    while ( !Thread.currentThread().isInterrupted()) {
                            //LOGGER.debug("Waiting for a new message...");
                            try{
                                if(clientSocket.isClosed()) {
                                    Thread.currentThread().interrupt();
                                }
                                inputObject = in.readObject();
                            }catch (SocketTimeoutException e){
                                //LOGGER.debug("Timeout");
                                continue;
                            }
                            //LOGGER.debug("Received a new message...");
                            if (inputObject != null) {
                                synchronized (messageLock) {
                                    /*Controllo se ho ricevuto un marker*/
                                    if (inputObject instanceof Marker) {
                                        LOGGER.debug("Received a new marker:\n Id: " + ((Marker) inputObject).getSnapshotId());
                                        handleMarker((Marker) inputObject);
                                    } else {
                                        LOGGER.debug("Received a new message: " + inputObject);
                                        handleMessage(inputObject);
                                        listener.onMessageReceived(inputObject);
                                    }
                                }
                            }
                    }
            }
            catch (EOFException e) {
                handleEOFException(e);
            } catch (InterruptedException e) {
                handleInterruptedException(e);
            } catch (IOException | ClassNotFoundException e) {
                // Other I/O errors
                if(clientSocket.isClosed())
                    LOGGER.debug("Client closed connection: " + clientSocket.getRemoteSocketAddress());
                else
                    LOGGER.error("Error handling client connection.", e);
                e.printStackTrace();
            } finally {
                // Socket closing
                try {
                    clientSocket.close();
                    inputNodes.remove(clientSocket.getRemoteSocketAddress());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            }
        }
    }


