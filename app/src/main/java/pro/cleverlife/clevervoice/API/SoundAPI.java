package pro.cleverlife.clevervoice.API;

import android.content.Context;
import android.util.Log;

import pro.cleverlife.clevervoice.CleverServices.SoundService;

public class SoundAPI {
    public enum SoundCommand {
        SET_MEDIA, SET_RING, SET_ALARM, SET_NOTIFICATION, //не использовать напрямую!
        INCREASE_MEDIA, INCREASE_RING, INCREASE_ALARM, INCREASE_NOTIFICATION,
        DECREASE_MEDIA, DECREASE_RING, DECREASE_ALARM, DECREASE_NOTIFICATION,
        MAX_MEDIA, MAX_RING, MAX_ALARM, MAX_NOTIFICATION,
        MIN_MEDIA, MIN_RING, MIN_ALARM, MIN_NOTIFICATION,
        GET_MEDIA_INFO, GET_RING_INFO, GET_ALARM_INFO, GET_NOTIFICATION_INFO,
        MUTE_MEDIA, MUTE_RING, MUTE_ALARM, MUTE_NOTIFICATION,
        UNMUTE_MEDIA, UNMUTE_RING, UNMUTE_ALARM, UNMUTE_NOTIFICATION
    }

    public static String executeCommand(Context context, SoundCommand command, String... params) {
        SoundService service = new SoundService(context);

        try {
            switch (command) {
                // SET commands
                case SET_MEDIA:
                    if (params.length > 0) {
                        int mediaValue = Integer.parseInt(params[0]);
                        service.setMediaVolume(context, mediaValue);
                        return "Громкость медиа установлена на " + mediaValue;
                    }
                    break;

                case SET_RING:
                    if (params.length > 0) {
                        int ringValue = Integer.parseInt(params[0]);
                        service.setRingVolume(context, ringValue);
                        return "Громкость звонка установлена на " + ringValue;
                    }
                    break;

                case SET_ALARM:
                    if (params.length > 0) {
                        int alarmValue = Integer.parseInt(params[0]);
                        service.setAlarmVolume(context, alarmValue);
                        return "Громкость будильника установлена на " + alarmValue;
                    }
                    break;

                case SET_NOTIFICATION:
                    if (params.length > 0) {
                        int notificationValue = Integer.parseInt(params[0]);
                        service.setNotificationVolume(context, notificationValue);
                        return "Громкость уведомлений установлена на " + notificationValue;
                    }
                    break;

                // INCREASE commands
                case INCREASE_MEDIA:
                    int mediaIncrease = params.length > 0 ? Integer.parseInt(params[0]) : 1;
                    int currentMedia = service.getCurrentMediaVolume(context);
                    int newMedia = Math.min(currentMedia + mediaIncrease, service.getMaxMediaVolume(context));
                    service.setMediaVolume(context, newMedia);
                    return "Громкость медиа увеличена";

                case INCREASE_RING:
                    int ringIncrease = params.length > 0 ? Integer.parseInt(params[0]) : 1;
                    int currentRing = service.getCurrentRingVolume(context);
                    int newRing = Math.min(currentRing + ringIncrease, service.getMaxRingVolume(context));
                    service.setRingVolume(context, newRing);
                    return "Громкость звонка увеличена";

                case INCREASE_ALARM:
                    int alarmIncrease = params.length > 0 ? Integer.parseInt(params[0]) : 1;
                    int currentAlarm = service.getCurrentAlarmVolume(context);
                    int newAlarm = Math.min(currentAlarm + alarmIncrease, service.getMaxAlarmVolume(context));
                    service.setAlarmVolume(context, newAlarm);
                    return "Громкость будильника увеличена";

                case INCREASE_NOTIFICATION:
                    int notificationIncrease = params.length > 0 ? Integer.parseInt(params[0]) : 1;
                    int currentNotification = service.getCurrentNotificationVolume(context);
                    int newNotification = Math.min(currentNotification + notificationIncrease, service.getMaxNotificationVolume(context));
                    service.setNotificationVolume(context, newNotification);
                    return "Громкость уведомлений увеличена";

                // DECREASE commands
                case DECREASE_MEDIA:
                    int mediaDecrease = params.length > 0 ? Integer.parseInt(params[0]) : 1;
                    int currentMediaDec = service.getCurrentMediaVolume(context);
                    int newMediaDec = Math.max(currentMediaDec - mediaDecrease, 0);
                    service.setMediaVolume(context, newMediaDec);
                    return "Громкость медиа уменьшена";

                case DECREASE_RING:
                    int ringDecrease = params.length > 0 ? Integer.parseInt(params[0]) : 1;
                    int currentRingDec = service.getCurrentRingVolume(context);
                    int newRingDec = Math.max(currentRingDec - ringDecrease, 0);
                    service.setRingVolume(context, newRingDec);
                    return "Громкость звонка уменьшена";

                case DECREASE_ALARM:
                    int alarmDecrease = params.length > 0 ? Integer.parseInt(params[0]) : 1;
                    int currentAlarmDec = service.getCurrentAlarmVolume(context);
                    int newAlarmDec = Math.max(currentAlarmDec - alarmDecrease, 0);
                    service.setAlarmVolume(context, newAlarmDec);
                    return "Громкость будильника уменьшена";

                case DECREASE_NOTIFICATION:
                    int notificationDecrease = params.length > 0 ? Integer.parseInt(params[0]) : 1;
                    int currentNotificationDec = service.getCurrentNotificationVolume(context);
                    int newNotificationDec = Math.max(currentNotificationDec - notificationDecrease, 0);
                    service.setNotificationVolume(context, newNotificationDec);
                    return "Громкость уведомлений уменьшена";

                // MAX commands
                case MAX_MEDIA:
                    service.setMediaVolume(context, service.getMaxMediaVolume(context));
                    return "Громкость медиа установлена на максимум";

                case MAX_RING:
                    service.setRingVolume(context, service.getMaxRingVolume(context));
                    return "Громкость звонка установлена на максимум";

                case MAX_ALARM:
                    service.setAlarmVolume(context, service.getMaxAlarmVolume(context));
                    return "Громкость будильника установлена на максимум";

                case MAX_NOTIFICATION:
                    service.setNotificationVolume(context, service.getMaxNotificationVolume(context));
                    return "Громкость уведомлений установлена на максимум";

                // MIN commands (mute)
                case MIN_MEDIA:
                case MUTE_MEDIA:
                    service.setMediaVolume(context, 0);
                    return "Медиа звук выключен";

                case MIN_RING:
                case MUTE_RING:
                    service.setRingVolume(context, 0);
                    return "Звонок выключен";

                case MIN_ALARM:
                case MUTE_ALARM:
                    service.setAlarmVolume(context, 0);
                    return "Будильник выключен";

                case MIN_NOTIFICATION:
                case MUTE_NOTIFICATION:
                    service.setNotificationVolume(context, 0);
                    return "Уведомления выключены";

                // UNMUTE commands (set to medium level)
                case UNMUTE_MEDIA:
                    int mediumMedia = service.getMaxMediaVolume(context) / 2;
                    service.setMediaVolume(context, mediumMedia);
                    return "Медиа звук включен";

                case UNMUTE_RING:
                    int mediumRing = service.getMaxRingVolume(context) / 2;
                    service.setRingVolume(context, mediumRing);
                    return "Звонок включен";

                case UNMUTE_ALARM:
                    int mediumAlarm = service.getMaxAlarmVolume(context) / 2;
                    service.setAlarmVolume(context, mediumAlarm);
                    return "Будильник включен";

                case UNMUTE_NOTIFICATION:
                    int mediumNotification = service.getMaxNotificationVolume(context) / 2;
                    service.setNotificationVolume(context, mediumNotification);
                    return "Уведомления включены";

                // GET_INFO commands
                case GET_MEDIA_INFO:
                    int mediaCurrent = service.getCurrentMediaVolume(context);
                    int mediaMax = service.getMaxMediaVolume(context);
                    int mediaPercent = (int) (mediaCurrent * 100.0 / mediaMax);
                    return String.format("Громкость медиа: %d%% (%d/%d)", mediaPercent, mediaCurrent, mediaMax);

                case GET_RING_INFO:
                    int ringCurrent = service.getCurrentRingVolume(context);
                    int ringMax = service.getMaxRingVolume(context);
                    int ringPercent = (int) (ringCurrent * 100.0 / ringMax);
                    return String.format("Громкость звонка: %d%% (%d/%d)", ringPercent, ringCurrent, ringMax);

                case GET_ALARM_INFO:
                    int alarmCurrent = service.getCurrentAlarmVolume(context);
                    int alarmMax = service.getMaxAlarmVolume(context);
                    int alarmPercent = (int) (alarmCurrent * 100.0 / alarmMax);
                    return String.format("Громкость будильника: %d%% (%d/%d)", alarmPercent, alarmCurrent, alarmMax);

                case GET_NOTIFICATION_INFO:
                    int notificationCurrent = service.getCurrentNotificationVolume(context);
                    int notificationMax = service.getMaxNotificationVolume(context);
                    int notificationPercent = (int) (notificationCurrent * 100.0 / notificationMax);
                    return String.format("Громкость уведомлений: %d%% (%d/%d)", notificationPercent, notificationCurrent, notificationMax);
            }
        } catch (Exception e) {
            Log.e("SoundAPI", "Error executing sound command", e);
            return "Ошибка при выполнении команды звука";
        }

        return "Команда звука выполнена";
    }
}
