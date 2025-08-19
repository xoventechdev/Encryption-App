package dev.xoventech.encryptionapp;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

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
    private static final String TAG = "FileUtils";
    private static final String METADATA_FILE = "metadata.dat";

    public static class FileMetadata implements Serializable {
        private static final long serialVersionUID = 1L; // For serialization compatibility
        String fileName;
        String date;
        String type;
        String content;

        public FileMetadata(String fileName, String date, String type, String content) {
            this.fileName = fileName;
            this.date = date;
            this.type = type;
            this.content = content;
        }
    }

    public static boolean isExternalStorageWritable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static void saveFile(Context context, String fileName, String content, String type, boolean useExternalStorage) throws Exception {
        try {
            String timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
            FileOutputStream fos;
            if (useExternalStorage && isExternalStorageWritable()) {
                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, fileName);
                fos = new FileOutputStream(file);
            } else {
                fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            }
            fos.write(content.getBytes("UTF-8"));
            fos.close();

            List<FileMetadata> metadataList = loadMetadata(context);
            metadataList.add(new FileMetadata(fileName, timeStamp, type, content));
            saveMetadata(context, metadataList);
        } catch (Exception e) {
            Log.e(TAG, "Error saving file: " + e.getMessage(), e);
            throw e; // Re-throw to let caller handle
        }
    }

    public static String readFile(Context context, String fileName, boolean useExternalStorage) throws Exception {
        try {
            FileInputStream fis;
            if (useExternalStorage && isExternalStorageWritable()) {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
                fis = new FileInputStream(file);
            } else {
                fis = context.openFileInput(fileName);
            }
            byte[] buffer = new byte[1024];
            StringBuilder content = new StringBuilder();
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                content.append(new String(buffer, 0, bytesRead, "UTF-8"));
            }
            fis.close();
            return content.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error reading file: " + e.getMessage(), e);
            throw e;
        }
    }

    public static void deleteFile(Context context, String fileName, boolean useExternalStorage) throws Exception {
        try {
            if (useExternalStorage && isExternalStorageWritable()) {
                File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), fileName);
                if (file.exists()) file.delete();
            } else {
                context.deleteFile(fileName);
            }
            List<FileMetadata> metadataList = loadMetadata(context);
            metadataList.removeIf(metadata -> metadata.fileName.equals(fileName));
            saveMetadata(context, metadataList);
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file: " + e.getMessage(), e);
            throw e;
        }
    }

    public static void renameFile(Context context, String oldName, String newName, boolean useExternalStorage) throws Exception {
        try {
            List<FileMetadata> metadataList = loadMetadata(context);
            for (FileMetadata metadata : metadataList) {
                if (metadata.fileName.equals(oldName)) {
                    metadata.fileName = newName;
                    String content = readFile(context, oldName, useExternalStorage);
                    deleteFile(context, oldName, useExternalStorage);
                    saveFile(context, newName, content, metadata.type, useExternalStorage);
                    break;
                }
            }
            saveMetadata(context, metadataList);
        } catch (Exception e) {
            Log.e(TAG, "Error renaming file: " + e.getMessage(), e);
            throw e;
        }
    }

    public static List<FileMetadata> loadMetadata(Context context) {
        try {
            FileInputStream fis = context.openFileInput(METADATA_FILE);
            ObjectInputStream ois = new ObjectInputStream(fis);
            List<FileMetadata> metadataList = (List<FileMetadata>) ois.readObject();
            ois.close();
            fis.close();
            return metadataList;
        } catch (Exception e) {
            Log.e(TAG, "Error loading metadata: " + e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private static void saveMetadata(Context context, List<FileMetadata> metadataList) throws Exception {
        try {
            FileOutputStream fos = context.openFileOutput(METADATA_FILE, Context.MODE_PRIVATE);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(metadataList);
            oos.close();
            fos.close();
        } catch (Exception e) {
            Log.e(TAG, "Error saving metadata: " + e.getMessage(), e);
            throw e;
        }
    }
}