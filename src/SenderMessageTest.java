import SnapshotLibrary.DistributedSnapshot;
import SnapshotLibrary.MessageListener;
import SnapshotLibrary.State;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetAddress;
import java.util.UUID;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class SenderMessageTest{
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
        int serverPort1 = 24071;
        int serverPort2 = 24079;
        DistributedSnapshot system = new DistributedSnapshot();
        MyState state = new MyState();
        MessageListener listener = new MyListener(state);
        System.out.println("Starting SenderMessageTest");
        if (args.length == 0) {
            System.err.println("Please specify a node ID (0 or 1)");
            System.exit(1);
        }

        int nodeId = Integer.parseInt(args[0]);
        if (nodeId == 0) {
             system = new DistributedSnapshot("Snapshot1", listener, state);
            while (!system.init(serverPort1)) {
                sleep(5000);
                Logger.getLogger("SenderMessageTest").info("Port " + serverPort1 + " is occupied, waiting 5 seconds");
            }
            sleep(5000);
        } else if (nodeId == 1) {
            system = new DistributedSnapshot("Snapshot2", listener, state);
            while (!system.init(serverPort2)) {
                sleep(1000);
                Logger.getLogger("SenderMessageTest").info("Port " + serverPort2 + " is occupied, waiting 5 seconds");
            }
        } else {
            System.err.println("Invalid node ID (must be 0 or 1)");
            System.exit(1);
        }


        InetAddress ipAddress = InetAddress.getByName("127.0.0.1");
        String serverAddress = system.installNewConnectionToNode(ipAddress, (nodeId == 0) ? serverPort2 : serverPort1);



        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            Object input = reader.readLine();
            UUID snapshotId = null;
            while (!input.equals("fine")) {
                if (input.equals("marker")) {
                    system.startSnapshot();
                } else if (input.equals("restore")) {
                    system.restoreSnapshot(snapshotId);
                    input = snapshotId;
                } else {
                    system.sendMessage(serverAddress, input);
                }
                input = reader.readLine();
            }
        }

        System.out.println("Program closed");
        system.end();
    }

}
