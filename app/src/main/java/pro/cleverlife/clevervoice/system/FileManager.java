package pro.cleverlife.clevervoice.system;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class FileManager {
    private static final String TAG = "FileManager";

    private Context context;
    private Map<String, String> pathAliases;

    public FileManager(Context context) {
        this.context = context;
        initializePathAliases();
    }

    private void initializePathAliases() {
        pathAliases = new HashMap<>();
        pathAliases.put("внутренняя память", Environment.getExternalStorageDirectory().getPath());
        pathAliases.put("internal", Environment.getExternalStorageDirectory().getPath());
        pathAliases.put("карта памяти", "/sdcard");
        pathAliases.put("sdcard", "/sdcard");
        pathAliases.put("downloads", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        pathAliases.put("загрузки", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath());
        pathAliases.put("documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath());
        pathAliases.put("документы", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getPath());
        pathAliases.put("pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath());
        pathAliases.put("фото", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getPath());
        pathAliases.put("music", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath());
        pathAliases.put("музыка", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getPath());
    }

    public boolean moveFile(String sourcePath, String destinationPath) {
        try {
            File sourceFile = resolvePath(sourcePath);
            File destinationDir = resolvePath(destinationPath);

            if (!sourceFile.exists()) {
                Log.e(TAG, "Source file does not exist: " + sourcePath);
                return false;
            }

            if (!destinationDir.exists() && !destinationDir.mkdirs()) {
                Log.e(TAG, "Cannot create destination directory: " + destinationPath);
                return false;
            }

            File destinationFile = new File(destinationDir, sourceFile.getName());

            if (sourceFile.renameTo(destinationFile)) {
                Log.i(TAG, "File moved successfully: " + sourceFile.getName());
                return true;
            } else {
                // Попробовать копирование и удаление оригинала
                return copyAndDelete(sourceFile, destinationFile);
            }

        } catch (Exception e) {
            Log.e(TAG, "Error moving file", e);
            return false;
        }
    }

    private boolean copyAndDelete(File source, File destination) {
        try (InputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }

            // Удалить оригинал после успешного копирования
            return source.delete();

        } catch (IOException e) {
            Log.e(TAG, "Error copying file", e);
            return false;
        }
    }

    public boolean copyFile(String sourcePath, String destinationPath) {
        try {
            File sourceFile = resolvePath(sourcePath);
            File destinationFile = resolvePath(destinationPath);

            return copyFile(sourceFile, destinationFile);

        } catch (Exception e) {
            Log.e(TAG, "Error copying file", e);
            return false;
        }
    }

    private boolean copyFile(File source, File destination) {
        try (FileInputStream in = new FileInputStream(source);
             FileOutputStream out = new FileOutputStream(destination)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error copying file", e);
            return false;
        }
    }

    public boolean deleteFile(String filePath) {
        try {
            File file = resolvePath(filePath);
            if (file.exists()) {
                return file.delete();
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error deleting file", e);
            return false;
        }
    }

    public String[] listFiles(String directoryPath) {
        try {
            File directory = resolvePath(directoryPath);
            if (directory.exists() && directory.isDirectory()) {
                return directory.list();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error listing files", e);
        }
        return new String[0];
    }

    private File resolvePath(String path) {
        // Замена алиасов путей
        for (Map.Entry<String, String> alias : pathAliases.entrySet()) {
            if (path.toLowerCase().contains(alias.getKey())) {
                path = path.toLowerCase().replace(alias.getKey(), alias.getValue());
                break;
            }
        }

        // Очистка пути от лишних пробелов
        path = path.trim().replaceAll("\\s+", " ");

        return new File(path);
    }

    public void addPathAlias(String alias, String realPath) {
        pathAliases.put(alias.toLowerCase(), realPath);
    }

    public boolean createDirectory(String path) {
        try {
            File directory = resolvePath(path);
            return directory.mkdirs();
        } catch (Exception e) {
            Log.e(TAG, "Error creating directory", e);
            return false;
        }
    }

    public long getFileSize(String filePath) {
        try {
            File file = resolvePath(filePath);
            return file.length();
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size", e);
            return -1;
        }
    }
}