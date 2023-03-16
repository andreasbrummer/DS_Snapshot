import SnapshotLibrary.DistributedSnapshot;
import SnapshotLibrary.MessageListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class SenderMessageTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        int port1=24071,port2=24079;
        DistributedSnapshot ds = null;
        //listener port
       int port = 24072;

        MessageListener listener = new MessageListener() {
            @Override
            public void onMessageReceived(Object message) {
                System.out.println("Messaggio ricevuto nel main: " + message);
            }
        };

            if (args.length > 0) {
                if(Integer.parseInt(args[0])==0){
                    ds = new DistributedSnapshot("Snapshot1",listener);
                    while(!ds.init(port1)){
                        sleep(5000);
                        Logger.getLogger("SenderMessageTest").info("Porta "+port1 + " occupata, aspetto 5 secondi");
                    }
                    port = port2;
                    sleep(5000);
                }
                else {
                    ds = new DistributedSnapshot("Snapshot2",listener);
                    while (!ds.init(port2)) {
                        sleep(1000);
                        Logger.getLogger("SenderMessageTest").info("Porta " + port2 + "occupata, aspetto 5 secondi");
                    }
                    port = port1;
                }
            }
            else{
                System.out.println("Insert 0 o 1 as argument\n");
            }



        InetAddress ipAddress = InetAddress.getByName("127.0.0.1");
        //harcodare qui la porta del nodo server 10720
        String node1 = ds.installNewConnectionToNode(ipAddress, port);
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        Object object = reader.readLine();
        while (!object.equals("fine")) {
                System.out.print("Inserisci l'oggetto da inviare: \n");
                // Qui si assume che l'oggetto sia di tipo String, ma è possibile utilizzare qualsiasi altro tipo di oggetto
                if(object.equals("marker")){
                    ds.startSnapshot();
                }
                else{
                    ds.sendMessage(node1,object);
                }
                object = reader.readLine();
      }
        System.out.println("Chiusura del programma");
        ds.end();
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
