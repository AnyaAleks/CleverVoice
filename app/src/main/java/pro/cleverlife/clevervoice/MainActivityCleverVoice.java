package pro.cleverlife.clevervoice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONObject;

import pro.cleverlife.clevervoice.AI.TinyLLMProcessor;
import pro.cleverlife.clevervoice.AI.VoskAIProcessor;
import pro.cleverlife.clevervoice.API.WiFiAPI;
import pro.cleverlife.clevervoice.TestInterface.TestBrightnessActivity;
import pro.cleverlife.clevervoice.TestInterface.TestSoundActivity;
import pro.cleverlife.clevervoice.processor.CommandProcessor;
import pro.cleverlife.clevervoice.service.SoundManager;
import pro.cleverlife.clevervoice.service.VoiceRecognitionService;
import pro.cleverlife.clevervoice.utils.PermissionManager;

public class MainActivityCleverVoice extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CODE = 123;

    private VoiceRecognitionService voiceService;
    private CommandProcessor commandProcessor;
    private SoundManager soundManager;
    private VoskAIProcessor aiProcessor;
    private TinyLLMProcessor tinyLLMProcessor;

    private TextView statusText;
    private TextView logText;
    private Button startButton;
    private CountDownTimer commandTimer;
    private boolean isListening = false;
    private StringBuilder logBuilder = new StringBuilder();

    // Объявляем BroadcastReceiver как поле класса
    private BroadcastReceiver wifiScanReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_clever_voice);

        // Инициализация AI процессоров
        aiProcessor = new VoskAIProcessor(this);
        tinyLLMProcessor = new TinyLLMProcessor(this);

        initViews();
        checkPermissions();

        // Инициализируем BroadcastReceiver
        initWifiScanReceiver();
    }

    private void initViews() {
        statusText = findViewById(R.id.statusText);
        logText = findViewById(R.id.logText);
        startButton = findViewById(R.id.startButton);
        Button buttonTestBrightness = findViewById(R.id.buttonTestBrightness);
        Button buttonTestSound = findViewById(R.id.buttonTestSound);
        startButton.setOnClickListener(v -> toggleListening());

        // Обработчик для перехода в тест яркости
        buttonTestBrightness.setOnClickListener(v -> {
            Intent intent = new Intent(this, TestBrightnessActivity.class);
            startActivity(intent);
        });

        // Обработчик для перехода в тест звука
        buttonTestSound.setOnClickListener(v -> {
            Intent intent = new Intent(this, TestSoundActivity.class);
            startActivity(intent);
        });

        // Изначально кнопка неактивна
        startButton.setEnabled(false);
        statusText.setText("Проверка разрешений...");
        logText.setText("Инициализация системы...\n");
    }

    private void initWifiScanReceiver() {
        wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("WIFI_SCAN_RESULTS".equals(intent.getAction())) {
                    int count = intent.getIntExtra("count", 0);
                    addLog("📡 Сканирование завершено! Найдено сетей: " + count);

                    if (count > 0) {
                        String net1 = intent.getStringExtra("network1");
                        String net2 = intent.getStringExtra("network2");
                        String net3 = intent.getStringExtra("network3");

                        if (net1 != null) addLog("  1. " + net1);
                        if (net2 != null) addLog("  2. " + net2);
                        if (net3 != null) addLog("  3. " + net3);

                        if (count > 3) {
                            addLog("  ... и еще " + (count - 3) + " сетей");
                        }
                    }
                }
            }
        };
    }

    private void testWiFiCommands() {
        addLog("Запуск теста WiFi команд...");

        if (!WiFiAPI.isInitialized()) {
            addLog("WiFiAPI не инициализирован!");
            return;
        }

        new Thread(() -> {
            try {
                // 1. Проверяем статус WiFi
                runOnUiThread(() -> addLog("1. Проверяем статус WiFi..."));
                String status = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_STATUS);
                String finalStatus = status;
                runOnUiThread(() -> addLog("   Результат: " + finalStatus));

                Thread.sleep(1000);

                // 2. Проверяем разрешение на геолокацию
                runOnUiThread(() -> addLog("2. Проверяем разрешение на геолокацию..."));
                String permission = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.CHECK_LOCATION_PERMISSION);
                runOnUiThread(() -> addLog("   Результат: " + permission));

                Thread.sleep(1000);

                // 3. Включаем WiFi (если есть root)
                if (WiFiAPI.getWiFiService().hasRootAccess()) {
                    runOnUiThread(() -> addLog("3. Пытаемся включить WiFi..."));
                    String enableResult = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.ENABLE);
                    runOnUiThread(() -> addLog("   Результат: " + enableResult));

                    Thread.sleep(2000);

                    // 4. Проверяем статус после включения
                    runOnUiThread(() -> addLog("4. Проверяем статус после включения..."));
                    status = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.GET_STATUS);
                    String finalStatus1 = status;
                    runOnUiThread(() -> addLog("   Результат: " + finalStatus1));

                    Thread.sleep(1000);
                }

                // 5. Тестируем сканирование
                runOnUiThread(() -> addLog("5. Тестируем сканирование сетей..."));
                String scanResult = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.SCAN_WITH_RESULTS);
                runOnUiThread(() -> addLog("   Результат: " + scanResult));

                runOnUiThread(() -> {
                    addLog("=== ТЕСТ WiFi ЗАВЕРШЕН ===");
                    Toast.makeText(MainActivityCleverVoice.this, "Тест WiFi завершен", Toast.LENGTH_SHORT).show();
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                runOnUiThread(() -> addLog("Тест прерван"));
            } catch (Exception e) {
                runOnUiThread(() -> {
                    addLog("Ошибка теста WiFi: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        }).start();
    }

    private void testWiFiScan() {
        new Thread(() -> {
            try {
                if (!WiFiAPI.isInitialized()) {
                    runOnUiThread(() -> addLog("WiFiAPI не инициализирован"));
                    return;
                }

                // Метод 1: Стандартное сканирование
                runOnUiThread(() -> addLog("1. Стандартное сканирование..."));
                String scanResult1 = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.SCAN_WITH_RESULTS);
                runOnUiThread(() -> addLog(scanResult1));

                Thread.sleep(1000);

                // Метод 2: Root сканирование (если есть root)
                if (WiFiAPI.getWiFiService().hasRootAccess()) {
                    runOnUiThread(() -> addLog("\n2. Root сканирование..."));
                    String scanResult2 = WiFiAPI.executeCommand(WiFiAPI.WiFiCommand.SCAN_ROOT);
                    runOnUiThread(() -> addLog(scanResult2));
                }

            } catch (Exception e) {
                runOnUiThread(() -> addLog("Ошибка теста сканирования: " + e.getMessage()));
            }
        }).start();
    }

    private void checkPermissions() {
        addLog("Проверка разрешений...");

        if (PermissionManager.hasAllRequiredPermissions(this)) {
            addLog("Все разрешения предоставлены");
            initServices();
        } else {
            addLog("Запрос недостающих разрешений");
            String[] missingPermissions = PermissionManager.getMissingPermissions(this);
            ActivityCompat.requestPermissions(this, missingPermissions, PERMISSION_REQUEST_CODE);
        }
    }

    private void initServices() {
        addLog("Инициализация сервисов...");

        // ИНИЦИАЛИЗАЦИЯ WiFiAPI
        WiFiAPI.initialize(this);
        if (WiFiAPI.isInitialized()) {
            addLog("WiFiAPI успешно инициализирован");
        } else {
            addLog("WiFiAPI не инициализирован!");
        }

        voiceService = new VoiceRecognitionService(this);
        commandProcessor = new CommandProcessor(this);

        // Инициализируем SoundManager и передаем звуковые файлы
        soundManager = new SoundManager(this);
        soundManager.initializeWithSounds(R.raw.victory_tone, R.raw.error_notification);

        if (soundManager.isInitialized()) {
            addLog("Менеджер звуков инициализирован");
        } else {
            addLog("Менеджер звуков не смог загрузить звуковые файлы");
        }

        // Загружаем модель TinyLLaMA в фоновом потоке
        new Thread(() -> {
            boolean initialized = tinyLLMProcessor.initialize();
            runOnUiThread(() -> {
                if (initialized) {
                    addLog("AI процессоры инициализированы");

                    // ПРОВЕРЯЕМ, ЗАГРУЖЕНА ЛИ МОДЕЛЬ
                    boolean aiLoaded = tinyLLMProcessor.isModelLoaded();
                    addLog("AI модель: " + (aiLoaded ? "ЗАГРУЖЕНА" : "НЕ загружена"));

                    if (aiLoaded) {
                        addLog("TinyLLaMA будет исправлять ошибки распознавания");
                    } else {
                        addLog("Будут использоваться только простые правила");
                    }
                } else {
                    addLog("Ошибка инициализации AI процессоров");
                }
            });
        }).start();

        setupVoiceRecognition();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                addLog("Все разрешения получены");
                initServices();
                Toast.makeText(this, "Все разрешения получены", Toast.LENGTH_SHORT).show();
            } else {
                addLog("Не все разрешения предоставлены");
                statusText.setText("Не все разрешения предоставлены");

                Toast.makeText(this, "Для работы приложения нужны все разрешения", Toast.LENGTH_LONG).show();
                startButton.setEnabled(false);
            }
        }
    }

    private void setupVoiceRecognition() {
        addLog("Настройка голосового распознавания...");
        voiceService.setActivationListener(new VoiceRecognitionService.ActivationListener() {
            @Override
            public void onActivationWordDetected() {
                runOnUiThread(() -> {
                    addLog(">>> АКТИВАЦИЯ: слово 'Клевер' обнаружено!");
                    addLog(">< У вас 10 секунд для команды...");
                    statusText.setText("Слушаю команду...");
                    if (soundManager.isInitialized()) {
                        soundManager.playActivationSound();
                    }
                    startCommandTimer();
                });
            }

            @Override
            public void onCommandReceived(String command) {
                runOnUiThread(() -> {
                    addLog(">>> КОМАНДА: \"" + command + "\"");
                    if (commandTimer != null) {
                        commandTimer.cancel();
                    }
                    processCommand(command);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    addLog("!!! ОШИБКА: " + error);
                    statusText.setText("Ошибка: " + error);
                    if (soundManager.isInitialized()) {
                        soundManager.playErrorSound();
                    }

                    if (commandTimer != null) {
                        commandTimer.cancel();
                    }

                    if (isListening) {
                        addLog("Перезапуск прослушивания...");
                        voiceService.startListening();
                    }
                });
            }

            @Override
            public void onInitialized() {
                runOnUiThread(() -> {
                    addLog("Система голосового распознавания готова");
                    statusText.setText("Система готова");
                    startButton.setEnabled(true);
                    startButton.setText("Запустить прослушивание");
                });
            }

            @Override
            public void onPartialResult(String partialText) {
                runOnUiThread(() -> {
                    if (!partialText.trim().isEmpty() && !partialText.equals("[ожидание активации...]")) {
                        addLog("... частично: \"" + partialText + "\"");
                    }
                });
            }

            @Override
            public void onSpeechDetected() {

            }

            @Override
            public void onSilenceDetected() {

            }
        });
    }

    private void stopListening() {
        if (voiceService != null) {
            addLog("=== ОСТАНОВКА ПРОСЛУШИВАНИЯ ===");
            addLog("• Микрофон деактивирован");
            voiceService.stopListening();
        }
        isListening = false;
        startButton.setText("Запустить прослушивание");
        statusText.setText("Остановлено");

        if (commandTimer != null) {
            commandTimer.cancel();
        }
    }

    private void toggleListening() {
        if (isListening) {
            stopListening();
        } else {
            startListening();
        }
    }

    private void startListening() {
        if (voiceService != null) {
            addLog("=== ЗАПУСК ПРОСЛУШИВАНИЯ ===");
            addLog("• Микрофон активирован");
            addLog("• Ожидание активационного слова: 'Клевер'");
            addLog("• Речь до активации игнорируется");
            voiceService.startListening();
            isListening = true;
            startButton.setText("Остановить прослушивание");
            statusText.setText("Скажите 'Клевер' для активации...");
        } else {
            addLog("!!! Ошибка: сервис распознавания не инициализирован");
        }
    }

    private void startCommandTimer() {
        if (commandTimer != null) {
            commandTimer.cancel();
        }

        commandTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                String seconds = String.valueOf(millisUntilFinished / 1000);
                statusText.setText(">< Команда: " + seconds + "с");

                if (millisUntilFinished % 5000 == 0) {
                    addLog(">< Осталось " + seconds + " секунд...");
                }
            }

            public void onFinish() {
                addLog(">< ВРЕМЯ ВЫШЛО! Активация сброшена.");
                statusText.setText(">< Время вышло! Скажите 'Клевер'...");
                if (soundManager.isInitialized()) {
                    soundManager.playErrorSound();
                }

                if (voiceService != null) {
                    voiceService.resetActivation();
                }

                if (isListening) {
                    addLog("Ожидание активационного слова...");
                    voiceService.startListening();
                }
            }
        }.start();
    }

    private void processCommand(String command) {
        addLog("-><- Обработка команды: \"" + command + "\"");
        statusText.setText("Обрабатываю: " + command);

        if (commandTimer != null) {
            commandTimer.cancel();
        }

        // Проверяем, является ли команда WiFi командой
        if (isWiFiCommand(command)) {
            addLog("Обнаружена WiFi команда");
            processWiFiCommand(command);
        } else {
            if (aiProcessor != null) {
                aiProcessor.processRecognizedText(command, new VoskAIProcessor.SimpleCallback() {
                    @Override
                    public void onCommandProcessed(boolean success) {
                        handleCommandResult(success);
                    }

                    @Override
                    public void onAIResult(String cmd, String action, JSONObject params, boolean usedAI) {
                        handleAIResult(cmd, action, params, usedAI);
                    }

                    @Override
                    public void onCommandResult(String resultMessage) {
                        handleCommandResultMessage(resultMessage);
                    }
                });
            } else {
                addLog("VoskAIProcessor не инициализирован!");
                useTinyLLMProcessorFallback(command);
            }
        }
    }

    private boolean isWiFiCommand(String command) {
        if (command == null) return false;

        String lowerCommand = command.toLowerCase();
        return lowerCommand.contains("wifi") ||
                lowerCommand.contains("вайфай") ||
                lowerCommand.contains("wi-fi") ||
                lowerCommand.contains("беспроводн") ||
                lowerCommand.contains("интернет") ||
                lowerCommand.contains("сеть") ||
                lowerCommand.contains("сканировать") ||
                lowerCommand.contains("подключить") ||
                lowerCommand.contains("отключить") ||
                lowerCommand.contains("включить") ||
                lowerCommand.contains("выключить");
    }

    private void processWiFiCommand(String command) {
        addLog("Обработка через WiFiAPI...");

        try {
            String result = WiFiAPI.handleVoiceCommand(command);
            addLog("Результат WiFi команды: " + result);

            if (result != null && !result.contains("Ошибка") && !result.contains("Неизвестная")) {
                if (soundManager.isInitialized()) {
                    soundManager.playSuccessSound();
                }
                addLog("WiFi команда выполнена успешно");
            } else {
                if (soundManager.isInitialized()) {
                    soundManager.playErrorSound();
                }
                addLog("WiFi команда не выполнена");
            }

        } catch (Exception e) {
            addLog("Ошибка обработки WiFi команды: " + e.getMessage());
            if (soundManager.isInitialized()) {
                soundManager.playErrorSound();
            }
        }

        if (isListening) {
            addLog("Ожидание новой активации...");
            statusText.setText("Скажите 'Клевер' для новой команды...");
            if (voiceService != null) {
                voiceService.startListening();
            }
        }
    }

    private void handleCommandResult(boolean success) {
        runOnUiThread(() -> {
            if (success) {
                addLog("Команда успешно выполнена");
                if (soundManager.isInitialized()) {
                    soundManager.playSuccessSound();
                }
            } else {
                addLog("Не удалось выполнить команду");
                if (soundManager.isInitialized()) {
                    soundManager.playErrorSound();
                }
            }
            returnToListeningMode();
        });
    }

    private void handleAIResult(String command, String action, JSONObject params, boolean usedAI) {
        runOnUiThread(() -> {
            addLog("AI АНАЛИЗ:");
            addLog("   Команда: " + command);
            addLog("   Действие: " + (action != null && !action.isEmpty() ? action : "не определено"));
            addLog("   Параметры: " + formatParamsForDisplay(params));
            addLog("   Использован AI: " + (usedAI ? "Да" : "Нет (правила)"));
            addLog("   " + getCommandEmoji(command) + " Тип: " + getCommandTypeDescription(command));
        });
    }

    private void handleCommandResultMessage(String resultMessage) {
        runOnUiThread(() -> {
            addLog("РЕЗУЛЬТАТ:");
            if (resultMessage != null && !resultMessage.isEmpty()) {
                addLog("   " + resultMessage);
            }
        });
    }

    private void returnToListeningMode() {
        if (isListening) {
            addLog("Ожидание новой активации...");
            statusText.setText("Скажите 'Клевер' для новой команды...");
            if (voiceService != null) {
                voiceService.startListening();
            }
        }
    }

    private void useTinyLLMProcessorFallback(String command) {
        new Thread(() -> {
            try {
                TinyLLMProcessor.CommandResult result = tinyLLMProcessor.understandCommand(command);

                runOnUiThread(() -> {
                    if (result != null) {
                        String logMsg = "TinyLLaMA распознал: " + result.command;
                        if (result.action != null && !result.action.isEmpty()) {
                            logMsg += " -> " + result.action;
                        }
                        addLog(logMsg);

                        if (soundManager.isInitialized()) {
                            if (!"unknown".equals(result.command)) {
                                soundManager.playSuccessSound();
                            } else {
                                soundManager.playErrorSound();
                                addLog("⚠ Команда не распознана");
                            }
                        }
                    } else {
                        addLog("Ошибка обработки команды");
                        if (soundManager.isInitialized()) {
                            soundManager.playErrorSound();
                        }
                    }
                    returnToListeningMode();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    addLog("Ошибка AI обработки: " + e.getMessage());
                    e.printStackTrace();
                    if (soundManager.isInitialized()) {
                        soundManager.playErrorSound();
                    }
                    returnToListeningMode();
                });
            }
        }).start();
    }

    private String getCommandEmoji(String command) {
        if (command == null) return "";

        switch (command.toLowerCase()) {
            case "wifi": return "";
            case "brightness": return "";
            case "volume": return "";
            case "launch": return "";
            case "system": return "";
            case "media": return "";
            default: return "";
        }
    }

    private String getCommandTypeDescription(String command) {
        if (command == null) return "Неизвестная команда";

        switch (command.toLowerCase()) {
            case "wifi": return "Управление WiFi";
            case "brightness": return "Управление яркостью";
            case "volume": return "Управление звуком";
            case "launch": return "Запуск приложения";
            case "system": return "Системная команда";
            case "media": return "Медиа команда";
            default: return command;
        }
    }

    private void addLog(String message) {
        runOnUiThread(() -> {
            String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
            String logMessage = "[" + timestamp + "] " + message;

            logBuilder.append(logMessage).append("\n");
            logText.setText(logBuilder.toString());

            if (logText.getLayout() != null) {
                int scrollAmount = logText.getLayout().getLineTop(logText.getLineCount()) - logText.getHeight();
                if (scrollAmount > 0) {
                    logText.scrollTo(0, scrollAmount);
                } else {
                    logText.scrollTo(0, 0);
                }
            }
        });
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    @Override
    protected void onResume() {
        super.onResume();

        // Регистрируем BroadcastReceiver
        if (wifiScanReceiver != null) {
            IntentFilter filter = new IntentFilter("WIFI_SCAN_RESULTS");
            registerReceiver(wifiScanReceiver, filter);
        }

        // При возвращении в приложение перезапускаем прослушивание если оно было активно
        if (isListening && voiceService != null) {
            addLog("Возобновление прослушивания...");
            voiceService.startListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Отменяем регистрацию BroadcastReceiver
        if (wifiScanReceiver != null) {
            try {
                unregisterReceiver(wifiScanReceiver);
            } catch (IllegalArgumentException e) {
                // Receiver не был зарегистрирован
            }
        }

        // При сворачивании приложения останавливаем прослушивание
        if (voiceService != null) {
            addLog("Приостановка прослушивания...");
            voiceService.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (wifiScanReceiver != null) {
            try {
                unregisterReceiver(wifiScanReceiver);
            } catch (IllegalArgumentException e) {
                // Игнорируем
            }
        }

        if (tinyLLMProcessor != null) {
            tinyLLMProcessor.release();
        }

        if (aiProcessor != null) {
            aiProcessor.release();
        }
        if (voiceService != null) {
            voiceService.release();
        }
        if (soundManager != null) {
            soundManager.release();
        }
        if (commandTimer != null) {
            commandTimer.cancel();
        }
        addLog("Приложение закрыто");
    }

    private String formatParamsForDisplay(JSONObject params) {
        if (params == null || params.length() == 0) {
            return "нет";
        }

        try {
            StringBuilder sb = new StringBuilder();
            java.util.Iterator<String> keys = params.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                String value = params.optString(key, "");
                sb.append(key).append("=").append(value);
                if (keys.hasNext()) {
                    sb.append(", ");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            return params.toString();
        }
    }
}