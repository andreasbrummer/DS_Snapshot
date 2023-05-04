package SnapshotLibrary;

import java.io.IOException;

public interface MessageListener {
    void onMessageReceived(Object message) throws InterruptedException, IOException, ClassNotFoundException;

    void setDistributedSnapshot(DistributedSnapshot distr_snap);
}