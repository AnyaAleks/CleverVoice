package pro.cleverlife.clevervoice;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

import pro.cleverlife.clevervoice.CleverLauncher.AppCrashReceiver;
import pro.cleverlife.clevervoice.CleverLauncher.AppInstallWatcher;
import pro.cleverlife.clevervoice.R;

public class MainActivity extends Activity implements AppInstallWatcher.IAppInstallWatcher{

    Button buttonStart;
    Button buttonUpdate;
    Button buttonUpdateLauncher;
    Button buttonReloadApplication;
    Button buttonReloadDevice;
    Button buttonOpenCleverVoice;
    TextView textVersion;
    TextView textDownlanding;

    private static MainActivity sMyself;
    private static final String TAG = "ADBCommands";
    //ADWApiManager mADWApiManager;

    // Для 8-дюймовых устройств
    private static final String APK_URL_8INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_8/release/binupdate/CleverRoom.apk";
    private static final String APK_URL_TEST_8INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_8/test/binupdate/CleverRoom.apk";
    private static final String VERSION_URL_8INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_8/release/versionRoom.txt";
    private static final String VERSION_URL_TEST_8INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_8/test/versionRoom.txt";


    // Для 4-дюймовых устройств
    private static final String APK_URL_4INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_4/release/binupdate/CleverRoom.apk";
    private static final String APK_URL_TEST_4INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_4/test/binupdate/CleverRoom.apk";
    private static final String VERSION_URL_4INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_4/release/versionRoom.txt";
    private static final String VERSION_URL_TEST_4INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_4/test/versionRoom.txt";


    private static final String CLEVER_FILE = "CleverHome.apk";
    //private static final String HASH_URL = "http://update.cleverlife.pro:19199/updates/clever/android_panel_8/release/hashsumm_from_apk.txt";
    //private static final String HASH_FILE = "hashsumm.txt";
    private static final String VERSION_FILE = "versionCleverHome.txt";
    private static final String ADB_Download = "/storage/emulated/0/Android/data/pro.cleverlife.cleverlauncher/files/Download/";
    private static final String ADB_CleverHome = "/system/app/CleverHome/";
    private static final String UPDATE_CHANEL_FILE = "/system/app/CleverHome/updateChanel.txt";
    private static final String UPDATE_CHANEL_LAUNCHER_FILE = "/system/app/CleverHome/updateChanelLauncher.txt";
    private static final String LAUNCHER_FILE = "launcher.apk";
    private static final String VERSION_FILE_LAUNCHER = "versionLauncher.txt";

    //для всех устройств, но для 8 дюймов
    private static final String APK_LAUNCHER_8INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_8/release/sysupdate/CleverLauncher.apk";
    private static final String APK_LAUNCHER_TEST_8INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_8/test/sysupdate/CleverLauncher.apk";
    private static final String VERSION_APK_LAUNCHER_8INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_8/release/versionLauncher.txt";
    private static final String VERSION_APK_LAUNCHER_TEST_8INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_8/test/versionLauncher.txt";

    //для всех устройств, но для 4 дюймов
    private static final String APK_LAUNCHER_4INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_4/release/sysupdate/CleverLauncher.apk";
    private static final String APK_LAUNCHER_TEST_4INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_4/test/sysupdate/CleverLauncher.apk";
    private static final String VERSION_APK_LAUNCHER_4INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_4/release/versionLauncher.txt";
    private static final String VERSION_APK_LAUNCHER_TEST_4INCH = "http://update.cleverlife.pro:19199/updates/clever/android_panel_4/test/versionLauncher.txt";

    public boolean isTest_APK = false;
    public boolean isTest_LAUNCHER = false;
    public int oldVersionAPK=-1;
    public int oldVersionAPKLauncher=-1;
    public int newVersionAPK=-1;
    private Handler handler = new Handler();
    private Runnable hourlyTask;


    public static MainActivity getInstance() {
        return sMyself;
    }

    public boolean startTimer = false;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sMyself = this;
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        //Log.i("HelloWorld", "We starting_2...");

//        mADWApiManager = new ADWApiManager(this);
//        mADWApiManager.OpenOrCloseNav(true);

        //! textDownlanding = (TextView) findViewById(R.id.textDownlanding);


        textVersion = (TextView) findViewById(R.id.textVersion);
        readVersionFileOld(VERSION_FILE_LAUNCHER, "launcher");
        textVersion.setText("Версия: " + oldVersionAPKLauncher);

        //Таймер НА ОБНОВЛЕНИЯ С СЕРВЕРА
        //initHourlyTimer();

