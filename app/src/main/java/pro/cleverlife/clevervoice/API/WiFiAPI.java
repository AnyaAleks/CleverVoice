package pro.cleverlife.clevervoice.API;

import android.content.Context;
import android.util.Log;

import pro.cleverlife.clevervoice.CleverServices.WiFiService;

public class WiFiAPI {
    public enum WiFiCommand {
        SCAN,
        ENABLE,
        DISABLE,
        GET_STATUS,
        GET_STATUS_ONLY,
        CONNECT,
        DISCONNECT,
        CHECK_LOCATION_PERMISSION,
        CHECK_GPS_ENABLED,
        CHECK_WIFI_ENABLED
    }

    private static Context appContext;

    // Инициализация контекста из MainActivity
    public static void initialize(Context context) {
        appContext = context.getApplicationContext();
    }

    // Основной метод выполнения команд
    public static String executeCommand(WiFiCommand command, String... params) {
        if (appContext == null) {
            return "Ошибка: WiFiAPI не инициализирован. Вызовите WiFiAPI.initialize(context) в MainActivity";
        }

        WiFiService service = new WiFiService(appContext);

        try {
            switch (command) {
                case SCAN:
                    if (WiFiService.checkLocationPermission()) {
                        WiFiService.startScan(appContext);
                        return "Сканирование WiFi запущено";
                    } else {
                        WiFiService.requestLocationPermission(appContext);
                        return "Запрошено разрешение на геолокацию для сканирования";
                    }

                case ENABLE:
                    service.enableWifi();
                    return "WiFi включен";

                case DISABLE:
                    service.disableWifi();
                    return "WiFi выключен";

                case GET_STATUS:
                    String status = WiFiService.getWifiStatus(appContext);
                    if (status.equals("-1")) {
                        return "Ошибка: WiFi менеджер недоступен";
                    } else if (status.equals("0")) {
                        return "WiFi выключен";
                    } else if (status.equals("1")) {
                        return "WiFi включен, но не подключен к сети";
                    } else {
                        // Парсим детальную информацию о подключении
                        String[] parts = status.split(",");
                        if (parts.length >= 7) {
                            return String.format("Подключен к %s, защита: %s, уровень сигнала: %s, IP: %s, маска: %s",
                                    parts[1], parts[2], parts[3], parts[4], parts[5]);
                        }
                        return "Подключен к WiFi сети";
                    }

                case GET_STATUS_ONLY:
                    int statusCode = WiFiService.getWifiStatusOnly(appContext);
                    switch (statusCode) {
                        case -1: return "Ошибка доступа";
                        case 0: return "WiFi выключен";
                        case 1: return "WiFi включен, не подключен";
                        case 2: return "WiFi подключен";
                        default: return "Неизвестный статус";
                    }

                case CONNECT:
                    if (params.length >= 2) {
                        String ssid = params[0];
                        String password = params[1];
                        WiFiService.connectToWifi(appContext, ssid, password);
                        return "Подключение к сети " + ssid + " запущено";
                    } else {
                        return "Ошибка: укажите SSID и пароль";
                    }

                case DISCONNECT:
                    WiFiService.disconnectFromWifi(appContext);
                    return "Отключение от WiFi выполнено";

                case CHECK_LOCATION_PERMISSION:
                    boolean hasPermission = WiFiService.checkLocationPermission();
                    return hasPermission ? "Разрешение на геолокацию есть" : "Разрешения на геолокацию нет";

                case CHECK_GPS_ENABLED:
                    WiFiService.checkGPSEnable();
                    return "Проверка GPS выполнена";

                case CHECK_WIFI_ENABLED:
                    WiFiService.checkWiFiEnable();
                    return "Проверка WiFi выполнена";
            }
        } catch (Exception e) {
            Log.e("WiFiAPI", "Error executing WiFi command", e);
            return "Ошибка при выполнении команды WiFi";
        }

        return "Команда WiFi выполнена";
    }

    // Дополнительные методы для голосовых команд
    public static String handleVoiceCommand(String voiceCommand, String... params) {
        if (appContext == null) {
            return "Ошибка: WiFiAPI не инициализирован";
        }

        switch (voiceCommand.toLowerCase()) {
            case "сканировать wifi":
            case "scan wifi":
                return executeCommand(WiFiCommand.SCAN);

            case "включить wifi":
            case "enable wifi":
                return executeCommand(WiFiCommand.ENABLE);

            case "выключить wifi":
            case "disable wifi":
                return executeCommand(WiFiCommand.DISABLE);

            case "статус wifi":
            case "wifi status":
                return executeCommand(WiFiCommand.GET_STATUS);

            case "подключиться к wifi":
            case "connect to wifi":
                if (params.length >= 2) {
                    return executeCommand(WiFiCommand.CONNECT, params[0], params[1]);
                }
                return "Укажите название сети и пароль";

            case "отключиться от wifi":
            case "disconnect wifi":
                return executeCommand(WiFiCommand.DISCONNECT);

            case "проверить разрешения":
            case "check permissions":
                return executeCommand(WiFiCommand.CHECK_LOCATION_PERMISSION);

            default:
                return "Неизвестная команда WiFi";
        }
    }

    // Метод для получения текущего контекста
    public static Context getContext() {
        return appContext;
    }

    // Метод для проверки инициализации
    public static boolean isInitialized() {
        return appContext != null;
    }
}