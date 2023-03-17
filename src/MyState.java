import SnapshotLibrary.State;

import java.io.Serializable;

public class MyState implements State {
    int state = 0;
    @Override
    public void setState(Serializable newState) {
        this.state = (int) newState;
    }
    @Override
    public Serializable getState() {
        return null;
    }
    public void updateState(Integer message){
        state += message;
    }
}
