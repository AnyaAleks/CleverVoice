package pro.cleverlife.clevervoice.system;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppLauncher {
    private static final String TAG = "AppLauncher";

    private Context context;
    private PackageManager packageManager;
    private Map<String, String> appPackageMap;

    public AppLauncher(Context context) {
        this.context = context;
        this.packageManager = context.getPackageManager();
        initializeAppMap();
    }

    private void initializeAppMap() {
        appPackageMap = new HashMap<>();

        // Основные системные приложения
        appPackageMap.put("chrome", "com.android.chrome");
        appPackageMap.put("браузер", "com.android.chrome");
        appPackageMap.put("messages", "com.android.messaging");
        appPackageMap.put("сообщения", "com.android.messaging");
        appPackageMap.put("contacts", "com.android.contacts");
        appPackageMap.put("контакты", "com.android.contacts");
        appPackageMap.put("phone", "com.android.dialer");
        appPackageMap.put("телефон", "com.android.dialer");
        appPackageMap.put("camera", "com.android.camera2");
        appPackageMap.put("камера", "com.android.camera2");
        appPackageMap.put("gallery", "com.android.gallery3d");
        appPackageMap.put("галерея", "com.android.gallery3d");
        appPackageMap.put("settings", "com.android.settings");
        appPackageMap.put("настройки", "com.android.settings");
        appPackageMap.put("calculator", "com.android.calculator2");
        appPackageMap.put("калькулятор", "com.android.calculator2");
        appPackageMap.put("email", "com.android.email");
        appPackageMap.put("почта", "com.android.email");
        appPackageMap.put("calendar", "com.android.calendar");
        appPackageMap.put("календарь", "com.android.calendar");
    }

    public boolean launchAppByName(String appName) {
        String packageName = findPackageName(appName);

        if (packageName != null) {
            return launchAppByPackage(packageName);
        } else {
            // Попытка найти приложение по имени через поиск
            return searchAndLaunchApp(appName);
        }
    }

    private String findPackageName(String appName) {
        // Прямое сопоставление по карте
        if (appPackageMap.containsKey(appName.toLowerCase())) {
            return appPackageMap.get(appName.toLowerCase());
        }

        // Поиск по частичному совпадению
        for (Map.Entry<String, String> entry : appPackageMap.entrySet()) {
            if (appName.toLowerCase().contains(entry.getKey())) {
                return entry.getValue();
            }
        }

        return null;
    }

    public boolean launchAppByPackage(String packageName) {
        try {
            Intent launchIntent = packageManager.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                Log.i(TAG, "Launched app: " + packageName);
                return true;
            } else {
                Log.w(TAG, "No launch intent for package: " + packageName);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching app: " + packageName, e);
            return false;
        }
    }

    private boolean searchAndLaunchApp(String appName) {
        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);

            for (ResolveInfo app : apps) {
                String label = app.loadLabel(packageManager).toString().toLowerCase();
                if (label.contains(appName.toLowerCase())) {
                    String packageName = app.activityInfo.packageName;
                    return launchAppByPackage(packageName);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error searching for app: " + appName, e);
        }

        return false;
    }

    public void addCustomAppMapping(String appKey, String packageName) {
        appPackageMap.put(appKey.toLowerCase(), packageName);
    }

    public Map<String, String> getInstalledApps() {
        Map<String, String> installedApps = new HashMap<>();

        try {
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

            List<ResolveInfo> apps = packageManager.queryIntentActivities(mainIntent, 0);

            for (ResolveInfo app : apps) {
                String label = app.loadLabel(packageManager).toString();
                String packageName = app.activityInfo.packageName;
                installedApps.put(label.toLowerCase(), packageName);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting installed apps", e);
        }

        return installedApps;
    }
}