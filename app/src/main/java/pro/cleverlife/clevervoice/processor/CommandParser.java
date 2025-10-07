package pro.cleverlife.clevervoice.processor;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class CommandParser {

    public enum CommandType {
        OPEN_APP,
        MOVE_FILE,
        TOGGLE_WIFI,
        TOGGLE_BLUETOOTH,
        ADJUST_VOLUME,
        SET_BRIGHTNESS,
        UNKNOWN
    }

    public static class Command {
        private CommandType type;
        private Map<String, String> parameters;

        public Command(CommandType type) {
            this.type = type;
            this.parameters = new HashMap<>();
        }

        public CommandType getType() { return type; }
        public String getParameter() {
            return parameters.get("default");
        }
        public String getParameter(String key) {
            return parameters.get(key);
        }
        public int getIntParameter() {
            try {
                return Integer.parseInt(parameters.get("default"));
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        public boolean getBooleanParameter() {
            String param = parameters.get("default");
            return param != null && (param.contains("включи") || param.contains("on") || param.contains("true"));
        }
        public void addParameter(String key, String value) {
            parameters.put(key, value);
        }
    }

    // Паттерны для распознавания команд
    private final Pattern OPEN_APP_PATTERN = Pattern.compile(
            "открой\\s+(.*?)(?:\\.|$)|запусти\\s+(.*?)(?:\\.|$)",
            Pattern.CASE_INSENSITIVE
    );

    private final Pattern MOVE_FILE_PATTERN = Pattern.compile(
            "перемести\\s+файл\\s+(.*?)\\s+в\\s+(.*?)(?:\\.|$)|" +
                    "перемести\\s+(.*?)\\s+в\\s+(.*?)(?:\\.|$)",
            Pattern.CASE_INSENSITIVE
    );

    private final Pattern WIFI_PATTERN = Pattern.compile(
            "(включи|выключи)\\s+(wi\\-fi|вайфай|вай-фай)",
            Pattern.CASE_INSENSITIVE
    );

    private final Pattern VOLUME_PATTERN = Pattern.compile(
            "(сделай|поставь)\\s+громкость\\s+(на\\s+)?(\\d+)|" +
                    "(увеличь|уменьши)\\s+громкость",
            Pattern.CASE_INSENSITIVE
    );

    private final Pattern BRIGHTNESS_PATTERN = Pattern.compile(
            "(установи|сделай)\\s+яркость\\s+(на\\s+)?(\\d+)",
            Pattern.CASE_INSENSITIVE
    );

    public Command parse(String text) {
        text = preprocessText(text);

        if (OPEN_APP_PATTERN.matcher(text).find()) {
            return parseOpenAppCommand(text);
        } else if (MOVE_FILE_PATTERN.matcher(text).find()) {
            return parseMoveFileCommand(text);
        } else if (WIFI_PATTERN.matcher(text).find()) {
            return parseWifiCommand(text);
        } else if (VOLUME_PATTERN.matcher(text).find()) {
            return parseVolumeCommand(text);
        } else if (BRIGHTNESS_PATTERN.matcher(text).find()) {
            return parseBrightnessCommand(text);
        }

        return new Command(CommandType.UNKNOWN);
    }

    private String preprocessText(String text) {
        return text.toLowerCase()
                .replace("ё", "е")
                .replaceAll("[^а-яa-z0-9\\s]", "")
                .trim();
    }

    private Command parseOpenAppCommand(String text) {
        Command command = new Command(CommandType.OPEN_APP);
        java.util.regex.Matcher matcher = OPEN_APP_PATTERN.matcher(text);

        if (matcher.find()) {
            String appName = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            command.addParameter("default", normalizeAppName(appName));
        }

        return command;
    }

    private Command parseMoveFileCommand(String text) {
        Command command = new Command(CommandType.MOVE_FILE);
        java.util.regex.Matcher matcher = MOVE_FILE_PATTERN.matcher(text);

        if (matcher.find()) {
            String source = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
            String destination = matcher.group(2) != null ? matcher.group(2) : matcher.group(4);

            command.addParameter("source", normalizePath(source));
            command.addParameter("destination", normalizePath(destination));
        }

        return command;
    }

    private Command parseWifiCommand(String text) {
        Command command = new Command(CommandType.TOGGLE_WIFI);
        java.util.regex.Matcher matcher = WIFI_PATTERN.matcher(text);

        if (matcher.find()) {
            String action = matcher.group(1);
            command.addParameter("default", action);
        }

        return command;
    }

    private Command parseVolumeCommand(String text) {
        Command command = new Command(CommandType.ADJUST_VOLUME);
        java.util.regex.Matcher matcher = VOLUME_PATTERN.matcher(text);

        if (matcher.find()) {
            if (matcher.group(3) != null) {
                // Установка конкретного уровня громкости
                command.addParameter("default", matcher.group(3));
            } else {
                // Увеличение/уменьшение громкости
                String action = matcher.group(4);
                command.addParameter("action", action);
            }
        }

        return command;
    }

    private Command parseBrightnessCommand(String text) {
        Command command = new Command(CommandType.SET_BRIGHTNESS);
        java.util.regex.Matcher matcher = BRIGHTNESS_PATTERN.matcher(text);

        if (matcher.find()) {
            command.addParameter("default", matcher.group(3));
        }

        return command;
    }

    private String normalizeAppName(String appName) {
        Map<String, String> appMapping = new HashMap<>();
        appMapping.put("браузер", "chrome");
        appMapping.put("хром", "chrome");
        appMapping.put("сообщения", "messages");
        appMapping.put("смс", "messages");
        appMapping.put("контакты", "contacts");
        appMapping.put("звонки", "phone");
        appMapping.put("телефон", "phone");
        appMapping.put("камера", "camera");
        appMapping.put("галерея", "gallery");
        appMapping.put("фото", "gallery");
        appMapping.put("настройки", "settings");
        appMapping.put("калькулятор", "calculator");

        return appMapping.getOrDefault(appName.toLowerCase(), appName);
    }

    private String normalizePath(String path) {
        // Нормализация путей файлов
        return path.replace("слот", "slot")
                .replace("карта памяти", "sdcard")
                .replace("внутренняя память", "internal");
    }
}