        //Регистрация ресивера для поднятия приложения при вылете
        IntentFilter filter = new IntentFilter("android.intent.action.PACKAGE_RESTARTED");
        filter.addDataScheme("package");
        registerReceiver(new AppCrashReceiver(), filter);

        //Давать разрешения другим приложениям
        executeCommand("pm grant pro.cleverlife.cleverroom android.permission.ACCESS_FINE_LOCATION");
        executeCommand("pm grant pro.cleverlife.cleverroom android.permission.CAMERA");
        executeCommand("pm grant pro.cleverlife.cleverroom android.permission.RECORD_AUDIO");
        executeCommand("pm grant pro.cleverlife.cleverroom android.permission.ACCESS_COARSE_LOCATION");
        executeCommand("pm grant pro.cleverlife.cleverroom android.permission.ACCESS_BACKGROUND_LOCATION");
        executeCommand("pm grant pro.cleverlife.cleverroom android.permission.READ_EXTERNAL_STORAGE");
        executeCommand("pm grant pro.cleverlife.cleverroom android.permission.WRITE_EXTERNAL_STORAGE");

        //включение wifi и gps на старте
        executeCommand("svc wifi enable");
        executeCommand("settings put secure location_mode 3");
        //executeCommand("settings put secure location_providers_allowed +gps");

//        readHashSummOld();
//        readHashSummNew();



        buttonStart = (Button) findViewById(R.id.buttonStart);
        buttonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //seriaADBInstall();
                executeCommand("am start -n pro.cleverlife.cleverroom/.SHActivity");
            }
        });

        buttonUpdate = (Button) findViewById(R.id.buttonUpdate);
        buttonUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Проверка файла типа установки из канала обновлений (test, debug, release)

                //Устанавливаем канал обновления
                String updateChanelLine = readTXTFile(UPDATE_CHANEL_FILE);
                isTest_APK = Objects.equals(updateChanelLine, "test");
                Log.i("UPDATEVERSION", "Update chanel app: " + updateChanelLine + " = " + isTest_APK);
                downloadFile(VERSION_FILE, getVersionUrl(), 1);
//                if(isTest_APK){
//                    downloadFile(VERSION_FILE, VERSION_URL_TEST_8INCH, 1);
//                } else{
//                    downloadFile(VERSION_FILE, VERSION_URL_8INCH, 1);
//                }
            }
        });

        buttonUpdateLauncher = (Button) findViewById(R.id.buttonUpdateLauncher);
        buttonUpdateLauncher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Устанавливаем канал обновления
                String updateChanelLine = readTXTFile(UPDATE_CHANEL_LAUNCHER_FILE);
                isTest_LAUNCHER = Objects.equals(updateChanelLine, "test");
                Log.i("UPDATEVERSION_LAUNCHER", "Update chanel launcher: " + updateChanelLine + " = " + isTest_LAUNCHER);
                //isTest_LAUNCHER
                //Проверка файла типа установки из канала обновлений (test, debug, release)
                downloadFile(VERSION_FILE_LAUNCHER, getVersionUrlLauncher(), 1);
