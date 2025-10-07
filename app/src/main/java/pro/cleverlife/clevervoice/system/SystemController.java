package pro.cleverlife.clevervoice.system;

import android.content.Context;
import android.content.pm.PackageManager;
import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.core.content.ContextCompat;

import pro.cleverlife.clevervoice.utils.SettingsManager;

public class SystemController {
    private Context context;
    private AppLauncher appLauncher;
    private FileManager fileManager;
    private SettingsManager settingsManager;

    public SystemController(Context context) {
        this.context = context;
        this.appLauncher = new AppLauncher(context);
        this.fileManager = new FileManager(context);
        this.settingsManager = new SettingsManager(context);
    }

    public void launchApplication(String appName) {
        appLauncher.launchAppByName(appName);
    }

    public void moveFile(String source, String destination) {
        if (hasFilePermission()) {
            fileManager.moveFile(source, destination);
        }
    }

    public void toggleWifi(boolean enable) {
        if (hasWriteSettingsPermission()) {
            settingsManager.setWifiEnabled(enable);
        }
    }

    public void toggleBluetooth(boolean enable) {
        if (hasBluetoothPermission()) {
            settingsManager.setBluetoothEnabled(enable);
        }
    }

    public void setVolume(int volumeLevel) {
        settingsManager.setVolume(volumeLevel);
    }

    public void increaseVolume() {
        settingsManager.increaseVolume();
    }

    public void decreaseVolume() {
        settingsManager.decreaseVolume();
    }

    public void setBrightness(int brightness) {
        settingsManager.setBrightness(brightness);
    }

    public void setAutoRotate(boolean enabled) {
        settingsManager.setAutoRotateEnabled(enabled);
    }

    private boolean hasFilePermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasWriteSettingsPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            return android.provider.Settings.System.canWrite(context);
        }
        return true;
    }

    private boolean hasBluetoothPermission() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED;
    }

    // Геттеры для менеджеров (если нужен прямой доступ)
    public AppLauncher getAppLauncher() {
        return appLauncher;
    }

    public FileManager getFileManager() {
        return fileManager;
    }

    public SettingsManager getSettingsManager() {
        return settingsManager;
    }
}