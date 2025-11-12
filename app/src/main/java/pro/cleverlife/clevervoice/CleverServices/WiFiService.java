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
//import android.widget.ArrayAdapter;
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

//import android.widget.ArrayAdapter;
import android.widget.Toast;

public class WiFiService {

    private static WifiManager wifiManager;
    private static Context context;
    private static ArrayList<String> arrayList = new ArrayList<>();

    private static int isCPPLoaded = 0;

    private NetworkChangeReceiver networkChangeReceiver = new NetworkChangeReceiver();

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    public WiFiService(Context context) {
        WiFiService.context = context;
        WiFiService.wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // Регистрация BroadcastReceiver
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.registerReceiver(networkChangeReceiver, filter);
    }

    public static void requestLocationPermission(Context _context_) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.i("WiFiService", "Permission ACCESS_FINE_LOCATION NOT granted");
            //ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            Log.i("WiFiService", "Permission ACCESS_FINE_LOCATION granted");
            startScan(_context_);
        }
    }

    public static boolean checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return false;
        } else {
            return true;
        }
    }

    public static void checkGPSEnable(){
        // Пример использования функции
        if (isGPSEnabled(context)) {
            //Toast.makeText(context, "GPS включен", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(context, "Включите GPS", Toast.LENGTH_SHORT).show();
        }
    }

    public static void checkWiFiEnable(){
        // Пример использования функции
        if (isWiFiEnabled(context)) {
            //Log.i("hhh", "1");
            //Toast.makeText(context, "Wi-Fi включен", Toast.LENGTH_SHORT).show();
        } else {
            //Log.i("hhh", "2");
            Toast.makeText(context, "Включите Wi-Fi", Toast.LENGTH_SHORT).show();
        }
    }

    public static void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            Log.i("JJJ", "LLL");
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //startScan();
            } else {
                Toast.makeText(context, "Разрешение на использование геолокации было запрошено и отклонено", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public static void startScan(Context _context_) {
        Log.i("WiFiService", "We start scanning - " + _context_);
        wifiManager = (WifiManager) _context_.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager == null){
            Log.i("WiFiService", "wifiManager not create");
            return;
        }

        //ВКЛЮЧЕНИЕ WIFI РЕАЛИЗОВАНО ЧЕРЕЗ ЛАУНЧЕР
        // if (!wifiManager.isWifiEnabled()) {
        //     //Toast.makeText(context, "WiFi выключен", Toast.LENGTH_LONG).show();
        //     wifiManager.setWifiEnabled(true);
        // }

        Log.i("WiFiService", "wifi manager create");
        arrayList.clear();
        Log.i("WiFiService", "clear array list");
        _context_.registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        Log.i("WiFiService", "wifi manager start scan");
        wifiManager.startScan();
        Log.i("WiFiService", "wifi manager start scan OK");
        //Toast.makeText(context, "Идет сканирование WiFi", Toast.LENGTH_SHORT).show();
    }

    // private static final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
    //     @Override
    //     public void onReceive(Context context, Intent intent) {
    //     if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
    //         Toast.makeText(context, "Нет разрешения на использование геолокации", Toast.LENGTH_SHORT).show();
    //         return;
    //     }
    //     List<ScanResult> results = wifiManager.getScanResults();
    //     context.unregisterReceiver(this);
    //     for (ScanResult scanResult : results) {
    //         Log.i("MyWiFiList",scanResult.SSID + "," + isNetworkSecured(scanResult.capabilities) + "," + scanResult.level + "," + scanResult.BSSID);
    //         arrayList.add(scanResult.SSID + "," + isNetworkSecured(scanResult.capabilities) + "," + scanResult.level + "," + scanResult.BSSID);
    //     }
    //     Log.i("MyWiFiListArray", arrayList.toString());
    //     //adapter.notifyDataSetChanged(); // Обновляем адаптер после получения результатов
    //     }

    // };
    private static final BroadcastReceiver wifiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("WiFiService", "onReceive start " + isCPPLoaded);

            if(isCPPLoaded != 1){
                return;
            }
                //wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Нет разрешения на использование геолокации", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.i("WiFiService", "onReceive continue");


            // WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            // int ipAddress = wifiInfo.getIpAddress();

            // String ipString = String.format(
            //             "%d.%d.%d.%d",
            //             (ipAddress & 0xff),
            //             (ipAddress >> 8 & 0xff),
            //             (ipAddress >> 16 & 0xff),
            //             (ipAddress >> 24 & 0xff)
            //         );

            List<ScanResult> results = wifiManager.getScanResults();
            Log.i("WiFiService", "step 1");
            context.unregisterReceiver(this);
            Log.i("WiFiService", "step 2");
            ArrayList<String> arrayList = new ArrayList<>(); // создайте новый ArrayList для хранения результатов
            Log.i("WiFiService", "step 3");
            for (ScanResult scanResult : results) {
                Log.i("WiFiService", "step 4 " + scanResult);
                Log.i("MyWiFiList", scanResult.SSID + "," + isNetworkSecured(scanResult.capabilities) + "," + Math.abs(scanResult.level) + "," + scanResult.BSSID /*+ "," + ipString*/);
                arrayList.add(scanResult.SSID + "," + isNetworkSecured(scanResult.capabilities) + "," + Math.abs(scanResult.level) + "," + scanResult.BSSID /*+ "," + ipString*/);
            }
            Log.i("MyWiFiListArray", arrayList.toString());

            // Создание экземпляра вашего класса и вызов нативного метода
           //processScanResults(arrayList); // Вызов метода C++
           callFromJava(arrayList);

           Log.i("WiFiService", "onReceive OK");

        }
    };

    public static boolean isNetworkSecured(String capabilities) {
        // Проверяем, содержит ли строка указания на защиту
        return capabilities.contains("WPA") || capabilities.contains("WPA2") ||
               capabilities.contains("WEP") || capabilities.contains("PSK");
    }

    public static boolean isGPSEnabled(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        return locationManager != null && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public static boolean isWiFiEnabled(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return networkInfo != null && networkInfo.isConnected();
        }
        return false;
    }

    // public void openGpsSettings() {
    //     Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    //     context.startActivity(intent);
    // }

    public void enableWifi() {
        if (wifiManager != null && !wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }
    public void disableWifi() {
        if (wifiManager != null && wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(false);
        }
    }

    public static String getWifiStatus(Context _context) {
        if (ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(_context, "Нет разрешения на использование геолокации", Toast.LENGTH_SHORT).show();
            return "";
        }

        wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return "-1";//"WiFi Manager not available";
        }

        if (!wifiManager.isWifiEnabled()) {
            return "0";//"WiFi is turned off";
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (networkInfo != null && networkInfo.isConnected()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String bssid = wifiInfo.getBSSID();
            int ipAddress = wifiInfo.getIpAddress();

            String ipString = String.format(
                        "%d.%d.%d.%d",
                        (ipAddress & 0xff),
                        (ipAddress >> 8 & 0xff),
                        (ipAddress >> 16 & 0xff),
                        (ipAddress >> 24 & 0xff)
                    );

            // Получаем информацию о DHCP
            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
            int netmask = dhcpInfo.netmask;

            String netmaskString = String.format(
                   "%d.%d.%d.%d",
                   (netmask & 0xff),
                   (netmask >> 8 & 0xff),
                   (netmask >> 16 & 0xff),
                   (netmask >> 24 & 0xff)
            );

            //Log.i("WiFiStatus", "DhcpInfo is " + (dhcpInfo != null) + " netmask " + netmask + " n " + netmaskString);

            List<ScanResult> results = wifiManager.getScanResults();
            String line = "2,"; //"Wifi connected to the net"
            for (ScanResult scanResult : results) {
                //Log.i("MyWiFiList", scanResult.SSID + "," + isNetworkSecured(scanResult.capabilities) + "," + scanResult.level + "," + scanResult.BSSID);
                if (scanResult.BSSID.equals(bssid)) {
                    line += scanResult.SSID
                    + "," + isNetworkSecured(scanResult.capabilities)
                    + "," + Math.abs(scanResult.level)
                    + "," + scanResult.BSSID
                    + "," + ipString
                    + "," + netmaskString;
                    break;
                }
            }
            return line;
        } else {
            return "1";//"WiFi is on, but not connected to any network";
        }
    }

    public static int getWifiStatusOnly(Context _context) {
        if (ActivityCompat.checkSelfPermission(_context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(_context, "Нет разрешения на использование геолокации", Toast.LENGTH_SHORT).show();
            return -1;
        }

        wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return -1;//"WiFi Manager not available";
        }

        if (!wifiManager.isWifiEnabled()) {
            return 0;//"WiFi is turned off";
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        if (networkInfo != null && networkInfo.isConnected()) {
            return 2;
        } else {
            return 1;//"WiFi is on, but not connected to any network";
        }
    }

    public class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                int wifiStatus = getWifiStatusOnly(context);

                if(isCPPLoaded != 1){
                    return;
                }

                callFromJavaWiFiStateChanged(wifiStatus);
            }
        }
    }

    public static void connectToWifi(Context _context, String ssid, String password){
        Log.i("DISCONNECT_FROM_WIFI", "We start to connect to WiFi: " + ssid + " " + password);
           // wifiManager.addNetworkSuggestions(WifiNetworkSuggestion.Builder());
           // connectToWifi
    //        WifiNetworkSuggestion.Builder builder = null;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                Log.i("DISCONNECT_FROM_WIFI", "SDK lower than we need");
                return;
            }

    //        builder = new WifiNetworkSuggestion.Builder();
    //        builder.setSsid(ssid);
    //        builder.setWpa2Passphrase(password);
    //        WifiNetworkSuggestion wifiNetworkSuggestion = builder.build();

            final WifiNetworkSuggestion suggestion1 =
                    new WifiNetworkSuggestion.Builder()
                            .setSsid(ssid)
                            .setWpa2Passphrase(password)
                            //.setIsAppInteractionRequired(true) // Optional (Needs location permission)
                            .build();




            final List<WifiNetworkSuggestion> suggestionsListRem = new ArrayList<WifiNetworkSuggestion>();
            suggestionsListRem.add(
            new WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            //.setIsAppInteractionRequired(true) // Optional (Needs location permission)
            .build()
            );

            final List<WifiNetworkSuggestion> suggestionsListAdd = new ArrayList<WifiNetworkSuggestion>();
            suggestionsListAdd.add(
            new WifiNetworkSuggestion.Builder()
            .setSsid(ssid)
            .setWpa2Passphrase(password)
            //.setIsAppInteractionRequired(true) // Optional (Needs location permission)
            .build()
            );
    //                new ArrayList<WifiNetworkSuggestion> {{
    //            add(suggestion1);
    //        }};

            WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);

            int status_rem = wifiManager.removeNetworkSuggestions(suggestionsListRem);
            if (status_rem != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
               Log.i("DISCONNECT_FROM_WIFI", "!Status_rem " + status_rem);
            }

            int status = wifiManager.addNetworkSuggestions(suggestionsListAdd);
            if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
               Log.i("DISCONNECT_FROM_WIFI", "!Status_add " + status);
            }

            final IntentFilter intentFilter =
                        new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

            final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (!intent.getAction().equals(
                            WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
                            Log.i("DISCONNECT_FROM_WIFI", "Error - POST CON");
                                return;
                    }

                    Log.i("DISCONNECT_FROM_WIFI", "We need postconnect");
                    // do post connect processing here...
                }
            };
            _context.registerReceiver(broadcastReceiver, intentFilter);

            Log.i("DISCONNECT_FROM_WIFI", "OK");
        }