//                if(isTest_LAUNCHER){
//                    downloadFile(VERSION_FILE_LAUNCHER, VERSION_APK_LAUNCHER_TEST_8INCH, 1);
//                } else{
//                    downloadFile(VERSION_FILE_LAUNCHER, VERSION_APK_LAUNCHER_8INCH, 1);
//                }
            }
        });

        buttonReloadApplication = (Button) findViewById(R.id.buttonReloadApplication);
        buttonReloadApplication.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                executeCommand("am force-stop pro.cleverlife.cleverroom");
                executeCommand("am start -n pro.cleverlife.cleverroom/.SHActivity");
            }
        });

        buttonReloadDevice = (Button) findViewById(R.id.buttonReloadDevice);
        buttonReloadDevice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                executeCommand("reboot");
            }
        });

        buttonOpenCleverVoice = (Button) findViewById(R.id.buttonOpenCleverVoice);
        buttonOpenCleverVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //executeCommand("reboot");
                Intent intent = new Intent(MainActivity.this, MainActivityCleverVoice.class);
                startActivity(intent);
            }
        });


        //Установка времени
        //executeCommand("setprop persist.sys.timezone Europe/Moscow");
        //persist.sys.language ru
        //setprop persist.sys.country RU
        //executeCommand("reboot");
    }

    public boolean updateLauncher = false;
    private void initHourlyTimer() {
        hourlyTask = new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "Hourly task executed -> Let's downland apk version");

                if(startTimer){
                    if(updateLauncher){
                        //скачиваем новую версию лаунчера
                        downloadFile(VERSION_FILE_LAUNCHER, getVersionUrlLauncher(), 1);
                        updateLauncher = false;
                    } else{
                        //Скачивание версии будущего apk
                        //Сравнение 2х версий
                        //если версия новая выше -> перенос файла
                        downloadFile(VERSION_FILE, getVersionUrl(), 1);
                        updateLauncher = true;
                    }
                }

                startTimer = true;

                // Повторно запускаем задачу через час
                handler.postDelayed(this, 30 * 60 * 1000);
                //handler.postDelayed(this,  1000);
            }
        };

        // Запускаем задачу в первый раз
        handler.post(hourlyTask);
    }

    private boolean is8InchDevice() {
        //640 на 4 дюймах, 800 на 8 дюймах
        int smallestWidthDp = getResources().getConfiguration().smallestScreenWidthDp;
        Log.d("ScreenSize", "The screen width of device: " + smallestWidthDp + "dp");
        return smallestWidthDp >= 750;
    }

    private String getApkUrl() {
        if (isTest_APK) {
            if (is8InchDevice()) {
                Log.d("ApkUrl", "Using TEST 8-inch APK URL: " + APK_URL_TEST_8INCH);
                return APK_URL_TEST_8INCH;
            } else {
                Log.d("ApkUrl", "Using TEST 4-inch APK URL: " + APK_URL_TEST_4INCH);
                return APK_URL_TEST_4INCH;
            }
        } else {
            if (is8InchDevice()) {
                Log.d("ApkUrl", "Using PROD 8-inch APK URL: " + APK_URL_8INCH);
                return APK_URL_8INCH;
            } else {
                Log.d("ApkUrl", "Using PROD 4-inch APK URL: " + APK_URL_4INCH);
                return APK_URL_4INCH;
            }
        }
    }

    private String getApkUrlLauncher() {
        if (isTest_LAUNCHER) {
            if (is8InchDevice()) {
                Log.d("ApkUrl", "Using TEST 8-inch APK LAUNCHER URL: " + APK_LAUNCHER_TEST_8INCH);
                return APK_LAUNCHER_TEST_8INCH;
            } else {
                Log.d("ApkUrl", "Using TEST 4-inch APK LAUNCHER URL: " + APK_LAUNCHER_TEST_4INCH);
                return APK_LAUNCHER_TEST_4INCH;
            }
        } else {
            if (is8InchDevice()) {
                Log.d("ApkUrl", "Using PROD 8-inch APK LAUNCHER URL: " + APK_LAUNCHER_8INCH);
                return APK_LAUNCHER_8INCH;
            } else {
                Log.d("ApkUrl", "Using PROD 4-inch APK LAUNCHER URL: " + APK_LAUNCHER_4INCH);
                return APK_LAUNCHER_4INCH;
            }
        }
    }

    private String getVersionUrl() {
        if (isTest_APK) {
            if (is8InchDevice()) {
                Log.d("VersionUrl", "Using TEST 8-inch URL: " + VERSION_URL_TEST_8INCH);
                return VERSION_URL_TEST_8INCH;
            } else {
                Log.d("VersionUrl", "Using TEST 4-inch URL: " + VERSION_URL_TEST_4INCH);
                return VERSION_URL_TEST_4INCH;
            }
        } else {
            if (is8InchDevice()) {
                Log.d("VersionUrl", "Using PROD 8-inch URL: " + VERSION_URL_8INCH);
                return VERSION_URL_8INCH;
            } else {
                Log.d("VersionUrl", "Using PROD 4-inch URL: " + VERSION_URL_4INCH);
                return VERSION_URL_4INCH;
            }
        }
    }

    private String getVersionUrlLauncher() {
        if (isTest_LAUNCHER) {
            if (is8InchDevice()) {
                Log.d("LauncherUrl", "Using TEST Launcher 8-inch URL: " + VERSION_APK_LAUNCHER_TEST_8INCH);
                return VERSION_APK_LAUNCHER_TEST_8INCH;
            } else {
                Log.d("LauncherUrl", "Using TEST Launcher 4-inch URL: " + VERSION_APK_LAUNCHER_TEST_4INCH);
                return VERSION_APK_LAUNCHER_TEST_4INCH;
            }
        } else {
            if (is8InchDevice()) {
                Log.d("LauncherUrl", "Using PROD Launcher 8-inch URL: " + VERSION_APK_LAUNCHER_8INCH);
                return VERSION_APK_LAUNCHER_8INCH;
            } else {
                Log.d("LauncherUrl", "Using PROD Launcher 4-inch URL: " + VERSION_APK_LAUNCHER_4INCH);
                return VERSION_APK_LAUNCHER_4INCH;
            }
        }
    }



    public void seriaADBMove(String fileName){
        //executeCommand("mount -o rw,remount /system");
        executeCommand("remount");
        executeCommand("mkdir -p /system/app/CleverHome");
        executeCommand("cp /storage/emulated/0/Android/data/pro.cleverlife.cleverlauncher/files/Download/" + fileName + " /system/app/CleverHome/");
        executeCommand("chmod 644 /system/app/CleverHome/" + fileName);
    }
    public void seriaADBInstall(){
        executeCommand("pm install /system/app/CleverHome/CleverHome.apk");
        executeCommand("am start -n pro.cleverlife.cleverroom/.SHActivity");
    }

    public void seriaADBInstallLauncher(){
        executeCommand("pm install /system/app/CleverHome/launcher.apk");
        executeCommand("am start -n pro.cleverlife.cleverlauncher/.Activity");
    }

