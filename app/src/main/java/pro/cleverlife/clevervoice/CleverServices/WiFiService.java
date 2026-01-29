package pro.cleverlife.clevervoice.CleverServices;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.Manifest;

import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiNetworkSpecifier;
import android.net.wifi.WifiNetworkSuggestion;
import android.os.Build;
import android.net.DhcpInfo;
import java.io.IOException;

import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;

public class WiFiService {

    private static final String TAG = "WiFiService";
    private static WifiManager wifiManager;
    private static Context context;
    private static ArrayList<String> arrayList = new ArrayList<>();

    private static int isCPPLoaded = 0;

    private NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    public WiFiService(Context context) {
        WiFiService.context = context.getApplicationContext();
        this.wifiManager = (WifiManager) WiFiService.context.getSystemService(Context.WIFI_SERVICE);

        // Регистрация BroadcastReceiver
        try {
            IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            WiFiService.context.registerReceiver(networkChangeReceiver, filter);
            Log.i(TAG, "NetworkChangeReceiver зарегистрирован");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при регистрации NetworkChangeReceiver", e);
        }
    }

    // ========== ROOT МЕТОДЫ ==========

    //Выполнение команды с root правами
    private String executeRootCommand(String command) {
        Process process = null;
        DataOutputStream os = null;
        BufferedReader reader = null;
        BufferedReader errorReader = null;

        try {
            Log.d(TAG, "Executing root command: " + command);

            // Запрашиваем root права
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());

            // Пишем команду
            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();

            // Ждем выполнения
            process.waitFor();

            // Читаем вывод
            reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            // Читаем ошибки
            errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            Log.d(TAG, "Command output: " + output.toString());
            if (errorOutput.length() > 0) {
                Log.e(TAG, "Command error: " + errorOutput.toString());
            }

            return output.toString();

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error executing root command", e);
            return "Error: " + e.getMessage();
        } finally {
            try {
                if (os != null) os.close();
                if (reader != null) reader.close();
                if (errorReader != null) errorReader.close();
                if (process != null) process.destroy();
            } catch (IOException e) {
                Log.e(TAG, "Error closing streams", e);
            }
        }
    }

    //Проверка наличия root доступа
    public boolean hasRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su -c id");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            process.waitFor();

            if (output != null && output.contains("uid=0")) {
                Log.i(TAG, "Root доступ есть: " + output);
                return true;
            }

            Log.i(TAG, "Root доступа нет");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка проверки root", e);
            return false;
        }
    }

    //Включение WiFi через root
    public boolean enableWifi() {
        try {
            Log.i(TAG, "Включаем WiFi через root...");

            // Проверяем, может уже включен
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WiFi уже включен");
                return true;
            }

            // Способ 1: svc wifi enable
            executeRootCommand("svc wifi enable");

            Thread.sleep(2000);

            // Проверяем состояние
            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WiFi успешно включен через svc wifi enable");
                return true;
            }

            // Способ 2: settings put global wifi_on
            Log.i(TAG, "Пробуем альтернативный метод...");
            executeRootCommand("settings put global wifi_on 1");
            executeRootCommand("am broadcast -a android.net.wifi.WIFI_STATE_CHANGED");

            Thread.sleep(2000);

            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WiFi включен альтернативным методом");
                return true;
            }

            // Способ 3: Прямое управление через wpa_supplicant
            Log.i(TAG, "Пробуем управление через wpa_supplicant...");
            executeRootCommand("setprop wifi.interface wlan0");
            executeRootCommand("setprop ctl.start wpa_supplicant");

            Thread.sleep(2000);

            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WiFi включен через wpa_supplicant");
                return true;
            }

            Log.e(TAG, "Не удалось включить WiFi ни одним методом");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при включении WiFi", e);
            return false;
        }
    }

    //Выключение WiFi через root
    public boolean disableWifi() {
        try {
            Log.i(TAG, "Выключаем WiFi через root...");

            // Проверяем, может уже выключен
            if (wifiManager != null && !wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WiFi уже выключен");
                return true;
            }

            // Способ 1: svc wifi disable
            executeRootCommand("svc wifi disable");

            Thread.sleep(2000);

            // Проверяем состояние
            if (wifiManager != null && !wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WiFi успешно выключен через svc wifi disable");
                return true;
            }

            // Способ 2: settings put global wifi_on
            Log.i(TAG, "Пробуем альтернативный метод...");
            executeRootCommand("settings put global wifi_on 0");
            executeRootCommand("am broadcast -a android.net.wifi.WIFI_STATE_CHANGED");

            Thread.sleep(2000);

            if (wifiManager != null && !wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WiFi выключен альтернативным методом");
                return true;
            }

            // Способ 3: Остановка wpa_supplicant
            Log.i(TAG, "Пробуем остановить wpa_supplicant...");
            executeRootCommand("setprop ctl.stop wpa_supplicant");

            Thread.sleep(2000);

            if (wifiManager != null && !wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WiFi выключен через остановку wpa_supplicant");
                return true;
            }

            Log.e(TAG, "Не удалось выключить WiFi ни одним методом");
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при выключении WiFi", e);
            return false;
        }
    }

    //Сброс WiFi (выключение и включение)
    public boolean resetWiFi() {
        try {
            Log.i(TAG, "Сбрасываем WiFi через root...");

            executeRootCommand("svc wifi disable");
            Log.i(TAG, "WiFi выключен");
            Thread.sleep(1000);

            executeRootCommand("svc wifi enable");
            Log.i(TAG, "WiFi включен");
            Thread.sleep(2000);

            if (wifiManager != null && wifiManager.isWifiEnabled()) {
                Log.i(TAG, "WiFi успешно сброшен");
                return true;
            }

            return false;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка сброса WiFi", e);
            return false;
        }
    }

    //Перезапуск WiFi сервиса
    public boolean restartWifiService() {
        try {
            Log.i(TAG, "Перезапускаем WiFi сервис...");

            executeRootCommand("stop wpa_supplicant");
            Log.i(TAG, "wpa_supplicant остановлен");
            Thread.sleep(1000);

            executeRootCommand("start wpa_supplicant");
            Log.i(TAG, "wpa_supplicant запущен");
            Thread.sleep(2000);

            Log.i(TAG, "WiFi сервис перезапущен");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка перезапуска WiFi сервиса", e);
            return false;
        }
    }

    //Получение детальной информации о WiFi через root
    public String getWiFiInfo() {
        try {
            StringBuilder info = new StringBuilder();

            // Базовая информация
            if (wifiManager != null) {
                info.append("=== Базовая информация ===\n");
                info.append("WiFi включен: ").append(wifiManager.isWifiEnabled() ? "Да" : "Нет").append("\n");

                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                if (wifiInfo != null) {
                    info.append("SSID: ").append(wifiInfo.getSSID()).append("\n");
                    info.append("BSSID: ").append(wifiInfo.getBSSID()).append("\n");
                    info.append("Сигнал: ").append(wifiInfo.getRssi()).append(" dBm\n");
                    info.append("Частота: ").append(wifiInfo.getFrequency()).append(" MHz\n");
                }
            }

            // Root информация
            info.append("\n=== Root информация ===\n");
            info.append("dumpsys wifi:\n");
            info.append(executeRootCommand("dumpsys wifi | head -50"));

            info.append("\niwconfig:\n");
            info.append(executeRootCommand("iwconfig wlan0 2>/dev/null || echo 'iwconfig не доступен'"));

            info.append("\nifconfig wlan0:\n");
            info.append(executeRootCommand("ifconfig wlan0 2>/dev/null || echo 'wlan0 не найден'"));

            info.append("\nСписок сетей:\n");
            info.append(executeRootCommand("wpa_cli -i wlan0 list_networks 2>/dev/null || echo 'wpa_cli не доступен'"));

            return info.toString();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения WiFi информации", e);
            return "Ошибка: " + e.getMessage();
        }
    }

    //Очистка всех сохраненных WiFi сетей
    public boolean forgetAllNetworks() {
        try {
            Log.i(TAG, "Удаляем все сохраненные сети WiFi...");

            String result = executeRootCommand("wpa_cli -i wlan0 list_networks");
            if (result.contains("network id")) {
                // Извлекаем ID сетей и удаляем их
                String[] lines = result.split("\n");
                for (String line : lines) {
                    if (line.matches("^\\d+\\s+.*")) {
                        String networkId = line.split("\\s+")[0];
                        executeRootCommand("wpa_cli -i wlan0 remove_network " + networkId);
                        Log.i(TAG, "Удалена сеть с ID: " + networkId);
                    }
                }
                executeRootCommand("wpa_cli -i wlan0 save_config");
            }

            Log.i(TAG, "Все сети удалены");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка удаления сетей", e);
            return false;
        }
    }

    // ========== СТАРЫЕ МЕТОДЫ ==========

    public static void requestLocationPermission(Context _context_) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("WiFiService", "Permission ACCESS_FINE_LOCATION NOT granted");
        } else {
            Log.i("WiFiService", "Permission ACCESS_FINE_LOCATION granted");
            startScan(_context_);
        }
    }

    public static boolean checkLocationPermission() {
        if (context == null) {
            Log.e("WiFiService", "Context is null");
            return false;
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    public static void checkGPSEnable(){
        if (isGPSEnabled(context)) {
            Log.i("WiFiService", "GPS включен");
        } else {
            Toast.makeText(context, "Включите GPS", Toast.LENGTH_SHORT).show();
        }
    }

    public static void checkWiFiEnable(){
        if (isWiFiEnabled(context)) {
            Log.i("WiFiService", "Wi-Fi включен");
        } else {
            Toast.makeText(context, "Включите Wi-Fi", Toast.LENGTH_SHORT).show();
        }
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("WiFiService", "Разрешение на геолокацию предоставлено");
            } else {
                Toast.makeText(context, "Разрешение на использование геолокации отклонено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void startScan(Context _context_) {
        Log.i("WiFiService", "Начинаем сканирование WiFi");

        if (_context_ == null) {
            Log.e("WiFiService", "Context is null");
            return;
        }

        WifiManager localWifiManager = (WifiManager) _context_.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (localWifiManager == null) {
            Log.e("WiFiService", "Не удалось получить WifiManager");
            return;
        }

        arrayList.clear();
        Log.i("WiFiService", "Очищен список сетей");

        try {
            _context_.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            Log.i("WiFiService", "BroadcastReceiver зарегистрирован");

            boolean scanStarted = localWifiManager.startScan();
            if (scanStarted) {
                Log.i("WiFiService", "Сканирование WiFi запущено успешно");
            } else {
                Log.e("WiFiService", "Не удалось запустить сканирование WiFi");
            }
        } catch (Exception e) {
            Log.e("WiFiService", "Ошибка при запуске сканирования", e);
        }
    }

    private static final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "Получены результаты сканирования, isCPPLoaded=" + isCPPLoaded);

            WifiManager localWifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (localWifiManager == null) {
                Log.e(TAG, "WifiManager не доступен в onReceive");
                return;
            }

            // Проверяем разрешение на геолокацию
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "Нет разрешения на геолокацию в onReceive");
                return;
            }

            try {
                List<ScanResult> results = localWifiManager.getScanResults();
                Log.i(TAG, "BroadcastReceiver: Получено " + (results != null ? results.size() : 0) + " сетей");

                if (results != null && !results.isEmpty()) {
                    // Логируем первые 5 сетей
                    for (int i = 0; i < Math.min(results.size(), 5); i++) {
                        ScanResult result = results.get(i);
                        Log.i(TAG, String.format("Сеть %d: %s, сигнал: %d dBm, защита: %s",
                                i + 1,
                                result.SSID.isEmpty() ? "[Скрытая]" : result.SSID,
                                result.level,
                                isNetworkSecured(result.capabilities) ? "Да" : "Нет"));
                    }

                    // Отправляем уведомление в MainActivity
                    sendScanResultsToUI(results);
                }

                // Вызываем C++ код если нужно
                if (isCPPLoaded == 1) {
                    ArrayList<String> arrayList = new ArrayList<>();
                    for (ScanResult scanResult : results) {
                        String networkInfo = scanResult.SSID + "," +
                                isNetworkSecured(scanResult.capabilities) + "," +
                                Math.abs(scanResult.level) + "," +
                                scanResult.BSSID;
                        arrayList.add(networkInfo);
                    }
                    callFromJava(arrayList);
                }

            } catch (Exception e) {
                Log.e(TAG, "Ошибка в BroadcastReceiver", e);
            } finally {
                try {
                    context.unregisterReceiver(this);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка отмены регистрации receiver", e);
                }
            }
        }
    };

    // Метод для отправки результатов в UI
    private static void sendScanResultsToUI(List<ScanResult> results) {
        if (context == null) return;

        Intent intent = new Intent("WIFI_SCAN_RESULTS");
        intent.putExtra("count", results.size());

        // Отправляем информацию о первых 3 сетях
        if (results.size() > 0) {
            ScanResult r = results.get(0);
            intent.putExtra("network1", r.SSID + " (" + r.level + " dBm)");
        }
        if (results.size() > 1) {
            ScanResult r = results.get(1);
            intent.putExtra("network2", r.SSID + " (" + r.level + " dBm)");
        }
        if (results.size() > 2) {
            ScanResult r = results.get(2);
            intent.putExtra("network3", r.SSID + " (" + r.level + " dBm)");
        }

        context.sendBroadcast(intent);
    }

    public static boolean isNetworkSecured(String capabilities) {
        return capabilities.contains("WPA") || capabilities.contains("WPA2") ||
                capabilities.contains("WEP") || capabilities.contains("PSK") ||
                capabilities.contains("EAP");
    }

    public static boolean isGPSEnabled(Context context) {
        try {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception e) {
            Log.e("WiFiService", "Ошибка при проверке GPS", e);
            return false;
        }
    }

    public static boolean isWiFiEnabled(Context context) {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
                return networkInfo != null && networkInfo.isConnected();
            }
            return false;
        } catch (Exception e) {
            Log.e("WiFiService", "Ошибка при проверке WiFi", e);
            return false;
        }
    }

    public static String getWifiStatus(Context _context) {
        if (_context == null) {
            return "-1";
        }

        if (!checkLocationPermission()) {
            return "-2";
        }

        WifiManager localWifiManager = (WifiManager) _context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (localWifiManager == null) {
            return "-1";
        }

        if (!localWifiManager.isWifiEnabled()) {
            return "0";
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return "1";
        }

        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null && networkInfo.isConnected()) {
            WifiInfo wifiInfo = localWifiManager.getConnectionInfo();
            String bssid = wifiInfo.getBSSID();
            int ipAddress = wifiInfo.getIpAddress();

            String ipString = String.format(
                    "%d.%d.%d.%d",
                    (ipAddress & 0xff),
                    (ipAddress >> 8 & 0xff),
                    (ipAddress >> 16 & 0xff),
                    (ipAddress >> 24 & 0xff)
            );

            DhcpInfo dhcpInfo = localWifiManager.getDhcpInfo();
            int netmask = dhcpInfo.netmask;

            String netmaskString = String.format(
                    "%d.%d.%d.%d",
                    (netmask & 0xff),
                    (netmask >> 8 & 0xff),
                    (netmask >> 16 & 0xff),
                    (netmask >> 24 & 0xff)
            );

            List<ScanResult> results = localWifiManager.getScanResults();
            for (ScanResult scanResult : results) {
                if (scanResult.BSSID != null && scanResult.BSSID.equals(bssid)) {
                    return "2," + scanResult.SSID + "," +
                            isNetworkSecured(scanResult.capabilities) + "," +
                            Math.abs(scanResult.level) + "," +
                            scanResult.BSSID + "," +
                            ipString + "," +
                            netmaskString;
                }
            }
            return "2,Unknown,,,,,";
        } else {
            return "1";
        }
    }

    public static int getWifiStatusOnly(Context _context) {
        if (_context == null) {
            return -1;
        }

        if (!checkLocationPermission()) {
            return -1;
        }

        WifiManager localWifiManager = (WifiManager) _context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (localWifiManager == null) {
            return -1;
        }

        if (!localWifiManager.isWifiEnabled()) {
            return 0;
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return 1;
        }

        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        if (networkInfo != null && networkInfo.isConnected()) {
            return 2;
        } else {
            return 1;
        }
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                int wifiStatus = getWifiStatusOnly(context);
                Log.i("WiFiService", "Состояние сети изменилось: " + wifiStatus);

                if (isCPPLoaded != 1) {
                    return;
                }

                callFromJavaWiFiStateChanged(wifiStatus);
            }
        }
    }

    public static void connectToWifi(Context _context, String ssid, String password){
        Log.i("WiFiService", "Попытка подключения к WiFi: " + ssid);

        if (_context == null) {
            Log.e("WiFiService", "Context is null");
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.i("WiFiService", "SDK версия ниже необходимой (Q/29)");
            return;
        }

        try {
            final WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setWpa2Passphrase(password)
                    .setIsHiddenSsid(false)
                    .build();

            final List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
            suggestionsList.add(suggestion);

            WifiManager wifiManager = (WifiManager) _context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            // Удаляем старые предложения
            wifiManager.removeNetworkSuggestions(suggestionsList);

            // Добавляем новые
            int status = wifiManager.addNetworkSuggestions(suggestionsList);
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                Log.i("WiFiService", "Сетевое предложение успешно добавлено");
                Toast.makeText(_context, "Подключение к " + ssid + " инициировано", Toast.LENGTH_SHORT).show();
            } else {
                Log.e("WiFiService", "Ошибка при добавлении сетевого предложения: " + status);
            }
        } catch (Exception e) {
            Log.e("WiFiService", "Ошибка при подключении к WiFi", e);
        }
    }

    public static void disconnectFromWifi(Context _context) {
        Log.i("WiFiService", "Попытка отключиться от WiFi");

        if (_context == null) {
            Log.e("WiFiService", "Context is null");
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.i("WiFiService", "SDK версия ниже необходимой (Q/29)");
            return;
        }

        try {
            WifiManager wifiManager = (WifiManager) _context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

            // Получаем текущие предложения
            List<WifiNetworkSuggestion> currentSuggestions = Collections.emptyList();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                currentSuggestions = wifiManager.getNetworkSuggestions();
            }

            // Удаляем все предложения
            if (!currentSuggestions.isEmpty()) {
                int status = wifiManager.removeNetworkSuggestions(currentSuggestions);
                if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
                    Log.i("WiFiService", "Все сетевые предложения удалены");
                    Toast.makeText(_context, "Отключение от WiFi выполнено", Toast.LENGTH_SHORT).show();
                }
            }

            // Также отключаемся от текущей сети
            wifiManager.disconnect();

        } catch (Exception e) {
            Log.e("WiFiService", "Ошибка при отключении от WiFi", e);
        }
    }

    private static native void callFromJava(ArrayList<String> results);
    private static native void callFromJavaWiFiStateChanged(int wifiState);

    public static void setCPPLoaded(int value) {
        isCPPLoaded = value;
        Log.i("WiFiService", "isCPPLoaded установлен в: " + value);
    }

    public void unregisterReceiver() {
        try {
            if (context != null && networkChangeReceiver != null) {
                context.unregisterReceiver(networkChangeReceiver);
                Log.i("WiFiService", "NetworkChangeReceiver отменен");
            }
        } catch (Exception e) {
            Log.e("WiFiService", "Ошибка при отмене NetworkChangeReceiver", e);
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    public boolean isWifiEnabled() {
        return wifiManager != null && wifiManager.isWifiEnabled();
    }

    public WifiManager getWifiManager() {
        return wifiManager;
    }

    public static List<ScanResult> getScanResults() {
        if (context == null || wifiManager == null) {
            Log.e(TAG, "Context или WifiManager не инициализированы");
            return new ArrayList<>();
        }

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Нет разрешения на геолокацию");
            return new ArrayList<>();
        }

        try {
            List<ScanResult> results = wifiManager.getScanResults();
            Log.i(TAG, "Получено " + (results != null ? results.size() : 0) + " сетей");
            return results != null ? results : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения результатов сканирования", e);
            return new ArrayList<>();
        }
    }

    public static String startScanWithResults(Context _context) {
        Log.i(TAG, "Начинаем сканирование WiFi с возвратом результатов");

        if (_context == null) {
            Log.e(TAG, "Context is null");
            return "Ошибка: контекст не инициализирован";
        }

        WifiManager localWifiManager = (WifiManager) _context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (localWifiManager == null) {
            Log.e(TAG, "Не удалось получить WifiManager");
            return "Ошибка: WifiManager недоступен";
        }

        // Проверяем разрешение
        if (ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Нет разрешения на геолокацию");
            return "Ошибка: нет разрешения на геолокацию";
        }

        // Проверяем, включен ли WiFi
        if (!localWifiManager.isWifiEnabled()) {
            Log.i(TAG, "WiFi выключен, пытаемся включить для сканирования");
            try {
                Process process = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(process.getOutputStream());
                os.writeBytes("svc wifi enable\n");
                os.writeBytes("exit\n");
                os.flush();
                process.waitFor();
                Thread.sleep(2000);
            } catch (Exception e) {
                Log.e(TAG, "Не удалось включить WiFi для сканирования", e);
                return "Ошибка: WiFi выключен и не удалось его включить";
            }
        }

        try {
            // Запускаем сканирование
            boolean scanStarted = localWifiManager.startScan();
            if (!scanStarted) {
                Log.e(TAG, "Не удалось запустить сканирование");
                return "Ошибка: не удалось запустить сканирование";
            }

            Log.i(TAG, "Сканирование запущено, ждем результаты...");

            // Ждем результатов сканирования
            for (int i = 0; i < 10; i++) {
                Thread.sleep(500);
                List<ScanResult> results = localWifiManager.getScanResults();
                if (results != null && !results.isEmpty()) {
                    Log.i(TAG, "Найдено " + results.size() + " сетей");

                    // Форматируем результаты
                    StringBuilder resultBuilder = new StringBuilder();
                    resultBuilder.append("Найдено ").append(results.size()).append(" WiFi сетей:\n\n");

                    for (int j = 0; j < Math.min(results.size(), 15); j++) { // Показываем максимум 15 сетей
                        ScanResult result = results.get(j);
                        String ssid = result.SSID.isEmpty() ? "[Скрытая сеть]" : result.SSID;
                        String security = isNetworkSecured(result.capabilities) ? "Защищена" : "Открыта";
                        int signal = Math.abs(result.level);
                        String signalStrength;

                        if (signal < 50) signalStrength = "Отличный";
                        else if (signal < 70) signalStrength = "Хороший";
                        else if (signal < 85) signalStrength = "Средний";
                        else signalStrength = "Слабый";

                        resultBuilder.append(String.format("%d. %s\n", j + 1, ssid))
                                .append(String.format("   %s | %s (%d dBm)\n", security, signalStrength, signal))
                                .append(String.format("   Канал: %d МГц | MAC: %s\n\n",
                                        result.frequency, result.BSSID));
                    }

                    if (results.size() > 15) {
                        resultBuilder.append("... и еще ").append(results.size() - 15).append(" сетей");
                    }

                    return resultBuilder.toString();
                }
            }

            Log.i(TAG, "Не найдено ни одной сети или таймаут");
            return "Не найдено ни одной WiFi сети или превышено время ожидания";

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сканировании WiFi", e);
            return "Ошибка сканирования: " + e.getMessage();
        }
    }

    // Добавьте метод для получения сетей через root
    public String scanNetworksWithRoot() {
        try {
            Log.i(TAG, "Сканируем WiFi сети через root...");

            // Включаем WiFi если выключен
            if (!isWifiEnabled()) {
                Log.i(TAG, "WiFi выключен, включаем...");
                enableWifi();
                Thread.sleep(2000);
            }

            // Команда для сканирования через wpa_cli
            String result = executeRootCommand("wpa_cli -i wlan0 scan");
            Log.i(TAG, "Результат scan команды: " + result);

            // Ждем сканирования
            Thread.sleep(3000);

            // Получаем результаты
            result = executeRootCommand("wpa_cli -i wlan0 scan_results");
            Log.i(TAG, "Результаты сканирования: " + result);

            // Парсим результаты
            StringBuilder networks = new StringBuilder();
            String[] lines = result.split("\n");
            int networkCount = 0;

            networks.append("=== WiFi СЕТИ (через root) ===\n\n");

            for (String line : lines) {
                // Пример строки: bssid / frequency / signal level / flags / ssid
                if (line.matches("[0-9a-fA-F:]{17}\\s+.*")) {
                    String[] parts = line.split("\\t");
                    if (parts.length >= 5) {
                        networkCount++;
                        String bssid = parts[0];
                        String frequency = parts[1];
                        String signal = parts[2];
                        String flags = parts[3];
                        String ssid = parts[4];

                        boolean isSecured = flags.contains("WPA") || flags.contains("WEP") || flags.contains("PSK");

                        networks.append(String.format("%d. %s\n", networkCount, ssid.isEmpty() ? "[Скрытая]" : ssid))
                                .append(String.format("   MAC: %s\n", bssid))
                                .append(String.format("   Частота: %s МГц\n", frequency))
                                .append(String.format("   Сигнал: %s dBm\n", signal))
                                .append(String.format("   Защита: %s\n", isSecured ? "Да" : "Нет"))
                                .append("\n");
                    }
                }
            }

            if (networkCount == 0) {
                return "Не найдено WiFi сетей через root";
            }

            networks.append(String.format("\nВсего найдено: %d сетей", networkCount));
            return networks.toString();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка сканирования через root", e);
            return "Ошибка сканирования: " + e.getMessage();
        }
    }
}