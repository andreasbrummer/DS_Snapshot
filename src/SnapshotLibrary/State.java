package SnapshotLibrary;
import java.io.Serializable;

public interface State extends Serializable {


    void setState(Serializable newState);

    Serializable getState();
}
