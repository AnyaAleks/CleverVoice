package pro.cleverlife.clevervoice.AI;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import org.json.JSONObject;

import pro.cleverlife.clevervoice.API.BrightnessAPI;
import pro.cleverlife.clevervoice.API.SoundAPI;
import pro.cleverlife.clevervoice.API.WiFiAPI;
import pro.cleverlife.clevervoice.system.AppLauncher;

/**
 * AI процессор для обработки текста от Vosk распознавания
 * Использует TinyLLM для понимания естественного языка
 */
public class VoskAIProcessor {
    private static final String TAG = "VoskAIProcessor";

    private Context context;
    private TinyLLMProcessor llmProcessor;
    private boolean useAI = true; // Флаг использования AI vs простых правил
    private CommandHistory commandHistory;
    private Handler handler = new Handler();

    public void debugStatus() {
        Log.d(TAG, "=== DEBUG STATUS ===");
        Log.d(TAG, "llmProcessor: " + (llmProcessor != null ? "INITIALIZED" : "NULL"));
        Log.d(TAG, "useAI: " + useAI);
        Log.d(TAG, "handler: " + (handler != null ? "OK" : "NULL"));
        Log.d(TAG, "context: " + (context != null ? "OK" : "NULL"));
        Log.d(TAG, "===================");
    }

    public interface SimpleCallback {
        void onCommandProcessed(boolean success);
    }

