package pro.cleverlife.clevervoice.CleverServices;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

public class BrightnessService {
    private static Context context;

    public BrightnessService(Context context) {
        this.context = context;
    }

    public static int getCurrentBrightness(Context _context) {
        int brightness = 128; // Значение по умолчанию
        try {
            brightness = Settings.System.getInt(_context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return brightness;
    }

    public static void setScreenBrightness(Context _context, int brightnessValue) {
        Log.i("hhhh", brightnessValue + "");
        // Убедитесь, что значение яркости находится в пределах от 0 до 255
        if (brightnessValue < 0) {
            brightnessValue = 0;
        } else if (brightnessValue > 255) {
            brightnessValue = 255;
        }

        // Изменение яркости в системных настройках
        try {
            Settings.System.putInt(_context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, brightnessValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean requestPermissionWriteSettings(){
        return Settings.System.canWrite(context);
    //        Log.i("Request!", "start");
    //        int res = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS);
    //        Log.i("Request!", "res: " + res);
    //        if (res == PackageManager.PERMISSION_GRANTED) {
    //            return true;
    //        }
    //        Log.i("Request!", "res-continue");
    //        ActivityCompat.requestPermissions((MainActivity) context, new String[]{Manifest.permission.WRITE_SETTINGS}, REQUEST_CODE_WRITE_SETTINGS);
    //        res = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS);
    //        boolean resContext = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SETTINGS) == PackageManager.PERMISSION_GRANTED;
    //        Log.i("Request!", "res-checkselfper:" + res + "  " + resContext);
    //
    //        boolean b = (res == PackageManager.PERMISSION_GRANTED);
    //        Log.i("Request!", "b " + b);
    //        return b;
    }
}
