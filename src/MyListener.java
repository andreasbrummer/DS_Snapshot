import SnapshotLibrary.MessageListener;

import static java.lang.Thread.sleep;

public class MyListener implements MessageListener {
    MyState state;
    public MyListener(MyState state){
        this.state = state;
    }
    @Override
    public void onMessageReceived(Object message) throws InterruptedException {
        //sleep(5000);
        if (message instanceof String) {
            String stringMessage = (String) message;
            try {
                int intMessage = Integer.parseInt(stringMessage);
                state.updateState(intMessage);
            } catch (NumberFormatException e) {
                // la stringa ricevuta non può essere convertita in un intero, non fare niente
            }
        }

    }
}
