package pro.cleverlife.clevervoice.TestInterface;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pro.cleverlife.clevervoice.API.SoundAPI;
import pro.cleverlife.clevervoice.R;

public class TestSoundActivity extends AppCompatActivity {

    private EditText editTextVolume;
    private RadioGroup radioGroupStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_sound);

        initViews();
        setupListeners();
    }

    private void initViews() {
        editTextVolume = findViewById(R.id.editTextVolume);
        radioGroupStream = findViewById(R.id.radioGroupStream);
    }

    private void setupListeners() {
        // SET
        findViewById(R.id.buttonSet).setOnClickListener(v -> executeSetCommand());

        // Быстрые кнопки
        findViewById(R.id.buttonPlus1).setOnClickListener(v -> executeIncreaseCommand(1));
        findViewById(R.id.buttonPlus5).setOnClickListener(v -> executeIncreaseCommand(5));
        findViewById(R.id.buttonMinus1).setOnClickListener(v -> executeDecreaseCommand(1));
        findViewById(R.id.buttonMinus5).setOnClickListener(v -> executeDecreaseCommand(5));

        // Остальные команды
        findViewById(R.id.buttonMax).setOnClickListener(v -> executeMaxCommand());
        findViewById(R.id.buttonMin).setOnClickListener(v -> executeMinCommand());
        findViewById(R.id.buttonUnmute).setOnClickListener(v -> executeUnmuteCommand());
        findViewById(R.id.buttonGetInfo).setOnClickListener(v -> executeGetInfoCommand());
    }

    private SoundAPI.SoundCommand getCommandBase() {
        int selectedId = radioGroupStream.getCheckedRadioButtonId();
        if (selectedId == R.id.radioMedia) return SoundAPI.SoundCommand.SET_MEDIA;
        if (selectedId == R.id.radioRing) return SoundAPI.SoundCommand.SET_RING;
        if (selectedId == R.id.radioAlarm) return SoundAPI.SoundCommand.SET_ALARM;
        if (selectedId == R.id.radioNotification) return SoundAPI.SoundCommand.SET_NOTIFICATION;
        return SoundAPI.SoundCommand.SET_MEDIA;
    }

    private SoundAPI.SoundCommand mapBaseToCommand(SoundAPI.SoundCommand base, String suffix) {
        String baseName = base.name();
        String type = baseName.substring(4); // e.g. "MEDIA", "RING"
        return SoundAPI.SoundCommand.valueOf(suffix + "_" + type);
    }

    private void executeSetCommand() {
        String input = editTextVolume.getText().toString().trim();
        if (input.isEmpty()) {
            Toast.makeText(this, "Введите значение громкости", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            int value = Integer.parseInt(input);
            if (value < 0) {
                Toast.makeText(this, "Громкость не может быть меньше 0", Toast.LENGTH_SHORT).show();
                return;
            }
            SoundAPI.SoundCommand base = getCommandBase();
            String result = SoundAPI.executeCommand(this, base, String.valueOf(value));
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Некорректное число", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeIncreaseCommand(int delta) {
        SoundAPI.SoundCommand base = getCommandBase();
        SoundAPI.SoundCommand incCommand = mapBaseToCommand(base, "INCREASE");
        String result = SoundAPI.executeCommand(this, incCommand, String.valueOf(delta));
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    private void executeDecreaseCommand(int delta) {
        SoundAPI.SoundCommand base = getCommandBase();
        SoundAPI.SoundCommand decCommand = mapBaseToCommand(base, "DECREASE");
        String result = SoundAPI.executeCommand(this, decCommand, String.valueOf(delta));
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    private void executeMaxCommand() {
        SoundAPI.SoundCommand base = getCommandBase();
        SoundAPI.SoundCommand maxCommand = mapBaseToCommand(base, "MAX");
        String result = SoundAPI.executeCommand(this, maxCommand);
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    private void executeMinCommand() {
        SoundAPI.SoundCommand base = getCommandBase();
        SoundAPI.SoundCommand minCommand = mapBaseToCommand(base, "MIN");
        String result = SoundAPI.executeCommand(this, minCommand);
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    private void executeUnmuteCommand() {
        SoundAPI.SoundCommand base = getCommandBase();
        SoundAPI.SoundCommand unmuteCommand = mapBaseToCommand(base, "UNMUTE");
        String result = SoundAPI.executeCommand(this, unmuteCommand);
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
    }

    private void executeGetInfoCommand() {
        SoundAPI.SoundCommand base = getCommandBase();
        SoundAPI.SoundCommand infoCommand = mapBaseToCommand(base, "GET");
        // GET_MEDIA_INFO → base = SET_MEDIA → replace SET → GET
        String commandName = infoCommand.name().replace("SET_", "GET_") + "_INFO";
        try {
            SoundAPI.SoundCommand finalCommand = SoundAPI.SoundCommand.valueOf(commandName);
            String result = SoundAPI.executeCommand(this, finalCommand);
            Toast.makeText(this, result, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка получения информации", Toast.LENGTH_SHORT).show();
        }
    }
}