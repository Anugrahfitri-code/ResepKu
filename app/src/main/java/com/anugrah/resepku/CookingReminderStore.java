package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public final class CookingReminderStore {
    private static final String KEY_REMINDERS = "cooking_reminders";
    private static final String KEY_NEXT_ID = "cooking_reminder_next_id";

    private CookingReminderStore() {
    }

    public static List<CookingReminder> getReminders(Context context) {
        migrateLegacyReminder(context);
        List<CookingReminder> reminders = new ArrayList<>();
        String rawJson = prefs(context).getString(KEY_REMINDERS, "[]");
        try {
            JSONArray array = new JSONArray(rawJson);
            for (int i = 0; i < array.length(); i++) {
                JSONObject json = array.optJSONObject(i);
                if (json != null) {
                    reminders.add(fromJson(json));
                }
            }
        } catch (JSONException ignored) {
        }
        return reminders;
    }

    public static CookingReminder getReminder(Context context, int id) {
        for (CookingReminder reminder : getReminders(context)) {
            if (reminder.id == id) {
                return reminder;
            }
        }
        return null;
    }

    public static CookingReminder addReminder(Context context, int hour, int minute, String recipeName) {
        SharedPreferences preferences = prefs(context);
        int id = preferences.getInt(KEY_NEXT_ID, 1);
        CookingReminder reminder = new CookingReminder(id, hour, minute, recipeName, true);
        List<CookingReminder> reminders = getReminders(context);
        reminders.add(reminder);
        preferences.edit()
                .putInt(KEY_NEXT_ID, id + 1)
                .putString(KEY_REMINDERS, toJson(reminders).toString())
                .apply();
        return reminder;
    }

    public static void updateReminder(Context context, CookingReminder updatedReminder) {
        List<CookingReminder> reminders = getReminders(context);
        for (int i = 0; i < reminders.size(); i++) {
            if (reminders.get(i).id == updatedReminder.id) {
                reminders.set(i, updatedReminder);
                prefs(context).edit().putString(KEY_REMINDERS, toJson(reminders).toString()).apply();
                return;
            }
        }
    }

    public static void removeReminder(Context context, int id) {
        List<CookingReminder> reminders = getReminders(context);
        List<CookingReminder> remaining = new ArrayList<>();
        for (CookingReminder reminder : reminders) {
            if (reminder.id != id) {
                remaining.add(reminder);
            }
        }
        prefs(context).edit().putString(KEY_REMINDERS, toJson(remaining).toString()).apply();
    }

    public static void clearReminders(Context context) {
        prefs(context).edit()
                .remove(KEY_REMINDERS)
                .remove(KEY_NEXT_ID)
                .remove(CookingReminderScheduler.KEY_REMINDER_RECIPE)
                .remove(CookingReminderScheduler.KEY_REMINDER_HOUR)
                .remove(CookingReminderScheduler.KEY_REMINDER_MINUTE)
                .apply();
    }

    public static String summary(List<CookingReminder> reminders) {
        if (reminders == null || reminders.isEmpty()) {
            return "Atur jam dan resep pengingat";
        }
        if (reminders.size() == 1) {
            CookingReminder reminder = reminders.get(0);
            return CookingReminderScheduler.formatTime(reminder.hour, reminder.minute)
                    + " - " + reminder.recipeName;
        }
        return reminders.size() + " pengingat memasak aktif";
    }

    private static void migrateLegacyReminder(Context context) {
        SharedPreferences preferences = prefs(context);
        if (preferences.contains(KEY_REMINDERS)) {
            return;
        }

        boolean enabled = preferences.getBoolean(CookingReminderScheduler.KEY_COOKING_REMINDER, false);
        String recipe = preferences.getString(CookingReminderScheduler.KEY_REMINDER_RECIPE, "");
        if (!enabled || recipe == null || recipe.trim().isEmpty()) {
            return;
        }

        CookingReminder reminder = new CookingReminder(
                1,
                preferences.getInt(CookingReminderScheduler.KEY_REMINDER_HOUR, 8),
                preferences.getInt(CookingReminderScheduler.KEY_REMINDER_MINUTE, 0),
                recipe,
                true
        );
        List<CookingReminder> reminders = new ArrayList<>();
        reminders.add(reminder);
        preferences.edit()
                .putString(KEY_REMINDERS, toJson(reminders).toString())
                .putInt(KEY_NEXT_ID, 2)
                .apply();
    }

    private static JSONArray toJson(List<CookingReminder> reminders) {
        JSONArray array = new JSONArray();
        for (CookingReminder reminder : reminders) {
            try {
                JSONObject json = new JSONObject();
                json.put("id", reminder.id);
                json.put("hour", reminder.hour);
                json.put("minute", reminder.minute);
                json.put("recipeName", reminder.recipeName);
                json.put("enabled", reminder.enabled);
                array.put(json);
            } catch (JSONException ignored) {
            }
        }
        return array;
    }

    private static CookingReminder fromJson(JSONObject json) {
        return new CookingReminder(
                json.optInt("id", 0),
                json.optInt("hour", 8),
                json.optInt("minute", 0),
                json.optString("recipeName", "resep pilihanmu"),
                json.optBoolean("enabled", true)
        );
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(CookingReminderScheduler.PREF_NAME, Context.MODE_PRIVATE);
    }

    public static final class CookingReminder {
        final int id;
        final int hour;
        final int minute;
        final String recipeName;
        final boolean enabled;

        CookingReminder(int id, int hour, int minute, String recipeName, boolean enabled) {
            this.id = id;
            this.hour = hour;
            this.minute = minute;
            this.recipeName = recipeName == null ? "resep pilihanmu" : recipeName;
            this.enabled = enabled;
        }
    }
}
