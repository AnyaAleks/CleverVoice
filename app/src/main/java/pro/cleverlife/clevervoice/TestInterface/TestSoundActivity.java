package pro.cleverlife.clevervoice.TestInterface;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import pro.cleverlife.clevervoice.API.SoundAPI;
import pro.cleverlife.clevervoice.R;

public class TestSoundActivity extends AppCompatActivity {

    private EditText editTextVolume;
    private ToggleButton toggleMedia, toggleRing, toggleAlarm, toggleNotification;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_sound);

        initViews();
        setupListeners();
    }

    private void initViews() {
        editTextVolume = findViewById(R.id.editTextVolume);
        toggleMedia = findViewById(R.id.toggleMedia);
        toggleRing = findViewById(R.id.toggleRing);
        toggleAlarm = findViewById(R.id.toggleAlarm);
        toggleNotification = findViewById(R.id.toggleNotification);
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

    private SoundAPI.SoundCommand[] getSelectedCommands() {
        java.util.ArrayList<SoundAPI.SoundCommand> selectedCommands = new java.util.ArrayList<>();

        if (toggleMedia.isChecked()) selectedCommands.add(SoundAPI.SoundCommand.SET_MEDIA);
        if (toggleRing.isChecked()) selectedCommands.add(SoundAPI.SoundCommand.SET_RING);
        if (toggleAlarm.isChecked()) selectedCommands.add(SoundAPI.SoundCommand.SET_ALARM);
        if (toggleNotification.isChecked()) selectedCommands.add(SoundAPI.SoundCommand.SET_NOTIFICATION);

        if (selectedCommands.isEmpty()) {
            Toast.makeText(this, "Выберите хотя бы один тип звука", Toast.LENGTH_SHORT).show();
            return null;
        }

        return selectedCommands.toArray(new SoundAPI.SoundCommand[0]);
    }

    private SoundAPI.SoundCommand mapBaseToCommand(SoundAPI.SoundCommand base, String suffix) {
        String baseName = base.name();
        String type = baseName.substring(4); // Обрезаем "SET_"
        return SoundAPI.SoundCommand.valueOf(suffix + type);
    }

    // Новый метод специально для GET_INFO команд
    private SoundAPI.SoundCommand getInfoCommand(SoundAPI.SoundCommand base) {
        String baseName = base.name();
        String type = baseName.substring(4); // Обрезаем "SET_"

        // Пробуем разные варианты названий команд
        String[] possibleCommands = {
                "GET_" + type + "_INFO",  // GET_MEDIA_INFO
                "GET_" + type,            // GET_MEDIA
                "INFO_" + type,           // INFO_MEDIA
                "GET_INFO_" + type        // GET_INFO_MEDIA
        };

        for (String command : possibleCommands) {
            try {
                return SoundAPI.SoundCommand.valueOf(command);
            } catch (IllegalArgumentException e) {
                // Пробуем следующий вариант
                continue;
            }
        }

        throw new IllegalArgumentException("No info command found for: " + baseName);
    }

    private void executeSetCommand() {
        SoundAPI.SoundCommand[] bases = getSelectedCommands();
        if (bases == null) return;

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

            for (SoundAPI.SoundCommand base : bases) {
                String result = SoundAPI.executeCommand(this, base, String.valueOf(value));
                Toast.makeText(this, base.name() + ": " + result, Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Некорректное число", Toast.LENGTH_SHORT).show();
        }
    }

    private void executeIncreaseCommand(int delta) {
        SoundAPI.SoundCommand[] bases = getSelectedCommands();
        if (bases == null) return;

        for (SoundAPI.SoundCommand base : bases) {
            SoundAPI.SoundCommand incCommand = mapBaseToCommand(base, "INCREASE_");
            String result = SoundAPI.executeCommand(this, incCommand, String.valueOf(delta));
            Toast.makeText(this, base.name() + ": " + result, Toast.LENGTH_SHORT).show();
        }
    }

    private void executeDecreaseCommand(int delta) {
        SoundAPI.SoundCommand[] bases = getSelectedCommands();
        if (bases == null) return;

        for (SoundAPI.SoundCommand base : bases) {
            SoundAPI.SoundCommand decCommand = mapBaseToCommand(base, "DECREASE_");
            String result = SoundAPI.executeCommand(this, decCommand, String.valueOf(delta));
            Toast.makeText(this, base.name() + ": " + result, Toast.LENGTH_SHORT).show();
        }
    }

    private void executeMaxCommand() {
        SoundAPI.SoundCommand[] bases = getSelectedCommands();
        if (bases == null) return;

        for (SoundAPI.SoundCommand base : bases) {
            SoundAPI.SoundCommand maxCommand = mapBaseToCommand(base, "MAX_");
            String result = SoundAPI.executeCommand(this, maxCommand);
            Toast.makeText(this, base.name() + ": " + result, Toast.LENGTH_SHORT).show();
        }
    }

    private void executeMinCommand() {
        SoundAPI.SoundCommand[] bases = getSelectedCommands();
        if (bases == null) return;

        for (SoundAPI.SoundCommand base : bases) {
            SoundAPI.SoundCommand minCommand = mapBaseToCommand(base, "MIN_");
            String result = SoundAPI.executeCommand(this, minCommand);
            Toast.makeText(this, base.name() + ": " + result, Toast.LENGTH_SHORT).show();
        }
    }

    private void executeUnmuteCommand() {
        SoundAPI.SoundCommand[] bases = getSelectedCommands();
        if (bases == null) return;

        for (SoundAPI.SoundCommand base : bases) {
            SoundAPI.SoundCommand unmuteCommand = mapBaseToCommand(base, "UNMUTE_");
            String result = SoundAPI.executeCommand(this, unmuteCommand);
            Toast.makeText(this, base.name() + ": " + result, Toast.LENGTH_SHORT).show();
        }
    }

    private void executeGetInfoCommand() {
        SoundAPI.SoundCommand[] bases = getSelectedCommands();
        if (bases == null) return;

        for (SoundAPI.SoundCommand base : bases) {
            try {
                SoundAPI.SoundCommand infoCommand = getInfoCommand(base);
                String result = SoundAPI.executeCommand(this, infoCommand);
                Toast.makeText(this, base.name() + ": " + result, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, base.name() + ": Команда GET_INFO не поддерживается", Toast.LENGTH_SHORT).show();
            }
        }
    }
}