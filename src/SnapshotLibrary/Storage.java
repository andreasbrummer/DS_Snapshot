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

    //method to retrieve the snapshot , null if not found
    public static Snapshot loadSnapshot(UUID snapshotId, Path path) throws IOException, ClassNotFoundException {
        File file = new File(path + File.separator +"snapshot_" + snapshotId.toString());
        if (!file.exists()) {
            // se il file non esiste, ritorna null
            return null;
        }

        FileInputStream fileIn = new FileInputStream(file);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        Snapshot snapshot = (Snapshot) in.readObject();
        return snapshot;
    }


    //method to retrieve last saved snapshot name
    public static UUID getLastSnapshotId(String folderPath) {
        // crea un oggetto File che rappresenta la cartella
        File folder = new File(folderPath);

        // ottiene la lista dei file nella cartella
        File[] files = folder.listFiles();

        // se la cartella è vuota, restituisci null
        if (files == null || files.length == 0) {
            return DistributedSnapshot.getUuidNull();
        }

        // ordina la lista di file in ordine cronologico (dal più vecchio al più recente)
        Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

        // ottiene il nome del primo file in ordine cronologico
        String firstFileName = files[files.length-1].getName();




        return UUID.fromString(firstFileName.substring(9));
    }



    // metodo per eliminare tutti gli snapshot salvati nella cartella
    public static void deleteAllSnapshots(Path folderPath) throws IOException {
        // crea un oggetto File che rappresenta la cartella
        File folder = folderPath.toFile();

        // ottiene la lista dei file nella cartella
        File[] files = folder.listFiles();

        // elimina tutti i file nella cartella
        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
    }

    // metodo per eliminare tutti gli snapshot successivi in ordine cronologico ad uno snapshot dato (escluso quello che gli passo)
    public static void deleteSnapshotsAfter(UUID snapshotId, Path folderPath) throws IOException {
        // crea un oggetto File che rappresenta la cartella
        File folder = folderPath.toFile();

        // ottiene la lista dei file nella cartella
        File[] files = folder.listFiles();

        // se la cartella è vuota, non fa niente
        if (files == null || files.length == 0) {
            return;
        }

        // ordina la lista di file in ordine cronologico (dal più vecchio al più recente)
        Arrays.sort(files, (f1, f2) -> Long.compare(f1.lastModified(), f2.lastModified()));

        // elimina tutti i file successivi a quello con lo snapshotId dato (escluso quello con lo snapshotId dato)
        boolean delete = false;
        for (File file : files) {
            if (!file.isDirectory()) {
                if (delete) {
                    file.delete();
                } else if (file.getName().equals("snapshot_" + snapshotId.toString())) {
                    delete = true;
                }
            }
        }
    }





    }
