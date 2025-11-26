package pro.cleverlife.clevervoice.API;

import android.content.Context;
import android.util.Log;

import pro.cleverlife.clevervoice.CleverServices.BrightnessService;

public class BrightnessAPI {
    public enum BrightnessCommand {
        SET, INCREASE, DECREASE, MAX, MIN, MEDIUM, GET_INFO
    }

    public static String executeCommand(Context context, BrightnessCommand command, String... params) {
        BrightnessService service = new BrightnessService(context);

        if (!service.hasPermission()) {
            return "Нужно разрешение на изменение настроек системы";
        }

        try {
            switch (command) {
                case SET:
                    if (params.length > 0) {
                        int value = Integer.parseInt(params[0]);
                        if (service.setScreenBrightness(value)) {
                            return "Яркость установлена на " + value;
                        }
                    }
                    break;

                case INCREASE:
                    int increaseDelta = params.length > 0 ? Integer.parseInt(params[0]) : 50;
                    if (service.increaseBrightness(increaseDelta)) {
                        return "Яркость увеличена на " + increaseDelta;
                    }
                    break;

                case DECREASE:
                    int decreaseDelta = params.length > 0 ? Integer.parseInt(params[0]) : 50;
                    if (service.decreaseBrightness(decreaseDelta)) {
                        return "Яркость уменьшена на " + decreaseDelta;
                    }
                    break;

                case MAX:
                    if (service.setMaxBrightness()) {
                        return "Яркость установлена на максимум";
                    }
                    break;

                case MIN:
                    if (service.setMinBrightness()) {
                        return "Яркость установлена на минимум";
                    }
                    break;

                case MEDIUM:
                    if (service.setMediumBrightness()) {
                        return "Яркость установлена на средний уровень";
                    }
                    break;

                case GET_INFO:
                    return service.getBrightnessInfo();
            }
        } catch (Exception e) {
            Log.e("BrightnessAPI", "Error executing command", e);
            return "Ошибка при выполнении команды";
        }

        return "Команда выполнена";
    }
}