package pro.cleverlife.clevervoice.service;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

public class SoundManager {
    private static final String TAG = "SoundManager";
    private Context context;
    private MediaPlayer successPlayer;
    private MediaPlayer errorPlayer;
    private boolean isInitialized = false;

    public SoundManager(Context context) {
        this.context = context;
        Log.i(TAG, "SoundManager initialized");
    }

    public void initializeWithSounds(int successSoundResId, int errorSoundResId) {
        try {
            // Инициализация звуков используя переданные resource ID
            successPlayer = MediaPlayer.create(context, successSoundResId);
            errorPlayer = MediaPlayer.create(context, errorSoundResId);

            // Настройка громкости
            if (successPlayer != null) {
                successPlayer.setVolume(0.7f, 0.7f);
            }
            if (errorPlayer != null) {
                errorPlayer.setVolume(0.7f, 0.7f);
            }

            isInitialized = (successPlayer != null && errorPlayer != null);

            if (isInitialized) {
                Log.i(TAG, "SoundManager fully initialized with sound files");
            } else {
                Log.w(TAG, "SoundManager initialized but some sound files are missing");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing sound players: " + e.getMessage());
        }
    }

    public void playActivationSound() {
        Log.d(TAG, "Воспроизведение звука активации");
        playSuccessSound();
    }

    public void playSuccessSound() {
        if (!isInitialized) {
            Log.w(TAG, "SoundManager not initialized with sound files");
            return;
        }
        Log.d(TAG, "Воспроизведение успешного звука");
        playSound(successPlayer, "success");
    }

    public void playErrorSound() {
        if (!isInitialized) {
            Log.w(TAG, "SoundManager not initialized with sound files");
            return;
        }
        Log.d(TAG, "Воспроизведение звука ошибки");
        playSound(errorPlayer, "error");
    }

    private void playSound(MediaPlayer player, String soundType) {
        if (player == null) {
            Log.w(TAG, "Player for " + soundType + " sound is null");
            return;
        }

        try {
            if (player.isPlaying()) {
                player.seekTo(0);
            } else {
                player.start();
            }
            Log.d(TAG, "Sound played: " + soundType);
        } catch (Exception e) {
            Log.e(TAG, "Error playing " + soundType + " sound: " + e.getMessage());
        }
    }

    public void release() {
        if (successPlayer != null) {
            successPlayer.release();
            successPlayer = null;
        }
        if (errorPlayer != null) {
            errorPlayer.release();
            errorPlayer = null;
        }
        isInitialized = false;
        Log.i(TAG, "SoundManager resources released");
    }

    public boolean isInitialized() {
        return isInitialized;
    }
}