import SnapshotLibrary.DistributedSnapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

public class main {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        DistributedSnapshot ds = new DistributedSnapshot();
        ds.init();

        //System.out.println("ora invio un messaggio: ");
        //sendMessage("Hello Snapshot", "127.0.0.1", 6942);
    }

    static void sendMessage(String message, String nodeAddress, int portNumber) {
        try {
            Socket socket = new Socket(nodeAddress, portNumber);
            OutputStream out = socket.getOutputStream();

            out.write(message.getBytes());
            out.flush();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

