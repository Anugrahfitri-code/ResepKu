package com.anugrah.resepku;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class CookingReminderBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        SharedPreferences preferences = context.getSharedPreferences(
                CookingReminderScheduler.PREF_NAME,
                Context.MODE_PRIVATE
        );
        if (!preferences.getBoolean(CookingReminderScheduler.KEY_COOKING_REMINDER, false)) {
            return;
        }

        CookingReminderScheduler.scheduleAll(context);
    }
}
