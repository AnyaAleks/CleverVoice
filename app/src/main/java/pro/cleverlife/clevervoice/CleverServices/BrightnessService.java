package pro.cleverlife.clevervoice.CleverServices;

import static android.content.ContentValues.TAG;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.WindowManager;

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
    }

    public boolean increaseBrightness(int delta) {
        int current = getCurrentBrightness(context);
        setScreenBrightness(context, current + delta);
        return true;
    }

    public boolean decreaseBrightness(int delta) {
        int current = getCurrentBrightness(context);
        setScreenBrightness(context, current - delta);
        return true;
    }

    public boolean setMinBrightness() {
        setScreenBrightness(context, 0);
        return true;
    }

    public boolean setMediumBrightness() {
        setScreenBrightness(context, 128);
        return true;
    }

    public String getBrightnessInfo() {
        int current = getCurrentBrightness(context);
        int percent = (int) (current * 100 / 255.0);
        return String.format("Яркость: %d%% (%d/255)", percent, current);
    }

    public boolean setMaxBrightness() {
        Log.d(TAG, "Вызов setMaxBrightness()");

        try {
            // Способ 1: Через системные настройки
            ContentResolver resolver = context.getContentResolver();

            // Получаем текущий режим
            int mode = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE);
            Log.d(TAG, "Текущий режим яркости: " + (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC ? "АВТО" : "РУЧНОЙ"));

            // Устанавливаем ручной режим
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

            // Устанавливаем максимальное значение (255)
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 255);
            Log.d(TAG, "Установлено значение яркости: 255");

            // Получаем активное окно (если context - это Activity)
            if (context instanceof Activity) {
                WindowManager.LayoutParams layout = ((Activity) context).getWindow().getAttributes();
                layout.screenBrightness = 1.0f; // 1.0f = максимум
                ((Activity) context).getWindow().setAttributes(layout);
                Log.d(TAG, "Установлена яркость окна: 1.0f (максимум)");
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в setMaxBrightness: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean setScreenBrightness(int brightness) {
        Log.d(TAG, "Вызов setScreenBrightness(" + brightness + ")");

        if (brightness < 0) brightness = 0;
        if (brightness > 255) brightness = 255;

        Log.d(TAG, "Устанавливаемая яркость: " + brightness);

        try {
            // Способ 1: Системные настройки
            ContentResolver resolver = context.getContentResolver();
            Settings.System.putInt(resolver, Settings.System.SCREEN_BRIGHTNESS, brightness);
            Log.d(TAG, "Значение записано в Settings.System: " + brightness);

            // Способ 2: Яркость текущего окна
            if (context instanceof Activity) {
                float brightnessFloat = brightness / 255.0f;
                WindowManager.LayoutParams layout = ((Activity) context).getWindow().getAttributes();
                layout.screenBrightness = brightnessFloat;
                ((Activity) context).getWindow().setAttributes(layout);
                Log.d(TAG, "Яркость окна установлена: " + brightnessFloat);
            }

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в setScreenBrightness: " + e.getMessage(), e);
            return false;
        }
    }

    public boolean hasPermission() {
        return Settings.System.canWrite(context);
    }
}
