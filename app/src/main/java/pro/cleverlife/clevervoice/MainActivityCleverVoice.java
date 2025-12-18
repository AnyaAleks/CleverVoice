package pro.cleverlife.clevervoice;

import android.Manifest;
import android.content.Intent;
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

import pro.cleverlife.clevervoice.AI.TinyLLMProcessor;
import pro.cleverlife.clevervoice.AI.VoskAIProcessor;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_clever_voice);

        // Инициализация AI процессоров
        aiProcessor = new VoskAIProcessor(this);
        tinyLLMProcessor = new TinyLLMProcessor(this);

        // Загружаем модель в фоновом потоке
        new Thread(() -> {
            boolean initialized = tinyLLMProcessor.initialize();
            runOnUiThread(() -> {
                if (initialized) {
                    addLog("✓ AI процессоры инициализированы");

                    // ПРОВЕРЯЕМ, ЗАГРУЖЕНА ЛИ МОДЕЛЬ
                    boolean aiLoaded = tinyLLMProcessor.isModelLoaded();
                    addLog("AI модель: " + (aiLoaded ? "ЗАГРУЖЕНА " : "НЕ загружена"));

                    if (aiLoaded) {
                        addLog("TinyLLaMA будет исправлять ошибки распознавания");
                    } else {
                        addLog("⚠Будут использоваться только простые правила");

                        // Тестовая команда для проверки
                        addLog("Тестовая команда...");
                        TinyLLMProcessor.CommandResult testResult =
                                tinyLLMProcessor.understandCommand("яркость максимум");
                        addLog("Результат: " + testResult.command + " -> " + testResult.action);
                    }
                } else {
                    addLog("✗ Ошибка инициализации AI процессоров");
                }
            });
        }).start();

        initViews();
        checkPermissions();
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
                    startCommandTimer(); // Запускаем таймер
                });
            }

            @Override
            public void onCommandReceived(String command) {
                runOnUiThread(() -> {
                    addLog(">>> КОМАНДА: \"" + command + "\"");
                    // Останавливаем таймер при получении команды
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

                    // Останавливаем таймер при ошибке
                    if (commandTimer != null) {
                        commandTimer.cancel();
                    }

                    // Автоматически перезапускаем прослушивание после ошибки
                    if (isListening) {
                        addLog("Перезапуск прослушивания...");
                        voiceService.startListening();
                    }
                });
            }

            @Override
            public void onInitialized() {
                runOnUiThread(() -> {
                    addLog("+ Система голосового распознавания готова");
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
                // Оставляем пустым или добавим минимальную логику
            }

            @Override
            public void onSilenceDetected() {
                // Оставляем пустым
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
            addLog("=== ЗАПУСК ПРОСЛУШИВАНИАЯ ===");
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

                // Обновляем лог каждые 5 секунд
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

                // Сбрасываем активацию в сервисе
                if (voiceService != null) {
                    voiceService.resetActivation();
                }

                // Возвращаемся в режим ожидания активации
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

        // Отменяем таймер команды
        if (commandTimer != null) {
            commandTimer.cancel();
        }

        // ИСПРАВЛЕНИЕ: Используем VoskAIProcessor вместо TinyLLMProcessor
        if (aiProcessor != null) {
            addLog("Передаю команду в VoskAIProcessor");

            // Создаем колбэк для обработки результата
            VoskAIProcessor.SimpleCallback callback = new VoskAIProcessor.SimpleCallback() {
                @Override
                public void onCommandProcessed(boolean success) {
                    runOnUiThread(() -> {
                        if (success) {
                            addLog("✅ Команда успешно выполнена через VoskAIProcessor");
                            if (soundManager.isInitialized()) {
                                soundManager.playSuccessSound();
                            }
                        } else {
                            addLog("Не удалось выполнить команду через VoskAIProcessor");
                            if (soundManager.isInitialized()) {
                                soundManager.playErrorSound();
                            }
                        }

                        // Возвращаемся в режим ожидания активации
                        if (isListening) {
                            addLog("Ожидание новой активации...");
                            statusText.setText("Скажите 'Клевер' для новой команды...");
                            voiceService.startListening();
                        }
                    });
                }
            };

            // Вызываем обработку команды через VoskAIProcessor
            aiProcessor.processRecognizedText(command, callback);
        } else {
            addLog("VoskAIProcessor не инициализирован!");
            // Fallback на старый метод
            useTinyLLMProcessorFallback(command);
        }
    }

    // Старый метод для совместимости
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

                        // Воспроизводим звук
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

                    // Возвращаемся в режим ожидания активации
                    if (isListening) {
                        addLog("Ожидание новой активации...");
                        statusText.setText("Скажите 'Клевер' для новой команды...");
                        voiceService.startListening();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    addLog("Ошибка AI обработки: " + e.getMessage());
                    e.printStackTrace();
                    if (soundManager.isInitialized()) {
                        soundManager.playErrorSound();
                    }
                });
            }
        }).start();
    }

    private void addLog(String message) {
        runOnUiThread(() -> {
            // Добавляем сообщение в StringBuilder
            logBuilder.append(message).append("\n");

            // Обновляем TextView
            logText.setText(logBuilder.toString());

            // Правильная прокрутка вниз
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

    @Override
    protected void onResume() {
        super.onResume();
        // При возвращении в приложение перезапускаем прослушивание если оно было активно
        if (isListening && voiceService != null) {
            addLog("Возобновление прослушивания...");
            voiceService.startListening();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // При сворачивании приложения останавливаем прослушивание
        if (voiceService != null) {
            addLog("Приостановка прослушивания...");
            voiceService.stopListening();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Освободите ресурсы TinyLLaMA
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
}