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
        CHECK_WIFI_ENABLED,
        GET_CURRENT_NETWORK,
        FORGET_NETWORK,
        SAVE_CONFIGURATION,
        RESET_WIFI,
        GET_WIFI_INFO,
        CHECK_ROOT,
        RESTART_WIFI_SERVICE,
        FORGET_ALL_NETWORKS,
        SCAN_WITH_RESULTS,
        SCAN_ROOT
    }

    private static Context appContext;
    private static WiFiService wifiService;

    // Инициализация контекста из MainActivity
    public static void initialize(Context context) {
        if (context == null) {
            Log.e("WiFiAPI", "Context не может быть null при инициализации");
            return;
        }

        appContext = context.getApplicationContext();
        wifiService = new WiFiService(appContext);

        //Проверка root доступа при инициализации
        checkAndLogRootAccess();

        Log.i("WiFiAPI", "WiFiAPI успешно инициализирован");
    }

    private static void checkAndLogRootAccess() {
        if (wifiService != null) {
            boolean hasRoot = wifiService.hasRootAccess();
            if (hasRoot) {
                Log.i("WiFiAPI", "Root доступ подтвержден");
            } else {
                Log.w("WiFiAPI", "Root доступ отсутствует. Некоторые функции могут не работать.");
            }
        }
    }

    // Основной метод выполнения команд
    public static String executeCommand(WiFiCommand command, String... params) {
        if (appContext == null) {
            return "Ошибка: WiFiAPI не инициализирован. Вызовите WiFiAPI.initialize(context) в MainActivity";
        }

        if (wifiService == null) {
            wifiService = new WiFiService(appContext);
        }

        try {
            switch (command) {
                case ENABLE:
                    boolean enabled = wifiService.enableWifi();
                    if (enabled) {
                        return "WiFi успешно включен";
                    } else {
                        // Проверка root доступа
                        boolean hasRoot = wifiService.hasRootAccess();
                        if (!hasRoot) {
                            return "Не удалось включить WiFi. Требуется root доступ!";
                        }
                        return "Не удалось включить WiFi. Попробуйте команду 'сбросить wifi'";
                    }

                case DISABLE:
                    boolean disabled = wifiService.disableWifi();
                    if (disabled) {
                        return "WiFi успешно выключен";
                    } else {
                        // Проверка root доступа
                        boolean hasRoot = wifiService.hasRootAccess();
                        if (!hasRoot) {
                            return "Не удалось выключить WiFi. Требуется root доступ!";
                        }
                        return "Не удалось выключить WiFi. Попробуйте команду 'сбросить wifi'";
                    }

                case GET_STATUS:
                    String status = WiFiService.getWifiStatus(appContext);
                    return parseWifiStatus(status);

                case GET_STATUS_ONLY:
                    int statusCode = WiFiService.getWifiStatusOnly(appContext);
                    return getStatusDescription(statusCode);

                case CONNECT:
                    if (params.length >= 2) {
                        String ssid = params[0];
                        String password = params[1];

                        if (ssid == null || ssid.trim().isEmpty()) {
                            return "Ошибка: SSID не может быть пустым";
                        }

                        WiFiService.connectToWifi(appContext, ssid, password);
                        return String.format("Запущено подключение к сети: %s\nПожалуйста, подождите...", ssid);
                    } else if (params.length == 1) {
                        // Попытка подключения к открытой сети
                        String ssid = params[0];
                        WiFiService.connectToWifi(appContext, ssid, "");
                        return String.format("Запущено подключение к открытой сети: %s", ssid);
                    } else {
                        return "Ошибка: укажите SSID и пароль (или только SSID для открытой сети)";
                    }

                case DISCONNECT:
                    WiFiService.disconnectFromWifi(appContext);
                    return "Выполнено отключение от текущей WiFi сети";

                case CHECK_LOCATION_PERMISSION:
                    boolean hasPermission = WiFiService.checkLocationPermission();
                    return hasPermission ? "Разрешение на геолокацию предоставлено" :
                            "Разрешения на геолокацию нет";

                case CHECK_GPS_ENABLED:
                    WiFiService.checkGPSEnable();
                    return "Проверка GPS выполнена";

                case CHECK_WIFI_ENABLED:
                    WiFiService.checkWiFiEnable();
                    return "Проверка WiFi выполнена";

                case GET_CURRENT_NETWORK:
                    return executeCommand(WiFiCommand.GET_STATUS);

                case FORGET_NETWORK:
                    WiFiService.disconnectFromWifi(appContext);
                    return "Текущая сеть отключена";

                case RESET_WIFI:
                    boolean reset = wifiService.resetWiFi();
                    return reset ? "WiFi успешно сброшен (выключен и включен)" :
                            "Не удалось сбросить WiFi";

                case GET_WIFI_INFO:
                    String info = wifiService.getWiFiInfo();
                    // Ограничение длины вывода
                    if (info.length() > 1000) {
                        info = info.substring(0, 1000) + "\n... (вывод обрезан)";
                    }
                    return "Информация о WiFi:\n" + info;

                case CHECK_ROOT:
                    boolean hasRoot = wifiService.hasRootAccess();
                    if (hasRoot) {
                        return "Root доступ ЕСТЬ! Все WiFi команды будут работать.";
                    } else {
                        return "Root доступа НЕТ! Команды включения/выключения WiFi не будут работать.";
                    }

                case RESTART_WIFI_SERVICE:
                    boolean restarted = wifiService.restartWifiService();
                    return restarted ? "WiFi сервис перезапущен" :
                            "Не удалось перезапустить WiFi сервис";

                case FORGET_ALL_NETWORKS:
                    boolean forgotten = wifiService.forgetAllNetworks();
                    return forgotten ? "Все сохраненные WiFi сети удалены" :
                            "Не удалось удалить все сети";

                case SAVE_CONFIGURATION:
                    return "Конфигурация сетей сохраняется автоматически системой";

                case SCAN_WITH_RESULTS:
                    return WiFiService.startScanWithResults(appContext);

                case SCAN_ROOT:
                    if (wifiService != null) {
                        return wifiService.scanNetworksWithRoot();
                    }
                    return "WiFiService не инициализирован";

                case SCAN:
                    return executeCommand(WiFiCommand.SCAN_WITH_RESULTS);

                default:
                    return "Неизвестная команда WiFi";
            }
        } catch (SecurityException e) {
            Log.e("WiFiAPI", "Ошибка безопасности", e);
            return "Ошибка безопасности: " + e.getMessage();
        } catch (IllegalArgumentException e) {
            Log.e("WiFiAPI", "Некорректные параметры", e);
            return "Некорректные параметры: " + e.getMessage();
        } catch (Exception e) {
            Log.e("WiFiAPI", "Неожиданная ошибка", e);
            return "Ошибка при выполнении команды: " + e.getMessage();
        }
    }

    private static String parseWifiStatus(String status) {
        if (status == null) return "Ошибка получения статуса";

        switch (status) {
            case "-1": return "WiFi менеджер недоступен";
            case "-2": return "Нет разрешения на геолокацию";
            case "0": return "WiFi выключен";
            case "1": return "WiFi включен, но не подключен";
            default:
                if (status.startsWith("2,")) {
                    String[] parts = status.split(",");
                    if (parts.length >= 7) {
                        String ssid = parts[1];
                        boolean isSecured = Boolean.parseBoolean(parts[2]);
                        String signalLevel = parts[3];
                        String bssid = parts[4];
                        String ip = parts[5];
                        String mask = parts[6];

                        // Определение качества сигнала
                        int signal = Integer.parseInt(signalLevel);
                        String quality;
                        if (signal < 50) quality = "Отличный";
                        else if (signal < 70) quality = "Хороший";
                        else if (signal < 85) quality = "Средний";
                        else quality = "Слабый";

                        return String.format(
                                "ПОДКЛЮЧЕН\n" +
                                        "Сеть: %s\n" +
                                        "Защита: %s\n" +
                                        "%s: %s dBm\n" +
                                        "MAC: %s\n" +
                                        "IP: %s\n" +
                                        "Маска: %s",
                                ssid.isEmpty() ? "Скрытая сеть" : ssid,
                                isSecured ? "Да" : "Нет",
                                quality,
                                signalLevel,
                                bssid,
                                ip,
                                mask
                        );
                    }
                }
                return "Подключен к WiFi сети";
        }
    }

    private static String getStatusDescription(int code) {
        switch (code) {
            case -1: return "Ошибка доступа";
            case 0: return "WiFi выключен";
            case 1: return "WiFi включен, не подключен";
            case 2: return "WiFi подключен";
            default: return "Неизвестный статус";
        }
    }

    // Дополнительные методы для голосовых команд
    public static String handleVoiceCommand(String voiceCommand, String... params) {
        if (appContext == null) {
            return "WiFiAPI не инициализирован. Сначала запустите приложение.";
        }

        String command = voiceCommand.toLowerCase().trim();

        // WiFi команды
        if (command.contains("wifi") || command.contains("вайфай") || command.contains("wi-fi") || command.contains("вай фай")) {
            if (command.contains("включи") || command.contains("вкл") || command.contains("enable") || command.contains("on")) {
                return executeCommand(WiFiCommand.ENABLE);
            } else if (command.contains("выключи") || command.contains("выкл") || command.contains("disable") || command.contains("off")) {
                return executeCommand(WiFiCommand.DISABLE);
            } else if (command.contains("статус") || command.contains("status") || command.contains("состоян")) {
                return executeCommand(WiFiCommand.GET_STATUS);
            } else if (command.contains("сканир") || command.contains("scan") || command.contains("поиск")) {
                return executeCommand(WiFiCommand.SCAN);
            } else if (command.contains("подключи") || command.contains("connect") || command.contains("подключ")) {
                if (params.length >= 2) {
                    return executeCommand(WiFiCommand.CONNECT, params[0], params[1]);
                } else if (params.length == 1) {
                    return executeCommand(WiFiCommand.CONNECT, params[0]);
                }
                return "Укажите название сети и пароль (или только название для открытой сети)";
            } else if (command.contains("отключи") || command.contains("disconnect")) {
                return executeCommand(WiFiCommand.DISCONNECT);
            } else if (command.contains("информация") || command.contains("info") || command.contains("инфо")) {
                return executeCommand(WiFiCommand.GET_WIFI_INFO);
            } else if (command.contains("сброс") || command.contains("reset")) {
                return executeCommand(WiFiCommand.RESET_WIFI);
            } else if (command.contains("перезапуск") || command.contains("restart") || command.contains("рестарт")) {
                return executeCommand(WiFiCommand.RESTART_WIFI_SERVICE);
            } else if (command.contains("удали") || command.contains("забудь") || command.contains("forget")) {
                if (command.contains("все") || command.contains("all")) {
                    return executeCommand(WiFiCommand.FORGET_ALL_NETWORKS);
                }
                return executeCommand(WiFiCommand.FORGET_NETWORK);
            }
            else if (command.contains("сканир") || command.contains("scan") || command.contains("поиск")) {
                // Можно предложить выбор метода сканирования
                if (wifiService != null && wifiService.hasRootAccess()) {
                    // Если есть root, использовать root метод
                    return executeCommand(WiFiCommand.SCAN_ROOT);
                } else {
                    // Иначе обычный метод
                    return executeCommand(WiFiCommand.SCAN_WITH_RESULTS);
                }
            }
        }

        // Проверка root
        if (command.contains("root") || command.contains("рут") || command.contains("рута")) {
            return executeCommand(WiFiCommand.CHECK_ROOT);
        }

        return "Неизвестная команда WiFi. Скажите: 'включи wifi', 'выключи wifi', 'статус wifi', 'сканировать wifi'";
    }

    // Быстрые команды (для текстового ввода)
    public static String quickCommand(String command) {
        if (command == null || command.isEmpty()) {
            return "Пустая команда";
        }

        String[] parts = command.split("\\s+", 3);
        String action = parts[0].toLowerCase();

        switch (action) {
            case "wifi":
            case "вайфай":
                if (parts.length > 1) {
                    String subCommand = parts[1].toLowerCase();
                    switch (subCommand) {
                        case "on":
                        case "вкл":
                        case "enable":
                        case "включи":
                            return executeCommand(WiFiCommand.ENABLE);
                        case "off":
                        case "выкл":
                        case "disable":
                        case "выключи":
                            return executeCommand(WiFiCommand.DISABLE);
                        case "status":
                        case "статус":
                            return executeCommand(WiFiCommand.GET_STATUS);
                        case "scan":
                        case "сканировать":
                            return executeCommand(WiFiCommand.SCAN);
                        case "info":
                        case "инфо":
                            return executeCommand(WiFiCommand.GET_WIFI_INFO);
                        case "reset":
                        case "сброс":
                            return executeCommand(WiFiCommand.RESET_WIFI);
                        case "restart":
                        case "перезапуск":
                            return executeCommand(WiFiCommand.RESTART_WIFI_SERVICE);
                        case "forget":
                        case "удали":
                            if (parts.length > 2 && (parts[2].equals("all") || parts[2].equals("все"))) {
                                return executeCommand(WiFiCommand.FORGET_ALL_NETWORKS);
                            }
                            return executeCommand(WiFiCommand.FORGET_NETWORK);
                        case "connect":
                        case "подключи":
                            if (parts.length == 4) {
                                return executeCommand(WiFiCommand.CONNECT, parts[2], parts[3]);
                            } else if (parts.length == 3) {
                                return executeCommand(WiFiCommand.CONNECT, parts[2]);
                            }
                            return "Формат: wifi connect SSID [пароль]";
                    }
                }
                return executeCommand(WiFiCommand.GET_STATUS);

            case "root":
            case "рут":
                return executeCommand(WiFiCommand.CHECK_ROOT);

            default:
                return handleVoiceCommand(command);
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    public static Context getContext() {
        return appContext;
    }

    public static boolean isInitialized() {
        return appContext != null && wifiService != null;
    }

    public static WiFiService getWiFiService() {
        return wifiService;
    }

    //Тестирование всех WiFi функций
    public static String testAllWiFiFunctions() {
        if (!isInitialized()) {
            return "WiFiAPI не инициализирован";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== ТЕСТИРОВАНИЕ WiFi ФУНКЦИЙ ===\n\n");

        // 1. Проверка root
        result.append("1. Проверка root доступа:\n");
        result.append(executeCommand(WiFiCommand.CHECK_ROOT)).append("\n\n");

        // 2. Статус WiFi
        result.append("2. Текущий статус WiFi:\n");
        result.append(executeCommand(WiFiCommand.GET_STATUS)).append("\n\n");

        // 3. Проверка разрешений
        result.append("3. Разрешения:\n");
        result.append(executeCommand(WiFiCommand.CHECK_LOCATION_PERMISSION)).append("\n\n");

        // 4. Тест включения/выключения (только если есть root)
        boolean hasRoot = wifiService.hasRootAccess();
        if (hasRoot) {
            result.append("4. Тест управления WiFi (требует root):\n");

            // Выключаем WiFi
            result.append("   - Выключаем WiFi: ");
            result.append(executeCommand(WiFiCommand.DISABLE)).append("\n");

            try { Thread.sleep(2000); } catch (InterruptedException e) {}

            // Включаем WiFi
            result.append("   - Включаем WiFi: ");
            result.append(executeCommand(WiFiCommand.ENABLE)).append("\n");

            try { Thread.sleep(2000); } catch (InterruptedException e) {}

            // Проверяем статус
            result.append("   - Итоговый статус: ");
            result.append(executeCommand(WiFiCommand.GET_STATUS)).append("\n\n");
        } else {
            result.append("4. Тест управления WiFi пропущен (требуется root)\n\n");
        }

        result.append("=== ТЕСТ ЗАВЕРШЕН ===");
        return result.toString();
    }
}