//    private void launchCleverHomeApp() {
//        Log.i("CleverHomeLogs", "Открываем активность...");
//        Intent intent = new Intent();
//        intent.setComponent(new ComponentName("pro.cleverlife.cleverhome", "pro.cleverlife.cleverhome.SHActivity"));
//        startActivity(intent);
//    }

    public String readTXTFile(String filePath) {
        String versionLine = null; // Инициализируем переменную для хранения строки версии

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            if ((line = br.readLine()) != null) {
                // Просто сохраняем строку из файла
                versionLine = line.trim(); // Убираем лишние пробелы
            }
        } catch (IOException e) {
            Log.e(TAG, "Error reading version from file", e);
        }

        return versionLine; // Возвращаем строку с версией или null, если не удалось прочитать
    }


    public void compareVersion() {
        if (newVersionAPK != -1) {
            if(oldVersionAPK != -1){
                if (newVersionAPK > oldVersionAPK) {
                    Log.i(TAG, "New version available: " + newVersionAPK);
                    //Нужно скачать apk
                    downloadFile("CleverHome.apk", getApkUrl(), 2);
//                    if(isTest_APK){
//                        downloadFile("CleverHome.apk", APK_URL_TEST_8INCH, 2);
//                    } else{
//                        downloadFile("CleverHome.apk", APK_URL_8INCH, 2);
//                    }
                    /////// downloadFile("CleverHome.apk", APK_URL, 3);
                    // downloadFile("CleverHome.apk", APK_LAUNCHER_8INCH, 3);
                } else {
                    Log.i(TAG, "You are on the latest version: " + oldVersionAPK);
                }
            } else{
                Log.e(TAG, "Failed to read version from file. No available old version");
                //Нужно скачать apk
                downloadFile("CleverHome.apk", getApkUrl(), 2);
//                if(isTest_APK){
//                    downloadFile("CleverHome.apk", APK_URL_TEST_8INCH, 2);
//                } else{
//                    downloadFile("CleverHome.apk", APK_URL_8INCH, 2);
//                }
                ///downloadFile("CleverHome.apk", APK_URL, 3);
                //downloadFile("CleverHome.apk", APK_LAUNCHER_8INCH, 3);
            }
        } else {
            Log.e(TAG, "Failed to read version from file. No available new version");
        }
    }

    public void compareVersionLauncher() {
        if (newVersionAPK != -1) {
            if(oldVersionAPK != -1){
                if (newVersionAPK > oldVersionAPK) {
                    Log.i(TAG, "New version available: " + newVersionAPK);
                    //Нужно скачать apk
                    //Проверка файла типа установки из канала обновлений (test, debug, release)
                    downloadFile(LAUNCHER_FILE, getApkUrlLauncher(), 4);
//                    if(isTest_LAUNCHER){
//                        downloadFile(LAUNCHER_FILE, APK_LAUNCHER_TEST_8INCH, 4);
//                    } else{
//                        downloadFile(LAUNCHER_FILE, APK_LAUNCHER_8INCH, 4);
//                    }
                } else {
                    Log.i(TAG, "You are on the latest version: " + oldVersionAPK);
                }
            } else{
                Log.e(TAG, "Failed to read version from file. No available old version");
                //Нужно скачать apk
                //Проверка файла типа установки из канала обновлений (test, debug, release)
                downloadFile(LAUNCHER_FILE, getApkUrlLauncher(), 4);
//                if(isTest_LAUNCHER){
//                    downloadFile(LAUNCHER_FILE, APK_LAUNCHER_TEST_8INCH, 4);
//                } else{
//                    downloadFile(LAUNCHER_FILE, APK_LAUNCHER_8INCH, 4);
//                }
            }
        } else {
            Log.e(TAG, "Failed to read version from file. No available new version");
        }
    }

    private void downloadFile(String fileName, String fileUrl, int typeFile) {
        new Thread(() -> {
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            FileOutputStream outputStream = null;

            try {
                // Определяем путь для сохранения
                File downloadDir = new File(getExternalFilesDir(null), "Download");
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs(); // Создаем папку, если она не существует
                }

                File downloadedFile = new File(downloadDir, fileName);

                // Удаляем файл, если он уже существует, чтобы перезаписать его
                if (downloadedFile.exists()) {
                    downloadedFile.delete(); // Удаляем существующий файл

                    Log.d(TAG, "Existing file deleted: " + downloadedFile.getAbsolutePath());
                }

                URL url = new URL(fileUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(0); // Установить таймаут соединения
                urlConnection.setReadTimeout(0); // Установить таймаут чтения
                urlConnection.connect();

                // Проверка на успешное соединение
                if (urlConnection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Server returned HTTP " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage());
                    return;
                }

                // Получаем входной поток
                inputStream = new BufferedInputStream(urlConnection.getInputStream());

                // Получаем общий размер файла
                int totalLength = urlConnection.getContentLength();
                byte[] data = new byte[1024];
                int count;
                int totalRead = 0;

                outputStream = new FileOutputStream(downloadedFile); // Создаем поток для записи

                while ((count = inputStream.read(data)) != -1) {
                    outputStream.write(data, 0, count);
                    totalRead += count;

                    // Вычисляем процент загрузки
                    if (totalLength > 0) {
                        int progress = (int) ((totalRead * 100L) / totalLength);
                        //Toast.makeText(MainActivity.this, "Download progress: " + progress + "%", Toast.LENGTH_LONG).show();
                        Log.d(TAG, "Download progress: " + progress + "%");
                        // Обновляем UI в основном потоке
                        runOnUiThread(() -> {
                            textDownlanding.setText("Загрузка " + progress + "%");
                        });
                    }
                }

                outputStream.flush();
                //runOnUiThread(() -> Toast.makeText(MainActivity.this, "File downloaded to: " + downloadedFile.getAbsolutePath(), Toast.LENGTH_LONG).show());
                Log.d(TAG, "File downloaded to: " + downloadedFile.getAbsolutePath());

                //После скачивания по типу файла направляем действия
                switch (typeFile){
                    case 1: //version - необходимо считать версию нового apk
                        //Toast.makeText(MainActivity.this, "Скачивание apk", Toast.LENGTH_LONG).show();
                        readVersionFileNew(fileName);
                        readVersionFileOld(fileName, "");
                        if(fileName.equals(VERSION_FILE)){
                            compareVersion();
                        } else{
                            compareVersionLauncher();
                        }
                        break;
                    case 2: //установка CleverHome
                        //Toast.makeText(MainActivity.this, "Работа с хэшами", Toast.LENGTH_LONG).show();
                        //Сравнить хэши
                        //ПОКА НЕ ДЕЛАТЬ!
//                        readHashSummOld();
//                        readHashSummNew();


                        seriaADBMove(VERSION_FILE);
                        seriaADBMove(CLEVER_FILE);

                        seriaADBInstall();
                        //чистить за собой папку downland
                        executeCommand("rm -rf " + ADB_Download + "*");
                        break;
                    //НЕ ИСПОЛЬЗУЕТСЯ
                    case 3: //apk
                        //Нужно скачать hash
                        //Toast.makeText(MainActivity.this, "Скачивание хэш", Toast.LENGTH_LONG).show();
//                        if(fileName.equals("CleverHome.apk")){
//                            downloadFile(HASH_FILE, HASH_URL, 2);
//                        } else{
//                            downloadFile(HASH_FILE, HASH_URL, 4);
//                        }
                        break;
                    case 4: //Launcher установка
                        seriaADBMove(VERSION_FILE_LAUNCHER);
                        seriaADBMove(LAUNCHER_FILE);
                        seriaADBInstallLauncher();
                        //чистить за собой папку downland
                        executeCommand("rm -rf " + ADB_Download + "*");
                    default:
                        break;
                }

            } catch (IOException e) {
                Log.e(TAG, "Error downloading file", e);
            } finally {
                // Закрытие потоков
                try {
                    if (outputStream != null) outputStream.close();
                    if (inputStream != null) inputStream.close();
                    if (urlConnection != null) urlConnection.disconnect();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing streams", e);
                }
            }
        }).start();
    }

    public void readVersionFileNew(String fileName){
        File versionFile = new File(ADB_Download, fileName);
        if (!versionFile.exists()) {
            Log.e(TAG, "Version file does not exist: " + versionFile.getAbsolutePath());
            return;
        }
        String versionLine = readTXTFile(versionFile.getAbsolutePath());
        if (versionLine != null) {
            Log.i(TAG, "Version line read_new: " + versionLine);
            newVersionAPK = Integer.parseInt(versionLine);
            //compareVersion(Integer.parseInt(versionLine));
        } else {
            Log.e(TAG, "Failed to read version line from file.");
        }
    }

    public void readVersionFileOld(String fileName, String typeFile){
        File versionFile = new File(ADB_CleverHome, fileName);
        if (!versionFile.exists()) {
            Log.e(TAG, "Version file does not exist: " + versionFile.getAbsolutePath());
            return;
        }
        String versionLine = readTXTFile(versionFile.getAbsolutePath());
        if (versionLine != null) {
            Log.i(TAG, "Version line read_old: " + versionLine);
            if(typeFile == "launcher"){
                oldVersionAPKLauncher = Integer.parseInt(versionLine);
            } else{
                oldVersionAPK = Integer.parseInt(versionLine);
            }

            //compareVersion(Integer.parseInt(versionLine));
        } else {
            Log.e(TAG, "Failed to read version line from file.");
        }
    }

