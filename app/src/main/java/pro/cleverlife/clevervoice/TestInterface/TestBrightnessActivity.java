package pro.cleverlife.clevervoice.TestInterface;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pro.cleverlife.clevervoice.API.BrightnessAPI;
import pro.cleverlife.clevervoice.R;

public class TestBrightnessActivity extends AppCompatActivity {

    private static final int REQUEST_WRITE_SETTINGS = 1001;
    private EditText editTextBrightness;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_interface);

        // Проверяем разрешение на запись системных настроек
        if (!hasWriteSettingsPermission()) {
            requestWriteSettingsPermission();
        }

        initBrightnessControls();
    }

    private boolean hasWriteSettingsPermission() {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.System.canWrite(this);
    }

    private void requestWriteSettingsPermission() {
        Toast.makeText(this, "Требуется разрешение на изменение яркости", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void initBrightnessControls() {
        editTextBrightness = findViewById(R.id.editTextBrightness);

        // === SET ===
        findViewById(R.id.buttonSet).setOnClickListener(v -> {
            if (!hasWriteSettingsPermission()) {
                requestWriteSettingsPermission();
                return;
            }
            String input = editTextBrightness.getText().toString().trim();
            if (input.isEmpty()) {
                Toast.makeText(this, "Введите значение для SET", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                int value = Integer.parseInt(input);
                if (value < 0 || value > 255) {
                    Toast.makeText(this, "Яркость должна быть от 0 до 255", Toast.LENGTH_SHORT).show();
                    return;
                }
                String result = BrightnessAPI.executeCommand(this, BrightnessAPI.BrightnessCommand.SET, String.valueOf(value));
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Некорректное число", Toast.LENGTH_SHORT).show();
            }
        });

        // === Быстрые кнопки: сразу вызывают SET с новым значением ===
        findViewById(R.id.buttonPlus5).setOnClickListener(v -> applyDeltaToSet(5));
        findViewById(R.id.buttonPlus10).setOnClickListener(v -> applyDeltaToSet(10));
        findViewById(R.id.buttonMinus5).setOnClickListener(v -> applyDeltaToSet(-5));
        findViewById(R.id.buttonMinus10).setOnClickListener(v -> applyDeltaToSet(-10));

        // === Остальные команды (без параметров) ===
        findViewById(R.id.buttonIncrease).setOnClickListener(v -> executeBrightnessCommand(BrightnessAPI.BrightnessCommand.INCREASE));
        findViewById(R.id.buttonDecrease).setOnClickListener(v -> executeBrightnessCommand(BrightnessAPI.BrightnessCommand.DECREASE));
        findViewById(R.id.buttonMax).setOnClickListener(v -> executeBrightnessCommand(BrightnessAPI.BrightnessCommand.MAX));
        findViewById(R.id.buttonMin).setOnClickListener(v -> executeBrightnessCommand(BrightnessAPI.BrightnessCommand.MIN));
        findViewById(R.id.buttonMedium).setOnClickListener(v -> executeBrightnessCommand(BrightnessAPI.BrightnessCommand.MEDIUM));
        findViewById(R.id.buttonGetInfo).setOnClickListener(v -> executeBrightnessCommand(BrightnessAPI.BrightnessCommand.GET_INFO));
    }

    private void executeBrightnessCommand(BrightnessAPI.BrightnessCommand command) {
        if (!hasWriteSettingsPermission()) {
            requestWriteSettingsPermission();
            return;
        }
        String result = BrightnessAPI.executeCommand(this, command);
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    private void applyDeltaToSet(int delta) {
        if (!hasWriteSettingsPermission()) {
            requestWriteSettingsPermission();
            return;
        }
        String currentText = editTextBrightness.getText().toString().trim();
        int currentValue = currentText.isEmpty() ? 128 : Integer.parseInt(currentText);
        int newValue = Math.max(0, Math.min(255, currentValue + delta));

        String result = BrightnessAPI.executeCommand(this, BrightnessAPI.BrightnessCommand.SET, String.valueOf(newValue));
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        editTextBrightness.setText(String.valueOf(newValue));
    }
}