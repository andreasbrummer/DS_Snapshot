import SnapshotLibrary.DistributedSnapshot;
import SnapshotLibrary.MessageListener;
import SnapshotLibrary.Storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class SenderMessageTest{
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        int serverPort1 = 24071;
        int serverPort2 = 24079;
        DistributedSnapshot distr_snap = null;
        MyState state = new MyState();
        MessageListener listener = new MyListener(state);
        System.out.println("Starting SenderMessageTest");
        if (args.length == 0) {
            System.err.println("Please specify a node ID (0 or 1)");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);
        if (nodeId == 0) {
             distr_snap = new DistributedSnapshot("Snapshot1", listener, state);
             listener.setDistributedSnapshot(distr_snap);
            while (!distr_snap.init(serverPort1)) {
                sleep(5000);
                Logger.getLogger("SenderMessageTest").info("Port " + serverPort1 + " is occupied, waiting 5 seconds");
            }
            sleep(5000);
        } else if (nodeId == 1) {
            distr_snap = new DistributedSnapshot("Snapshot2", listener, state);
            listener.setDistributedSnapshot(distr_snap);
            while (!distr_snap.init(serverPort2)) {
                sleep(1000);
                Logger.getLogger("SenderMessageTest").info("Port " + serverPort2 + " is occupied, waiting 5 seconds");
            }
        } else {
            System.err.println("Invalid node ID (must be 0 or 1)");
            System.exit(1);
        }


        InetAddress ipAddress = InetAddress.getByName("127.0.0.1");
        String serverAddress = distr_snap.installNewConnectionToNode(ipAddress, (nodeId == 0) ? serverPort2 : serverPort1);



        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            Object input = reader.readLine();
            String folderPath;
            //UUID snapshotId = null;
            while (!input.equals("fine")) {
                if (input.equals("marker")) {
                    distr_snap.startSnapshot();
                } else if (input.equals("restore")) {
                    UUID lastSnap;
                    folderPath =  (nodeId==0 ? "Snapshot1": "Snapshot2");
                    String retrievedSnapString = Storage.retrieveLastSnapshot(folderPath);
                    if(retrievedSnapString==null)//se non ci sono snapshot salvati
                        lastSnap= distr_snap.getUuidNull();
                    else
                        lastSnap = UUID.fromString(retrievedSnapString.substring(9));

                    distr_snap.sendMessage(serverAddress, lastSnap);
                    distr_snap.restoreSnapshot(lastSnap);
                }else {
                    distr_snap.sendMessage(serverAddress, input);
                }
                System.out.println("APP: Stato attuale: " + state.getState());
                input = reader.readLine();
            }
        }



        System.out.println("Program closed");
        distr_snap.end();
    }

}