//    public void readHashSummNew(){
//        File hashFile = new File(ADB_Download, HASH_FILE);
//        if (!hashFile.exists()) {
//            Log.e(TAG, "Hashsumm file does not exist: " + hashFile.getAbsolutePath());
//            return;
//        }
//        String hashLine = readTXTFile(hashFile.getAbsolutePath());
//        if (hashLine != null) {
//            Log.i(TAG, "Hashsumm line read: " + hashLine);
//            //oldVersionAPK = Integer.parseInt(hashLine);
//            //compareVersion(Integer.parseInt(versionLine));
//        } else {
//            Log.e(TAG, "Failed to read Hashsumm line from file.");
//        }
//    }

//    public void readHashSummOld(){
//        File apkFile = new File(ADB_Download+CLEVER_FILE);
//
//        String hash = calculateMD5(apkFile);
//        if (hash != null) {
//            Log.e(TAG, "MD5 Hash: " + hash);
//        } else {
//            Log.e(TAG, "Failed to calculate hash.");
//        }
//    }

    @Override
    public void onAppInstalled(String packageName) {
        Intent queryIntent = new Intent(Intent.ACTION_MAIN, null);
        queryIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        queryIntent.setPackage(packageName);
    }

    @Override
    public void onAppRemoved(String packageName) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sMyself = null;

        // Останавливаем таймер при уничтожении активности
        handler.removeCallbacks(hourlyTask);
    }

    public void executeCommand(String command) {
        try {
            Log.d(TAG, "Executing command: " + command);
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            StringBuilder errorOutput = new StringBuilder();
            while ((line = errorReader.readLine()) != null) {
                errorOutput.append(line).append("\n");
            }

            Log.d(TAG, "Command: " + command + " Output: " + output.toString());
            Log.e(TAG, "Error Output: " + errorOutput.toString());
            Log.d(TAG, "Exited with code: " + process.exitValue());

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error executing command", e);
        }
    }

