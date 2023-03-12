import SnapshotLibrary.DistributedSnapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class main {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        DistributedSnapshot ds = new DistributedSnapshot();
            try {
                Path path = Paths.get("snapshots");
                if (!Files.exists(path)) {
                    try {
                        System.out.println("Creating folder");
                        Files.createDirectory(path);
                    } catch (FileAlreadyExistsException ignored) {}
                }
            } catch (IOException e) {
                System.err.println("Could not create folder");
                e.printStackTrace();
            }
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

