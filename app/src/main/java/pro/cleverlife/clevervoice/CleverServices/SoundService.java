package pro.cleverlife.clevervoice.CleverServices;

import android.content.Context;
import android.media.AudioManager;

public class SoundService {

    private static AudioManager audioManager;

    public SoundService(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public static int getCurrentMediaVolume(Context _context) {
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
           return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        }
        return -1;
    }

    public static int getCurrentRingVolume(Context _context) {
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
           return audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
        }
        return -1;
    }

    public static int getCurrentAlarmVolume(Context _context) {
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
           return audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
        }
        return -1;
    }

    public static int getCurrentNotificationVolume(Context _context) {
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
           return audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);
        }
        return -1;
    }

    public static void setMediaVolume(Context _context, int newValue) {
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int newVolume = Math.min(newValue, getMaxMediaVolume(_context));
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
        }
    }

    public static void setRingVolume(Context _context, int newValue) {
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int newVolume = Math.min(newValue, getMaxRingVolume(_context));
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, newVolume, 0);
        }
    }

    public static void setAlarmVolume(Context _context, int newValue) {
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int newVolume = Math.min(newValue, getMaxAlarmVolume(_context));
            audioManager.setStreamVolume(AudioManager.STREAM_ALARM, newVolume, 0);
        }
    }

    public static void setNotificationVolume(Context _context, int newValue) {
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int newVolume = Math.min(newValue, getMaxNotificationVolume(_context));
            audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, newVolume, 0);
        }
    }

    public  static int getMaxMediaVolume(Context _context){
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    public  static int getMaxRingVolume(Context _context){
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);
    }

    public  static int getMaxAlarmVolume(Context _context){
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
    }

    public  static int getMaxNotificationVolume(Context _context){
        audioManager = (AudioManager) _context.getSystemService(Context.AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION);
    }
}