//    public static String findVersion(String filePath) {
//        String version = null;
//
//        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
//            String line;
//            while ((line = br.readLine()) != null) {
//                // Проверяем, содержит ли строка
//                if (line.startsWith("VERSION = ")) {
//                    // Извлекаем версию
//                    version = line.split("=")[1].trim();
//                    break;
//                }
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//
//        return version;
//    }

    public static String calculateMD5(File file) {

        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            try (FileInputStream fis = new FileInputStream(file)) {
                byte[] byteArray = new byte[1024];
                int bytesCount;

                while ((bytesCount = fis.read(byteArray)) != -1) {
                    digest.update(byteArray, 0, bytesCount);
                }
            }

            // Получаем хэш в виде байтового массива
            byte[] bytes = digest.digest();

            // Преобразуем байты в шестнадцатеричную строку
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }

            return sb.toString();
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}

//import android.Manifest;
//import android.content.Intent;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.widget.Button;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//
//import pro.cleverlife.clevervoice.TestInterface.TestBrightnessActivity;
//import pro.cleverlife.clevervoice.processor.CommandProcessor;
//import pro.cleverlife.clevervoice.service.SoundManager;
//import pro.cleverlife.clevervoice.service.VoiceRecognitionService;
//import pro.cleverlife.clevervoice.utils.PermissionManager;

