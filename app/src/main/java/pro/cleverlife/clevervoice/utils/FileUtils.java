package pro.cleverlife.clevervoice.utils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {
    private static final String TAG = "FileUtils";

    public static boolean copyAssetFile(Context context, String assetPath, String destinationPath) {
        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new FileOutputStream(destinationPath)) {

            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error copying asset file: " + assetPath, e);
            return false;
        }
    }

    public static boolean copyDirectoryFromAssets(Context context, String assetDir, String destinationDir) {
        try {
            String[] files = context.getAssets().list(assetDir);
            if (files == null || files.length == 0) {
                return false;
            }

            File destDir = new File(destinationDir);
            if (!destDir.exists() && !destDir.mkdirs()) {
                return false;
            }

            for (String file : files) {
                String assetFilePath = assetDir + File.separator + file;
                String destFilePath = destinationDir + File.separator + file;

                if (isAssetDirectory(context, assetFilePath)) {
                    copyDirectoryFromAssets(context, assetFilePath, destFilePath);
                } else {
                    copyAssetFile(context, assetFilePath, destFilePath);
                }
            }
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error copying directory from assets", e);
            return false;
        }
    }

    private static boolean isAssetDirectory(Context context, String assetPath) {
        try {
            String[] files = context.getAssets().list(assetPath);
            return files != null && files.length > 0;
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
        }
        return directory.delete();
    }

    public static String getFileExtension(String fileName) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }
        return "";
    }

    public static String humanReadableSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static boolean isStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    public static boolean isStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }
}
