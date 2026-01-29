package pro.cleverlife.clevervoice.service;

import android.content.Context;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.vosk.Model;
import org.vosk.Recognizer;

public class VoiceRecognitionService {
    private static final String TAG = "VoiceRecognitionService";

    private Model model;
    private Recognizer recognizer;
    private AudioRecordService audioService;
    private ActivationListener activationListener;

    private boolean isListening = false;
    private boolean isInCommandMode = false;
    private boolean isInitialized = false;

    private boolean isActivated = false;

    public interface ActivationListener {
        void onActivationWordDetected();
        void onCommandReceived(String command);
        void onError(String error);
        void onInitialized();
        void onPartialResult(String partialText);
        void onSpeechDetected();
        void onSilenceDetected();
    }

    public VoiceRecognitionService(Context context) {
        initVosk(context);
        audioService = new AudioRecordService();
    }

    private void initVosk(Context context) {
        new Thread(() -> {
            try {
                Log.i(TAG, "Starting Vosk initialization...");

                // Копирование модели из assets
                String modelPath = copyModelFromAssets(context);
                Log.d(TAG, "Model path: " + modelPath);

                // Проверка на корректное сканирование
                File modelDir = new File(modelPath);
                if (!modelDir.exists() || !modelDir.isDirectory()) {
                    throw new IOException("Model directory not found: " + modelPath);
                }

                // Детальная проверка файлов модели
                File[] modelFiles = modelDir.listFiles();
                if (modelFiles == null || modelFiles.length == 0) {
                    throw new IOException("Model directory is empty: " + modelPath);
                }

                Log.d(TAG, "Found " + modelFiles.length + " files in model directory");
                for (File file : modelFiles) {
                    Log.d(TAG, " - " + file.getName() + " (" + file.length() + " bytes)");
                }

                // Проверка на наличие обязательных файлов
                if (!checkRequiredFiles(modelDir)) {
                    throw new IOException("Required model files are missing");
                }

                // Загрузка модели
                Log.d(TAG, "Loading Vosk model...");
                model = new Model(modelPath);

                // Создание распознавателя
                Log.d(TAG, "Creating recognizer...");
                recognizer = new Recognizer(model, 16000.0f);

                isInitialized = true;
                Log.i(TAG, "Vosk initialized successfully");

                if (activationListener != null) {
                    activationListener.onInitialized();
                }

            } catch (Exception e) {
                Log.e(TAG, "Error initializing Vosk", e);
                if (activationListener != null) {
                    activationListener.onError("Initialization failed: " + e.getMessage());
                }
            }
        }).start();
    }

    private boolean checkRequiredFiles(File modelDir) {
        String[] requiredFiles = {
                "am/final.mdl",
                "conf/mfcc.conf"
        };

        for (String filePath : requiredFiles) {
            File file = new File(modelDir, filePath);
            if (!file.exists()) {
                Log.e(TAG, "Required file missing: " + file.getAbsolutePath());
                return false;
            }
        }
        return true;
    }

    private String copyModelFromAssets(Context context) throws IOException {
        File modelDir = new File(context.getFilesDir(), "vosk-model-ru");

        // Всегда пересоздается папка
        if (modelDir.exists()) {
            deleteRecursive(modelDir);
        }

        boolean created = modelDir.mkdirs();
        if (!created) {
            throw new IOException("Failed to create model directory: " + modelDir.getAbsolutePath());
        }

        Log.d(TAG, "Created model directory: " + modelDir.getAbsolutePath());

        // Копирование содержимого вложенной папки напрямую в modelDir
        copyModelContents(context, "vosk-model-ru/vosk-model-small-ru-0.22", modelDir.getAbsolutePath());

        return modelDir.getAbsolutePath();
    }

    private void copyModelContents(Context context, String assetPath, String destinationPath) throws IOException {
        Log.d(TAG, "Copying model contents from: " + assetPath + " to: " + destinationPath);

        String[] files = context.getAssets().list(assetPath);
        if (files == null) {
            throw new IOException("Asset path not found: " + assetPath);
        }

        if (files.length == 0) {
            throw new IOException("Asset directory is empty: " + assetPath);
        }

        Log.d(TAG, "Found " + files.length + " items in assets: " + assetPath);

        for (String file : files) {
            String newAssetPath = assetPath + "/" + file;
            String newDestinationPath = destinationPath + "/" + file;

            String[] subFiles = context.getAssets().list(newAssetPath);
            if (subFiles != null && subFiles.length > 0) {
                // Директория - рекурсивное копирование
                File destDir = new File(newDestinationPath);
                if (!destDir.exists()) {
                    destDir.mkdirs();
                }
                copyModelContents(context, newAssetPath, newDestinationPath);
            } else {
                // Файл - копируется
                copyFile(context, newAssetPath, newDestinationPath);
            }
        }
    }