//public class MainActivity extends AppCompatActivity {
//    private static final int PERMISSION_REQUEST_CODE = 123;
//
//    private VoiceRecognitionService voiceService;
//    private CommandProcessor commandProcessor;
//    private SoundManager soundManager;
//
//    private TextView statusText;
//    private TextView logText;
//    private Button startButton;
//    private CountDownTimer commandTimer;
//    private boolean isListening = false;
//    private StringBuilder logBuilder = new StringBuilder();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        initViews();
//        checkPermissions();
//    }
//
//    private void initViews() {
//        statusText = findViewById(R.id.statusText);
//        logText = findViewById(R.id.logText);
//        startButton = findViewById(R.id.startButton);
//        Button buttonTestBrightness = findViewById(R.id.buttonTestBrightness); // ← новая кнопка
//
//        startButton.setOnClickListener(v -> toggleListening());
//
//        // Обработчик для перехода в тест яркости
//        buttonTestBrightness.setOnClickListener(v -> {
//            Intent intent = new Intent(MainActivity.this, TestBrightnessActivity.class);
//            startActivity(intent);
//        });
//
//        // Изначально кнопка неактивна
//        startButton.setEnabled(false);
//        statusText.setText("Проверка разрешений...");
//        logText.setText("Инициализация системы...\n");
//    }
//
//    private void checkPermissions() {
//        addLog("Проверка разрешений...");
//        if (PermissionManager.hasAllRequiredPermissions(this)) {
//            addLog("Все разрешения предоставлены");
//            initServices();
//        } else {
//            addLog("Запрос недостающих разрешений");
//            String[] missingPermissions = PermissionManager.getMissingPermissions(this);
//            ActivityCompat.requestPermissions(this, missingPermissions, PERMISSION_REQUEST_CODE);
//        }
//    }
//
//    private void initServices() {
//        addLog("Инициализация сервисов...");
//        voiceService = new VoiceRecognitionService(this);
//        commandProcessor = new CommandProcessor(this);
//
//        // Инициализируем SoundManager и передаем звуковые файлы
//        soundManager = new SoundManager(this);
//        soundManager.initializeWithSounds(R.raw.victory_tone, R.raw.error_notification);
//
//        if (soundManager.isInitialized()) {
//            addLog("Менеджер звуков инициализирован");
//        } else {
//            addLog("Менеджер звуков не смог загрузить звуковые файлы");
//        }
//
//        setupVoiceRecognition();
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//
//        if (requestCode == PERMISSION_REQUEST_CODE) {
//            boolean allGranted = true;
//            for (int result : grantResults) {
//                if (result != PackageManager.PERMISSION_GRANTED) {
//                    allGranted = false;
//                    break;
//                }
//            }
//
//            if (allGranted) {
//                addLog("Все разрешения получены");
//                initServices();
//                Toast.makeText(this, "Все разрешения получены", Toast.LENGTH_SHORT).show();
//            } else {
//                addLog("Не все разрешения предоставлены");
//                statusText.setText("Не все разрешения предоставлены");
//                Toast.makeText(this, "Для работы приложения нужны все разрешения", Toast.LENGTH_LONG).show();
//                startButton.setEnabled(false);
//            }
//        }
//    }
//
//    private void setupVoiceRecognition() {
//        addLog("Настройка голосового распознавания...");
//        voiceService.setActivationListener(new VoiceRecognitionService.ActivationListener() {
//            @Override
//            public void onActivationWordDetected() {
//                runOnUiThread(() -> {
//                    addLog(">>> АКТИВАЦИЯ: слово 'Клевер' обнаружено!");
//                    addLog(">< У вас 10 секунд для команды...");
//                    statusText.setText("Слушаю команду...");
//                    if (soundManager.isInitialized()) {
//                        soundManager.playActivationSound();
//                    }
//                    startCommandTimer(); // Запускаем таймер
//                });
//            }
//
//            @Override
//            public void onCommandReceived(String command) {
//                runOnUiThread(() -> {
//                    addLog(">>> КОМАНДА: \"" + command + "\"");
//                    // Останавливаем таймер при получении команды
//                    if (commandTimer != null) {
//                        commandTimer.cancel();
//                    }
//                    processCommand(command);
//                });
//            }
//
//            @Override
//            public void onError(String error) {
//                runOnUiThread(() -> {
//                    addLog("!!! ОШИБКА: " + error);
//                    statusText.setText("Ошибка: " + error);
//                    if (soundManager.isInitialized()) {
//                        soundManager.playErrorSound();
//                    }
//
//                    // Останавливаем таймер при ошибке
//                    if (commandTimer != null) {
//                        commandTimer.cancel();
//                    }
//
//                    // Автоматически перезапускаем прослушивание после ошибки
//                    if (isListening) {
//                        addLog("Перезапуск прослушивания...");
//                        voiceService.startListening();
//                    }
//                });
//            }
//
//            @Override
//            public void onInitialized() {
//                runOnUiThread(() -> {
//                    addLog("+ Система голосового распознавания готова");
//                    statusText.setText("Система готова");
//                    startButton.setEnabled(true);
//                    startButton.setText("Запустить прослушивание");
//                });
//            }
//
//            @Override
//            public void onPartialResult(String partialText) {
//                runOnUiThread(() -> {
//                    if (!partialText.trim().isEmpty() && !partialText.equals("[ожидание активации...]")) {
//                        addLog("... частично: \"" + partialText + "\"");
//                    }
//                });
//            }
//
//            @Override
//            public void onSpeechDetected() {
//                // Оставляем пустым или добавим минимальную логику
//            }
//
//            @Override
//            public void onSilenceDetected() {
//                // Оставляем пустым
//            }
//        });
//    }
//
//    private void stopListening() {
//        if (voiceService != null) {
//            addLog("=== ОСТАНОВКА ПРОСЛУШИВАНИЯ ===");
//            addLog("• Микрофон деактивирован");
//            voiceService.stopListening();
//        }
//        isListening = false;
//        startButton.setText("Запустить прослушивание");
//        statusText.setText("Остановлено");
//
//        if (commandTimer != null) {
//            commandTimer.cancel();
//        }
//    }
//
//    private void toggleListening() {
//        if (isListening) {
//            stopListening();
//        } else {
//            startListening();
//        }
//    }
//
//    private void startListening() {
//        if (voiceService != null) {
//            addLog("=== ЗАПУСК ПРОСЛУШИВАНИАЯ ===");
//            addLog("• Микрофон активирован");
//            addLog("• Ожидание активационного слова: 'Клевер'");
//            addLog("• Речь до активации игнорируется");
//            voiceService.startListening();
//            isListening = true;
//            startButton.setText("Остановить прослушивание");
//            statusText.setText("Скажите 'Клевер' для активации...");
//        } else {
//            addLog("!!! Ошибка: сервис распознавания не инициализирован");
//        }
//    }
//
//    private void startCommandTimer() {
//        if (commandTimer != null) {
//            commandTimer.cancel();
//        }
//
//        commandTimer = new CountDownTimer(10000, 1000) {
//            public void onTick(long millisUntilFinished) {
//                String seconds = String.valueOf(millisUntilFinished / 1000);
//                statusText.setText(">< Команда: " + seconds + "с");
//
//                // Обновляем лог каждые 5 секунд
//                if (millisUntilFinished % 5000 == 0) {
//                    addLog(">< Осталось " + seconds + " секунд...");
//                }
//            }
//
//            public void onFinish() {
//                addLog(">< ВРЕМЯ ВЫШЛО! Активация сброшена.");
//                statusText.setText(">< Время вышло! Скажите 'Клевер'...");
//                if (soundManager.isInitialized()) {
//                    soundManager.playErrorSound();
//                }
//
//                // Сбрасываем активацию в сервисе
//                if (voiceService != null) {
//                    voiceService.resetActivation();
//                }
//
//                // Возвращаемся в режим ожидания активации
//                if (isListening) {
//                    addLog("Ожидание активационного слова...");
//                    voiceService.startListening();
//                }
//            }
//        }.start();
//    }
//
//    private void processCommand(String command) {
//        if (soundManager.isInitialized()) {
//            soundManager.playSuccessSound();
//        }
//        addLog("-><- Обработка команды: \"" + command + "\"");
//        statusText.setText("Обрабатываю: " + command);
//
//        // Обрабатываем команду
//        commandProcessor.processCommand(command);
//
//        // Возвращаемся в режим ожидания активации
//        if (isListening) {
//            addLog("Ожидание новой активации...");
//            statusText.setText("Скажите 'Клевер' для новой команды...");
//            voiceService.startListening();
//        }
//    }
//
//    private void addLog(String message) {
//        runOnUiThread(() -> {
//            // Добавляем сообщение в StringBuilder
//            logBuilder.append(message).append("\n");
//
//            // Обновляем TextView
//            logText.setText(logBuilder.toString());
//
//            // Правильная прокрутка вниз
//            if (logText.getLayout() != null) {
//                int scrollAmount = logText.getLayout().getLineTop(logText.getLineCount()) - logText.getHeight();
//                if (scrollAmount > 0) {
//                    logText.scrollTo(0, scrollAmount);
//                } else {
//                    logText.scrollTo(0, 0);
//                }
//            }
//        });
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//        // При возвращении в приложение перезапускаем прослушивание если оно было активно
//        if (isListening && voiceService != null) {
//            addLog("Возобновление прослушивания...");
//            voiceService.startListening();
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        super.onPause();
//        // При сворачивании приложения останавливаем прослушивание
//        if (voiceService != null) {
//            addLog("Приостановка прослушивания...");
//            voiceService.stopListening();
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        if (voiceService != null) {
//            voiceService.release();
//        }
//        if (soundManager != null) {
//            soundManager.release();
//        }
//        if (commandTimer != null) {
//            commandTimer.cancel();
//        }
//        addLog("Приложение закрыто");
//    }
//}