    public VoskAIProcessor(Context context) {
        this.context = context;
        this.commandHistory = new CommandHistory();

        // Немедленная инициализация AI
        initializeAI();

        // Также создаем llmProcessor сразу
        this.llmProcessor = new TinyLLMProcessor(context);

        // Запускаем асинхронную инициализацию
        new Thread(() -> {
            try {
                boolean aiReady = llmProcessor.initialize();
                useAI = aiReady;
                if (aiReady) {
                    Log.i(TAG, "AI система инициализирована успешно");
                } else {
                    Log.w(TAG, "AI система не загрузилась, используется простой парсер");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка инициализации AI", e);
                useAI = false;
            }
        }).start();
    }

    /**
     * Инициализация AI системы
     */
    private void initializeAI() {
        new Thread(() -> {
            try {
                llmProcessor = new TinyLLMProcessor(context);
                boolean aiReady = llmProcessor.initialize();
                useAI = aiReady;

                if (aiReady) {
                    Log.i(TAG, "AI система инициализирована успешно");
                } else {
                    Log.w(TAG, "AI система не загрузилась, используется простой парсер");
                }
            } catch (Exception e) {
                Log.e(TAG, "Ошибка инициализации AI", e);
                useAI = false;
            }
        }).start();
    }

    /**
     * Обработка распознанного текста от Vosk
     * @param recognizedText текст, распознанный из голоса
     */
    public void processRecognizedText(String recognizedText) {
        processRecognizedText(recognizedText, null);
    }

    /**
     * Обработка распознанного текста с колбэком
     */
    public void processRecognizedText(String recognizedText, SimpleCallback callback) {
        Log.i(TAG, "=== НАЧАЛО обработки ===");
        Log.i(TAG, "Текст: \"" + recognizedText + "\"");

        debugStatus();

        if (recognizedText == null || recognizedText.trim().isEmpty()) {
            Log.w(TAG, "Пустой текст для обработки");
            if (callback != null) {
                callback.onCommandProcessed(false);
            }
            return;
        }

        String text = recognizedText.trim();
        Log.i(TAG, "Обработка текста: \"" + text + "\"");

        // Проверка на активационное слово
        if (isActivationWord(text)) {
            Log.i(TAG, "Это активационное слово - пропускаем");
            commandHistory.clear();
            if (callback != null) {
                callback.onCommandProcessed(true);
            }
            return;
        }

        // Проверяем, инициализирован ли процессор
        if (llmProcessor == null) {
            Log.e(TAG, "LLMProcessor не инициализирован!");
            llmProcessor = new TinyLLMProcessor(context);
            // Пробуем быструю инициализацию
            try {
                llmProcessor.initialize();
            } catch (Exception e) {
                Log.e(TAG, "Не удалось инициализировать LLMProcessor", e);
            }
        }

        // Добавляем в историю
        commandHistory.add(text);

        Log.i(TAG, "Вызываю processCommand()...");
        processCommand(text, callback);
        Log.i(TAG, "=== КОНЕЦ обработки ===");
    }

    /**
     * Основная логика обработки команды
     */
    private void processCommand(String text, final SimpleCallback callback) {
        Log.d(TAG, "НАЧАЛО processCommand для текста: \"" + text + "\"");

        try {
            TinyLLMProcessor.CommandResult result = null;
            boolean useFallback = false;

            Log.d(TAG, "Проверяю llmProcessor: " + (llmProcessor != null ? "ИНИЦИАЛИЗИРОВАН" : "NULL"));

            if (llmProcessor != null) {
                Log.d(TAG, "Вызываю llmProcessor.understandCommand()...");
                result = llmProcessor.understandCommand(text);
                Log.d(TAG, "Получен результат: " +
                        (result != null ?
                                "command=" + result.command + ", action=" + result.action + ", params=" + (result.params != null ? result.params.toString() : "null") :
                                "NULL"));

                // ВАЖНО: Проверяем, действительно ли AI распознал команду
                if (result == null || "unknown".equals(result.command)) {
                    Log.w(TAG, "AI не распознал команду, использую fallback парсер");
                    useFallback = true;
                }
            } else {
                Log.w(TAG, "llmProcessor не инициализирован, использую fallback");
                useFallback = true;
            }

            // Если нужен fallback, парсим простыми правилами
            if (useFallback) {
                result = parseWithSimpleRules(text);
                Log.d(TAG, "Fallback результат: command=" + result.command + ", action=" + result.action);
            }

            // ВАЖНОЕ ИСПРАВЛЕНИЕ: ВСЕГДА вызываем executeCommand()
            boolean success = false;
            if (result != null) {
                Log.i(TAG, "Выполняю команду: " + result.command + " -> " + result.action);
                if (result.params != null) {
                    Log.i(TAG, "Параметры: " + result.params.toString());
                }
                success = executeCommand(result);

                // ДОПОЛНИТЕЛЬНАЯ ПРОВЕРКА: если основной обработчик не справился, пробуем fallback
                if (!success && !useFallback) {
                    Log.w(TAG, "Основной обработчик не справился, пробуем fallback...");
                    success = executeFallbackCommand(text);
                }
            } else {
                Log.e(TAG, "КРИТИЧЕСКАЯ ОШИБКА: result всё ещё null!");
                success = executeFallbackCommand(text);
            }

            Log.i(TAG, "Финальный результат: " + (success ? "УСПЕХ" : "НЕУДАЧА"));

            // Логируем выполнение
            logCommandExecution(text, result != null ? result : new TinyLLMProcessor.CommandResult("unknown", "", new JSONObject()));

            // Вызываем колбэк
            if (callback != null) {
                Log.d(TAG, "Вызываю callback с результатом: " + success);
                boolean finalSuccess = success;
                handler.postDelayed(() -> {
                    callback.onCommandProcessed(finalSuccess);
                }, 300);
            } else {
                Log.w(TAG, "Callback is null!");
            }

        } catch (Exception e) {
            Log.e(TAG, "КРИТИЧЕСКАЯ ошибка в processCommand", e);
            e.printStackTrace();

            // Пробуем fallback
            try {
                boolean fallbackSuccess = executeFallbackCommand(text);
                Log.i(TAG, "Fallback после ошибки: " + (fallbackSuccess ? "УСПЕХ" : "НЕУДАЧА"));

                if (callback != null) {
                    handler.postDelayed(() -> {
                        callback.onCommandProcessed(fallbackSuccess);
                    }, 300);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Ошибка в fallback", ex);
                if (callback != null) {
                    handler.postDelayed(() -> {
                        callback.onCommandProcessed(false);
                    }, 300);
                }
            }
        }

        Log.d(TAG, "КОНЕЦ processCommand");
    }

    /**
     * Выполнение команды, определенной AI
     */
    private boolean executeCommand(TinyLLMProcessor.CommandResult result) {
        if (result == null || "unknown".equals(result.command)) {
            Log.w(TAG, "Неизвестная команда");
            return false;
        }

        try {
            switch (result.command.toLowerCase()) {
                case "brightness":
                case "яркость":
                case "свет":
                    return executeBrightnessCommand(result);

                case "volume":
                case "sound":
                case "громкость":
                case "звук":
                    return executeVolumeCommand(result);

                case "wifi":
                case "вайфай":
                case "интернет":
                    return executeWifiCommand(result);

                case "launch":
                case "app":
                case "приложение":
                case "открыть":
                    return executeAppLaunchCommand(result);

                case "system":
                case "система":
                    return executeSystemCommand(result);

                case "media":
                case "медиа":
                    // ИСПРАВЛЕНО: "медиа" - это команда звука
                    JSONObject mediaParams = new JSONObject();
                    mediaParams.put("type", "media");
                    TinyLLMProcessor.CommandResult volumeResult =
                            new TinyLLMProcessor.CommandResult("volume", result.action, mediaParams);
                    return executeVolumeCommand(volumeResult);

                default:
                    Log.w(TAG, "Неподдерживаемая команда: " + result.command);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды: " + result.command, e);
            return false;
        }
    }

    /**
     * Команды управления яркостью
     */
    private boolean executeBrightnessCommand(TinyLLMProcessor.CommandResult result) {
        if (result == null) {
            Log.e(TAG, "executeBrightnessCommand: result is null!");
            return false;
        }

        String action = result.action;
        JSONObject params = result.params;

        Log.i(TAG, "Управление яркостью: команда=" + result.command +
                ", действие=" + action + ", params: " + (params != null ? params.toString() : "null"));

        Log.d(TAG, "Action.toLowerCase() = " + action.toLowerCase());

        try {
            switch (action.toLowerCase()) {
                case "increase":
                case "увеличить":
                case "прибавить":
                    int incValue = params != null ?
                            Integer.parseInt(params.optString("value", "30")) : 30;
                    Log.i(TAG, "Увеличиваю яркость на " + incValue + "%");
                    BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.INCREASE,
                            String.valueOf(incValue));
                    return true;

                case "decrease":
                case "уменьшить":
                case "убавить":
                case "меньше":
                    int decValue = params != null ?
                            Integer.parseInt(params.optString("value", "30")) : 30;
                    Log.i(TAG, "Уменьшаю яркость на " + decValue + "%");
                    BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.DECREASE,
                            String.valueOf(decValue));
                    return true;

                case "max":
                case "максимум":
                case "полная":
                case "maximum":
                    Log.i(TAG, "Устанавливаю максимальную яркость");
                    BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.MAX);
                    return true;

                case "min":
                case "минимум":
                case "выключить":
                case "minimum":
                    Log.i(TAG, "Устанавливаю минимальную яркость");
                    BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.MIN);
                    return true;

                case "set":
                case "установить":
                case "поставить":
                    if (params != null) {
                        String value = params.optString("value", "50");
                        Log.i(TAG, "Устанавливаю яркость на " + value + "%");
                        BrightnessAPI.executeCommand(context,
                                BrightnessAPI.BrightnessCommand.SET,
                                value);
                        return true;
                    }
                    Log.w(TAG, "Нет параметра value для установки яркости");
                    return false;

                case "medium":
                case "средняя":
                case "половина":
                    Log.i(TAG, "Устанавливаю среднюю яркость");
                    BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.MEDIUM);
                    return true;

                case "get_info":
                case "информация":
                case "статус":
                    String info = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.GET_INFO);
                    Log.i(TAG, "Информация о яркости: " + info);
                    return true;

                default:
                    Log.w(TAG, "Неизвестное действие для яркости: " + action);

