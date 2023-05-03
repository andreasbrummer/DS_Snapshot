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
        DistributedSnapshot distrSnap = null;
        MyState state = new MyState();
        MessageListener listener = new MyListener(state);
        UUID lastSnap ;

        Logger.getLogger("SendereMessageTest").info("Starting SenderMessageTest");
        if (args.length == 0) {
            Logger.getLogger("SendereMessageTest").info("Please specify a node ID (0 or 1)");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);

        if (nodeId == 0) {
             distrSnap = new DistributedSnapshot("Snapshot1", listener, state);
             listener.setDistributedSnapshot(distrSnap);
            Logger.getLogger("SenderMessageTest"+nodeId+1).info("Setted listener");
            while (!distrSnap.init(serverPort1)) {
                sleep(5000);
                Logger.getLogger("SenderMessageTest1").info("Port " + serverPort1 + " is occupied, waiting 5 seconds");
            }
            sleep(5000);
        } else if (nodeId == 1) {
            distrSnap = new DistributedSnapshot("Snapshot2", listener, state);
            listener.setDistributedSnapshot(distrSnap);
            Logger.getLogger("SenderMessageTest"+nodeId+1).info("Setted listener");
            while (!distrSnap.init(serverPort2)) {
                sleep(1000);
                Logger.getLogger("SenderMessageTest2").info("Port " + serverPort2 + " is occupied, waiting 5 seconds");
            }
        } else {
            Logger.getLogger("SenderMessageTest").warning("Invalid node ID (must be 0 or 1)");
            System.exit(1);
        }


        InetAddress ipAddress = InetAddress.getByName("127.0.0.1");
        String serverAddress = distrSnap.installNewConnectionToNode(ipAddress, (nodeId == 0) ? serverPort2 : serverPort1);
        String folderPath= (nodeId == 0 ? "Snapshot1" : "Snapshot2");


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            Object input = reader.readLine();

            while (!input.equals("fine")) {
                if (input.equals("marker"))
                    distrSnap.startSnapshot();
                else if (input.equals("restore")) {
                    lastSnap = Storage.getLastSnapshotId(folderPath);
                    distrSnap.sendMessage(serverAddress, lastSnap);
                    distrSnap.restoreSnapshot(lastSnap);
                }else
                    distrSnap.sendMessage(serverAddress, input);
                Logger.getLogger("SendereMessageTest"+nodeId+1).info("Stato attuale: " + state.getState());
                input = reader.readLine();
            }
        }

        distrSnap.end();
        Logger.getLogger("SendereMessageTest"+nodeId+1).info("Program closed");
    }

}
