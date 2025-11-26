package pro.cleverlife.clevervoice.CleverLauncher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import pro.cleverlife.clevervoice.MainActivity;

public class AppInstallWatcher extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        // ssp is the package name
        String packageName = intent.getData().getSchemeSpecificPart();
        Log.w("APP", action + "   |   " + packageName);
        if (Intent.ACTION_PACKAGE_ADDED.equals(action) || Intent.ACTION_PACKAGE_REPLACED.equals(action)) {
            IAppInstallWatcher appWatcher = MainActivity.getInstance();
            if (appWatcher != null) {
                appWatcher.onAppInstalled(packageName);
            }
        } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action) || Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(action)) {
            IAppInstallWatcher appWatcher = MainActivity.getInstance();
            if (appWatcher != null) {
                appWatcher.onAppRemoved(packageName);
            }
        }
    }

    public interface IAppInstallWatcher {

        void onAppInstalled(String packageName);

        void onAppRemoved(String packageName);
    }
}
