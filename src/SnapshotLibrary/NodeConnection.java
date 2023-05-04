package SnapshotLibrary;

import java.io.ObjectOutputStream;
import java.net.Socket;

class NodeConnection {
    private final Socket socket;
    private final ObjectOutputStream output;
    private boolean connected;

    public NodeConnection(Socket socket, ObjectOutputStream output) {
        this.socket = socket;
        this.output = output;
        this.connected = true;
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectOutputStream getOutput() {
        return output;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
