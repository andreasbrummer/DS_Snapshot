import SnapshotLibrary.DistributedSnapshot;
import SnapshotLibrary.MessageListener;
import SnapshotLibrary.Storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;

public class SenderMessageTest{

    private static final String INPUT_MARKER = "marker";
    private static final String INPUT_RESTORE = "restore";
    private static final String INPUT_MESSAGE = "message";
    private static final String INPUT_STATE = "state";
    private static final String INPUT_SUM = "sum";
    private static final String INPUT_EXIT = "fine";
    private static final String OPEN_CONNECTION = "open";
    private static final String REMOTE_IP = "192.168.6.51";
    private static final String CLOSE_CONNECTION = "close";

    private static int serverPort1 = 24071;
    private static int serverPort2 = 24079;
    public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {

        DistributedSnapshot distrSnap = null;
        MyState state = new MyState();
        MessageListener listener = new MyListener(state);


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


        InetAddress ipAddress = InetAddress.getByName(REMOTE_IP);
        String serverAddress = distrSnap.installNewConnectionToNode(ipAddress, (nodeId == 0) ? serverPort2 : serverPort1);
        String folderPath= (nodeId == 0 ? "Snapshot1" : "Snapshot2");

        Random rand = new Random();
        int sum = 0;


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            Object input = reader.readLine();

            while (!input.equals(INPUT_EXIT)) {
                handleInput((String) input, reader, distrSnap, serverAddress, rand, nodeId,state,folderPath,sum);
                input = reader.readLine();
            }

        }

        distrSnap.end();
        Logger.getLogger("SendereMessageTest"+nodeId+1).info("Program closed");
    }

    private static void restoreSnapshot(DistributedSnapshot distrSnap, String folderPath, String serverAddress) throws IOException, ClassNotFoundException, InterruptedException {
        UUID lastSnap ;
        lastSnap = Storage.getLastSnapshotId(folderPath);
        distrSnap.sendMessage(serverAddress, lastSnap);
        distrSnap.restoreSnapshot(lastSnap);
    }

    private static void sendMessage(DistributedSnapshot distrSnap, String serverAddress, Random rand,int nodeId) throws IOException, InterruptedException {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            int sum = 0;
            for (int i = 0; i < 1; i++) {
                int num = rand.nextInt(101) - 50;
                try {
                    distrSnap.sendMessage(serverAddress, num);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                sum += num;
            }
        });
        executor.shutdown();
    }
    private static void logState(int nodeId, MyState state) {
        Logger.getLogger("SendereMessageTest" + nodeId + 1).info("Ciao Stato attuale: " + state.getState());

    }
    private static void logSum(int sum,int nodeId) {
        Logger.getLogger("SendereMessageTest" + nodeId + 1).info("Somma attuale: " + sum);
    }
    public static void openConnection(String ServerAddress,DistributedSnapshot distrSnap, int nodeId) throws IOException {
        Logger.getLogger("SendereMessageTest").info("Opening connection to " + InetAddress.getByName(REMOTE_IP));
        try {
        distrSnap.reconnectToNode(ServerAddress,InetAddress.getByName(REMOTE_IP), (nodeId == 0) ? serverPort2 : serverPort1);
        } catch (ConnectException e) {
            Logger.getLogger("SendereMessageTest").warning("Connection refused");
        }
    }
    private static void handleInput(String input, BufferedReader reader, DistributedSnapshot distrSnap, String serverAddress, Random rand, int nodeId, MyState state,String folderPath,int sum) throws IOException, ClassNotFoundException, InterruptedException {
        switch (input) {
            case INPUT_MARKER:
                distrSnap.startSnapshot();
                break;
            case INPUT_RESTORE:
                restoreSnapshot(distrSnap, folderPath,serverAddress);
                break;
            case INPUT_MESSAGE:
                sendMessage(distrSnap, serverAddress, rand,nodeId);
                break;
            case INPUT_STATE:
                logState(nodeId,state);
                break;
            case INPUT_EXIT:
                // Do nothing, just exit the loop
                break;
            case CLOSE_CONNECTION:
                    distrSnap.closeConnection(serverAddress);
                    break;
            case OPEN_CONNECTION:
                    openConnection(serverAddress,distrSnap,nodeId);
                    break;
            default:
                Logger.getLogger("SendereMessageTest" + nodeId + 1).info("Comando non riconosciuto");
                break;
        }
    }

}
