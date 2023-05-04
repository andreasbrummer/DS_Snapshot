import SnapshotLibrary.State;

import java.io.Serializable;

public class MyState implements State {
    int state = 0;
    @Override
    public void setState(State newState) {
        this.state = (int) newState.getState();
    }
    @Override
    public Integer getState() {
        return state;
    }
    public void updateState(Integer message){
        state += message;
    }

    public void resetState(){state=0;} //caso restore senza snapshot salvati

    @Override
    public State copy() {
        MyState newState = new MyState();
        newState.state = this.state;
        return newState;
    }
}
