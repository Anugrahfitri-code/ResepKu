package com.anugrah.resepku;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.Calendar;

public final class CookingReminderScheduler {
    public static final String PREF_NAME = "resepku_settings";
    public static final String KEY_COOKING_REMINDER = "cooking_reminder";
    public static final String KEY_REMINDER_HOUR = "cooking_reminder_hour";
    public static final String KEY_REMINDER_MINUTE = "cooking_reminder_minute";
    public static final String KEY_REMINDER_RECIPE = "cooking_reminder_recipe";

    private static final int REQUEST_CODE = 4107;

    private CookingReminderScheduler() {
    }

    public static boolean schedule(Context context, int hour, int minute, String recipeName) {
        return schedule(context, REQUEST_CODE, hour, minute, recipeName);
    }

    public static boolean schedule(Context context, CookingReminderStore.CookingReminder reminder) {
        return schedule(context, reminder.id, reminder.hour, reminder.minute, reminder.recipeName);
    }

    public static boolean schedule(Context context, int reminderId, int hour, int minute, String recipeName) {
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, CookingReminderReceiver.class);
        intent.putExtra(CookingReminderReceiver.EXTRA_RECIPE_NAME, recipeName);
        intent.putExtra(CookingReminderReceiver.EXTRA_REMINDER_ID, reminderId);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                requestCode(reminderId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            return false;
        }

        long triggerAtMillis = nextTriggerAtMillis(hour, minute);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
            return true;
        } catch (Exception ignored) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                return true;
            } catch (Exception secondIgnored) {
                return false;
            }
        }
    }

    public static boolean scheduleAll(Context context) {
        boolean allScheduled = true;
        for (CookingReminderStore.CookingReminder reminder : CookingReminderStore.getReminders(context)) {
            if (reminder.enabled) {
                allScheduled = schedule(context, reminder) && allScheduled;
            }
        }
        return allScheduled;
    }

    public static void cancel(Context context) {
        cancel(context, REQUEST_CODE);
    }

    public static void cancel(Context context, int reminderId) {
        Context appContext = context.getApplicationContext();
        Intent intent = new Intent(appContext, CookingReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                appContext,
                requestCode(reminderId),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) appContext.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        pendingIntent.cancel();
    }

    public static void cancelAll(Context context) {
        cancel(context);
        for (CookingReminderStore.CookingReminder reminder : CookingReminderStore.getReminders(context)) {
            cancel(context, reminder.id);
        }
    }

    public static String formatTime(int hour, int minute) {
        return String.format("%02d.%02d", hour, minute);
    }

    private static long nextTriggerAtMillis(int hour, int minute) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        return calendar.getTimeInMillis();
    }

    private static int requestCode(int reminderId) {
        return REQUEST_CODE + Math.abs(reminderId % 100000);
    }
}
