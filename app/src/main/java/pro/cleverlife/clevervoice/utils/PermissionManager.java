package pro.cleverlife.clevervoice.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;

public class PermissionManager {
    private static final String TAG = "PermissionManager";

    // Основные разрешения для приложения
    public static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

    // Дополнительные разрешения для расширенного функционала
    public static final String[] OPTIONAL_PERMISSIONS = {
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.WRITE_SETTINGS
    };

    public static boolean hasAllRequiredPermissions(Context context) {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static String[] getMissingPermissions(Context context) {
        List<String> missingPermissions = new ArrayList<>();

        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        return missingPermissions.toArray(new String[0]);
    }

    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestPermissions(Activity activity, String[] permissions, int requestCode) {
        if (permissions.length > 0) {
            ActivityCompat.requestPermissions(activity, permissions, requestCode);
        }
    }

    public static boolean shouldShowRequestPermissionRationale(Activity activity, String permission) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission);
    }

    public static boolean isRecordAudioPermissionGranted(Context context) {
        return hasPermission(context, Manifest.permission.RECORD_AUDIO);
    }

    public static boolean isStoragePermissionGranted(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // На Android 11+ используется MANAGE_EXTERNAL_STORAGE
            return Environment.isExternalStorageManager();
        } else {
            return hasPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) &&
                    hasPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    public static boolean isWifiPermissionGranted(Context context) {
        return hasPermission(context, Manifest.permission.ACCESS_WIFI_STATE) &&
                hasPermission(context, Manifest.permission.CHANGE_WIFI_STATE);
    }

    public static boolean canWriteSystemSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.System.canWrite(context);
        }
        return true;
    }

    public static String getPermissionDescription(String permission) {
        switch (permission) {
            case Manifest.permission.RECORD_AUDIO:
                return "Разрешение на запись аудио необходимо для распознавания голосовых команд";

            case Manifest.permission.WRITE_EXTERNAL_STORAGE:
            case Manifest.permission.READ_EXTERNAL_STORAGE:
                return "Разрешение на доступ к хранилищу необходимо для работы с файлами";

            case Manifest.permission.ACCESS_WIFI_STATE:
            case Manifest.permission.CHANGE_WIFI_STATE:
                return "Разрешение на управление Wi-Fi необходимо для включения/выключения беспроводной сети";

            case Manifest.permission.BLUETOOTH:
            case Manifest.permission.BLUETOOTH_ADMIN:
                return "Разрешение на управление Bluetooth необходимо для работы с беспроводными устройствами";

            default:
                return "Это разрешение необходимо для полноценной работы приложения";
        }
    }
}