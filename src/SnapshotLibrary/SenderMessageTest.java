package SnapshotLibrary;

import SnapshotLibrary.Messages.Marker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.UUID;

public class SenderMessageTest {
    public static void main(String[] args) throws IOException {
        DistributedSnapshot ds = new DistributedSnapshot();
        ds.init();

        InetAddress ipAddress = InetAddress.getByName("192.168.121.239");
        int port = 43731; //harcodare qui la porta del nodo server
        String node1 = ds.installNewConnectionToNode(ipAddress,port );
        System.out.println("Connessione installata");

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
                System.out.print("Inserisci l'oggetto da inviare: ");
                // Qui si assume che l'oggetto sia di tipo String, ma è possibile utilizzare qualsiasi altro tipo di oggetto
                Object object = reader.readLine();
                if(object.equals("marker")){
                    object = new Marker(UUID.randomUUID());

                }
                ds.sendMessage(node1,object);
      }
    }
}
//        try {
//            // Creazione del socket e connessione all'indirizzo IP e alla porta specificati
//            InetAddress ipAddress = InetAddress.getByName("192.168.121.51");
//            int port = 43462; //harcodare qui la porta del nodo server
//            Socket socket = new Socket(ipAddress, port);
//
//            // Preparazione dei buffer di input e output
//            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//            ObjectOutputStream objectOutput = new ObjectOutputStream(socket.getOutputStream());
//
//            // Ciclo while infinito per leggere l'oggetto dalla tastiera e inviarlo tramite socket
//            while (true) {
//                System.out.print("Inserisci l'oggetto da inviare: ");
//                // Qui si assume che l'oggetto sia di tipo String, ma è possibile utilizzare qualsiasi altro tipo di oggetto
//                String object = reader.readLine();
//                objectOutput.writeObject(object);
//            }
//        } catch (IOException e) {
//            System.out.println("Errore durante la connessione al server: " + e.getMessage());
//        }
//    }
//}