                    // Автоматически определяем действие по тексту
                    if (action.contains("увелич") || action.contains("increase")) {
                        return executeBrightnessCommand(new TinyLLMProcessor.CommandResult(
                                "brightness", "increase", params));
                    } else if (action.contains("уменьш") || action.contains("decrease") ||
                            action.contains("меньше")) {
                        return executeBrightnessCommand(new TinyLLMProcessor.CommandResult(
                                "brightness", "decrease", params));
                    } else if (action.contains("макс") || action.contains("max")) {
                        return executeBrightnessCommand(new TinyLLMProcessor.CommandResult(
                                "brightness", "max", params));
                    } else if (action.contains("мин") || action.contains("min")) {
                        return executeBrightnessCommand(new TinyLLMProcessor.CommandResult(
                                "brightness", "min", params));
                    }

                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды яркости", e);
            return false;
        }
    }

    /**
     * Команды управления громкостью (ИСПРАВЛЕННАЯ ВЕРСИЯ)
     */
    private boolean executeVolumeCommand(TinyLLMProcessor.CommandResult result) {
        String action = result.action;
        JSONObject params = result.params;

        // ОПРЕДЕЛЯЕМ ТИП ЗВУКА ИЗ ИСХОДНОЙ КОМАНДЫ
        String soundType = "media"; // по умолчанию медиа

        // Сначала проверяем параметры
        if (params != null && params.has("type")) {
            soundType = params.optString("type", "media");
        }
        // Иначе определяем по контексту самой команды
        else if (result.command != null) {
            String commandLower = result.command.toLowerCase();
            if (commandLower.contains("звонк") || commandLower.contains("ring")) {
                soundType = "ring";
            } else if (commandLower.contains("будильник") || commandLower.contains("alarm")) {
                soundType = "alarm";
            } else if (commandLower.contains("уведомл") || commandLower.contains("notification")) {
                soundType = "notification";
            }
        }

        Log.i(TAG, "Управление громкостью: действие=" + action + ", тип: " + soundType);

        try {
            switch (action.toLowerCase()) {
                case "increase":
                case "увеличить":
                case "прибавить":
                case "добавить":
                    int incValue = params != null ?
                            Integer.parseInt(params.optString("value", "1")) : 1;

                    // ЕСЛИ НЕ УКАЗАН КОНКРЕТНЫЙ ТИП - УВЕЛИЧИВАЕМ ВСЕ 4
                    if (soundType.equals("media")) {
                        Log.i(TAG, "Увеличиваю громкость ВСЕХ типов на " + incValue);
                        return increaseAllVolumes(incValue);
                    } else {
                        Log.i(TAG, "Увеличиваю громкость " + getSoundTypeName(soundType) + " на " + incValue);
                        return increaseVolume(soundType, incValue);
                    }

                case "decrease":
                case "уменьшить":
                case "убавить":
                case "снизить":
                    int decValue = params != null ?
                            Integer.parseInt(params.optString("value", "1")) : 1;

                    // ЕСЛИ НЕ УКАЗАН КОНКРЕТНЫЙ ТИП - УМЕНЬШАЕМ ВСЕ 4
                    if (soundType.equals("media")) {
                        Log.i(TAG, "Уменьшаю громкость ВСЕХ типов на " + decValue);
                        return decreaseAllVolumes(decValue);
                    } else {
                        Log.i(TAG, "Уменьшаю громкость " + getSoundTypeName(soundType) + " на " + decValue);
                        return decreaseVolume(soundType, decValue);
                    }

                case "mute":
                case "выключить":
                case "отключить":
                case "заглушить":
                case "min":
                case "минимум":
                    // ВЫКЛЮЧАЕМ ВСЕ 4 ТИПА ЗВУКА
                    Log.i(TAG, "Выключаю звук ВСЕХ типов");
                    return muteAllVolumes();

                case "unmute":
                case "включить":
                case "включить звук":
                    // ВКЛЮЧАЕМ ВСЕ 4 ТИПА ЗВУКА
                    Log.i(TAG, "Включаю звук ВСЕХ типов");
                    return unmuteAllVolumes();

                case "max":
                case "максимум":
                case "полная":
                    // ДЛЯ УВЕДОМЛЕНИЙ ИСПОЛЬЗУЕМ БЕЗОПАСНЫЙ МЕТОД (чтобы избежать ошибки Do Not Disturb)
                    if (soundType.equals("notification") || soundType.equals("уведомление")) {
                        Log.i(TAG, "Устанавливаю высокую (но не максимальную) громкость уведомлений");
                        return setNotificationVolumeSafe(85); // 85% вместо 100%
                    } else {
                        Log.i(TAG, "Максимальная громкость " + getSoundTypeName(soundType));
                        return setMaxVolume(soundType);
                    }

                case "set":
                case "установить":
                case "поставить":
                    if (params != null) {
                        String valueStr = params.optString("value", "50");
                        int value;
                        try {
                            value = Integer.parseInt(valueStr);
                            if (value < 0) value = 0;
                            if (value > 100) value = 100;
                        } catch (NumberFormatException e) {
                            Log.w(TAG, "Некорректное значение громкости: " + valueStr);
                            value = 50;
                        }
                        Log.i(TAG, "Устанавливаю громкость " + getSoundTypeName(soundType) + " на " + value + "%");
                        return setVolume(soundType, value);
                    }
                    Log.w(TAG, "Нет параметров для установки громкости");
                    return false;

                case "get_info":
                case "информация":
                case "статус":
                case "уровень":
                    Log.i(TAG, "Получаю информацию о громкости " + getSoundTypeName(soundType));
                    return getVolumeInfo(soundType);

                default:
                    Log.w(TAG, "Неизвестное действие для громкости: " + action);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды громкости", e);
            return false;
        }
    }

    /**
     * Увеличить громкость всех 4 типов
     */
    private boolean increaseAllVolumes(int value) {
        try {
            boolean success = true;

            // Медиа
            try {
                SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.INCREASE_MEDIA,
                        String.valueOf(value));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка увеличения медиа", e);
                success = false;
            }

            // Уведомления (безопасно)
            try {
                SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.INCREASE_NOTIFICATION,
                        String.valueOf(value));
            } catch (Exception e) {
                Log.w(TAG, "Не удалось увеличить уведомления, пробуем медиа: " + e.getMessage());
                try {
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_MEDIA,
                            String.valueOf(value));
                } catch (Exception e2) {
                    Log.e(TAG, "Ошибка увеличения медиа (fallback)", e2);
                    success = false;
                }
            }

            // Звонок
            try {
                SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.INCREASE_RING,
                        String.valueOf(value));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка увеличения звонка", e);
                success = false;
            }

            // Будильник
            try {
                SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.INCREASE_ALARM,
                        String.valueOf(value));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка увеличения будильника", e);
                success = false;
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка увеличения всех громкостей", e);
            return false;
        }
    }

    /**
     * Уменьшить громкость всех 4 типов
     */
    private boolean decreaseAllVolumes(int value) {
        try {
            boolean success = true;

            // Медиа
            try {
                SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.DECREASE_MEDIA,
                        String.valueOf(value));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка уменьшения медиа", e);
                success = false;
            }

            // Уведомления (безопасно)
            try {
                SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.DECREASE_NOTIFICATION,
                        String.valueOf(value));
            } catch (Exception e) {
                Log.w(TAG, "Не удалось уменьшить уведомления, пробуем медиа: " + e.getMessage());
                try {
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_MEDIA,
                            String.valueOf(value));
                } catch (Exception e2) {
                    Log.e(TAG, "Ошибка уменьшения медиа (fallback)", e2);
                    success = false;
                }
            }

            // Звонок
            try {
                SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.DECREASE_RING,
                        String.valueOf(value));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка уменьшения звонка", e);
                success = false;
            }

            // Будильник
            try {
                SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.DECREASE_ALARM,
                        String.valueOf(value));
            } catch (Exception e) {
                Log.e(TAG, "Ошибка уменьшения будильника", e);
                success = false;
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка уменьшения всех громкостей", e);
            return false;
        }
    }

    /**
     * Выключить звук всех 4 типов
     */
    private boolean muteAllVolumes() {
        try {
            boolean success = true;

            // Медиа
            try {
                SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_MEDIA);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка выключения медиа", e);
                success = false;
            }

            // Уведомления
            try {
                SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_NOTIFICATION);
            } catch (Exception e) {
                Log.w(TAG, "Не удалось выключить уведомления: " + e.getMessage());
                success = false; // Но продолжаем с другими типами
            }

            // Звонок
            try {
                SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_RING);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка выключения звонка", e);
                success = false;
            }

            // Будильник
            try {
                SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_ALARM);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка выключения будильника", e);
                success = false;
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выключения всех звуков", e);
            return false;
        }
    }

    /**
     * Включить звук всех 4 типов
     */
    private boolean unmuteAllVolumes() {
        try {
            boolean success = true;

            // Медиа
            try {
                SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_MEDIA);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка включения медиа", e);
                success = false;
            }

            // Уведомления
            try {
                SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_NOTIFICATION);
            } catch (Exception e) {
                Log.w(TAG, "Не удалось включить уведомления: " + e.getMessage());
                success = false; // Но продолжаем с другими типами
            }

            // Звонок
            try {
                SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_RING);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка включения звонка", e);
                success = false;
            }

            // Будильник
            try {
                SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_ALARM);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка включения будильника", e);
                success = false;
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка включения всех звуков", e);
            return false;
        }
    }

    /**
     * Увеличить громкость конкретного типа
     */
    private boolean increaseVolume(String soundType, int value) {
        try {
            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_MEDIA,
                            String.valueOf(value));
                    return true;

                case "notification":
                case "уведомление":
                    // Безопасный метод для уведомлений
                    try {
                        SoundAPI.executeCommand(context,
                                SoundAPI.SoundCommand.INCREASE_NOTIFICATION,
                                String.valueOf(value));
                        return true;
                    } catch (SecurityException e) {
                        Log.w(TAG, "Нет разрешения для уведомлений, использую медиа");
                        SoundAPI.executeCommand(context,
                                SoundAPI.SoundCommand.INCREASE_MEDIA,
                                String.valueOf(value));
                        return true;
                    }

                case "ring":
                case "звонок":
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_RING,
                            String.valueOf(value));
                    return true;

                case "alarm":
                case "будильник":
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_ALARM,
                            String.valueOf(value));
                    return true;

                default:
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_MEDIA,
                            String.valueOf(value));
                    return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка увеличения громкости " + soundType, e);
            return false;
        }
    }

    /**
     * Уменьшить громкость конкретного типа
     */
    private boolean decreaseVolume(String soundType, int value) {
        try {
            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_MEDIA,
                            String.valueOf(value));
                    return true;

                case "notification":
                case "уведомление":
                    // Безопасный метод для уведомлений
                    try {
                        SoundAPI.executeCommand(context,
                                SoundAPI.SoundCommand.DECREASE_NOTIFICATION,
                                String.valueOf(value));
                        return true;
                    } catch (SecurityException e) {
                        Log.w(TAG, "Нет разрешения для уведомлений, использую медиа");
                        SoundAPI.executeCommand(context,
                                SoundAPI.SoundCommand.DECREASE_MEDIA,
                                String.valueOf(value));
                        return true;
                    }

                case "ring":
                case "звонок":
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_RING,
                            String.valueOf(value));
                    return true;

                case "alarm":
                case "будильник":
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_ALARM,
                            String.valueOf(value));
                    return true;

                default:
                    SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_MEDIA,
                            String.valueOf(value));
                    return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка уменьшения громкости " + soundType, e);
            return false;
        }
    }

    /**
     * Установить максимальную громкость (с безопасной обработкой уведомлений)
     */
    private boolean setMaxVolume(String soundType) {
        try {
            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MAX_MEDIA);
                    return true;

                case "notification":
                case "уведомление":
                    // Безопасная установка уведомлений (85% вместо 100%)
                    return setNotificationVolumeSafe(85);

                case "ring":
                case "звонок":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MAX_RING);
                    return true;

                case "alarm":
                case "будильник":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MAX_ALARM);
                    return true;

                default:
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MAX_MEDIA);
                    return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка установки максимальной громкости " + soundType, e);
            return false;
        }
    }

    /**
     * Безопасная установка громкости уведомлений (избегаем Do Not Disturb ошибки)
     */
    private boolean setNotificationVolumeSafe(int volume) {
        try {
            // Пробуем стандартный метод
            SoundAPI.executeCommand(context,
                    SoundAPI.SoundCommand.SET_NOTIFICATION,
                    String.valueOf(volume));
            return true;
        } catch (SecurityException e) {
            Log.w(TAG, "Не удалось установить громкость уведомлений, используем медиа: " + e.getMessage());
            try {
                // Fallback на медиа
                SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.SET_MEDIA,
                        String.valueOf(volume));
                return true;
            } catch (Exception e2) {
                Log.e(TAG, "Ошибка установки громкости медиа (fallback)", e2);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка безопасной установки уведомлений", e);
            return false;
        }
    }

    /**
     * Установить конкретную громкость
     */
    private boolean setVolume(String soundType, int value) {
        try {
            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.SET_MEDIA, String.valueOf(value));
                    return true;

                case "notification":
                case "уведомление":
                    return setNotificationVolumeSafe(value);

                case "ring":
                case "звонок":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.SET_RING, String.valueOf(value));
                    return true;

                case "alarm":
                case "будильник":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.SET_ALARM, String.valueOf(value));
                    return true;

                default:
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.SET_MEDIA, String.valueOf(value));
                    return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка установки громкости " + soundType, e);
            return false;
        }
    }

    /**
     * Получить информацию о громкости
     */
    private boolean getVolumeInfo(String soundType) {
        try {
            String info = "";
            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    info = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.GET_MEDIA_INFO);
                    break;

                case "notification":
                case "уведомление":
                    info = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.GET_NOTIFICATION_INFO);
                    break;

                case "ring":
                case "звонок":
                    info = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.GET_RING_INFO);
                    break;

                case "alarm":
                case "будильник":
                    info = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.GET_ALARM_INFO);
                    break;

                default:
                    info = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.GET_MEDIA_INFO);
                    break;
            }

            Log.i(TAG, "Информация о громкости " + getSoundTypeName(soundType) + ": " + info);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения информации о громкости", e);
            return false;
        }
    }

    /**
     * Получить читаемое имя типа звука
     */
    private String getSoundTypeName(String soundType) {
        switch (soundType.toLowerCase()) {
            case "media": return "медиа";
            case "notification": return "уведомлений";
            case "ring": return "звонка";
            case "alarm": return "будильника";
            case "system": return "системных звуков";
            default: return soundType;
        }
    }

    /**
     * Простой парсер на правилах (fallback)
     */
    private TinyLLMProcessor.CommandResult parseWithSimpleRules(String text) {
        text = text.toLowerCase().trim();
        Log.d(TAG, "Парсим текст: \"" + text + "\"");

        JSONObject params = new JSONObject();

        try {
            String cleanText = text.replaceAll("\\b(она|он|оно|немного|чуть|чуть-чуть|сильно|очень|пожалуйста|свет|подсветка)\\b", "").trim();
            cleanText = cleanText.replaceAll("\\s+", " ");

            Log.d(TAG, "Очищенный текст: \"" + cleanText + "\"");

            // 1. КОМАНДЫ ЯРКОСТИ
            if (text.contains("ярк") || text.contains("свет") || text.contains("подсвет") || cleanText.contains("ярк")) {
                Log.d(TAG, "Распознана команда яркости");

                // УВЕЛИЧЬ ЯРКОСТЬ
                if (cleanText.contains("увелич") || cleanText.contains("больше") || cleanText.contains("прибав")) {
                    int value = extractNumber(text, 30);
                    params.put("value", value);
                    Log.d(TAG, "Определено: brightness + increase, value=" + value);
                    return new TinyLLMProcessor.CommandResult("brightness", "increase", params);
                }
                // УМЕНЬШЬ ЯРКОСТЬ
                else if (cleanText.contains("уменьш") || cleanText.contains("меньше") || cleanText.contains("убав")) {
                    int value = extractNumber(text, 30);
                    params.put("value", value);
                    Log.d(TAG, "Определено: brightness + decrease, value=" + value);
                    return new TinyLLMProcessor.CommandResult("brightness", "decrease", params);
                }
                // МАКСИМУМ ЯРКОСТИ
                else if (cleanText.contains("макс") || cleanText.contains("максимум") || cleanText.contains("полную") ||
                        cleanText.contains("максимальную") || cleanText.contains("на максимум")) {
                    Log.d(TAG, "Определено: brightness + max");
                    return new TinyLLMProcessor.CommandResult("brightness", "max", params);
                }
                // МИНИМУМ ЯРКОСТИ
                else if (cleanText.contains("мин") || cleanText.contains("минимум") || cleanText.contains("выключи") ||
                        cleanText.contains("на минимум")) {
                    Log.d(TAG, "Определено: brightness + min");
                    return new TinyLLMProcessor.CommandResult("brightness", "min", params);
                }
                // СРЕДНЯЯ ЯРКОСТЬ
                else if (cleanText.contains("средн") || cleanText.contains("половин") || cleanText.contains("среднюю")) {
                    Log.d(TAG, "Определено: brightness + medium");
                    return new TinyLLMProcessor.CommandResult("brightness", "medium", params);
                }
                // УСТАНОВИТЬ КОНКРЕТНУЮ ЯРКОСТЬ
                else if (cleanText.contains("установи") || cleanText.contains("поставь") || cleanText.contains("сделай")) {
                    int value = extractNumber(text, 50);
                    params.put("value", value);
                    Log.d(TAG, "Определено: brightness + set, value=" + value);
                    return new TinyLLMProcessor.CommandResult("brightness", "set", params);
                }
                // ИНФОРМАЦИЯ О ЯРКОСТИ
                else if (cleanText.contains("информация") || cleanText.contains("статус") || cleanText.contains("сколько") ||
                        cleanText.contains("уровень")) {
                    Log.d(TAG, "Определено: brightness + get_info");
                    return new TinyLLMProcessor.CommandResult("brightness", "get_info", params);
                }
            }

            // 2. КОМАНДЫ ЗВУКА (ДОБАВЛЯЕМ РАЗДЕЛЕНИЕ ПО ТИПАМ)
            else if (text.contains("громк") || text.contains("звук") || text.contains("медиа") ||
                    text.contains("уведомл") || text.contains("звонок") || text.contains("будильник")) {

                Log.d(TAG, "Распознана команда звука");

                // Определяем тип звука
                String soundType = "media"; // по умолчанию
                if (text.contains("уведомл") || text.contains("оповещ")) {
                    soundType = "notification";
                } else if (text.contains("звонок") || text.contains("вызов")) {
                    soundType = "ring";
                } else if (text.contains("будильник") || text.contains("alarm")) {
                    soundType = "alarm";
                }

                params.put("type", soundType);

                // УВЕЛИЧЬ ГРОМКОСТЬ
                if (cleanText.contains("увелич") || cleanText.contains("больше") || cleanText.contains("прибав") ||
                        cleanText.contains("добав") || cleanText.contains("повысь")) {

                    int value = extractNumber(text, 1);
                    if (value == 0) value = 1;
                    params.put("value", value);
                    Log.d(TAG, "Определено: volume + increase, type=" + soundType + ", value=" + value);
                    return new TinyLLMProcessor.CommandResult("volume", "increase", params);
                }
                // УМЕНЬШЬ ГРОМКОСТЬ
                else if (cleanText.contains("уменьш") || cleanText.contains("меньше") || cleanText.contains("убав") ||
                        cleanText.contains("снизь") || cleanText.contains("понизь")) {

                    int value = extractNumber(text, 1);
                    if (value == 0) value = 1;
                    params.put("value", value);
                    Log.d(TAG, "Определено: volume + decrease, type=" + soundType + ", value=" + value);
                    return new TinyLLMProcessor.CommandResult("volume", "decrease", params);
                }
                // ВЫКЛЮЧИ ЗВУК
                else if (cleanText.contains("выключи") || cleanText.contains("отключи") || cleanText.contains("заглуши") ||
                        cleanText.contains("mute") || cleanText.contains("ноль") || cleanText.contains("тихо")) {

                    Log.d(TAG, "Определено: volume + mute, type=" + soundType);
                    return new TinyLLMProcessor.CommandResult("volume", "mute", params);
                }
                // ВКЛЮЧИ ЗВУК
                else if (cleanText.contains("включи звук") || cleanText.contains("unmute") || cleanText.contains("включи")) {

                    Log.d(TAG, "Определено: volume + unmute, type=" + soundType);
                    return new TinyLLMProcessor.CommandResult("volume", "unmute", params);
                }
                // МАКСИМАЛЬНАЯ ГРОМКОСТЬ
                else if (cleanText.contains("макс") || cleanText.contains("максимум") || cleanText.contains("полную") ||
                        cleanText.contains("до упора") || cleanText.contains("на максимум")) {

                    Log.d(TAG, "Определено: volume + max, type=" + soundType);
                    return new TinyLLMProcessor.CommandResult("volume", "max", params);
                }
                // УСТАНОВИТЬ КОНКРЕТНУЮ ГРОМКОСТЬ
                else if (cleanText.contains("установи") || cleanText.contains("поставь") || cleanText.contains("сделай")) {
                    int value = extractNumber(text, 50);
                    params.put("value", value);
                    Log.d(TAG, "Определено: volume + set, type=" + soundType + ", value=" + value);
                    return new TinyLLMProcessor.CommandResult("volume", "set", params);
                }
                // ИНФОРМАЦИЯ О ГРОМКОСТИ
                else if (cleanText.contains("информация") || cleanText.contains("статус") || cleanText.contains("сколько") ||
                        cleanText.contains("уровень")) {
                    Log.d(TAG, "Определено: volume + get_info, type=" + soundType);
                    return new TinyLLMProcessor.CommandResult("volume", "get_info", params);
                }
            }

            // 3. КОМАНДЫ WIFI
            else if (text.contains("wifi") || text.contains("вайфай") || text.contains("интернет")) {
                Log.d(TAG, "Распознана команда Wi-Fi");

                // ВКЛЮЧИТЬ WIFI
                if (cleanText.contains("включи") || cleanText.contains("подключи")) {
                    Log.d(TAG, "Определено: wifi + enable");
                    return new TinyLLMProcessor.CommandResult("wifi", "enable", params);
                }
                // ВЫКЛЮЧИТЬ WIFI
                else if (cleanText.contains("выключи") || cleanText.contains("отключи")) {
                    Log.d(TAG, "Определено: wifi + disable");
                    return new TinyLLMProcessor.CommandResult("wifi", "disable", params);
                }
                // СТАТУС WIFI
                else if (cleanText.contains("статус") || cleanText.contains("состояние") || cleanText.contains("включен") ||
                        cleanText.contains("выключен")) {
                    Log.d(TAG, "Определено: wifi + status");
                    return new TinyLLMProcessor.CommandResult("wifi", "status", params);
                }
                // СКАНИРОВАНИЕ
                else if (cleanText.contains("сканируй") || cleanText.contains("найди") || cleanText.contains("поиск")) {
                    Log.d(TAG, "Определено: wifi + scan");
                    return new TinyLLMProcessor.CommandResult("wifi", "scan", params);
                }
            }

            // 4. КОМАНДЫ ЗАПУСКА ПРИЛОЖЕНИЙ
            else if (text.contains("открой") || text.contains("запусти") || text.contains("приложение") ||
                    text.contains("программу") || text.contains("запусти приложение")) {
                Log.d(TAG, "Распознана команда запуска приложения");

                if (cleanText.contains("настройк") || cleanText.contains("settings")) {
                    params.put("app", "settings");
                    Log.d(TAG, "Определено: launch + open, app=settings");
                    return new TinyLLMProcessor.CommandResult("launch", "open", params);
                } else if (cleanText.contains("камер") || cleanText.contains("camera")) {
                    params.put("app", "camera");
                    Log.d(TAG, "Определено: launch + open, app=camera");
                    return new TinyLLMProcessor.CommandResult("launch", "open", params);
                } else if (cleanText.contains("телефон") || cleanText.contains("phone") || cleanText.contains("звонк")) {
                    params.put("app", "phone");
                    Log.d(TAG, "Определено: launch + open, app=phone");
                    return new TinyLLMProcessor.CommandResult("launch", "open", params);
                } else if (cleanText.contains("галере") || cleanText.contains("gallery") || cleanText.contains("фото")) {
                    params.put("app", "gallery");
                    Log.d(TAG, "Определено: launch + open, app=gallery");
                    return new TinyLLMProcessor.CommandResult("launch", "open", params);
                } else if (cleanText.contains("браузер") || cleanText.contains("browser") || cleanText.contains("интернет")) {
                    params.put("app", "chrome");
                    Log.d(TAG, "Определено: launch + open, app=chrome");
                    return new TinyLLMProcessor.CommandResult("launch", "open", params);
                } else if (cleanText.contains("сообщен") || cleanText.contains("messages") || cleanText.contains("смс")) {
                    params.put("app", "messages");
                    Log.d(TAG, "Определено: launch + open, app=messages");
                    return new TinyLLMProcessor.CommandResult("launch", "open", params);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в простом парсере", e);
        }

        Log.w(TAG, "Не удалось распознать команду: \"" + text + "\"");
        return new TinyLLMProcessor.CommandResult("unknown", "", params);
    }

    /**
     * Команды управления Wi-Fi
     */
    private boolean executeWifiCommand(TinyLLMProcessor.CommandResult result) {
        String action = result.action;
        JSONObject params = result.params;

        Log.i(TAG, "Управление Wi-Fi: " + action + ", params: " + (params != null ? params.toString() : "null"));

        try {
            switch (action.toLowerCase()) {
                case "enable":
                case "включить":
                case "подключить":
                    WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.ENABLE);
                    return true;

                case "disable":
                case "выключить":
                case "отключить":
                    WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.DISABLE);
                    return true;

                case "status":
                case "статус":
                case "состояние":
                    String status = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_STATUS);
                    Log.i(TAG, "Статус Wi-Fi: " + status);
                    return true;

                case "status_only":
                case "статус_только":
                case "простой_статус":
                    String simpleStatus = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_STATUS_ONLY);
                    Log.i(TAG, "Простой статус Wi-Fi: " + simpleStatus);
                    return true;

                case "scan":
                case "сканировать":
                    WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.SCAN);
                    return true;

                case "connect":
                case "подключиться":
                case "подключить_к":
                    if (params != null) {
                        String ssid = params.optString("ssid", "");
                        String password = params.optString("password", "");
                        if (!ssid.isEmpty()) {
                            WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CONNECT, ssid, password);
                            return true;
                        }
                    }
                    Log.w(TAG, "Не указан SSID для подключения");
                    return false;

                case "disconnect":
                case "отключиться":
                case "разъединить":
                    WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.DISCONNECT);
                    return true;

                case "check_permission":
                case "проверить_разрешение":
                case "проверить_разрешения":
                    String permissionStatus = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CHECK_LOCATION_PERMISSION);
                    Log.i(TAG, "Статус разрешений: " + permissionStatus);
                    return true;

                case "check_gps":
                case "проверить_gps":
                case "проверить_геолокацию":
                    String gpsStatus = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CHECK_GPS_ENABLED);
                    Log.i(TAG, "Статус GPS: " + gpsStatus);
                    return true;

                case "check_wifi":
                case "проверить_wifi":
                case "проверить_вайфай":
                    String wifiStatus = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CHECK_WIFI_ENABLED);
                    Log.i(TAG, "Статус Wi-Fi (проверка): " + wifiStatus);
                    return true;

                default:
                    Log.w(TAG, "Неизвестное действие для Wi-Fi: " + action);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды Wi-Fi", e);
            return false;
        }
    }

    /**
     * Команды запуска приложений
     */
    private boolean executeAppLaunchCommand(TinyLLMProcessor.CommandResult result) {
        if (result.params == null) {
            Log.w(TAG, "Нет параметров для запуска приложения");
            return false;
        }

        String appName = result.params.optString("app", "");
        if (appName.isEmpty()) {
            Log.w(TAG, "Не указано имя приложения");
            return false;
        }

        Log.i(TAG, "Запуск приложения: " + appName);

        try {
            AppLauncher launcher = new AppLauncher(context);
            boolean success = launcher.launchAppByName(appName);

            if (!success) {
                Log.w(TAG, "Не удалось запустить приложение: " + appName);
                return tryAlternativeAppNames(appName);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка запуска приложения", e);
            return false;
        }
    }

    private boolean tryAlternativeAppNames(String appName) {
        AppLauncher launcher = new AppLauncher(context);

        try {
            switch (appName.toLowerCase()) {
                case "настройки":
                case "settings":
                    return launcher.launchAppByName("settings");
                case "камера":
                case "camera":
                    return launcher.launchAppByName("camera");
                case "галерея":
                case "gallery":
                case "фото":
                    return launcher.launchAppByName("gallery");
                case "телефон":
                case "phone":
                case "звонки":
                    return launcher.launchAppByName("phone");
                case "сообщения":
                case "messages":
                case "смс":
                    return launcher.launchAppByName("messages");
                case "браузер":
                case "browser":
                case "интернет":
                    return launcher.launchAppByName("chrome");
                default:
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка запуска приложения по альтернативному имени", e);
            return false;
        }
    }

    /**
     * Системные команды
     */
    private boolean executeSystemCommand(TinyLLMProcessor.CommandResult result) {
        String action = result.action;

        Log.i(TAG, "Системная команда: " + action);

        switch (action.toLowerCase()) {
            case "reboot":
            case "перезагрузка":
                Log.i(TAG, "Запрошена перезагрузка системы");
                return true;

            case "sleep":
            case "сон":
            case "режим сна":
                Log.i(TAG, "Запрошен режим сна");
                return true;

            case "wake":
            case "проснуться":
            case "разбудить":
                Log.i(TAG, "Запрошено пробуждение");
                return true;

            default:
                return false;
        }
    }

    /**
     * Команды управления медиа
     */
    private boolean executeMediaCommand(TinyLLMProcessor.CommandResult result) {
        String action = result.action;

        Log.i(TAG, "Медиа команда: " + action);

        switch (action.toLowerCase()) {
            case "play":
            case "играть":
            case "старт":
                Log.i(TAG, "Воспроизведение медиа");
                return true;
            case "pause":
            case "пауза":
            case "стоп":
                Log.i(TAG, "Пауза медиа");
                return true;
            case "next":
            case "следующий":
                Log.i(TAG, "Следующий трек");
                return true;
            case "previous":
            case "предыдущий":
                Log.i(TAG, "Предыдущий трек");
                return true;
            default:
                return false;
        }
    }

    /**
     * Fallback выполнение команды (если AI не сработал)
     */
    private boolean executeFallbackCommand(String text) {
        text = text.toLowerCase().trim();
        Log.d(TAG, "Fallback для: \"" + text + "\"");

        try {
            // Простые команды яркости
            if (text.contains("ярк") || text.contains("свет")) {
                if (text.contains("увелич") || text.contains("больше") || text.contains("прибав")) {
                    int value = extractNumber(text, 30);
                    String result = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.INCREASE,
                            String.valueOf(value));
                    Log.d(TAG, "Яркость увеличена: " + result);
                    return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
                } else if (text.contains("уменьш") || text.contains("меньше") || text.contains("убав")) {
                    int value = extractNumber(text, 30);
                    String result = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.DECREASE,
                            String.valueOf(value));
                    Log.d(TAG, "Яркость уменьшена: " + result);
                    return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
                } else if (text.contains("макс") || text.contains("максимум") || text.contains("полную")) {
                    String result = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.MAX);
                    Log.d(TAG, "Яркость на максимум: " + result);
                    return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
                } else if (text.contains("мин") || text.contains("минимум") || text.contains("выключи")) {
                    String result = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.MIN);
                    Log.d(TAG, "Яркость на минимум: " + result);
                    return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
                } else if (text.contains("средн") || text.contains("половин")) {
                    String result = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.MEDIUM);
                    Log.d(TAG, "Яркость средняя: " + result);
                    return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
                }
            }
            // Простые команды звука
            else if (text.contains("громк") || text.contains("звук") ||
                    text.contains("медиа") || text.contains("уведомл") ||
                    text.contains("звонок") || text.contains("будильник")) {

                // Определяем тип звука
                String soundType = "media";
                if (text.contains("уведомл")) soundType = "notification";
                else if (text.contains("звонок")) soundType = "ring";
                else if (text.contains("будильник")) soundType = "alarm";

                if (text.contains("увелич") || text.contains("больше") || text.contains("прибав")) {
                    int value = extractNumber(text, 1);
                    Log.d(TAG, "Увеличиваю " + soundType + " на " + value);
                    return increaseVolume(soundType, value);
                } else if (text.contains("уменьш") || text.contains("меньше") || text.contains("убав")) {
                    int value = extractNumber(text, 1);
                    Log.d(TAG, "Уменьшаю " + soundType + " на " + value);
                    return decreaseVolume(soundType, value);
                } else if (text.contains("выключи") || text.contains("отключи") ||
                        text.contains("mute") || text.contains("ноль") || text.contains("тихо")) {
                    Log.d(TAG, "Выключаю " + soundType);
                    return muteVolume(soundType);
                } else if (text.contains("включи") && (text.contains("звук") || text.contains(soundType))) {
                    Log.d(TAG, "Включаю " + soundType);
                    return unmuteVolume(soundType);
                } else if (text.contains("макс") || text.contains("максимум") || text.contains("полную")) {
                    Log.d(TAG, "Максимум " + soundType);
                    return setMaxVolume(soundType);
                } else if (text.contains("установи") || text.contains("поставь") || text.contains("сделай")) {
                    int value = extractNumber(text, 50);
                    Log.d(TAG, "Устанавливаю " + soundType + " на " + value);
                    return setVolume(soundType, value);
                }
            }
            // Простые команды Wi-Fi
            else if (text.contains("wifi") || text.contains("вайфай") || text.contains("интернет")) {
                if (text.contains("включи")) {
                    String result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.ENABLE);
                    Log.d(TAG, "Wi-Fi включен: " + result);
                    return !result.contains("Ошибка");
                } else if (text.contains("выключи")) {
                    String result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.DISABLE);
                    Log.d(TAG, "Wi-Fi выключен: " + result);
                    return !result.contains("Ошибка");
                } else if (text.contains("статус") || text.contains("состояние")) {
                    String result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_STATUS);
                    Log.d(TAG, "Статус Wi-Fi: " + result);
                    return !result.contains("Ошибка");
                }
            }
            // Простые команды запуска приложений
            else if (text.contains("открой") || text.contains("запусти")) {
                if (text.contains("настройк")) {
                    AppLauncher launcher = new AppLauncher(context);
                    boolean success = launcher.launchAppByName("settings");
                    Log.d(TAG, "Открываю настройки: " + success);
                    return success;
                } else if (text.contains("камер")) {
                    AppLauncher launcher = new AppLauncher(context);
                    boolean success = launcher.launchAppByName("camera");
                    Log.d(TAG, "Открываю камеру: " + success);
                    return success;
                } else if (text.contains("телефон")) {
                    AppLauncher launcher = new AppLauncher(context);
                    boolean success = launcher.launchAppByName("phone");
                    Log.d(TAG, "Открываю телефон: " + success);
                    return success;
                }
            }

            Log.w(TAG, "Fallback не смог обработать команду: " + text);
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в fallback выполнении", e);
            return false;
        }
    }

    /**
     * Выключить звук конкретного типа
     */
    private boolean muteVolume(String soundType) {
        try {
            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_MEDIA);
                    return true;

                case "notification":
                case "уведомление":
                    try {
                        SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_NOTIFICATION);
                        return true;
                    } catch (SecurityException e) {
                        Log.w(TAG, "Нет разрешения для уведомлений, использую медиа");
                        SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_MEDIA);
                        return true;
                    }

                case "ring":
                case "звонок":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_RING);
                    return true;

                case "alarm":
                case "будильник":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_ALARM);
                    return true;

                default:
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_MEDIA);
                    return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выключения звука " + soundType, e);
            return false;
        }
    }

    /**
     * Включить звук конкретного типа
     */
    private boolean unmuteVolume(String soundType) {
        try {
            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_MEDIA);
                    return true;

                case "notification":
                case "уведомление":
                    try {
                        SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_NOTIFICATION);
                        return true;
                    } catch (SecurityException e) {
                        Log.w(TAG, "Нет разрешения для уведомлений, использую медиа");
                        SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_MEDIA);
                        return true;
                    }

                case "ring":
                case "звонок":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_RING);
                    return true;

                case "alarm":
                case "будильник":
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_ALARM);
                    return true;

                default:
                    SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_MEDIA);
                    return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка включения звука " + soundType, e);
            return false;
        }
    }

    //Извлечение числа из текста
    private int extractNumber(String text, int defaultValue) {
        try {
            String[] words = text.split(" ");
            for (String word : words) {
                word = word.replaceAll("[^0-9]", "");
                if (!word.isEmpty()) {
                    return Integer.parseInt(word);
                }
            }

            if (text.contains("десять") || text.contains("10")) return 10;
            if (text.contains("двадцать") || text.contains("20")) return 20;
            if (text.contains("тридцать") || text.contains("30")) return 30;
            if (text.contains("пятьдесят") || text.contains("50")) return 50;
            if (text.contains("сто") || text.contains("100")) return 100;

        } catch (Exception e) {
            Log.w(TAG, "Не удалось извлечь число из: " + text);
        }

        return defaultValue;
    }

    //Проверка на активационное слово
    private boolean isActivationWord(String text) {
        text = text.toLowerCase().trim();
        return text.contains("клевер") || text.contains("clever");
    }

    //Логирование выполнения команды
    private void logCommandExecution(String originalText, TinyLLMProcessor.CommandResult result) {
        Log.i(TAG, String.format(
                "Лог команды:\n" +
                        "   Оригинал: %s\n" +
                        "   Команда: %s\n" +
                        "   Действие: %s\n" +
                        "   AI использован: %s",
                originalText,
                result.command,
                result.action,
                useAI ? "Да" : "Нет (правила)"
        ));
    }

    //Освобождение ресурсов
    public void release() {
        if (llmProcessor != null) {
            llmProcessor.release();
        }
        commandHistory.clear();
        Log.i(TAG, "Ресурсы AI процессора освобождены");
    }

    //Внутренний класс для хранения истории команд
    private static class CommandHistory {
        private static final int MAX_HISTORY = 10;
        private String[] history = new String[MAX_HISTORY];
        private int index = 0;

        public void add(String command) {
            history[index % MAX_HISTORY] = command;
            index++;
        }

        public void clear() {
            history = new String[MAX_HISTORY];
            index = 0;
        }

        public String getLastCommand() {
            if (index == 0) return null;
            return history[(index - 1) % MAX_HISTORY];
        }
    }
}