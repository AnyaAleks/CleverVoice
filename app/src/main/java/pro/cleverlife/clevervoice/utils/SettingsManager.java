package pro.cleverlife.clevervoice.utils;

import android.content.Context;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.bluetooth.BluetoothAdapter;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.core.content.ContextCompat;

public class SettingsManager {
    private static final String TAG = "SettingsManager";

    private Context context;
    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private AudioManager audioManager;

    public SettingsManager(Context context) {
        this.context = context;
        initializeManagers();
    }

    private void initializeManagers() {
        try {
            wifiManager = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        } catch (Exception e) {
            Log.e(TAG, "Error initializing system managers", e);
        }
    }

    // Wi-Fi управление
    public boolean setWifiEnabled(boolean enabled) {
        try {
            if (wifiManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // На Android 10+ требуется специальное разрешение
                    if (!hasWifiPermission()) {
                        Log.w(TAG, "No permission to change WiFi state");
                        return false;
                    }
                }
                return wifiManager.setWifiEnabled(enabled);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when changing WiFi state", e);
        } catch (Exception e) {
            Log.e(TAG, "Error changing WiFi state", e);
        }
        return false;
    }

    public boolean isWifiEnabled() {
        try {
            return wifiManager != null && wifiManager.isWifiEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Error checking WiFi state", e);
            return false;
        }
    }

    // Bluetooth управление
    public boolean setBluetoothEnabled(boolean enabled) {
        try {
            if (bluetoothAdapter != null) {
                if (enabled) {
                    return bluetoothAdapter.enable();
                } else {
                    return bluetoothAdapter.disable();
                }
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when changing Bluetooth state", e);
        } catch (Exception e) {
            Log.e(TAG, "Error changing Bluetooth state", e);
        }
        return false;
    }

    public boolean isBluetoothEnabled() {
        try {
            return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
        } catch (Exception e) {
            Log.e(TAG, "Error checking Bluetooth state", e);
            return false;
        }
    }

    // Управление громкостью
    public void setVolume(int volumeLevel) {
        try {
            if (audioManager != null) {
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int actualVolume = Math.min(volumeLevel, maxVolume);
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, actualVolume, 0);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when changing volume", e);
        } catch (Exception e) {
            Log.e(TAG, "Error changing volume", e);
        }
    }

    public void increaseVolume() {
        adjustVolume(AudioManager.ADJUST_RAISE);
    }

    public void decreaseVolume() {
        adjustVolume(AudioManager.ADJUST_LOWER);
    }

    private void adjustVolume(int direction) {
        try {
            if (audioManager != null) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, 0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error adjusting volume", e);
        }
    }

    public int getCurrentVolume() {
        try {
            if (audioManager != null) {
                return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting current volume", e);
        }
        return 0;
    }

    public int getMaxVolume() {
        try {
            if (audioManager != null) {
                return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting max volume", e);
        }
        return 0;
    }

    // Управление яркостью (требует специальных разрешений)
    public boolean setBrightness(int brightness) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.System.canWrite(context)) {
                    int normalizedBrightness = Math.max(0, Math.min(255, brightness));
                    Settings.System.putInt(context.getContentResolver(),
                            Settings.System.SCREEN_BRIGHTNESS, normalizedBrightness);
                    return true;
                } else {
                    // Запрос разрешения на изменение системных настроек
                    requestWriteSettingsPermission();
                    return false;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting brightness", e);
        }
        return false;
    }

    public int getBrightness() {
        try {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Brightness setting not found", e);
            return 0;
        } catch (Exception e) {
            Log.e(TAG, "Error getting brightness", e);
            return 0;
        }
    }

    // Управление режимом "В самолете"
    public boolean setAirplaneMode(boolean enabled) {
        try {
            Settings.Global.putInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON, enabled ? 1 : 0);

            // Broadcast изменения режима
            Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
            intent.putExtra("state", enabled);
            context.sendBroadcast(intent);
            return true;

        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when changing airplane mode", e);
        } catch (Exception e) {
            Log.e(TAG, "Error changing airplane mode", e);
        }
        return false;
    }

    public boolean isAirplaneModeEnabled() {
        try {
            return Settings.Global.getInt(context.getContentResolver(),
                    Settings.Global.AIRPLANE_MODE_ON) != 0;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Airplane mode setting not found", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking airplane mode", e);
            return false;
        }
    }

    // Управление поворотом экрана
    public void setAutoRotateEnabled(boolean enabled) {
        try {
            Settings.System.putInt(context.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION, enabled ? 1 : 0);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception when changing auto-rotate", e);
        } catch (Exception e) {
            Log.e(TAG, "Error changing auto-rotate", e);
        }
    }

    public boolean isAutoRotateEnabled() {
        try {
            return Settings.System.getInt(context.getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION) != 0;
        } catch (Settings.SettingNotFoundException e) {
            Log.e(TAG, "Auto-rotate setting not found", e);
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error checking auto-rotate", e);
            return false;
        }
    }

    // Проверка разрешений
    private boolean hasWifiPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.CHANGE_WIFI_STATE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasBluetoothPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
    }

    // Запрос разрешения на запись системных настроек
    private void requestWriteSettingsPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
    }

    // Утилиты для преобразования значений
    public int convertBrightnessPercentageToValue(int percentage) {
        return (int) (255 * (percentage / 100.0));
    }

    public int convertBrightnessValueToPercentage(int value) {
        return (int) ((value / 255.0) * 100);
    }

    public int convertVolumePercentageToValue(int percentage, int maxVolume) {
        return (int) (maxVolume * (percentage / 100.0));
    }

    public int convertVolumeValueToPercentage(int value, int maxVolume) {
        return (int) ((value / (double) maxVolume) * 100);
    }
}