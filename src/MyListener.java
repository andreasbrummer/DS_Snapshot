import SnapshotLibrary.DistributedSnapshot;
import SnapshotLibrary.MessageListener;

import java.io.IOException;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class MyListener implements MessageListener {
    MyState state;
    DistributedSnapshot distr_snap;
    public MyListener(MyState state){
        this.state = state;
    }

    public void setDistributedSnapshot(DistributedSnapshot distr_snap){
        this.distr_snap = distr_snap;
    }

    @Override
    public void onMessageReceived(Object message) throws InterruptedException, IOException, ClassNotFoundException {
        //sleep(5000);
        if (message instanceof String) {
            String stringMessage = (String) message;
            System.out.println("ricevuto:" + stringMessage);
            try {
                int intMessage = Integer.parseInt(stringMessage);
                state.updateState(intMessage);
            } catch (NumberFormatException e) {
                // la stringa ricevuta non può essere convertita in un intero, non fare niente
            }
        }
        else if(message instanceof UUID){
            //chiama funzione libreria per fare restore Snapshot
            distr_snap.restoreSnapshot((UUID)message);
        }

    }
}
