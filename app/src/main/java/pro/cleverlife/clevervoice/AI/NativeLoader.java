package pro.cleverlife.clevervoice.AI;

import android.content.Context;
import android.util.Log;

public class NativeLoader {
    private static final String TAG = "NativeLoader";
    private static boolean libraryLoaded = false;

    public static boolean loadLibrary() {
        if (libraryLoaded) {
            return true;
        }

        try {
            System.loadLibrary("llama-wrapper");
            libraryLoaded = true;
            Log.i(TAG, "Native library loaded successfully");
            return true;
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library", e);
            return false;
        }
    }

    public static boolean isLibraryLoaded() {
        return libraryLoaded;
    }
}