//MINE
    // public static void connectToWifi(Context _context, String ssid, String password) {
    //     wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);

    //     Log.i("PASSW", "Вошли " + ssid +" "+ password);
    //     WifiNetworkSpecifier wifiNetworkSpecifier = null;
    //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    //         wifiNetworkSpecifier = new WifiNetworkSpecifier.Builder()
    //                 .setSsid(ssid)
    //                 .setWpa2Passphrase(password)
    //                 .build();
    //     }

    //     ConnectivityManager connectivityManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);

    //     // Подключение к сети
    //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    //         connectivityManager.requestNetwork(new NetworkRequest.Builder()
    //                 .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
    //                 .setNetworkSpecifier(wifiNetworkSpecifier)
    //                 .build(), new ConnectivityManager.NetworkCallback() {
    //             @Override
    //             public void onAvailable(Network network) {
    //                 // Сеть доступна, подключение выполнено
    //                 Toast.makeText(_context, "Подключено к " + ssid, Toast.LENGTH_SHORT).show();
    //                 Log.i("PASSW", "Да");
    //             }

    //             @Override
    //             public void onUnavailable() {
    //                 // Не удалось подключиться
    //                 Toast.makeText(_context, "Не удалось подключиться к " + ssid, Toast.LENGTH_SHORT).show();
    //                 Log.i("PASSW", "Нет");
    //             }
    //         });
    //     }

    //     if (wifiManager != null && wifiManager.isWifiEnabled()) {

    //         // Получаем ID текущей сети
    //         int networkId = wifiManager.getConnectionInfo().getNetworkId();

    //         Log.i("DISCONNECT_FROM_WIFI_CON", "network id = " + networkId);
    //     }
    // }

    // public static void connectToWifi(Context _context, String ssid, String password) {
    //     Log.i("PASSW", "Вошли " + ssid + " " + password);

    //     if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    //         // Создаем предложение для подключения к сети
    //         WifiNetworkSuggestion suggestion = new WifiNetworkSuggestion.Builder()
    //                 .setSsid(ssid)
    //                 .setWpa2Passphrase(password)
    //                 .setIsHiddenSsid(false) // Указываем, если сеть скрыта
    //                 .build();

    //         // Добавляем предложение в список
    //         List<WifiNetworkSuggestion> suggestionsList = new ArrayList<>();
    //         suggestionsList.add(suggestion);

    //         // Получаем WifiManager
    //         WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);

    //         // Удаляем старые предложения (если есть)
    //         wifiManager.removeNetworkSuggestions(suggestionsList);

    //         // Добавляем новые предложения
    //         int status = wifiManager.addNetworkSuggestions(suggestionsList);

    //         if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
    //             Log.i("PASSW", "Предложение сети успешно добавлено");
    //         } else {
    //             Log.i("PASSW", "Ошибка при добавлении предложения сети");
    //             return;
    //         }

    //         // Отключаемся от текущей сети (если подключены)
    //         disconnectFromWifi(_context);

    //         // Подключаемся к новой сети
    //         ConnectivityManager connectivityManager = (ConnectivityManager) _context.getSystemService(Context.CONNECTIVITY_SERVICE);
    //         NetworkRequest networkRequest = new NetworkRequest.Builder()
    //                 .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
    //                 .build();

    //         connectivityManager.requestNetwork(networkRequest, new ConnectivityManager.NetworkCallback() {
    //             @Override
    //             public void onAvailable(Network network) {
    //                 // Сеть доступна, подключение выполнено
    //                 Log.i("PASSW", "Подключено к " + ssid);
    //                 Toast.makeText(_context, "Подключено к " + ssid, Toast.LENGTH_SHORT).show();
    //             }

    //             @Override
    //             public void onUnavailable() {
    //                 // Не удалось подключиться
    //                 Log.i("PASSW", "Не удалось подключиться к " + ssid);
    //                 Toast.makeText(_context, "Не удалось подключиться к " + ssid, Toast.LENGTH_SHORT).show();
    //             }
    //         });
    //     } else {
    //         Log.i("PASSW", "Метод не поддерживается на этой версии Android");
    //         Toast.makeText(_context, "Метод не поддерживается на этой версии Android", Toast.LENGTH_SHORT).show();
    //     }
    // }

    public static void disconnectFromWifi(Context _context) {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Log.i("DISCONNECT_FROM_WIFI_DISC", "SDK lower than we need");
            return;
        }

        WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);
        List<WifiNetworkSuggestion> suggestionsListRem = Collections.emptyList();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            suggestionsListRem = wifiManager.getNetworkSuggestions();
        }

        // final WifiNetworkSuggestion suggestion1 =
        //         new WifiNetworkSuggestion.Builder()
        //                 .setSsid(getNetworkSuggestions())
        //                 .setWpa2Passphrase(password)
        //                 //.setIsAppInteractionRequired(true) // Optional (Needs location permission)
        //                 .build();


        //     final List<WifiNetworkSuggestion> suggestionsListRem = new ArrayList<WifiNetworkSuggestion>();
        //     suggestionsListRem.add(
        //     new WifiNetworkSuggestion.Builder()
        //     .setSsid(ssid)
        //     .setWpa2Passphrase(password)
        //     //.setIsAppInteractionRequired(true) // Optional (Needs location permission)
        //     .build()
        //     );



            int status_rem = wifiManager.removeNetworkSuggestions(suggestionsListRem);
            if (status_rem != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS) {
               Log.i("DISCONNECT_FROM_WIFI", "!Status_rem " + status_rem);
            }


            // final IntentFilter intentFilter =
            //             new IntentFilter(WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION);

            // final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
            //     @Override
            //     public void onReceive(Context context, Intent intent) {
            //         if (!intent.getAction().equals(
            //                 WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION)) {
            //                 Log.i("DISCONNECT_FROM_WIFI", "Error - POST CON");
            //                     return;
            //         }

            //         Log.i("DISCONNECT_FROM_WIFI", "We need postconnect");
            //         // do post connect processing here...
            //     }
            // };
            // _context.registerReceiver(broadcastReceiver, intentFilter);

            // Log.i("DISCONNECT_FROM_WIFI", "OK");
    }

    public static void disconnectFromWifi_OLD(Context _context) {
        wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);

        if (wifiManager != null && wifiManager.isWifiEnabled()) {

            try{
                // Получаем ID текущей сети
                int networkId = wifiManager.getConnectionInfo().getNetworkId();

                Log.i("DISCONNECT_FROM_WIFI", "network id = " + networkId);

                // Отключаемся от сети
                //wifiManager.removeNetwork(networkId);
                //wifiManager.disableNetwork(networkId);
                //wifiManager.saveConfiguration();
                wifiManager.removeNetwork(networkId);
                Log.i("DISCONNECT_FROM_WIFI", "removeNetwork");
                wifiManager.saveConfiguration();
                Log.i("DISCONNECT_FROM_WIFI", "saveConfiguration");
                wifiManager.disconnect();
                Log.i("DISCONNECT_FROM_WIFI", "disconnect");
                wifiManager.saveConfiguration();
                Log.i("DISCONNECT_FROM_WIFI", "saveConfiguration");

                connectToWifi(_context, "Brodskiy", "jjjj");
            } catch(Exception e){
                Log.i("DISCONNECT_FROM_WIFI", "error = " + e.getMessage());
            }
        }
    }

    // public static void disconnectFromWifi(Context _context) {
    //     WifiManager wifiManager = (WifiManager) _context.getSystemService(Context.WIFI_SERVICE);

    //     if (wifiManager != null && wifiManager.isWifiEnabled()) {
    //         // Получаем текущую сеть
    //         WifiInfo wifiInfo = wifiManager.getConnectionInfo();
    //         int networkId = wifiInfo.getNetworkId();

    //         // Отключаемся от сети
    //         wifiManager.disableNetwork(networkId);
    //         wifiManager.disconnect();

    //         // Отключаем Wi-Fi полностью (опционально)
    //         wifiManager.setWifiEnabled(false);

    //         Log.i("PASSW", "Отключено от текущей сети и Wi-Fi выключен");
    //     } else {
    //         Log.i("PASSW", "Wi-Fi уже отключен");
    //     }
    // }

    private static native void callFromJava(ArrayList<String> results);
    private static native void callFromJavaWiFiStateChanged(int wifiState);


    // public static native void processScanResults(ArrayList<String> results);

    public static void setCPPLoaded(int value) {
        isCPPLoaded = value;
    }

    public void unregisterReceiver() {
        context.unregisterReceiver(networkChangeReceiver);
    }
}
