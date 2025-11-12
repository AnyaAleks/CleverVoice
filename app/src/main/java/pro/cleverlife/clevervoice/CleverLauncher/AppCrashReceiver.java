package pro.cleverlife.clevervoice.CleverLauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AppCrashReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String packageName = intent.getStringExtra("package_name");
        Log.i("WE CRASHED", "packageName");
        if ("pro.cleverlife.cleverroom".equals(packageName)) {
            Log.i("WE CRASHED", "equals");
            // Перезапуск CleverHome
            Intent launchIntent = context.getPackageManager().getLaunchIntentForPackage("pro.cleverlife.cleverroom");
            if (launchIntent != null) {
                Log.i("WE CRASHED", "launchIntent != null");
                context.startActivity(launchIntent);
            }
        }
    }
}