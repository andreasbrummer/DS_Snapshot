package SnapshotLibrary;

import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.UUID;

/* this class is used to store the snapshots in a folder */
public class Storage {

    public static Path createFolder(String folderName) {
        Path path = Paths.get(folderName);
        try {
            if (!Files.exists(path)) {
                try {
                    System.out.println("Creating folder...");
                    Files.createDirectory(path);
                } catch (FileAlreadyExistsException ignored) {}
            }
        } catch (IOException e) {
            System.err.println("Could not create folder");
            e.printStackTrace();
        }
        return path;
    }
    //method to store the snapshot
    public static void  storeSnapshot(Snapshot snapshot,Path path) {
        try (FileOutputStream out = new FileOutputStream(path + File.separator + "snapshot_" + snapshot.getSnapshotId().toString())) {
            out.write(SerializationUtils.serialize(snapshot));

        } catch (IOException e) {
            throw new RuntimeException(e);

        }

    }

    //method to retrieve the snapshot
    public static Snapshot loadSnapshot(UUID snapshotId, Path path) throws IOException, ClassNotFoundException {
        FileInputStream fileIn = new FileInputStream(path + File.separator +"snapshot_" + snapshotId.toString());
        ObjectInputStream in = new ObjectInputStream(fileIn);
        Snapshot snapshot = (Snapshot) in.readObject();
        return snapshot;
    }

    //method to retrieve last saved snapshot name
    public static String retrieveLastSnapshot(String folderPath) {
        // crea un oggetto File che rappresenta la cartella
        File folder = new File(folderPath);

        // ottiene la lista dei file nella cartella
        File[] files = folder.listFiles();

        // se la cartella è vuota, restituisci null
        if (files == null || files.length == 0) {
            return null;
        }

        // ordina la lista di file in ordine cronologico (dal più vecchio al più recente)
        Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

        // ottiene il nome del primo file in ordine cronologico
        String firstFileName = files[files.length-1].getName();

        return firstFileName;
    }



    //method to delete the snapshot

    //method to delete all the snapshots

    //method to delete the folder







    }
