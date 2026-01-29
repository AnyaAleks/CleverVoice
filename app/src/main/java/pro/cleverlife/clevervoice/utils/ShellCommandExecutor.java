package pro.cleverlife.clevervoice.utils;

import android.util.Log;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ShellCommandExecutor {
    private static final String TAG = "ShellCommandExecutor";

    public static String executeCommand(String command) {
        try {
            Log.d(TAG, "Executing shell command: " + command);
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());

            os.writeBytes(command + "\n");
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();

            // Читаем вывод
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

            String result = output.toString().trim();
            Log.d(TAG, "Command output: " + result);
            if (!errorOutput.toString().isEmpty()) {
                Log.e(TAG, "Command error: " + errorOutput.toString());
            }

            return result;

        } catch (IOException | InterruptedException e) {
            Log.e(TAG, "Error executing shell command", e);
            return "Ошибка: " + e.getMessage();
        }
    }

    public static String executeCommandWithoutRoot(String command) {
        try {
            Log.d(TAG, "Executing non-root command: " + command);
            Process process = Runtime.getRuntime().exec(command);
            process.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            String result = output.toString().trim();
            Log.d(TAG, "Non-root command output: " + result);
            return result;

        } catch (Exception e) {
            Log.e(TAG, "Error executing non-root command", e);
            return "Ошибка: " + e.getMessage();
        }
    }

    // Проверяем доступность root
    public static boolean hasRootAccess() {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("echo 'Root доступен'\n");
            os.writeBytes("exit\n");
            os.flush();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            Log.d(TAG, "Root доступ отсутствует: " + e.getMessage());
            return false;
        }
    }
}