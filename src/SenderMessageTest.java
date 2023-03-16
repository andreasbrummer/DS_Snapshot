import SnapshotLibrary.DistributedSnapshot;
import SnapshotLibrary.Messages.Marker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.LinkedHashMap;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class SenderMessageTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port1=24071,port2=24079;
        DistributedSnapshot ds = new DistributedSnapshot();
       int port = 24072; //porta d ascolto
            if (args.length > 0) {
                if(Integer.parseInt(args[0])==0){
                    ds.init(port1);
                    sleep(10000);
                    port=port2;
                }
                else {
                    sleep(1000);
                    ds.init(port2);
                    port = port1;
                }
            }
            else{
                System.out.println("Inserire 0 o 1 come argomento\n");
            }


        InetAddress ipAddress = InetAddress.getByName("127.0.0.1");
        //harcodare qui la porta del nodo server 10720
        String node1 = ds.installNewConnectionToNode(ipAddress, port);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
                System.out.print("Inserisci l'oggetto da inviare: \n");
                // Qui si assume che l'oggetto sia di tipo String, ma è possibile utilizzare qualsiasi altro tipo di oggetto
                Object object = reader.readLine();
                if(object.equals("marker")){
                    ds.startSnapshot();
                }
                else{
                    ds.sendMessage(node1,object);
                }
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
