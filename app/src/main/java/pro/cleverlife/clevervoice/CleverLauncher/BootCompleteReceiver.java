package pro.cleverlife.clevervoice.CleverLauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import pro.cleverlife.clevervoice.MainActivity;

public class BootCompleteReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompleteReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i(TAG, "Boot completed received, starting launcher...");

            Intent launchIntent = new Intent(context, MainActivity.class);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(launchIntent);

            executeStartupCommands(context);
        }
    }

    private void executeStartupCommands(Context context) {
        if (MainActivity.getInstance() != null) {
            MainActivity.getInstance().executeCommand("am start -n pro.cleverlife.cleverroom/.SHActivity");
        }
    }
}