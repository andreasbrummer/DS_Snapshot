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
import java.util.*;

public class DistributedSnapshot{
    private int serverPortNumber;
    private Serializable status;
    private Server server;

    private List<SocketAddress> input_nodes = new ArrayList<SocketAddress>();
    private Snapshot snapshot;//poi diventera una lista
    private final Storage storage = new Storage();

    //per tenere la lista di tutti i nodi a cui sono connesso. output_nodes.getPort(), ...getInetAddress() per l IP
    Map<UUID,Socket> output_nodes = new HashMap<>();
    Map<UUID,ObjectOutputStream> output_stream = new HashMap<>();
    //verrà sostiuito con un array di snapshot che sono anche quelli in corso
    private boolean snapshotRunning = false;

    public void init() throws IOException {
        server = new Server();
        server.start(43911);
        System.out.println("Aperto il server");
    }

    public void end() { //chiude il server
        server.stop();
        System.out.println("Chiuso il server");
    }

    public String installNewConnectionToNode(InetAddress ip, int port) throws IOException {
        Socket socket = new Socket(ip, port);
        UUID id = UUID.randomUUID();
        output_nodes.put(id,socket);
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
        snapshot = new Snapshot(marker.getSnapshotId(),0,input_nodes);
        snapshotRunning = true;
        //invio marker a tutti i nodi
        for (Map.Entry<UUID, ObjectOutputStream> entry : output_stream.entrySet()) {
            ObjectOutputStream objectOutput = entry.getValue();
            objectOutput.writeObject(marker);
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
                serverPortNumber = port;
                System.out.println("Server avviato sulla porta " + port);

                running = true;
                serverThread = new Thread(() -> {
                    while (running) {
                        try {
                            Socket socket = serverSocket.accept();
                            input_nodes.add(socket.getRemoteSocketAddress());
                            // Gestisci la connessione del client qui:
                            System.out.println("Nuova connessione stabilita con:" + socket.getInetAddress() + " port:" + socket.getPort());
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

        @Override
        public void run() {
            try {
                ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                //ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                Object inputObject;
                while ((inputObject = in.readObject()) != null) {
                    System.out.println("ricevuto messaggio: " + inputObject);
                    // Elabora l'oggetto qui
                    if (inputObject instanceof Marker && snapshotRunning) {
                        //CASO GIA INIZIATO: SMETTO DI SALVARE I MESSAGGI DA QUEL CANALE + CONTROLLO SNAPSHOT FINITO
                        if(snapshot.remove_from_node_address_list(clientSocket.getRemoteSocketAddress())){
                            storage.storeSnapshot(snapshot);
                            snapshotRunning=false;
                            //test
                            snapshot.print_snapshot();
                        }
                    } else if (inputObject instanceof Marker && !snapshotRunning) {
                        //CASO INIZIO LO SNAPSHOT: SALVO LO STATO E BLOCCO SALVATAGGIO DA QUEL CANALE, SALVO MESSAGGI DAGLI ALTRI CANALI E INVIO MARKER A TUTTI output_socket
                        System.out.println("INIZIO LO SNAPSHOT");
                        //attivo registrazione
                        snapshotRunning = true;

                        //creo lo snapshot e salvo lo stato
                        //mettere l'UUID del marker ricevuto
                        snapshot = new Snapshot(UUID.randomUUID(), status,input_nodes);
                        //blocco salvataggio da quel canale
                        if(snapshot.remove_from_node_address_list(clientSocket.getRemoteSocketAddress())){
                            storage.storeSnapshot(snapshot);
                            snapshotRunning=false;
                            //test
                            snapshot.print_snapshot();
                        }

                        //inoltro i marker ai miei outgoing channels (mettendo l UUID del marker ricevuto)
                        Object finalInputObject = inputObject;
                        output_nodes.forEach((k, v)->{
                            try {
                                output_stream.get(k).writeObject(new Marker(((Marker) finalInputObject).getSnapshotId()));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                        //qui metto una lista di snaphot, la lista é una variabile globale.

                    } else if (snapshotRunning) { //no marker
                        if(snapshot.getConnected_nodes().contains(clientSocket.getRemoteSocketAddress())){
                            snapshot.addNode_messages_List(clientSocket.getRemoteSocketAddress(), inputObject); }
                    } else { //not snapshot running, no marker
                        System.out.println("non salvo i messaggi che ricevo");
                        //non salvo i messaggi che ricevo
                    }
                    //Serializable outputObject = ...; // Crea l'oggetto di risposta

                    //out.writeObject(outputObject);
                    //out.flush();

                    //clientSocket.close();
                }
            } catch (IOException e) {
                System.out.println("chiuso il socket");
                throw new RuntimeException(e);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

    }
}




//import java.util.ArrayList;
//        import java.util.HashMap;
//        import java.util.List;
//        import java.util.Map;
//
//public class MessageReceiver {
//    private Map<String, List<String>> messagesByNode = new HashMap<>();
//
//    public void receiveMessage(String message, String nodeAddress) {
//        if (!messagesByNode.containsKey(nodeAddress)) {
//            messagesByNode.put(nodeAddress, new ArrayList<>());
//        }
//        messagesByNode.get(nodeAddress).add(message);
//    }
//}
//
//
//import java.io.IOException;
//        import java.io.OutputStream;
//        import java.net.Socket;
//
//public class MessageSender {
//    public void sendMessage(String message, String nodeAddress, int portNumber) {
//        try {
//            Socket socket = new Socket(nodeAddress, portNumber);
//            OutputStream out = socket.getOutputStream();
//
//            out.write(message.getBytes());
//            out.flush();
//
//            socket.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//}
