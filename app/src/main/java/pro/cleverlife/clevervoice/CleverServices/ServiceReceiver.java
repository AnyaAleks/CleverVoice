package pro.cleverlife.clevervoice.CleverServices;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;
import android.util.Log;

public class ServiceReceiver extends BroadcastReceiver {

    private WiFiService wifiService;

    // Метод для установки WiFiService
    public void setWiFiService(WiFiService wifiService) {
       this.wifiService = wifiService;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        //wifiService.startScan();

       // Проверка состояния Wi-Fi
       ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
       NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

       if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected()) {
           //Toast.makeText(context, "Wi-Fi включился", Toast.LENGTH_SHORT).show();
           if (wifiService != null) {
               //wifiService.requestLocationPermission();
               wifiService.startScan(context);
           }
       }

       // Проверка состояния GPS
       LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
       boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

       if (isGpsEnabled) {
           //Toast.makeText(context, "GPS включился", Toast.LENGTH_SHORT).show();
           if (wifiService != null) {
               //wifiService.requestLocationPermission();
               wifiService.startScan(context);
           }
       }
    }
}