    private void deleteRecursive(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteRecursive(child);
                }
            }
        }
        fileOrDirectory.delete();
    }

    private void copyFile(Context context, String assetPath, String destinationPath) throws IOException {
        File destinationFile = new File(destinationPath);
        File parentDir = destinationFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }

        Log.d(TAG, "Copying file: " + assetPath + " to " + destinationPath);

        try (InputStream in = context.getAssets().open(assetPath);
             OutputStream out = new java.io.FileOutputStream(destinationFile)) {

            byte[] buffer = new byte[1024];
            int length;
            long totalBytes = 0;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
                totalBytes += length;
            }
            Log.d(TAG, "Copied " + totalBytes + " bytes for " + assetPath);
        } catch (IOException e) {
            Log.e(TAG, "Error copying file: " + assetPath, e);
            throw e;
        }
    }

    public void startListening() {
        if (!isInitialized) {
            Log.e(TAG, "Vosk not initialized yet");
            if (activationListener != null) {
                activationListener.onError("System not ready");
            }
            return;
        }

        if (recognizer == null) {
            Log.e(TAG, "Recognizer not available");
            if (activationListener != null) {
                activationListener.onError("Recognizer not available");
            }
            return;
        }

        isListening = true;
        isActivated = false;
        audioService.startRecording(new AudioRecordService.AudioCallback() {
            @Override
            public void onAudioBuffer(short[] buffer) {
                if (isListening && recognizer != null) {
                    try {
                        if (recognizer.acceptWaveForm(buffer, buffer.length)) {
                            String result = recognizer.getResult();
                            processRecognitionResult(result);
                        } else {
                            String partialResult = recognizer.getPartialResult();
                            processPartialResult(partialResult);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing audio buffer", e);
                    }
                }
            }
        });

        Log.i(TAG, "Voice recognition started (waiting for activation)");
    }

    private boolean isSpeechDetected(short[] buffer) {
        long sum = 0;
        for (short sample : buffer) {
            sum += Math.abs(sample);
        }
        double average = sum / (double) buffer.length;
        return average > 1000; // Пороговое значение, можно настроить
    }

    public void stopListening() {
        isListening = false;
        audioService.stopRecording();
        Log.i(TAG, "Voice recognition stopped");
    }

    private boolean isActivationWord(String text) {
        String lower = text.toLowerCase().trim();

        // Проверяем разные варианты написания
        return lower.contains("клевер") ||
                lower.contains("кливер") ||
                lower.contains("кливэр") ||
                lower.contains("clever") ||
                lower.matches(".*к[лэи]вер.*") ||
                lower.contains(" clever ");
    }

    private void processRecognitionResult(String result) {
        try {
            JSONObject jsonResult = new JSONObject(result);
            String text = jsonResult.getString("text");
            Log.d(TAG, "Recognized text: " + text);

            if (!text.isEmpty()) {
                if (isActivationWord(text) && !isInCommandMode) {
                    // Обнаружено активационное слово
                    isInCommandMode = true;
                    isActivated = true; // Активируем режим команд
                    Log.i(TAG, "Activation word detected");
                    if (activationListener != null) {
                        activationListener.onActivationWordDetected();
                    }
                } else if (isInCommandMode && isActivated) {
                    // Обнаружена команда после активации
                    Log.i(TAG, "Command received: " + text);
                    if (activationListener != null) {
                        activationListener.onCommandReceived(text);
                    }
                    // После команды сбрасываем режим
                    isInCommandMode = false;
                    isActivated = false;
                } else if (!isActivated) {
                    // Игнорируем речь до активации
                    Log.d(TAG, "Speech ignored (waiting for activation): " + text);
                    if (activationListener != null) {
                        activationListener.onPartialResult("[ожидание 'Клевер']");
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing JSON result", e);
        }
    }

    private void processPartialResult(String partialResult) {
        try {
            JSONObject jsonResult = new JSONObject(partialResult);
            String partialText = jsonResult.getString("partial");
            if (!partialText.isEmpty()) {
                Log.d(TAG, "Partial recognition: " + partialText);

                // Логируем частичное распознавание только в режиме активации
                if (activationListener != null) {
                    if (isActivated) {
                        activationListener.onPartialResult(partialText);
                    } else {
                        // Показываем, что ждем активацию
                        if (partialText.toLowerCase().contains("клевер")) {
                            activationListener.onPartialResult("[обнаружено 'Клевер'...]");
                        } else {
                            activationListener.onPartialResult("[ожидание активации...]");
                        }
                    }
                }

                // Обработка частичных результатов для активационного слова
                if (partialText.toLowerCase().contains("клевер") && !isInCommandMode) {
                    Log.d(TAG, "Activation word detected in partial result");
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing partial result", e);
        }
    }

    public void resetActivation() {
        isInCommandMode = false;
        isActivated = false;
        if (recognizer != null) {
            recognizer.reset();
        }
        Log.d(TAG, "Activation reset");
    }

    public void setActivationListener(ActivationListener listener) {
        this.activationListener = listener;
    }

    public boolean isInitialized() {
        return isInitialized;
    }

    public void release() {
        stopListening();
        if (recognizer != null) {
            recognizer.close();
            recognizer = null;
        }
        if (model != null) {
            model.close();
            model = null;
        }
        Log.i(TAG, "Vosk resources released");
    }
}