import SnapshotLibrary.MessageListener;

public class MyListener implements MessageListener {
    MyState state;
    public MyListener(MyState state){
        this.state = state;
    }
    @Override
    public void onMessageReceived(Object message) {
       if(message instanceof Integer){
           state.updateState((Integer) message);
       }
       else {
           System.out.println("Message not recognized");
       }
    }
}
