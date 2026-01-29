package pro.cleverlife.clevervoice.AI;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import pro.cleverlife.clevervoice.API.BrightnessAPI;
import pro.cleverlife.clevervoice.API.SoundAPI;
import pro.cleverlife.clevervoice.API.WiFiAPI;
import pro.cleverlife.clevervoice.system.AppLauncher;
import pro.cleverlife.clevervoice.utils.ShellCommandExecutor;

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

        //Метод для вывода информации об AI анализе
        default void onAIResult(String command, String action, JSONObject params, boolean usedAI) {
            // Реализация по умолчанию пустая для обратной совместимости
        }

        //Метод для вывода результата выполнения
        default void onCommandResult(String resultMessage) {
            // Реализация по умолчанию пустая для обратной совместимости
        }
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

    //Обработка распознанного текста с колбэком
    public void processRecognizedText(String recognizedText, SimpleCallback callback) {
        Log.i(TAG, "=== НАЧАЛО обработки ===");
        Log.i(TAG, "Исходный текст: \"" + recognizedText + "\"");

        debugStatus();

        if (recognizedText == null || recognizedText.trim().isEmpty()) {
            Log.w(TAG, "Пустой текст для обработки");
            if (callback != null) {
                callback.onCommandProcessed(false);
            }
            return;
        }

        // ШАГ 1: Исправляем ошибки распознавания
        String correctedText = fixSpeechRecognitionErrors(recognizedText);
        Log.i(TAG, "Исправленный текст: \"" + correctedText + "\"");

        // ШАГ 2: Специальная обработка для команд CleverHome даже с ошибками
        String lowerCorrected = correctedText.toLowerCase();
        if (lowerCorrected.contains("у дом") ||
                lowerCorrected.contains("умн дом") ||
                lowerCorrected.contains("умны дом") ||
                (lowerCorrected.contains("у") && lowerCorrected.contains("дом") &&
                        (lowerCorrected.contains("запусти") || lowerCorrected.contains("открой") || lowerCorrected.contains("включи")))) {

            Log.i(TAG, "ОБНАРУЖЕНА КОМАНДА CLEVERHOME С ОШИБКАМИ РАСПОЗНАВАНИЯ");

            // Форсируем обработку как CleverHome команды
            String action = "launch"; // По умолчанию запуск
            if (lowerCorrected.contains("перезапуск") || lowerCorrected.contains("рестарт")) {
                action = "restart";
            } else if (lowerCorrected.contains("останови") || lowerCorrected.contains("закрой")) {
                action = "stop";
            } else if (lowerCorrected.contains("статус")) {
                action = "status";
            }

            TinyLLMProcessor.CommandResult forcedResult =
                    new TinyLLMProcessor.CommandResult("cleverhome", action, new JSONObject());

            if (callback != null) {
                callback.onAIResult("cleverhome", action, new JSONObject(), false);
            }

            boolean success = executeCleverHomeCommand(forcedResult, callback);

            if (callback != null) {
                handler.postDelayed(() -> {
                    callback.onCommandProcessed(success);
                }, 300);
            }
            return;
        }

        String text = correctedText.trim();
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

    //Основная логика обработки команды
    private void processCommand(String text, final SimpleCallback callback) {
        Log.d(TAG, "НАЧАЛО processCommand: \"" + text + "\"");

        try {
            String lower = text.toLowerCase();

            // Проверяем различные варианты "умный дом" с ошибками
            boolean isCleverHomeWithErrors =
                    (lower.contains("у") && lower.contains("дом")) ||
                            (lower.contains("умн") && lower.contains("дом")) ||
                            (lower.contains("умны") && lower.contains("дом")) ||
                            lower.contains("у дом") ||
                            lower.contains("умн дом") ||
                            lower.contains("умны дом");

            // Проверяем команды запуска/открытия с "у дом"
            boolean isLaunchCleverHomeWithErrors =
                    (lower.contains("запусти") || lower.contains("открой") || lower.contains("включи")) &&
                            (lower.contains("у дом") || (lower.contains("у") && lower.contains("дом")));

            if (isCleverHomeWithErrors || isLaunchCleverHomeWithErrors) {
                Log.i(TAG, "⚠️ КОМАНДА CLEVERHOME С ОШИБКАМИ РАСПОЗНАВАНИЯ");

                String action = "launch"; // По умолчанию запуск
                if (lower.contains("перезапуск") || lower.contains("рестарт")) {
                    action = "restart";
                } else if (lower.contains("останови") || lower.contains("закрой")) {
                    action = "stop";
                } else if (lower.contains("статус")) {
                    action = "status";
                }

                TinyLLMProcessor.CommandResult result =
                        new TinyLLMProcessor.CommandResult("cleverhome", action, new JSONObject());

                // Отправляем информацию о результате
                if (callback != null) {
                    callback.onAIResult("cleverhome", action, new JSONObject(), false);
                }

                // Выполняем команду
                boolean success = executeCleverHomeCommand(result, callback);

                if (callback != null) {
                    handler.postDelayed(() -> {
                        callback.onCommandProcessed(success);
                    }, 300);
                }
                return;
            }

            // ШАГ 1: Специальная обработка команд CleverHome ДО всего остального
            if (isCleverHomeCommand(text)) {
                Log.i(TAG, "⚠️ ОБНАРУЖЕНА КОМАНДА CLEVERHOME - специальная обработка");
                handleCleverHomeCommand(text, callback);
                return;
            }

            // ШАГ 2: Попытка AI распознавания
            TinyLLMProcessor.CommandResult result = null;
            boolean aiRecognized = false;

            if (llmProcessor != null) {
                Log.d(TAG, "Попытка AI распознавания...");
                result = llmProcessor.understandCommand(text);

                if (result != null && !"unknown".equals(result.command)) {
                    aiRecognized = true;
                    Log.i(TAG, "AI распознал: " + result.command + " -> " + result.action);
                } else {
                    Log.w(TAG, "AI не распознал команду");
                }
            }

            // ШАГ 3: Fallback на простые правила если AI не распознал
            if (!aiRecognized) {
                Log.d(TAG, "Использую fallback парсер...");
                result = parseWithSimpleRules(text);
                Log.d(TAG, "Fallback результат: " + result.command + " -> " + result.action);
            }

            // ШАГ 4: Отправляем информацию о результате анализа
            if (callback != null && result != null) {
                try {
                    callback.onAIResult(
                            result.command,
                            result.action,
                            result.params != null ? result.params : new JSONObject(),
                            aiRecognized
                    );
                } catch (Exception e) {
                    Log.w(TAG, "Ошибка onAIResult", e);
                }
            }

            // ШАГ 5: Выполняем команду
            boolean success = false;
            if (result != null && !"unknown".equals(result.command)) {
                Log.i(TAG, "Выполняю команду: " + result.command + " -> " + result.action);

                // СПЕЦИАЛЬНАЯ ПРОВЕРКА: Если команда содержит ключевые слова CleverHome, перенаправляем
                if (containsCleverHomeKeywords(text) && !"cleverhome".equals(result.command)) {
                    Log.w(TAG, "⚠️ ПЕРЕНАПРАВЛЕНИЕ: Текст содержит 'умный дом', но AI вернул " + result.command);
                    success = executeCleverHomeCommand(
                            new TinyLLMProcessor.CommandResult("cleverhome", "launch", result.params),
                            callback
                    );
                } else {
                    success = executeCommand(result, callback);
                }

                // Если не удалось выполнить, пробуем fallback
                if (!success) {
                    Log.w(TAG, "Основной обработчик не справился, пробуем fallback...");
                    success = executeFallbackCommand(text, callback);
                }
            } else {
                Log.w(TAG, "Команда не распознана, использую fallback...");
                success = executeFallbackCommand(text, callback);
            }

            // ШАГ 6: Логирование и завершение
            Log.i(TAG, "Финальный результат: " + (success ? "УСПЕХ" : "НЕУДАЧА"));

            if (result != null) {
                logCommandExecution(text, result);
            }

            if (callback != null) {
                boolean finalSuccess = success;
                handler.postDelayed(() -> {
                    callback.onCommandProcessed(finalSuccess);
                }, 300);
            }

        } catch (Exception e) {
            Log.e(TAG, "КРИТИЧЕСКАЯ ошибка в processCommand", e);
            e.printStackTrace();

            // Экстренный fallback
            try {
                boolean fallbackSuccess = executeFallbackCommand(text, callback);
                if (callback != null) {
                    handler.postDelayed(() -> {
                        callback.onCommandProcessed(fallbackSuccess);
                    }, 300);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Ошибка в экстренном fallback", ex);
                if (callback != null) {
                    handler.postDelayed(() -> {
                        callback.onCommandProcessed(false);
                    }, 300);
                }
            }
        }

        Log.d(TAG, "КОНЕЦ processCommand");
    }

    //Проверка, является ли команда командой CleverHome
    private boolean isCleverHomeCommand(String text) {
        if (text == null) return false;

        String lower = text.toLowerCase();
        return lower.contains("клевер") ||
                lower.contains("умный дом") ||
                lower.contains("clever") ||
                lower.contains("умныйдом") ||
                lower.contains("клевер хоум") ||
                lower.contains("cleverhome");
    }

    //Проверка содержит ли текст ключевые слова CleverHome
    private boolean containsCleverHomeKeywords(String text) {
        if (text == null) return false;

        String lower = text.toLowerCase();
        return lower.contains("умный дом") ||
                lower.contains("клевер") ||
                lower.contains("clever");
    }

    //Обработка команд CleverHome
    private void handleCleverHomeCommand(String text, SimpleCallback callback) {
        String lower = text.toLowerCase();
        JSONObject params = new JSONObject();
        String action = "launch"; // По умолчанию - запуск

        try {
            if (lower.contains("перезапуск") || lower.contains("рестарт")) {
                action = "restart";
            } else if (lower.contains("останови") || lower.contains("закрой") || lower.contains("выключи")) {
                action = "stop";
            } else if (lower.contains("статус") || lower.contains("состояние")) {
                action = "status";
            }

            TinyLLMProcessor.CommandResult result =
                    new TinyLLMProcessor.CommandResult("cleverhome", action, params);

            // Отправляем информацию о результате
            if (callback != null) {
                callback.onAIResult("cleverhome", action, params, false);
            }

            // Выполняем команду
            boolean success = executeCleverHomeCommand(result, callback);

            if (callback != null) {
                handler.postDelayed(() -> {
                    callback.onCommandProcessed(success);
                }, 300);
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка обработки CleverHome команды", e);
            if (callback != null) {
                handler.postDelayed(() -> {
                    callback.onCommandProcessed(false);
                }, 300);
            }
        }
    }

    //Выполнение команды, определенной AI
    private boolean executeCommand(TinyLLMProcessor.CommandResult result, SimpleCallback callback) {
        if (result == null || "unknown".equals(result.command)) {
            Log.w(TAG, "Неизвестная команда");
            return false;
        }

        try {
            switch (result.command.toLowerCase()) {
                case "brightness":
                case "яркость":
                case "свет":
                    return executeBrightnessCommand(result, callback);

                case "volume":
                case "sound":
                case "громкость":
                case "звук":
                    return executeVolumeCommand(result, callback);

                case "wifi":
                case "вайфай":
                case "вай фай":
                case "вай-фай":
                case "сеть":
                case "интернет":
                    Log.i(TAG, "Выполняю WiFi команду: " + result.action);
                    return executeWifiCommand(result, callback);

                case "launch":
                case "app":
                case "приложение":
                case "открыть":
                    return executeAppLaunchCommand(result, callback);

                case "system":
                case "система":
                    return executeSystemCommand(result, callback);

                case "media":
                case "медиа":
                    // ИСПРАВЛЕНО: "медиа" - это команда звука
                    JSONObject mediaParams = new JSONObject();
                    mediaParams.put("type", "media");
                    TinyLLMProcessor.CommandResult volumeResult =
                            new TinyLLMProcessor.CommandResult("volume", result.action, mediaParams);
                    return executeVolumeCommand(volumeResult, callback);

                case "cleverhome":
                case "клевер":
                case "умный дом":
                    Log.i(TAG, "Выполняю команду CleverHome: " + result.action);
                    return executeCleverHomeCommand(result, callback);

                case "device":
                case "устройство":
                    Log.i(TAG, "Выполняю команду устройства: " + result.action);
                    return executeDeviceCommand(result, callback);

                default:
                    Log.w(TAG, "Неподдерживаемая команда: " + result.command);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды: " + result.command, e);
            return false;
        }
    }

    //Команды управления яркостью
    private boolean executeBrightnessCommand(TinyLLMProcessor.CommandResult result, SimpleCallback callback) {
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
                    String incResult = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.INCREASE,
                            String.valueOf(incValue));
                    if (callback != null) {
                        callback.onCommandResult("Яркость увеличена на " + incValue + "%: " + incResult);
                    }
                    return true;

                case "decrease":
                case "уменьшить":
                case "убавить":
                case "меньше":
                    int decValue = params != null ?
                            Integer.parseInt(params.optString("value", "30")) : 30;
                    Log.i(TAG, "Уменьшаю яркость на " + decValue + "%");
                    String decResult = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.DECREASE,
                            String.valueOf(decValue));
                    if (callback != null) {
                        callback.onCommandResult("Яркость уменьшена на " + decValue + "%: " + decResult);
                    }
                    return true;

                case "max":
                case "максимум":
                case "полная":
                case "maximum":
                    Log.i(TAG, "Устанавливаю максимальную яркость");
                    String maxResult = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.MAX);
                    if (callback != null) {
                        callback.onCommandResult("Максимальная яркость установлена: " + maxResult);
                    }
                    return true;

                case "min":
                case "минимум":
                case "выключить":
                case "minimum":
                    Log.i(TAG, "Устанавливаю минимальную яркость");
                    String minResult = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.MIN);
                    if (callback != null) {
                        callback.onCommandResult("Минимальная яркость установлена: " + minResult);
                    }
                    return true;

                case "set":
                case "установить":
                case "поставить":
                    if (params != null) {
                        String value = params.optString("value", "50");
                        Log.i(TAG, "Устанавливаю яркость на " + value + "%");
                        String setResult = BrightnessAPI.executeCommand(context,
                                BrightnessAPI.BrightnessCommand.SET,
                                value);
                        if (callback != null) {
                            callback.onCommandResult("Яркость установлена на " + value + "%: " + setResult);
                        }
                        return true;
                    }
                    Log.w(TAG, "Нет параметра value для установки яркости");
                    return false;

                case "medium":
                case "средняя":
                case "половина":
                    Log.i(TAG, "Устанавливаю среднюю яркость");
                    String mediumResult = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.MEDIUM);
                    if (callback != null) {
                        callback.onCommandResult("Средняя яркость установлена: " + mediumResult);
                    }
                    return true;

                case "get_info":
                case "информация":
                case "статус":
                    String info = BrightnessAPI.executeCommand(context,
                            BrightnessAPI.BrightnessCommand.GET_INFO);
                    Log.i(TAG, "Информация о яркости: " + info);
                    if (callback != null) {
                        callback.onCommandResult("Информация о яркости: " + info);
                    }
                    return true;

                default:
                    Log.w(TAG, "Неизвестное действие для яркости: " + action);

                    // Автоматически определяем действие по тексту
                    if (action.contains("увелич") || action.contains("increase")) {
                        return executeBrightnessCommand(new TinyLLMProcessor.CommandResult(
                                "brightness", "increase", params), callback);
                    } else if (action.contains("уменьш") || action.contains("decrease") ||
                            action.contains("меньше")) {
                        return executeBrightnessCommand(new TinyLLMProcessor.CommandResult(
                                "brightness", "decrease", params), callback);
                    } else if (action.contains("макс") || action.contains("max")) {
                        return executeBrightnessCommand(new TinyLLMProcessor.CommandResult(
                                "brightness", "max", params), callback);
                    } else if (action.contains("мин") || action.contains("min")) {
                        return executeBrightnessCommand(new TinyLLMProcessor.CommandResult(
                                "brightness", "min", params), callback);
                    }

                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды яркости", e);
            return false;
        }
    }

    //Команды управления громкостью (ИСПРАВЛЕННАЯ ВЕРСИЯ)
    private boolean executeVolumeCommand(TinyLLMProcessor.CommandResult result, SimpleCallback callback) {
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
                        return increaseAllVolumes(incValue, callback);
                    } else {
                        Log.i(TAG, "Увеличиваю громкость " + getSoundTypeName(soundType) + " на " + incValue);
                        return increaseVolume(soundType, incValue, callback);
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
                        return decreaseAllVolumes(decValue, callback);
                    } else {
                        Log.i(TAG, "Уменьшаю громкость " + getSoundTypeName(soundType) + " на " + decValue);
                        return decreaseVolume(soundType, decValue, callback);
                    }

                case "mute":
                case "выключить":
                case "отключить":
                case "заглушить":
                case "min":
                case "минимум":
                    // ВЫКЛЮЧАЕМ ВСЕ 4 ТИПА ЗВУКА
                    Log.i(TAG, "Выключаю звук ВСЕХ типов");
                    return muteAllVolumes(callback);

                case "unmute":
                case "включить":
                case "включить звук":
                    // ВКЛЮЧАЕМ ВСЕ 4 ТИПА ЗВУКА
                    Log.i(TAG, "Включаю звук ВСЕХ типов");
                    return unmuteAllVolumes(callback);

                case "max":
                case "максимум":
                case "полная":
                    // ДЛЯ УВЕДОМЛЕНИЙ ИСПОЛЬЗУЕМ БЕЗОПАСНЫЙ МЕТОД (чтобы избежать ошибки Do Not Disturb)
                    if (soundType.equals("notification") || soundType.equals("уведомление")) {
                        Log.i(TAG, "Устанавливаю высокую (но не максимальную) громкость уведомлений");
                        return setNotificationVolumeSafe(85, callback); // 85% вместо 100%
                    } else {
                        Log.i(TAG, "Максимальная громкость " + getSoundTypeName(soundType));
                        return setMaxVolume(soundType, callback);
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
                        return setVolume(soundType, value, callback);
                    }
                    Log.w(TAG, "Нет параметров для установки громкости");
                    return false;

                case "get_info":
                case "информация":
                case "статус":
                case "уровень":
                    Log.i(TAG, "Получаю информацию о громкости " + getSoundTypeName(soundType));
                    return getVolumeInfo(soundType, callback);

                default:
                    Log.w(TAG, "Неизвестное действие для громкости: " + action);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды громкости", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при управлении громкостью: " + e.getMessage());
            }
            return false;
        }
    }

    //Увеличить громкость всех 4 типов
    private boolean increaseAllVolumes(int value, SimpleCallback callback) {
        try {
            boolean success = true;
            StringBuilder resultBuilder = new StringBuilder("Увеличение громкости всех типов на " + value + ":\n");

            // Медиа
            try {
                String result = SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.INCREASE_MEDIA,
                        String.valueOf(value));
                resultBuilder.append("• Медиа: ").append(result).append("\n");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка увеличения медиа", e);
                resultBuilder.append("• Медиа: ошибка\n");
                success = false;
            }

            // Уведомления (безопасно)
            try {
                String result = SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.INCREASE_NOTIFICATION,
                        String.valueOf(value));
                resultBuilder.append("• Уведомления: ").append(result).append("\n");
            } catch (Exception e) {
                Log.w(TAG, "Не удалось увеличить уведомления, пробуем медиа: " + e.getMessage());
                try {
                    String result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_MEDIA,
                            String.valueOf(value));
                    resultBuilder.append("• Уведомления (через медиа): ").append(result).append("\n");
                } catch (Exception e2) {
                    Log.e(TAG, "Ошибка увеличения медиа (fallback)", e2);
                    resultBuilder.append("• Уведомления: ошибка\n");
                    success = false;
                }
            }

            // Звонок
            try {
                String result = SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.INCREASE_RING,
                        String.valueOf(value));
                resultBuilder.append("• Звонок: ").append(result).append("\n");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка увеличения звонка", e);
                resultBuilder.append("• Звонок: ошибка\n");
                success = false;
            }

            // Будильник
            try {
                String result = SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.INCREASE_ALARM,
                        String.valueOf(value));
                resultBuilder.append("• Будильник: ").append(result);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка увеличения будильника", e);
                resultBuilder.append("• Будильник: ошибка");
                success = false;
            }

            if (callback != null) {
                callback.onCommandResult(resultBuilder.toString());
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка увеличения всех громкостей", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при увеличении громкости: " + e.getMessage());
            }
            return false;
        }
    }

    //Уменьшить громкость всех 4 типов
    private boolean decreaseAllVolumes(int value, SimpleCallback callback) {
        try {
            boolean success = true;
            StringBuilder resultBuilder = new StringBuilder("🔉 Уменьшение громкости всех типов на " + value + ":\n");

            // Медиа
            try {
                String result = SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.DECREASE_MEDIA,
                        String.valueOf(value));
                resultBuilder.append("• Медиа: ").append(result).append("\n");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка уменьшения медиа", e);
                resultBuilder.append("• Медиа: ошибка\n");
                success = false;
            }

            // Уведомления (безопасно)
            try {
                String result = SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.DECREASE_NOTIFICATION,
                        String.valueOf(value));
                resultBuilder.append("• Уведомления: ").append(result).append("\n");
            } catch (Exception e) {
                Log.w(TAG, "Не удалось уменьшить уведомления, пробуем медиа: " + e.getMessage());
                try {
                    String result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_MEDIA,
                            String.valueOf(value));
                    resultBuilder.append("• Уведомления (через медиа): ").append(result).append("\n");
                } catch (Exception e2) {
                    Log.e(TAG, "Ошибка уменьшения медиа (fallback)", e2);
                    resultBuilder.append("• Уведомления: ошибка\n");
                    success = false;
                }
            }

            // Звонок
            try {
                String result = SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.DECREASE_RING,
                        String.valueOf(value));
                resultBuilder.append("• Звонок: ").append(result).append("\n");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка уменьшения звонка", e);
                resultBuilder.append("• Звонок: ошибка\n");
                success = false;
            }

            // Будильник
            try {
                String result = SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.DECREASE_ALARM,
                        String.valueOf(value));
                resultBuilder.append("• Будильник: ").append(result);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка уменьшения будильника", e);
                resultBuilder.append("• Будильник: ошибка");
                success = false;
            }

            if (callback != null) {
                callback.onCommandResult(resultBuilder.toString());
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка уменьшения всех громкостей", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при уменьшении громкости: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Выключить звук всех 4 типов
     */
    private boolean muteAllVolumes(SimpleCallback callback) {
        try {
            boolean success = true;
            StringBuilder resultBuilder = new StringBuilder("Выключение звука всех типов:\n");

            // Медиа
            try {
                String result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_MEDIA);
                resultBuilder.append("• Медиа: ").append(result).append("\n");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка выключения медиа", e);
                resultBuilder.append("• Медиа: ошибка\n");
                success = false;
            }

            // Уведомления
            try {
                String result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_NOTIFICATION);
                resultBuilder.append("• Уведомления: ").append(result).append("\n");
            } catch (Exception e) {
                Log.w(TAG, "Не удалось выключить уведомления: " + e.getMessage());
                resultBuilder.append("• Уведомления: ошибка\n");
                success = false; // Но продолжаем с другими типами
            }

            // Звонок
            try {
                String result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_RING);
                resultBuilder.append("• Звонок: ").append(result).append("\n");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка выключения звонка", e);
                resultBuilder.append("• Звонок: ошибка\n");
                success = false;
            }

            // Будильник
            try {
                String result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_ALARM);
                resultBuilder.append("• Будильник: ").append(result);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка выключения будильника", e);
                resultBuilder.append("• Будильник: ошибка");
                success = false;
            }

            if (callback != null) {
                callback.onCommandResult(resultBuilder.toString());
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выключения всех звуков", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при выключении звука: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Включить звук всех 4 типов
     */
    private boolean unmuteAllVolumes(SimpleCallback callback) {
        try {
            boolean success = true;
            StringBuilder resultBuilder = new StringBuilder("Включение звука всех типов:\n");

            // Медиа
            try {
                String result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_MEDIA);
                resultBuilder.append("• Медиа: ").append(result).append("\n");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка включения медиа", e);
                resultBuilder.append("• Медиа: ошибка\n");
                success = false;
            }

            // Уведомления
            try {
                String result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_NOTIFICATION);
                resultBuilder.append("• Уведомления: ").append(result).append("\n");
            } catch (Exception e) {
                Log.w(TAG, "Не удалось включить уведомления: " + e.getMessage());
                resultBuilder.append("• Уведомления: ошибка\n");
                success = false; // Но продолжаем с другими типами
            }

            // Звонок
            try {
                String result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_RING);
                resultBuilder.append("• Звонок: ").append(result).append("\n");
            } catch (Exception e) {
                Log.e(TAG, "Ошибка включения звонка", e);
                resultBuilder.append("• Звонок: ошибка\n");
                success = false;
            }

            // Будильник
            try {
                String result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_ALARM);
                resultBuilder.append("• Будильник: ").append(result);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка включения будильника", e);
                resultBuilder.append("• Будильник: ошибка");
                success = false;
            }

            if (callback != null) {
                callback.onCommandResult(resultBuilder.toString());
            }

            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка включения всех звуков", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при включении звука: " + e.getMessage());
            }
            return false;
        }
    }

    //Увеличить громкость конкретного типа
    private boolean increaseVolume(String soundType, int value, SimpleCallback callback) {
        try {
            String result = "";
            String typeName = getSoundTypeName(soundType);

            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_MEDIA,
                            String.valueOf(value));
                    break;

                case "notification":
                case "уведомление":
                    // Безопасный метод для уведомлений
                    try {
                        result = SoundAPI.executeCommand(context,
                                SoundAPI.SoundCommand.INCREASE_NOTIFICATION,
                                String.valueOf(value));
                    } catch (SecurityException e) {
                        Log.w(TAG, "Нет разрешения для уведомлений, использую медиа");
                        result = SoundAPI.executeCommand(context,
                                SoundAPI.SoundCommand.INCREASE_MEDIA,
                                String.valueOf(value));
                    }
                    break;

                case "ring":
                case "звонок":
                    result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_RING,
                            String.valueOf(value));
                    break;

                case "alarm":
                case "будильник":
                    result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_ALARM,
                            String.valueOf(value));
                    break;

                default:
                    result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.INCREASE_MEDIA,
                            String.valueOf(value));
                    break;
            }

            if (callback != null) {
                String message = "Громкость " + typeName + " увеличена на " + value + ": " + result;
                callback.onCommandResult(message);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка увеличения громкости " + soundType, e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при увеличении громкости " + getSoundTypeName(soundType) + ": " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Уменьшить громкость конкретного типа
     */
    private boolean decreaseVolume(String soundType, int value, SimpleCallback callback) {
        try {
            String result = "";
            String typeName = getSoundTypeName(soundType);

            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_MEDIA,
                            String.valueOf(value));
                    break;

                case "notification":
                case "уведомление":
                    // Безопасный метод для уведомлений
                    try {
                        result = SoundAPI.executeCommand(context,
                                SoundAPI.SoundCommand.DECREASE_NOTIFICATION,
                                String.valueOf(value));
                    } catch (SecurityException e) {
                        Log.w(TAG, "Нет разрешения для уведомлений, использую медиа");
                        result = SoundAPI.executeCommand(context,
                                SoundAPI.SoundCommand.DECREASE_MEDIA,
                                String.valueOf(value));
                    }
                    break;

                case "ring":
                case "звонок":
                    result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_RING,
                            String.valueOf(value));
                    break;

                case "alarm":
                case "будильник":
                    result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_ALARM,
                            String.valueOf(value));
                    break;

                default:
                    result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.DECREASE_MEDIA,
                            String.valueOf(value));
                    break;
            }

            if (callback != null) {
                String message = "🔉 Громкость " + typeName + " уменьшена на " + value + ": " + result;
                callback.onCommandResult(message);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка уменьшения громкости " + soundType, e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при уменьшении громкости " + getSoundTypeName(soundType) + ": " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Установить максимальную громкость (с безопасной обработкой уведомлений)
     */
    private boolean setMaxVolume(String soundType, SimpleCallback callback) {
        try {
            String result = "";
            String typeName = getSoundTypeName(soundType);

            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MAX_MEDIA);
                    break;

                case "notification":
                case "уведомление":
                    // Безопасная установка уведомлений (85% вместо 100%)
                    return setNotificationVolumeSafe(85, callback);

                case "ring":
                case "звонок":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MAX_RING);
                    break;

                case "alarm":
                case "будильник":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MAX_ALARM);
                    break;

                default:
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MAX_MEDIA);
                    break;
            }

            if (callback != null) {
                String message = "Максимальная громкость " + typeName + " установлена: " + result;
                callback.onCommandResult(message);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка установки максимальной громкости " + soundType, e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при установке максимальной громкости " + getSoundTypeName(soundType) + ": " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Безопасная установка громкости уведомлений (избегаем Do Not Disturb ошибки)
     */
    private boolean setNotificationVolumeSafe(int volume, SimpleCallback callback) {
        try {
            String result = "";

            // Пробуем стандартный метод
            try {
                result = SoundAPI.executeCommand(context,
                        SoundAPI.SoundCommand.SET_NOTIFICATION,
                        String.valueOf(volume));
            } catch (SecurityException e) {
                Log.w(TAG, "Не удалось установить громкость уведомлений, используем медиа: " + e.getMessage());
                try {
                    // Fallback на медиа
                    result = SoundAPI.executeCommand(context,
                            SoundAPI.SoundCommand.SET_MEDIA,
                            String.valueOf(volume));
                } catch (Exception e2) {
                    Log.e(TAG, "Ошибка установки громкости медиа (fallback)", e2);
                    throw e2;
                }
            }

            if (callback != null) {
                String message = "Громкость уведомлений установлена на " + volume + "%: " + result;
                callback.onCommandResult(message);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка безопасной установки уведомлений", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при установке громкости уведомлений: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Установить конкретную громкость
     */
    private boolean setVolume(String soundType, int value, SimpleCallback callback) {
        try {
            String result = "";
            String typeName = getSoundTypeName(soundType);

            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.SET_MEDIA, String.valueOf(value));
                    break;

                case "notification":
                case "уведомление":
                    return setNotificationVolumeSafe(value, callback);

                case "ring":
                case "звонок":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.SET_RING, String.valueOf(value));
                    break;

                case "alarm":
                case "будильник":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.SET_ALARM, String.valueOf(value));
                    break;

                default:
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.SET_MEDIA, String.valueOf(value));
                    break;
            }

            if (callback != null) {
                String message = "Громкость " + typeName + " установлена на " + value + "%: " + result;
                callback.onCommandResult(message);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка установки громкости " + soundType, e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при установке громкости " + getSoundTypeName(soundType) + ": " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Получить информацию о громкости
     */
    private boolean getVolumeInfo(String soundType, SimpleCallback callback) {
        try {
            String info = "";
            String typeName = getSoundTypeName(soundType);

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

            Log.i(TAG, "Информация о громкости " + typeName + ": " + info);

            if (callback != null) {
                String message = "Информация о громкости " + typeName + ": " + info;
                callback.onCommandResult(message);
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка получения информации о громкости", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка при получении информации о громкости: " + e.getMessage());
            }
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
     * Fallback выполнение команды (если AI не сработал)
     */
    private boolean executeFallbackCommand(String text, SimpleCallback callback) {
        String lower = text.toLowerCase().trim();
        Log.d(TAG, "Fallback для: \"" + text + "\"");

        try {
            // ШАГ 1: Проверка команд CleverHome (самый высокий приоритет)
            if (lower.contains("клевер") || lower.contains("умный дом") || lower.contains("clever") ||
                    lower.contains("умныйдом") || lower.contains("клевер хоум")) {
                Log.d(TAG, "Fallback: Обнаружена команда CleverHome");

                if (lower.contains("перезапуск") || lower.contains("рестарт") || lower.contains("рестартнуть")) {
                    return restartCleverHomeApp(callback);
                } else if (lower.contains("останови") || lower.contains("закрой") || lower.contains("выключи")) {
                    return stopCleverHomeApp(callback);
                } else if (lower.contains("статус") || lower.contains("состояние")) {
                    return getCleverHomeStatus(callback);
                } else {
                    // По умолчанию - запустить CleverHome
                    return launchCleverHomeApp(callback);
                }
            }

            // ШАГ 2: Проверка команд перезагрузки устройства
            if ((lower.contains("перезагрузи") || lower.contains("рестарт") || lower.contains("перезапуск")) &&
                    (lower.contains("устройство") || lower.contains("телефон") || lower.contains("система") ||
                            lower.contains("гаджет") || lower.contains("девайс"))) {
                Log.d(TAG, "Fallback: Обнаружена команда перезагрузки устройства");
                return rebootDevice(callback);
            }

            // ШАГ 3: Проверка команд выключения устройства
            if ((lower.contains("выключи") || lower.contains("отключи")) &&
                    (lower.contains("устройство") || lower.contains("телефон") || lower.contains("система"))) {
                Log.d(TAG, "Fallback: Обнаружена команда выключения устройства");
                return shutdownDevice(callback);
            }

            // ШАГ 4: Простые команды яркости
            if (lower.contains("ярк") || lower.contains("свет") || lower.contains("подсвет")) {
                Log.d(TAG, "Fallback: Обработка команды яркости");
                return handleBrightnessFallback(lower, callback);
            }

            // ШАГ 5: Простые команды звука
            if (lower.contains("громк") || lower.contains("звук") || lower.contains("медиа") ||
                    lower.contains("уведомл") || lower.contains("звонок") || lower.contains("будильник")) {
                Log.d(TAG, "Fallback: Обработка команды звука");
                return handleVolumeFallback(lower, callback);
            }

            // ШАГ 6: Простые команды Wi-Fi
            if (lower.contains("wifi") || lower.contains("вайфай") || lower.contains("интернет") ||
                    lower.contains("вай фай") || lower.contains("вай-фай")) {
                Log.d(TAG, "Fallback: Обработка команды Wi-Fi");
                return handleWiFiFallback(lower, callback);
            }

            // ШАГ 7: Простые команды запуска приложений
            if (lower.contains("открой") || lower.contains("запусти") || lower.contains("открыть")) {
                Log.d(TAG, "Fallback: Обработка команды запуска приложения");
                return handleAppLaunchFallback(lower, callback);
            }

            Log.w(TAG, "Fallback не смог обработать команду: " + text);
            if (callback != null) {
                callback.onCommandResult("Команда не распознана: \"" + text + "\"");
            }
            return false;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в fallback выполнении", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка обработки команды: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Обработка команд яркости в fallback
     */
    private boolean handleBrightnessFallback(String lower, SimpleCallback callback) {
        try {
            if (lower.contains("увелич") || lower.contains("больше") || lower.contains("прибав")) {
                int value = extractNumber(lower, 30);
                String result = BrightnessAPI.executeCommand(context,
                        BrightnessAPI.BrightnessCommand.INCREASE,
                        String.valueOf(value));
                Log.d(TAG, "Яркость увеличена: " + result);
                if (callback != null) {
                    callback.onCommandResult("Яркость увеличена на " + value + "%: " + result);
                }
                return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
            } else if (lower.contains("уменьш") || lower.contains("меньше") || lower.contains("убав")) {
                int value = extractNumber(lower, 30);
                String result = BrightnessAPI.executeCommand(context,
                        BrightnessAPI.BrightnessCommand.DECREASE,
                        String.valueOf(value));
                Log.d(TAG, "Яркость уменьшена: " + result);
                if (callback != null) {
                    callback.onCommandResult("Яркость уменьшена на " + value + "%: " + result);
                }
                return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
            } else if (lower.contains("макс") || lower.contains("максимум") || lower.contains("полную")) {
                String result = BrightnessAPI.executeCommand(context,
                        BrightnessAPI.BrightnessCommand.MAX);
                Log.d(TAG, "Яркость на максимум: " + result);
                if (callback != null) {
                    callback.onCommandResult("Яркость установлена на максимум: " + result);
                }
                return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
            } else if (lower.contains("мин") || lower.contains("минимум") || lower.contains("выключи")) {
                String result = BrightnessAPI.executeCommand(context,
                        BrightnessAPI.BrightnessCommand.MIN);
                Log.d(TAG, "Яркость на минимум: " + result);
                if (callback != null) {
                    callback.onCommandResult("Яркость установлена на минимум: " + result);
                }
                return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
            } else if (lower.contains("средн") || lower.contains("половин")) {
                String result = BrightnessAPI.executeCommand(context,
                        BrightnessAPI.BrightnessCommand.MEDIUM);
                Log.d(TAG, "Яркость средняя: " + result);
                if (callback != null) {
                    callback.onCommandResult("Яркость установлена на средний уровень: " + result);
                }
                return !result.contains("Ошибка") && !result.contains("Нужно разрешение");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обработки яркости", e);
        }
        return false;
    }

    /**
     * Обработка команд звука в fallback
     */
    private boolean handleVolumeFallback(String lower, SimpleCallback callback) {
        try {
            // Определяем тип звука
            String soundType = "media";
            if (lower.contains("уведомл") || lower.contains("оповещ")) {
                soundType = "notification";
            } else if (lower.contains("звонок") || lower.contains("вызов")) {
                soundType = "ring";
            } else if (lower.contains("будильник") || lower.contains("alarm")) {
                soundType = "alarm";
            }

            if (lower.contains("увелич") || lower.contains("больше") || lower.contains("прибав")) {
                int value = extractNumber(lower, 1);
                Log.d(TAG, "Увеличиваю " + soundType + " на " + value);
                return increaseVolume(soundType, value, callback);
            } else if (lower.contains("уменьш") || lower.contains("меньше") || lower.contains("убав")) {
                int value = extractNumber(lower, 1);
                Log.d(TAG, "Уменьшаю " + soundType + " на " + value);
                return decreaseVolume(soundType, value, callback);
            } else if (lower.contains("выключи") || lower.contains("отключи") ||
                    lower.contains("mute") || lower.contains("ноль") || lower.contains("тихо")) {
                Log.d(TAG, "Выключаю " + soundType);
                return muteVolume(soundType, callback);
            } else if (lower.contains("включи") && (lower.contains("звук") || lower.contains(soundType))) {
                Log.d(TAG, "Включаю " + soundType);
                return unmuteVolume(soundType, callback);
            } else if (lower.contains("макс") || lower.contains("максимум") || lower.contains("полную")) {
                Log.d(TAG, "Максимум " + soundType);
                return setMaxVolume(soundType, callback);
            } else if (lower.contains("установи") || lower.contains("поставь") || lower.contains("сделай")) {
                int value = extractNumber(lower, 50);
                Log.d(TAG, "Устанавливаю " + soundType + " на " + value);
                return setVolume(soundType, value, callback);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обработки звука", e);
        }
        return false;
    }

    /**
     * Обработка команд Wi-Fi в fallback
     */
    private boolean handleWiFiFallback(String lower, SimpleCallback callback) {
        try {
            if (lower.contains("включи") || lower.contains("подключи")) {
                String result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.ENABLE);
                Log.d(TAG, "Wi-Fi включен: " + result);
                if (callback != null) {
                    callback.onCommandResult("Wi-Fi включен: " + result);
                }
                return !result.contains("Ошибка");
            } else if (lower.contains("выключи") || lower.contains("отключи")) {
                String result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.DISABLE);
                Log.d(TAG, "Wi-Fi выключен: " + result);
                if (callback != null) {
                    callback.onCommandResult("Wi-Fi выключен: " + result);
                }
                return !result.contains("Ошибка");
            } else if (lower.contains("статус") || lower.contains("состояние") ||
                    lower.contains("включен") || lower.contains("выключен")) {
                String result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_STATUS);
                Log.d(TAG, "Статус Wi-Fi: " + result);
                if (callback != null) {
                    callback.onCommandResult("Статус Wi-Fi: " + result);
                }
                return !result.contains("Ошибка");
            } else if (lower.contains("сканируй") || lower.contains("найди") || lower.contains("поиск")) {
                String result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.SCAN);
                Log.d(TAG, "Сканирование Wi-Fi: " + result);
                if (callback != null) {
                    callback.onCommandResult("Сканирование Wi-Fi: " + result);
                }
                return !result.contains("Ошибка");
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обработки Wi-Fi", e);
        }
        return false;
    }

    /**
     * Обработка команд запуска приложений в fallback
     */
    private boolean handleAppLaunchFallback(String lower, SimpleCallback callback) {
        try {
            AppLauncher launcher = new AppLauncher(context);

            if (lower.contains("настройк")) {
                boolean success = launcher.launchAppByName("settings");
                Log.d(TAG, "Открываю настройки: " + success);
                if (callback != null) {
                    callback.onCommandResult(success ? "Настройки открыты" : "Не удалось открыть настройки");
                }
                return success;
            } else if (lower.contains("камер")) {
                boolean success = launcher.launchAppByName("camera");
                Log.d(TAG, "Открываю камеру: " + success);
                if (callback != null) {
                    callback.onCommandResult(success ? "Камера открыта" : "Не удалось открыть камеру");
                }
                return success;
            } else if (lower.contains("телефон") || lower.contains("звонк")) {
                boolean success = launcher.launchAppByName("phone");
                Log.d(TAG, "Открываю телефон: " + success);
                if (callback != null) {
                    callback.onCommandResult(success ? "Телефон открыт" : "Не удалось открыть телефон");
                }
                return success;
            } else if (lower.contains("галере") || lower.contains("фото")) {
                boolean success = launcher.launchAppByName("gallery");
                Log.d(TAG, "Открываю галерею: " + success);
                if (callback != null) {
                    callback.onCommandResult(success ? "Галерея открыта" : "Не удалось открыть галерею");
                }
                return success;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка запуска приложения", e);
        }
        return false;
    }

    /**
     * Простой парсер на правилах (fallback)
     */
    private TinyLLMProcessor.CommandResult parseWithSimpleRules(String text) {
        Log.d(TAG, "Простые правила для: \"" + text + "\"");

        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Пустой текст для парсинга");
            return new TinyLLMProcessor.CommandResult("unknown", "", new JSONObject());
        }

        String lower = text.toLowerCase().trim();
        JSONObject params = new JSONObject();

        try {
            // ШАГ 1: Специальная обработка команд CleverHome с ошибками распознавания
            boolean isCleverHomeCommand = false;
            String action = "";

            // Проверяем все возможные варианты написания "умный дом"
            if (lower.contains("клевер") ||
                    lower.contains("clever") ||
                    lower.contains("умныйдом") ||
                    lower.contains("клевер хоум") ||
                    lower.contains("cleverhome")) {
                isCleverHomeCommand = true;
                Log.d(TAG, "Обнаружено прямое упоминание CleverHome");
            }

            // Проверяем варианты с ошибками распознавания "умный дом"
            if (lower.contains("у дом") ||
                    lower.contains("умн дом") ||
                    lower.contains("умны дом")) {
                isCleverHomeCommand = true;
                Log.d(TAG, "Обнаружен вариант 'умный дом' с ошибками распознавания");
            }

            // Проверяем комбинации слов (частичное распознавание)
            if ((lower.contains("у") && lower.contains("дом")) ||
                    (lower.contains("умн") && lower.contains("дом")) ||
                    (lower.contains("умны") && lower.contains("дом"))) {
                isCleverHomeCommand = true;
                Log.d(TAG, "Обнаружена комбинация слов, похожая на CleverHome");
            }

            // Проверяем команды запуска/открытия с упоминанием дома
            if ((lower.contains("запусти") || lower.contains("открой") ||
                    lower.contains("включи") || lower.contains("открыть")) &&
                    (lower.contains("дом") || lower.contains("дома"))) {
                isCleverHomeCommand = true;
                Log.d(TAG, "Обнаружена команда запуска/открытия с упоминанием дома");
            }

            if (isCleverHomeCommand) {
                Log.d(TAG, "Определена команда CleverHome");

                // Определяем действие
                if (lower.contains("перезапуск") || lower.contains("рестарт") ||
                        lower.contains("рестартнуть") || lower.contains("перезагрузи")) {
                    action = "restart";
                    Log.d(TAG, "Действие: restart");
                } else if (lower.contains("останови") || lower.contains("закрой") ||
                        lower.contains("выключи") || lower.contains("заверши")) {
                    action = "stop";
                    Log.d(TAG, "Действие: stop");
                } else if (lower.contains("статус") || lower.contains("состояние") ||
                        lower.contains("работает") || lower.contains("запущен")) {
                    action = "status";
                    Log.d(TAG, "Действие: status");
                } else if (lower.contains("запусти") || lower.contains("открой") ||
                        lower.contains("включи") || lower.contains("открыть") ||
                        lower.contains("старт") || lower.contains("запуск")) {
                    action = "launch";
                    Log.d(TAG, "Действие: launch (явная команда запуска)");
                } else {
                    action = "launch";
                    Log.d(TAG, "Действие: launch (по умолчанию)");
                }

                return new TinyLLMProcessor.CommandResult("cleverhome", action, params);
            }

            // ШАГ 2: Команды устройства (перезагрузка/выключение)
            if ((lower.contains("перезагрузи") || lower.contains("рестарт") ||
                    lower.contains("перезапуск")) &&
                    (lower.contains("устройство") || lower.contains("телефон") ||
                            lower.contains("система") || lower.contains("гаджет") ||
                            lower.contains("девайс"))) {
                Log.d(TAG, "Определена команда перезагрузки устройства");
                return new TinyLLMProcessor.CommandResult("device", "reboot", params);
            }

            if ((lower.contains("выключи") || lower.contains("отключи")) &&
                    (lower.contains("устройство") || lower.contains("телефон") ||
                            lower.contains("система"))) {
                Log.d(TAG, "Определена команда выключения устройства");
                return new TinyLLMProcessor.CommandResult("device", "shutdown", params);
            }

            // ШАГ 3: Команды яркости
            if (lower.contains("ярк") || lower.contains("свет") || lower.contains("подсвет")) {
                Log.d(TAG, "Обработка команды яркости");
                return parseBrightnessCommand(lower, params);
            }

            // ШАГ 4: Команды звука
            if (lower.contains("громк") || lower.contains("звук") || lower.contains("медиа") ||
                    lower.contains("уведомл") || lower.contains("звонок") || lower.contains("будильник")) {
                Log.d(TAG, "Обработка команды звука");
                return parseVolumeCommand(lower, params);
            }

            // ШАГ 5: Команды Wi-Fi
            if (lower.contains("wifi") || lower.contains("вайфай") || lower.contains("интернет") ||
                    lower.contains("вай фай") || lower.contains("вай-фай") || lower.contains("wi-fi")) {
                Log.d(TAG, "Обработка команды Wi-Fi");
                return parseWiFiCommand(lower, params);
            }

            // ШАГ 6: Команды запуска других приложений (НЕ CleverHome)
            if ((lower.contains("открой") || lower.contains("запусти") || lower.contains("открыть")) &&
                    !lower.contains("клевер") && !lower.contains("умный") && !lower.contains("дом")) {
                Log.d(TAG, "Обработка команды запуска приложения");
                return parseAppLaunchCommand(lower, params);
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в простом парсере", e);
        }

        Log.w(TAG, "Не удалось распознать команду: \"" + text + "\"");
        return new TinyLLMProcessor.CommandResult("unknown", "", params);
    }

    /**
     * Парсинг команд яркости
     */
    private TinyLLMProcessor.CommandResult parseBrightnessCommand(String cleanText, JSONObject params) {
        Log.d(TAG, "Парсинг команды яркости");

        try {
            if (cleanText.contains("увелич") || cleanText.contains("больше") || cleanText.contains("прибав")) {
                int value = extractNumber(cleanText, 30);
                params.put("value", value);
                Log.d(TAG, "Определено: brightness + increase, value=" + value);
                return new TinyLLMProcessor.CommandResult("brightness", "increase", params);
            } else if (cleanText.contains("уменьш") || cleanText.contains("меньше") || cleanText.contains("убав")) {
                int value = extractNumber(cleanText, 30);
                params.put("value", value);
                Log.d(TAG, "Определено: brightness + decrease, value=" + value);
                return new TinyLLMProcessor.CommandResult("brightness", "decrease", params);
            } else if (cleanText.contains("макс") || cleanText.contains("максимум") || cleanText.contains("полную")) {
                Log.d(TAG, "Определено: brightness + max");
                return new TinyLLMProcessor.CommandResult("brightness", "max", params);
            } else if (cleanText.contains("мин") || cleanText.contains("минимум") || cleanText.contains("выключи")) {
                Log.d(TAG, "Определено: brightness + min");
                return new TinyLLMProcessor.CommandResult("brightness", "min", params);
            } else if (cleanText.contains("средн") || cleanText.contains("половин")) {
                Log.d(TAG, "Определено: brightness + medium");
                return new TinyLLMProcessor.CommandResult("brightness", "medium", params);
            } else if (cleanText.contains("установи") || cleanText.contains("поставь")) {
                int value = extractNumber(cleanText, 50);
                params.put("value", value);
                Log.d(TAG, "Определено: brightness + set, value=" + value);
                return new TinyLLMProcessor.CommandResult("brightness", "set", params);
            } else {
                Log.d(TAG, "Определено: brightness + get_info");
                return new TinyLLMProcessor.CommandResult("brightness", "get_info", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга яркости", e);
            return new TinyLLMProcessor.CommandResult("brightness", "get_info", params);
        }
    }

    /**
     * Парсинг команд звука
     */
    private TinyLLMProcessor.CommandResult parseVolumeCommand(String cleanText, JSONObject params) {
        Log.d(TAG, "Парсинг команды звука");

        try {
            // Определяем тип звука
            String soundType = "media";
            if (cleanText.contains("уведомл") || cleanText.contains("оповещ")) {
                soundType = "notification";
            } else if (cleanText.contains("звонок") || cleanText.contains("вызов")) {
                soundType = "ring";
            } else if (cleanText.contains("будильник") || cleanText.contains("alarm")) {
                soundType = "alarm";
            }

            params.put("type", soundType);

            if (cleanText.contains("увелич") || cleanText.contains("больше") || cleanText.contains("прибав")) {
                int value = extractNumber(cleanText, 1);
                if (value == 0) value = 1;
                params.put("value", value);
                Log.d(TAG, "Определено: volume + increase, type=" + soundType);
                return new TinyLLMProcessor.CommandResult("volume", "increase", params);
            } else if (cleanText.contains("уменьш") || cleanText.contains("меньше") || cleanText.contains("убав")) {
                int value = extractNumber(cleanText, 1);
                if (value == 0) value = 1;
                params.put("value", value);
                Log.d(TAG, "Определено: volume + decrease, type=" + soundType);
                return new TinyLLMProcessor.CommandResult("volume", "decrease", params);
            } else if (cleanText.contains("выключи") || cleanText.contains("отключи") || cleanText.contains("заглуши")) {
                Log.d(TAG, "Определено: volume + mute, type=" + soundType);
                return new TinyLLMProcessor.CommandResult("volume", "mute", params);
            } else if (cleanText.contains("включи звук") || cleanText.contains("unmute")) {
                Log.d(TAG, "Определено: volume + unmute, type=" + soundType);
                return new TinyLLMProcessor.CommandResult("volume", "unmute", params);
            } else if (cleanText.contains("макс") || cleanText.contains("максимум") || cleanText.contains("полную")) {
                Log.d(TAG, "Определено: volume + max, type=" + soundType);
                return new TinyLLMProcessor.CommandResult("volume", "max", params);
            } else if (cleanText.contains("установи") || cleanText.contains("поставь")) {
                int value = extractNumber(cleanText, 50);
                params.put("value", value);
                Log.d(TAG, "Определено: volume + set, type=" + soundType);
                return new TinyLLMProcessor.CommandResult("volume", "set", params);
            } else {
                Log.d(TAG, "Определено: volume + get_info, type=" + soundType);
                return new TinyLLMProcessor.CommandResult("volume", "get_info", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга звука", e);
            return new TinyLLMProcessor.CommandResult("volume", "get_info", params);
        }
    }

    /**
     * Парсинг команд Wi-Fi
     */
    private TinyLLMProcessor.CommandResult parseWiFiCommand(String cleanText, JSONObject params) {
        Log.d(TAG, "Парсинг команды Wi-Fi");

        try {
            if (cleanText.contains("включи") || cleanText.contains("подключи")) {
                Log.d(TAG, "Определено: wifi + enable");
                return new TinyLLMProcessor.CommandResult("wifi", "enable", params);
            } else if (cleanText.contains("выключи") || cleanText.contains("отключи")) {
                Log.d(TAG, "Определено: wifi + disable");
                return new TinyLLMProcessor.CommandResult("wifi", "disable", params);
            } else if (cleanText.contains("статус") || cleanText.contains("состояние")) {
                Log.d(TAG, "Определено: wifi + status");
                return new TinyLLMProcessor.CommandResult("wifi", "status", params);
            } else if (cleanText.contains("сканируй") || cleanText.contains("найди") || cleanText.contains("поиск")) {
                Log.d(TAG, "Определено: wifi + scan");
                return new TinyLLMProcessor.CommandResult("wifi", "scan", params);
            } else {
                // По умолчанию - статус
                Log.d(TAG, "Определено: wifi + status (по умолчанию)");
                return new TinyLLMProcessor.CommandResult("wifi", "status", params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга Wi-Fi", e);
            return new TinyLLMProcessor.CommandResult("wifi", "status", params);
        }
    }

    /**
     * Парсинг команд запуска приложений
     */
    private TinyLLMProcessor.CommandResult parseAppLaunchCommand(String cleanText, JSONObject params) {
        Log.d(TAG, "Парсинг команды запуска приложения");

        try {
            if (cleanText.contains("настройк")) {
                params.put("app", "settings");
                Log.d(TAG, "Определено: launch + open, app=settings");
            } else if (cleanText.contains("камер")) {
                params.put("app", "camera");
                Log.d(TAG, "Определено: launch + open, app=camera");
            } else if (cleanText.contains("телефон") || cleanText.contains("звонк")) {
                params.put("app", "phone");
                Log.d(TAG, "Определено: launch + open, app=phone");
            } else if (cleanText.contains("галере") || cleanText.contains("фото")) {
                params.put("app", "gallery");
                Log.d(TAG, "Определено: launch + open, app=gallery");
            } else if (cleanText.contains("браузер") || cleanText.contains("интернет")) {
                params.put("app", "chrome");
                Log.d(TAG, "Определено: launch + open, app=chrome");
            } else if (cleanText.contains("сообщен") || cleanText.contains("смс")) {
                params.put("app", "messages");
                Log.d(TAG, "Определено: launch + open, app=messages");
            } else {
                // По умолчанию - настройки
                params.put("app", "settings");
                Log.d(TAG, "Определено: launch + open, app=settings (по умолчанию)");
            }

            return new TinyLLMProcessor.CommandResult("launch", "open", params);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка парсинга запуска приложения", e);
            return new TinyLLMProcessor.CommandResult("launch", "open", params);
        }
    }

    /**
     * Команды управления Wi-Fi
     */
    private boolean executeWifiCommand(TinyLLMProcessor.CommandResult result, SimpleCallback callback) {
        String action = result.action;
        JSONObject params = result.params;

        Log.i(TAG, "Управление Wi-Fi: " + action + ", params: " + (params != null ? params.toString() : "null"));

        try {
            switch (action.toLowerCase()) {
                case "enable":
                case "включить":
                case "подключить":
                    String enableResult = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.ENABLE);
                    Log.i(TAG, "Wi-Fi включен: " + enableResult);
                    // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                    if (callback != null) {
                        callback.onCommandResult("Wi-Fi включен: " + enableResult);
                    }
                    return true;

                case "disable":
                case "выключить":
                case "отключить":
                    String disableResult = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.DISABLE);
                    Log.i(TAG, "Wi-Fi выключен: " + disableResult);
                    // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                    if (callback != null) {
                        callback.onCommandResult("Wi-Fi выключен: " + disableResult);
                    }
                    return true;

                case "status":
                case "статус":
                case "состояние":
                    String status = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_STATUS);
                    Log.i(TAG, "Статус Wi-Fi: " + status);
                    // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                    if (callback != null) {
                        callback.onCommandResult("Статус Wi-Fi: " + status);
                    }
                    return true;

                case "status_only":
                case "статус_только":
                case "простой_статус":
                    String simpleStatus = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_STATUS_ONLY);
                    Log.i(TAG, "Простой статус Wi-Fi: " + simpleStatus);
                    // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                    if (callback != null) {
                        callback.onCommandResult("Простой статус Wi-Fi: " + simpleStatus);
                    }
                    return true;

                case "scan":
                case "сканировать":
                    String scanResult = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.SCAN);
                    Log.i(TAG, "Wi-Fi сканирование: " + scanResult);
                    // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                    if (callback != null) {
                        callback.onCommandResult("Wi-Fi сканирование: " + scanResult);
                    }
                    return true;

                case "connect":
                case "подключиться":
                case "подключить_к":
                    if (params != null) {
                        String ssid = params.optString("ssid", "");
                        String password = params.optString("password", "");
                        if (!ssid.isEmpty()) {
                            String connectResult = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CONNECT, ssid, password);
                            Log.i(TAG, "Подключение к Wi-Fi: " + connectResult);
                            // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                            if (callback != null) {
                                callback.onCommandResult("Подключение к Wi-Fi: " + connectResult);
                            }
                            return true;
                        }
                    }
                    Log.w(TAG, "Не указан SSID для подключения");
                    return false;

                case "disconnect":
                case "отключиться":
                case "разъединить":
                    String disconnectResult = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.DISCONNECT);
                    Log.i(TAG, "Отключение от Wi-Fi: " + disconnectResult);
                    // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                    if (callback != null) {
                        callback.onCommandResult("Отключение от Wi-Fi: " + disconnectResult);
                    }
                    return true;

                case "check_permission":
                case "проверить_разрешение":
                case "проверить_разрешения":
                    String permissionStatus = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CHECK_LOCATION_PERMISSION);
                    Log.i(TAG, "Статус разрешений: " + permissionStatus);
                    // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                    if (callback != null) {
                        callback.onCommandResult("Статус разрешений: " + permissionStatus);
                    }
                    return true;

                case "check_gps":
                case "проверить_gps":
                case "проверить_геолокацию":
                    String gpsStatus = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CHECK_GPS_ENABLED);
                    Log.i(TAG, "Статус GPS: " + gpsStatus);
                    // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                    if (callback != null) {
                        callback.onCommandResult("Статус GPS: " + gpsStatus);
                    }
                    return true;

                case "check_wifi":
                case "проверить_wifi":
                case "проверить_вайфай":
                    String wifiStatus = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CHECK_WIFI_ENABLED);
                    Log.i(TAG, "Статус Wi-Fi (проверка): " + wifiStatus);
                    // ПЕРЕДАЕМ РЕЗУЛЬТАТ ОБРАТНО
                    if (callback != null) {
                        callback.onCommandResult("Статус Wi-Fi (проверка): " + wifiStatus);
                    }
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
    private boolean executeAppLaunchCommand(TinyLLMProcessor.CommandResult result, SimpleCallback callback) {
        if (result.params == null) {
            Log.w(TAG, "Нет параметров для запуска приложения");
            if (callback != null) {
                callback.onCommandResult("Ошибка: не указано приложение для запуска");
            }
            return false;
        }

        String appName = result.params.optString("app", "");
        if (appName.isEmpty()) {
            Log.w(TAG, "Не указано имя приложения");
            if (callback != null) {
                callback.onCommandResult("Ошибка: не указано имя приложения");
            }
            return false;
        }

        Log.i(TAG, "Запуск приложения: " + appName);

        try {
            AppLauncher launcher = new AppLauncher(context);
            boolean success = launcher.launchAppByName(appName);

            if (success) {
                String message = "Приложение '" + appName + "' успешно запущено";
                Log.i(TAG, message);
                if (callback != null) {
                    callback.onCommandResult("" + message);
                }
                return true;
            } else {
                Log.w(TAG, "Не удалось запустить приложение: " + appName);
                boolean alternativeSuccess = tryAlternativeAppNames(appName, callback);
                if (!alternativeSuccess && callback != null) {
                    callback.onCommandResult("Не удалось запустить приложение: " + appName);
                }
                return alternativeSuccess;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка запуска приложения", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка запуска приложения: " + e.getMessage());
            }
            return false;
        }
    }

    private boolean tryAlternativeAppNames(String appName, SimpleCallback callback) {
        AppLauncher launcher = new AppLauncher(context);

        try {
            String alternativeName = "";
            boolean success = false;

            switch (appName.toLowerCase()) {
                case "настройки":
                case "settings":
                    alternativeName = "настройки";
                    success = launcher.launchAppByName("settings");
                    break;
                case "камера":
                case "camera":
                    alternativeName = "камеру";
                    success = launcher.launchAppByName("camera");
                    break;
                case "галерея":
                case "gallery":
                case "фото":
                    alternativeName = "галерею";
                    success = launcher.launchAppByName("gallery");
                    break;
                case "телефон":
                case "phone":
                case "звонки":
                    alternativeName = "телефон";
                    success = launcher.launchAppByName("phone");
                    break;
                case "сообщения":
                case "messages":
                case "смс":
                    alternativeName = "сообщения";
                    success = launcher.launchAppByName("messages");
                    break;
                case "браузер":
                case "browser":
                case "интернет":
                    alternativeName = "браузер";
                    success = launcher.launchAppByName("chrome");
                    break;
                default:
                    if (callback != null) {
                        callback.onCommandResult("Приложение '" + appName + "' не найдено");
                    }
                    return false;
            }

            if (success && callback != null) {
                callback.onCommandResult("Запущено приложение: " + alternativeName);
            }
            return success;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка запуска приложения по альтернативному имени", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка запуска приложения: " + e.getMessage());
            }
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
    private boolean executeSystemCommand(TinyLLMProcessor.CommandResult result, SimpleCallback callback) {
        String action = result.action;

        Log.i(TAG, "Системная команда: " + action);

        String message = "";
        String emoji = "";

        switch (action.toLowerCase()) {
            case "reboot":
            case "перезагрузка":
                message = "Запрошена перезагрузка системы";
                Log.i(TAG, message);
                break;

            case "sleep":
            case "сон":
            case "режим сна":
                message = "Запрошен режим сна";
                Log.i(TAG, message);
                break;

            case "wake":
            case "проснуться":
            case "разбудить":
                message = " Запрошено пробуждение";
                Log.i(TAG, message);
                break;

            default:
                message = "Неизвестная системная команда: " + action;
                if (callback != null) {
                    callback.onCommandResult(message);
                }
                return false;
        }

        if (callback != null && !message.isEmpty()) {
            callback.onCommandResult(message);
        }
        return true;
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
     * Выключить звук конкретного типа
     */
    private boolean muteVolume(String soundType, SimpleCallback callback) {
        try {
            String result = "";
            String typeName = getSoundTypeName(soundType);

            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_MEDIA);
                    break;

                case "notification":
                case "уведомление":
                    try {
                        result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_NOTIFICATION);
                    } catch (SecurityException e) {
                        Log.w(TAG, "Нет разрешения для уведомлений, использую медиа");
                        result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_MEDIA);
                    }
                    break;

                case "ring":
                case "звонок":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_RING);
                    break;

                case "alarm":
                case "будильник":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_ALARM);
                    break;

                default:
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.MUTE_MEDIA);
                    break;
            }

            if (callback != null) {
                String message = "Звук " + typeName + " выключен: " + result;
                callback.onCommandResult(message);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выключения звука " + soundType, e);
            if (callback != null) {
                callback.onCommandResult("Ошибка выключения звука " + getSoundTypeName(soundType) + ": " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Включить звук конкретного типа
     */
    private boolean unmuteVolume(String soundType, SimpleCallback callback) {
        try {
            String result = "";
            String typeName = getSoundTypeName(soundType);

            switch (soundType.toLowerCase()) {
                case "media":
                case "медиа":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_MEDIA);
                    break;

                case "notification":
                case "уведомление":
                    try {
                        result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_NOTIFICATION);
                    } catch (SecurityException e) {
                        Log.w(TAG, "Нет разрешения для уведомлений, использую медиа");
                        result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_MEDIA);
                    }
                    break;

                case "ring":
                case "звонок":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_RING);
                    break;

                case "alarm":
                case "будильник":
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_ALARM);
                    break;

                default:
                    result = SoundAPI.executeCommand(context, SoundAPI.SoundCommand.UNMUTE_MEDIA);
                    break;
            }

            if (callback != null) {
                String message = "Звук " + typeName + " включен: " + result;
                callback.onCommandResult(message);
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка включения звука " + soundType, e);
            if (callback != null) {
                callback.onCommandResult("Ошибка включения звука " + getSoundTypeName(soundType) + ": " + e.getMessage());
            }
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

    private void handleWiFiCommand(String action, JSONObject params) {
        Log.i(TAG, "Выполняю WiFi команду: " + action);

        if (!WiFiAPI.isInitialized()) {
            Log.e(TAG, "WiFiAPI не инициализирован!");
            return;
        }

        try {
            String result;
            switch (action.toLowerCase()) {
                case "enable":
                case "включить":
                    result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.ENABLE);
                    break;
                case "disable":
                case "выключить":
                    result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.DISABLE);
                    break;
                case "status":
                case "статус":
                    result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_STATUS);
                    break;
                case "scan":
                case "сканировать":
                case "сканирование":
                    result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.SCAN_WITH_RESULTS);
                    break;
                case "connect":
                case "подключиться":
                    if (params.has("ssid")) {
                        String ssid = params.optString("ssid");
                        String password = params.optString("password", "");
                        result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CONNECT, ssid, password);
                    } else {
                        result = "Ошибка: не указан SSID для подключения";
                    }
                    break;
                case "reset":
                case "сброс":
                    result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.RESET_WIFI);
                    break;
                case "info":
                case "информация":
                    result = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_WIFI_INFO);
                    break;
                default:
                    result = "Неизвестное действие WiFi: " + action;
            }

            Log.i(TAG, "Wi-Fi команда выполнена: " + result);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения WiFi команды", e);
        }
    }

    /**
     * Команды управления CleverHome приложением
     */
    private boolean executeCleverHomeCommand(TinyLLMProcessor.CommandResult result, SimpleCallback callback) {
        String action = result.action;
        JSONObject params = result.params;

        Log.i(TAG, "Управление CleverHome: действие=" + action);

        try {
            switch (action.toLowerCase()) {
                case "launch":
                case "запустить":
                case "открыть":
                case "старт":
                    // Запуск приложения CleverHome
                    return launchCleverHomeApp(callback);

                case "restart":
                case "перезапуск":
                case "рестарт":
                case "перезагрузить":
                case "рестартнуть":
                    // Перезапуск приложения CleverHome
                    return restartCleverHomeApp(callback);

                case "stop":
                case "остановить":
                case "закрыть":
                case "выключить":
                    // Остановка приложения CleverHome
                    return stopCleverHomeApp(callback);

                case "status":
                case "статус":
                case "состояние":
                    // Статус приложения CleverHome
                    return getCleverHomeStatus(callback);

                default:
                    Log.w(TAG, "Неизвестное действие для CleverHome: " + action);
                    if (callback != null) {
                        callback.onCommandResult("Неизвестная команда для CleverHome: " + action);
                    }
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды CleverHome", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка управления CleverHome: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Команды управления устройством
     */
    private boolean executeDeviceCommand(TinyLLMProcessor.CommandResult result, SimpleCallback callback) {
        String action = result.action;
        JSONObject params = result.params;

        Log.i(TAG, "Управление устройством: действие=" + action);

        try {
            switch (action.toLowerCase()) {
                case "reboot":
                case "перезагрузка":
                case "рестарт":
                case "перезагрузить":
                case "перезапуск":
                    // Перезагрузка устройства
                    return rebootDevice(callback);

                case "shutdown":
                case "выключить":
                case "выключение":
                case "отключить":
                    // Выключение устройства
                    return shutdownDevice(callback);

                case "sleep":
                case "сон":
                case "режим сна":
                case "спящий режим":
                    // Режим сна
                    return sleepDevice(callback);

                default:
                    Log.w(TAG, "Неизвестное действие для устройства: " + action);
                    return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды устройства", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка управления устройством: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Запуск приложения CleverHome
     */
    private boolean launchCleverHomeApp(SimpleCallback callback) {
        try {
            // Используем shell команду для запуска приложения
            String command = "am start -n pro.cleverlife.cleverroom/.SHActivity";
            String result = ShellCommandExecutor.executeCommand(command);

            Log.i(TAG, "Запуск CleverHome: " + result);

            if (callback != null) {
                if (result.contains("Starting") || result.contains("Error") || result.isEmpty()) {
                    callback.onCommandResult("Приложение CleverHome запускается");
                    return true;
                } else {
                    callback.onCommandResult("CleverHome запущен: " + result);
                    return true;
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка запуска CleverHome", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка запуска CleverHome: " + e.getMessage());
            }
            return false;
        }
    }


    /**
     * Перезапуск приложения CleverHome (УЛУЧШЕННАЯ ВЕРСИЯ)
     */
    private boolean restartCleverHomeApp(SimpleCallback callback) {
        try {
            if (callback != null) {
                callback.onCommandResult("Начинаю перезапуск CleverHome...");
            }

            Log.i(TAG, "Начинаю перезапуск CleverHome");

            // 1. Останавливаем приложение
            String stopCommand = "am force-stop pro.cleverlife.cleverroom";
            String stopResult = ShellCommandExecutor.executeCommand(stopCommand);
            Log.i(TAG, "Остановка CleverHome: " + stopResult);

            // Небольшая пауза
            Thread.sleep(1000);

            // 2. Запускаем приложение
            String launchCommand = "am start -n pro.cleverlife.cleverroom/.SHActivity";
            String launchResult = ShellCommandExecutor.executeCommand(launchCommand);
            Log.i(TAG, "Запуск CleverHome: " + launchResult);

            if (callback != null) {
                callback.onCommandResult("CleverHome успешно перезапущен");
            }
            Log.i(TAG, "CleverHome успешно перезапущен");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка перезапуска CleverHome", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка перезапуска: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Остановка приложения CleverHome
     */
    private boolean stopCleverHomeApp(SimpleCallback callback) {
        try {
            String command = "am force-stop pro.cleverlife.cleverroom";
            String result = ShellCommandExecutor.executeCommand(command);

            Log.i(TAG, "Остановка CleverHome: " + result);

            if (callback != null) {
                callback.onCommandResult("Приложение CleverHome остановлено");
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка остановки CleverHome", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка остановки CleverHome: " + e.getMessage());
            }
            return false;
        }
    }

    private boolean getCleverHomeStatus(SimpleCallback callback) {
        try {
            // Проверяем, запущено ли приложение
            String command = "ps | grep pro.cleverlife.cleverroom";
            String result = ShellCommandExecutor.executeCommand(command);

            Log.i(TAG, "Статус CleverHome: " + result);

            if (callback != null) {
                if (result.contains("pro.cleverlife.cleverroom")) {
                    callback.onCommandResult("CleverHome запущен и работает");
                } else {
                    callback.onCommandResult("CleverHome не запущен");
                }
            }
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка проверки статуса CleverHome", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка проверки статуса: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Перезагрузка устройства
     */
    private boolean rebootDevice(SimpleCallback callback) {
        try {
            // Предупреждаем пользователя
            if (callback != null) {
                callback.onCommandResult("Устройство будет перезагружено через 5 секунд!");
            }

            // Даем пользователю время отреагировать
            new Handler().postDelayed(() -> {
                try {
                    String command = "reboot";
                    String result = ShellCommandExecutor.executeCommand(command);
                    Log.i(TAG, "Перезагрузка устройства: " + result);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перезагрузки устройства", e);
                }
            }, 5000); // 5 секунд задержки

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка подготовки к перезагрузке", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка перезагрузки устройства: " + e.getMessage());
            }
            return false;
        }
    }

    private boolean shutdownDevice(SimpleCallback callback) {
        try {
            if (callback != null) {
                callback.onCommandResult("Устройство будет выключено через 5 секунд!");
            }

            new Handler().postDelayed(() -> {
                try {
                    String command = "reboot -p"; // Или "poweroff" в зависимости от системы
                    String result = ShellCommandExecutor.executeCommand(command);
                    Log.i(TAG, "Выключение устройства: " + result);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка выключения устройства", e);
                }
            }, 5000);

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка подготовки к выключению", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка выключения устройства: " + e.getMessage());
            }
            return false;
        }
    }

    private boolean sleepDevice(SimpleCallback callback) {
        try {
            String command = "input keyevent KEYCODE_SLEEP";
            String result = ShellCommandExecutor.executeCommand(command);

            Log.i(TAG, "Режим сна: " + result);

            if (callback != null) {
                callback.onCommandResult("Устройство переходит в режим сна");
            }
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка перевода в режим сна", e);
            if (callback != null) {
                callback.onCommandResult("Ошибка перевода в режим сна: " + e.getMessage());
            }
            return false;
        }
    }

    /**
     * Вспомогательный метод для выполнения shell команд (ИСПРАВЛЕННАЯ ВЕРСИЯ)
     */
    private String executeShellCommand(String command) {
        Process process = null;
        StringBuilder output = new StringBuilder();

        try {
            Log.i(TAG, "Выполняю команду: " + command);

            // Используем ProcessBuilder для лучшего контроля
            ProcessBuilder pb = new ProcessBuilder("sh", "-c", command);
            pb.redirectErrorStream(true); // Объединяем stdout и stderr
            process = pb.start();

            // Читаем вывод
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                Log.d(TAG, "Вывод команды: " + line);
            }

            // Ждем завершения процесса
            int exitCode = process.waitFor();
            Log.i(TAG, "Код выхода: " + exitCode);

            return output.toString().trim();

        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения команды: " + command, e);
            return "Ошибка: " + e.getMessage();
        } finally {
            if (process != null) {
                try {
                    process.destroy();
                } catch (Exception e) {
                    Log.w(TAG, "Ошибка при уничтожении процесса", e);
                }
            }
        }
    }

    /**
     * Исправление ошибок распознавания речи
     */
    private String fixSpeechRecognitionErrors(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        String lower = text.toLowerCase().trim();
        Log.d(TAG, "Исправление ошибок распознавания: \"" + text + "\"");

        // Исправляем распространенные ошибки
        Map<String, String> corrections = new HashMap<>();
        corrections.put("у дом", "умный дом");
        corrections.put("умн дом", "умный дом");
        corrections.put("умны дом", "умный дом");
        corrections.put("умный дом", "умный дом"); // Уже правильно
        corrections.put("клевер", "клевер");
        corrections.put("кливер", "клевер");
        corrections.put("клеверх", "клевер");
        corrections.put("увелич", "увеличить");
        corrections.put("уменьш", "уменьшить");
        corrections.put("ярк", "яркость");
        corrections.put("звук", "звук");

        String corrected = lower;
        for (Map.Entry<String, String> entry : corrections.entrySet()) {
            if (corrected.contains(entry.getKey())) {
                corrected = corrected.replace(entry.getKey(), entry.getValue());
                Log.d(TAG, "Исправлено: \"" + entry.getKey() + "\" → \"" + entry.getValue() + "\"");
            }
        }

        // Специальные исправления для команд CleverHome
        if (corrected.contains("запусти у") && corrected.contains("дом")) {
            corrected = corrected.replace("запусти у", "запусти умный");
            Log.d(TAG, "Специальное исправление: \"запусти у\" → \"запусти умный\"");
        }

        if (corrected.contains("открой у") && corrected.contains("дом")) {
            corrected = corrected.replace("открой у", "открой умный");
            Log.d(TAG, "Специальное исправление: \"открой у\" → \"открой умный\"");
        }

        if (corrected.contains("включи у") && corrected.contains("дом")) {
            corrected = corrected.replace("включи у", "включи умный");
            Log.d(TAG, "Специальное исправление: \"включи у\" → \"включи умный\"");
        }

        if (!corrected.equals(lower)) {
            Log.d(TAG, "Исправленный текст: \"" + corrected + "\"");
        }

        return corrected;
    }
}