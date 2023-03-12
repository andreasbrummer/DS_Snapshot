package SnapshotLibrary;

import SnapshotLibrary.Messages.Marker;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DistributedSnapshot{
    private int serverPortNumber;
    Server server;

    //per tenere la lista di tutti i nodi a cui sono connesso. output_nodes.getPort(), ...getInetAddress() per l IP
    Map<UUID,Socket> output_nodes = new HashMap<>();
    Map<UUID,ObjectOutputStream> output_stream = new HashMap<>();

    public void init() throws IOException {
        server = new Server();
        server.start(0);
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
                ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                Object inputObject;
                while ((inputObject = in.readObject()) != null) {
                    System.out.println("ricevuto messaggio: " + inputObject);
                    // Elabora l'oggetto qui
                    if(inputObject instanceof Marker){
                        System.out.println("INIZIO LO SNAPSHOT");
                    }
                    //Serializable outputObject = ...; // Crea l'oggetto di risposta

                    //out.writeObject(outputObject);
                    //out.flush();
                }

                clientSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
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
