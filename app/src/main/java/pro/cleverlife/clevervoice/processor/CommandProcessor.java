    package pro.cleverlife.clevervoice.processor;

    import android.content.Context;
    import android.util.Log;

    public class CommandProcessor {
        private static final String TAG = "CommandProcessor";
        private Context context;

        public CommandProcessor(Context context) {
            this.context = context;
        }

        public void processCommand(String command) {
            Log.d(TAG, "Обработка команды: " + command);

            // Простая логика обработки команд
            if (command.toLowerCase().contains("привет")) {
                Log.d(TAG, "Обработана команда: привет");
            } else if (command.toLowerCase().contains("пока")) {
                Log.d(TAG, "Обработана команда: пока");
            } else {
                Log.d(TAG, "Неизвестная команда: " + command);
            }
        }
    }