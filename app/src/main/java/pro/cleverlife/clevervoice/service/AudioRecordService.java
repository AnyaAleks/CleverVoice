package pro.cleverlife.clevervoice.service;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioRecordService {
    private static final String TAG = "AudioRecordService";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 4096;

    private AudioRecord audioRecord;
    private Thread recordingThread;
    private boolean isRecording = false;

    public interface AudioCallback {
        void onAudioBuffer(short[] buffer);
    }

    private AudioCallback audioCallback;

    public void startRecording(AudioCallback callback) {
        this.audioCallback = callback;

        if (isRecording) {
            stopRecording();
        }

        try {
            int minBufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
            int actualBufferSize = Math.max(BUFFER_SIZE, minBufferSize);

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.VOICE_RECOGNITION,
                    SAMPLE_RATE,
                    CHANNEL_CONFIG,
                    AUDIO_FORMAT,
                    actualBufferSize
            );

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed");
                return;
            }

            audioRecord.startRecording();
            isRecording = true;

            startRecordingThread();
            Log.i(TAG, "Audio recording started");

        } catch (SecurityException e) {
            Log.e(TAG, "Microphone permission denied", e);
        } catch (Exception e) {
            Log.e(TAG, "Audio recording start failed", e);
        }
    }

    private void startRecordingThread() {
        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                short[] buffer = new short[BUFFER_SIZE];

                while (isRecording && audioRecord != null &&
                        audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {

                    int bytesRead = audioRecord.read(buffer, 0, buffer.length);

                    if (bytesRead > 0 && audioCallback != null) {
                        audioCallback.onAudioBuffer(buffer);
                    }

                    //Снижение нагрузки
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }
        });

        recordingThread.start();
    }

    public void stopRecording() {
        isRecording = false;

        if (recordingThread != null) {
            recordingThread.interrupt();
            try {
                recordingThread.join(1000);
            } catch (InterruptedException e) {
                Log.e(TAG, "Error stopping recording thread", e);
            }
            recordingThread = null;
        }

        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error releasing AudioRecord", e);
            }
            audioRecord = null;
        }

        Log.i(TAG, "Audio recording stopped");
    }

    public boolean isRecording() {
        return isRecording;
    }

    public int getSampleRate() {
        return SAMPLE_RATE;
    }
}