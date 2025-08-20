package dev.xoventech.encryptionapp;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FileUtils {
    public static class FileMetadata implements Serializable {
        public String fileName;
        public String date;
        public String type;
        public String content;

        public FileMetadata(String fileName, String date, String type, String content) {
            this.fileName = fileName;
            this.date = date;
            this.type = type;
            this.content = content;
        }
    }

    public static void saveFile(Context context, String fileName, String content, String type, boolean useExternalStorage) throws Exception {
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        FileMetadata metadata = new FileMetadata(fileName, date, type, content);

        File file;
        if (useExternalStorage) {
            file = new File(Environment.getExternalStorageDirectory(), fileName);
        } else {
            file = new File(context.getFilesDir(), fileName);
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(content.getBytes());
        }

        List<FileMetadata> metadataList = loadMetadata(context);
        boolean updated = false;
        for (int i = 0; i < metadataList.size(); i++) {
            if (metadataList.get(i).fileName.equals(fileName)) {
                metadataList.set(i, metadata);
                updated = true;
                break;
            }
        }
        if (!updated) {
            metadataList.add(metadata);
        }

        try (FileOutputStream fos = context.openFileOutput("metadata.dat", Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(metadataList); // Fixed: Use writeObject instead of write
        }
    }

    public static List<FileMetadata> loadMetadata(Context context) {
        List<FileMetadata> metadataList = new ArrayList<>();
        try (FileInputStream fis = context.openFileInput("metadata.dat");
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            metadataList = (List<FileMetadata>) ois.readObject();
        } catch (Exception e) {
            // File may not exist yet
        }
        return metadataList;
    }

    public static void renameFile(Context context, String oldName, String newName, boolean useExternalStorage) throws Exception {
        File oldFile;
        File newFile;
        if (useExternalStorage) {
            oldFile = new File(Environment.getExternalStorageDirectory(), oldName);
            newFile = new File(Environment.getExternalStorageDirectory(), newName);
        } else {
            oldFile = new File(context.getFilesDir(), oldName);
            newFile = new File(context.getFilesDir(), newName);
        }

        if (!oldFile.renameTo(newFile)) {
            throw new Exception("Failed to rename file");
        }

        List<FileMetadata> metadataList = loadMetadata(context);
        for (FileMetadata metadata : metadataList) {
            if (metadata.fileName.equals(oldName)) {
                metadata.fileName = newName;
                metadata.date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            }
        }

        try (FileOutputStream fos = context.openFileOutput("metadata.dat", Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(metadataList); // Fixed: Use writeObject instead of write
        }
    }

    public static void deleteFile(Context context, String fileName, boolean useExternalStorage) throws Exception {
        File file;
        if (useExternalStorage) {
            file = new File(Environment.getExternalStorageDirectory(), fileName);
        } else {
            file = new File(context.getFilesDir(), fileName);
        }

        if (!file.delete()) {
            throw new Exception("Failed to delete file");
        }

        List<FileMetadata> metadataList = loadMetadata(context);
        metadataList.removeIf(metadata -> metadata.fileName.equals(fileName));

        try (FileOutputStream fos = context.openFileOutput("metadata.dat", Context.MODE_PRIVATE);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(metadataList); // Fixed: Use writeObject instead of write
        }
    }
}