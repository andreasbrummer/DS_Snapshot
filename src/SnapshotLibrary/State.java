package SnapshotLibrary;
import java.io.Serializable;

public interface State extends Serializable {


    void setState(State newState);

    State copy();

    Serializable getState();
}
