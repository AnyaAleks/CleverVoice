package pro.cleverlife.clevervoice.service;

import android.content.Context;
import android.util.Log;

public class SoundManager {
    private static final String TAG = "SoundManager";
    private Context context;

    public SoundManager(Context context) {
        this.context = context;
        Log.i(TAG, "SoundManager initialized");
    }

    public void playActivationSound() {
        Log.d(TAG, "Воспроизведение звука активации");
        // Можно добавить реальные звуки позже
    }

    public void playSuccessSound() {
        Log.d(TAG, "Воспроизведение успешного звука");
        // Можно добавить реальные звуки позже
    }

    public void playErrorSound() {
        Log.d(TAG, "Воспроизведение звука ошибки");
        // Можно добавить реальные звуки позже
    }
}