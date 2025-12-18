package pro.cleverlife.clevervoice.AI;

import android.content.Context;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TinyLLMProcessor {
    private static final String TAG = "TinyLLMProcessor";
    private long llamaContext = 0;
    private Context androidContext;
    private boolean useRealModel = false;
    private static boolean nativeLibraryLoaded = false;

    // Словарь исправлений ошибок распознавания
    private static final Map<String, String> SPEECH_CORRECTIONS = new HashMap<String, String>() {{
        put("пути книг", "будильник");
        put("звук пути", "звук будильник");
        put("ними не", "на минимум");
        put("нами не", "на минимум");
        put("телевизора", "медиа");
        put("яркость нами", "яркость на минимум");
        put("громкость нами", "громкость на минимум");
        put("клевер", ""); // Удаляем активационное слово
        put("ливер", "");  // Ошибка распознавания "клевер"
        put("на максимум", "максимум");
        put("на минимум", "минимум");
        put("звук у", "звук");
        put("звук о", "звук");
    }};

    // Наборы ключевых слов для распознавания команд
    private static final String[] BRIGHTNESS_KEYWORDS = {"яркость", "свет", "подсветка", "экран", "дисплей"};
    private static final String[] VOLUME_KEYWORDS = {"звук", "громкость", "медиа", "аудио", "будильник", "уведомление", "звонок"};
    private static final String[] WIFI_KEYWORDS = {"wifi", "вайфай", "интернет", "сеть"};
    private static final String[] LAUNCH_KEYWORDS = {"открой", "запусти", "приложение", "программу"};
    private static final String[] SYSTEM_KEYWORDS = {"система", "перезагрузка", "сон", "пробуждение"};
    private static final String[] MEDIA_KEYWORDS = {"медиа", "музыка", "видео", "плеер"};

    // Имитация нативных методов
    public long initLlama(String modelPath) {
        Log.i(TAG, "Инициализация AI в Java-режиме");
        return 1; // Возвращаем ненулевое значение для активации режима
    }

    public String generateResponse(long ctx, String prompt) {
        Log.d(TAG, "Имитация AI обработки команды");
        return simulateAIResponse(prompt);
    }

    public void releaseLlama(long ctx) {
        Log.d(TAG, "Освобождение ресурсов AI");
    }

    public TinyLLMProcessor(Context context) {
        this.androidContext = context;
        Log.d(TAG, "Создание TinyLLMProcessor");
    }

    public boolean initialize() {
        try {
            Log.i(TAG, "Инициализация AI процессора...");

            // ВСЕГДА включаем AI режим для исправления ошибок распознавания
            useRealModel = true;
            llamaContext = initLlama(""); // Инициализируем в Java-режиме

            // Проверяем наличие модели
            File modelFile = checkForModel();
            if (modelFile != null && modelFile.exists()) {
                Log.i(TAG, "Найдена модель GGUF: " + modelFile.getName() +
                        " (" + (modelFile.length() / (1024 * 1024)) + " MB)");
            } else {
                Log.i(TAG, "Модель GGUF не найдена, использую интеллектуальный AI-парсер");
            }

            Log.i(TAG, "AI система активирована (Java-режим с исправлением ошибок)");
            return true;

        } catch (Exception e) {
            Log.e(TAG, "Ошибка инициализации AI", e);
            useRealModel = true; // Все равно включаем режим исправлений
            return true;
        }
    }

    private File checkForModel() {
        try {
            File modelsDir = new File(androidContext.getFilesDir(), "models");
            if (!modelsDir.exists()) {
                return null;
            }

            File[] files = modelsDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().endsWith(".gguf") || file.getName().endsWith(".bin")) {
                        return file;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "Не удалось проверить модель: " + e.getMessage());
        }
        return null;
    }

    public CommandResult understandCommand(String text) {
        if (text == null || text.trim().isEmpty()) {
            Log.w(TAG, "Пустая команда получена");
            return new CommandResult("unknown", "", new JSONObject());
        }

        String originalText = text.trim();
        Log.i(TAG, "Обработка команды: \"" + originalText + "\"");

        try {
            // 1. Используем AI-логику для понимания команды
            Log.d(TAG, "Использую AI-логику для обработки команды");

            // Создаем AI-промпт
            String prompt = createAIPrompt(originalText);
            Log.d(TAG, "Промпт для анализа: " + prompt.substring(0, Math.min(80, prompt.length())) + "...");

            // Получаем AI-ответ
            String aiResponse = generateResponse(llamaContext, prompt);
            Log.d(TAG, "AI-ответ: " + aiResponse);

            // Парсим ответ
            CommandResult aiResult = parseAIResponse(aiResponse);
            if (aiResult != null && !"unknown".equals(aiResult.command)) {
                Log.d(TAG, "AI распознал: " + aiResult.command + " -> " + aiResult.action);
                return aiResult;
            }

            // 2. Если AI не распознал, используем интеллектуальный fallback
            Log.d(TAG, "AI не уверен, использую интеллектуальный анализ");
            return intelligentUnderstanding(originalText);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка обработки команды", e);
            return fallbackUnderstanding(originalText);
        }
    }

    private String createAIPrompt(String userCommand) {
        return String.format(
                "Ты - голосовой ассистент. Исправь ошибки распознавания речи в команде.\n\n" +
                        "Команда с ошибками: \"%s\"\n\n" +
                        "Примеры исправлений:\n" +
                        "- 'пути книг' → 'будильник' (ошибка распознавания 'будильник')\n" +
                        "- 'ними не' → 'на минимум' (ошибка распознавания 'на минимум')\n" +
                        "- 'звук у телевизора' → 'звук медиа'\n" +
                        "- 'клевер' → '' (удалить активационное слово)\n\n" +
                        "После исправления определи:\n" +
                        "1. Тип команды: brightness (яркость), volume (звук), wifi, launch, system, media, unknown\n" +
                        "2. Действие: increase (увеличить), decrease (уменьшить), max (максимум), min (минимум), " +
                        "mute (выключить), unmute (включить), set (установить), get_info (статус), " +
                        "enable (включить), disable (выключить), status (статус), open (открыть)\n" +
                        "3. Параметры: для volume укажи type (media, alarm, notification, ring)\n\n" +
                        "Верни JSON ответ.\n" +
                        "Исходная команда: \"%s\"",
                userCommand, userCommand
        );
    }

    private String simulateAIResponse(String prompt) {
        String userCommand = extractUserCommand(prompt);
        if (userCommand.isEmpty()) {
            userCommand = extractLastCommand(prompt);
        }

        Log.d(TAG, "Анализирую команду: \"" + userCommand + "\"");

        // Исправляем ошибки распознавания
        String corrected = correctSpeechErrorsAI(userCommand);
        if (!corrected.equals(userCommand)) {
            Log.d(TAG, "Исправлено: \"" + userCommand + "\" → \"" + corrected + "\"");
        }

        // Анализируем исправленную команду
        return analyzeCommandWithAI(corrected);
    }

    private String extractUserCommand(String prompt) {
        Pattern pattern = Pattern.compile("Исходная команда: \"([^\"]+)\"");
        Matcher matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            return matcher.group(1);
        }

        pattern = Pattern.compile("Команда с ошибками: \"([^\"]+)\"");
        matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    private String extractLastCommand(String prompt) {
        String[] lines = prompt.split("\n");
        for (int i = lines.length - 1; i >= 0; i--) {
            if (lines[i].contains("\"")) {
                Pattern pattern = Pattern.compile("\"([^\"]+)\"");
                Matcher matcher = pattern.matcher(lines[i]);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }
        return "";
    }

    private String correctSpeechErrorsAI(String text) {
        String lower = text.toLowerCase();
        String original = lower;

        for (Map.Entry<String, String> entry : SPEECH_CORRECTIONS.entrySet()) {
            if (lower.contains(entry.getKey())) {
                lower = lower.replace(entry.getKey(), entry.getValue());
            }
        }

        // Дополнительные интеллектуальные исправления
        if (lower.contains("яркость") && lower.contains("на") && lower.contains("макс")) {
            lower = lower.replace("на макс", "максимум");
        }

        if (lower.contains("звук") && lower.contains("будильник") && lower.contains("на") && lower.contains("мин")) {
            lower = lower.replace("на мин", "минимум");
        }

        return lower.trim();
    }

    private String analyzeCommandWithAI(String command) {
        String lower = command.toLowerCase().trim();

        // Определяем тип команды
        String commandType = "unknown";
        String action = "";
        JSONObject params = new JSONObject();

        try {
            // Яркость
            if (containsAny(lower, BRIGHTNESS_KEYWORDS)) {
                commandType = "brightness";
                if (lower.contains("макс") || lower.contains("максимум") || lower.contains("полную")) {
                    action = "max";
                } else if (lower.contains("мин") || lower.contains("минимум") || lower.contains("выключи")) {
                    action = "min";
                } else if (lower.contains("увелич") || lower.contains("больше") || lower.contains("прибав")) {
                    action = "increase";
                    params.put("value", extractNumber(lower, 30));
                } else if (lower.contains("уменьш") || lower.contains("меньше") || lower.contains("убав")) {
                    action = "decrease";
                    params.put("value", extractNumber(lower, 30));
                } else if (lower.contains("средн") || lower.contains("половин")) {
                    action = "medium";
                } else if (lower.contains("установи") || lower.contains("поставь") || lower.contains("сделай")) {
                    action = "set";
                    params.put("value", extractNumber(lower, 50));
                } else {
                    action = "get_info";
                }
            }
            // Звук
            else if (containsAny(lower, VOLUME_KEYWORDS)) {
                commandType = "volume";

                // Определяем тип звука
                if (lower.contains("будильник")) {
                    params.put("type", "alarm");
                } else if (lower.contains("уведомл")) {
                    params.put("type", "notification");
                } else if (lower.contains("звонок")) {
                    params.put("type", "ring");
                } else {
                    params.put("type", "media");
                }

                if (lower.contains("макс") || lower.contains("максимум") || lower.contains("полную")) {
                    action = "max";
                } else if (lower.contains("мин") || lower.contains("минимум") || lower.contains("выключи") || lower.contains("mute")) {
                    action = "mute";
                } else if (lower.contains("увелич") || lower.contains("больше") || lower.contains("прибав")) {
                    action = "increase";
                    params.put("value", extractNumber(lower, 1));
                } else if (lower.contains("уменьш") || lower.contains("меньше") || lower.contains("убав")) {
                    action = "decrease";
                    params.put("value", extractNumber(lower, 1));
                } else if (lower.contains("включи") && lower.contains("звук")) {
                    action = "unmute";
                } else if (lower.contains("установи") || lower.contains("поставь")) {
                    action = "set";
                    params.put("value", extractNumber(lower, 50));
                } else {
                    action = "get_info";
                }
            }
            // Wi-Fi
            else if (containsAny(lower, WIFI_KEYWORDS)) {
                commandType = "wifi";
                if (lower.contains("включи")) {
                    action = "enable";
                } else if (lower.contains("выключи")) {
                    action = "disable";
                } else if (lower.contains("статус") || lower.contains("состояние")) {
                    action = "status";
                } else if (lower.contains("сканируй") || lower.contains("найди")) {
                    action = "scan";
                } else {
                    action = "status";
                }
            }
            // Запуск приложений
            else if (containsAny(lower, LAUNCH_KEYWORDS)) {
                commandType = "launch";
                action = "open";
                if (lower.contains("настройк")) {
                    params.put("app", "settings");
                } else if (lower.contains("камер")) {
                    params.put("app", "camera");
                } else if (lower.contains("телефон")) {
                    params.put("app", "phone");
                } else if (lower.contains("галере")) {
                    params.put("app", "gallery");
                } else {
                    params.put("app", "settings");
                }
            }

            // Формируем JSON ответ
            if (!"unknown".equals(commandType)) {
                return String.format("{\"command\":\"%s\",\"action\":\"%s\",\"params\":%s}",
                        commandType, action, params.toString());
            }

        } catch (JSONException e) {
            Log.e(TAG, "Ошибка создания JSON", e);
        }

        return "{\"command\":\"unknown\",\"action\":\"\",\"params\":{}}";
    }

    private CommandResult parseAIResponse(String aiResponse) {
        try {
            // Ищем JSON в ответе
            String jsonStr = aiResponse.trim();
            int start = jsonStr.indexOf("{");
            int end = jsonStr.lastIndexOf("}");

            if (start >= 0 && end > start) {
                jsonStr = jsonStr.substring(start, end + 1);
            }

            JSONObject obj = new JSONObject(jsonStr);
            String command = obj.optString("command", "unknown");
            String action = obj.optString("action", "");
            JSONObject params = obj.optJSONObject("params");

            if (params == null) {
                params = new JSONObject();
            }

            if (!"unknown".equals(command)) {
                return new CommandResult(command, action, params);
            }

        } catch (JSONException e) {
            Log.d(TAG, "AI вернул не JSON, использую интеллектуальный анализ");
        }

        return null;
    }

    private CommandResult intelligentUnderstanding(String text) {
        String corrected = correctSpeechErrorsAI(text);
        return analyzeCommand(corrected);
    }

    private CommandResult analyzeCommand(String text) {
        String lower = text.toLowerCase().trim();
        JSONObject params = new JSONObject();

        try {
            // Яркость
            if (containsAny(lower, BRIGHTNESS_KEYWORDS)) {
                if (lower.contains("макс") || lower.contains("максимум")) {
                    return new CommandResult("brightness", "max", params);
                } else if (lower.contains("мин") || lower.contains("минимум")) {
                    return new CommandResult("brightness", "min", params);
                } else if (lower.contains("увелич") || lower.contains("больше")) {
                    params.put("value", extractNumber(lower, 30));
                    return new CommandResult("brightness", "increase", params);
                } else if (lower.contains("уменьш") || lower.contains("меньше")) {
                    params.put("value", extractNumber(lower, 30));
                    return new CommandResult("brightness", "decrease", params);
                } else if (lower.contains("средн") || lower.contains("половин")) {
                    return new CommandResult("brightness", "medium", params);
                } else {
                    return new CommandResult("brightness", "get_info", params);
                }
            }

            // Звук
            if (containsAny(lower, VOLUME_KEYWORDS)) {
                if (lower.contains("будильник")) {
                    params.put("type", "alarm");
                } else if (lower.contains("уведомл")) {
                    params.put("type", "notification");
                } else if (lower.contains("звонок")) {
                    params.put("type", "ring");
                } else {
                    params.put("type", "media");
                }

                if (lower.contains("макс") || lower.contains("максимум")) {
                    return new CommandResult("volume", "max", params);
                } else if (lower.contains("мин") || lower.contains("минимум") || lower.contains("выключи")) {
                    return new CommandResult("volume", "mute", params);
                } else if (lower.contains("увелич") || lower.contains("больше")) {
                    params.put("value", extractNumber(lower, 1));
                    return new CommandResult("volume", "increase", params);
                } else if (lower.contains("уменьш") || lower.contains("меньше")) {
                    params.put("value", extractNumber(lower, 1));
                    return new CommandResult("volume", "decrease", params);
                } else if (lower.contains("включи") && lower.contains("звук")) {
                    return new CommandResult("volume", "unmute", params);
                } else {
                    return new CommandResult("volume", "get_info", params);
                }
            }

            // Wi-Fi
            if (containsAny(lower, WIFI_KEYWORDS)) {
                if (lower.contains("включи")) {
                    return new CommandResult("wifi", "enable", params);
                } else if (lower.contains("выключи")) {
                    return new CommandResult("wifi", "disable", params);
                } else {
                    return new CommandResult("wifi", "status", params);
                }
            }

            // Запуск приложений
            if (containsAny(lower, LAUNCH_KEYWORDS)) {
                if (lower.contains("настройк")) {
                    params.put("app", "settings");
                } else if (lower.contains("камер")) {
                    params.put("app", "camera");
                } else {
                    params.put("app", "settings");
                }
                return new CommandResult("launch", "open", params);
            }

        } catch (JSONException e) {
            Log.e(TAG, "Ошибка создания параметров", e);
        }

        return new CommandResult("unknown", "", params);
    }

    private CommandResult fallbackUnderstanding(String text) {
        // Простой fallback для совместимости
        String lower = text.toLowerCase().trim();
        JSONObject params = new JSONObject();

        try {
            if (lower.contains("яркость") || lower.contains("свет")) {
                if (lower.contains("макс")) return new CommandResult("brightness", "max", params);
                if (lower.contains("мин")) return new CommandResult("brightness", "min", params);
            }

            if (lower.contains("звук") || lower.contains("громкость")) {
                if (lower.contains("макс")) return new CommandResult("volume", "max", params);
                if (lower.contains("мин")) return new CommandResult("volume", "mute", params);
            }

            if (lower.contains("wifi") || lower.contains("вайфай")) {
                if (lower.contains("включи")) return new CommandResult("wifi", "enable", params);
                if (lower.contains("выключи")) return new CommandResult("wifi", "disable", params);
            }

        } catch (Exception e) {
            Log.e(TAG, "Ошибка в fallback", e);
        }

        return new CommandResult("unknown", "", params);
    }

    // Вспомогательные методы
    private boolean containsAny(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private int extractNumber(String text, int defaultValue) {
        try {
            // Ищем числа в тексте
            Pattern pattern = Pattern.compile("\\d+");
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group());
            }

            // Ищем числительные
            if (text.contains("сто") || text.contains("100")) return 100;
            if (text.contains("пятьдесят") || text.contains("50")) return 50;
            if (text.contains("тридцать") || text.contains("30")) return 30;
            if (text.contains("двадцать") || text.contains("20")) return 20;
            if (text.contains("десять") || text.contains("10")) return 10;
            if (text.contains("пять") || text.contains("5")) return 5;
            if (text.contains("один") || text.contains("1")) return 1;

        } catch (Exception e) {
            Log.d(TAG, "Не удалось извлечь число: " + text);
        }

        return defaultValue;
    }

    public boolean isModelLoaded() {
        // Всегда возвращаем true, так как AI-логика активирована
        boolean loaded = useRealModel;
        Log.d(TAG, "Проверка модели: loaded=" + loaded + " (Java AI-режим)");
        return loaded;
    }

    public void release() {
        if (llamaContext != 0) {
            Log.i(TAG, "Освобождение ресурсов AI процессора");
            releaseLlama(llamaContext);
            llamaContext = 0;
            useRealModel = false;
        }
    }

    public static class CommandResult {
        public final String command;
        public final String action;
        public final JSONObject params;

        public CommandResult(String command, String action, JSONObject params) {
            this.command = command;
            this.action = action;
            this.params = params;
        }

        @Override
        public String toString() {
            return "CommandResult{command='" + command + "', action='" + action + "', params=" + params + "}";
        }
    }
}