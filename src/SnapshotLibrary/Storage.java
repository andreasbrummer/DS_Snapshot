package SnapshotLibrary;

import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/* this class is used to store the snapshots in a folder */
public class Storage {

    // the folder where the snapshots are stored
    private String folderName = "snapshots";

    // used to set the folder name
    public Storage() {
        createFolder(folderName);
    }
    public static void createFolder(String folderName) {
        try {
            Path path = Paths.get(folderName);
            if (!Files.exists(path)) {
                try {
                    System.out.println("Creating folder");
                    Files.createDirectory(path);
                } catch (FileAlreadyExistsException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("Could not create folder");
            e.printStackTrace();
        }
    }
    //method to store the snapshot
    public void  storeSnapshot(Snapshot snapshot) {
        try {
            Path path = Paths.get(folderName);
            if (!Files.exists(path)) {
                try {
                    Files.createFile(path);
                } catch (FileAlreadyExistsException ignored) {}
            }
            try (FileOutputStream out = new FileOutputStream(folderName + File.separator + "snapshot_" + snapshot.getSnapshotId().toString())) {
                out.write(SerializationUtils.serialize(snapshot));

            } catch (IOException e) {
                throw new RuntimeException(e);

            }

        } catch (IOException e) {
            System.err.println("Could not create file");
            e.printStackTrace();
        }
    }

    //method to retrieve the snapshot
    public Snapshot retrieveSnapshot(UUID snapshotId) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(folderName + File.separator +"snapshot_" + snapshotId.toString());
        ObjectInputStream in = new ObjectInputStream(fileIn);
        Snapshot snapshot = (Snapshot) in.readObject();
        return snapshot;
    }

    //method to delete the snapshot

    //method to delete all the snapshots

    //method to delete the folder







    }
