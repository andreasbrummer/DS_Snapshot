import SnapshotLibrary.DistributedSnapshot;
import SnapshotLibrary.MessageListener;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.util.UUID;

import static java.lang.Thread.sleep;

public class MyListener implements MessageListener {
    MyState state;
    DistributedSnapshot distrSnap;
    public MyListener(MyState state){
        this.state = state;
    }

    public void setDistributedSnapshot(DistributedSnapshot distrSnap){
        this.distrSnap = distrSnap;
    }

    @Override
    public void onMessageReceived(Object message) throws InterruptedException, IOException, ClassNotFoundException {
        if (message instanceof String) {
            String stringMessage = (String) message;
            System.out.println("ricevuto:" + stringMessage);
            try {
                int intMessage = Integer.parseInt(stringMessage);
                state.updateState(intMessage);
            } catch (NumberFormatException e) {
                Logger.getLogger("MyListener").warn("Invalid message received: " + stringMessage);
            }
        }
        else if(message instanceof UUID){
            if(distrSnap==null){
                Logger.getLogger("MyListener").info("Received restore request n."+message+" but no distributed snapshot is setted");
                //state.resetState();
                return;
            }
            Logger.getLogger("MyListener").info("Received restore request n."+message);
            distrSnap.restoreSnapshot((UUID)message);
        }

    }
}
