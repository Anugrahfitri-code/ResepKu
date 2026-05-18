package com.anugrah.resepku;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

public class CookingReminderReceiver extends BroadcastReceiver {
    public static final String EXTRA_RECIPE_NAME = "extra_recipe_name";

    private static final String CHANNEL_ID = "cooking_reminder_channel";
    private static final int NOTIFICATION_ID = 2241;

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences(
                CookingReminderScheduler.PREF_NAME,
                Context.MODE_PRIVATE
        );
        if (!preferences.getBoolean(CookingReminderScheduler.KEY_COOKING_REMINDER, false)) {
            return;
        }

        String recipeName = intent.getStringExtra(EXTRA_RECIPE_NAME);
        if (recipeName == null || recipeName.trim().isEmpty()) {
            recipeName = preferences.getString(CookingReminderScheduler.KEY_REMINDER_RECIPE, "resep pilihanmu");
        }

        showNotification(context, recipeName);
        CookingReminderScheduler.schedule(
                context,
                preferences.getInt(CookingReminderScheduler.KEY_REMINDER_HOUR, 8),
                preferences.getInt(CookingReminderScheduler.KEY_REMINDER_MINUTE, 0),
                recipeName
        );
    }

    private void showNotification(Context context, String recipeName) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        createChannel(context);

        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_bell)
                .setContentTitle("Waktunya memasak")
                .setContentText("Saatnya memasak " + recipeName + ". Yuk mulai sekarang!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Saatnya memasak " + recipeName + ". Buka ResepKu dan ikuti langkah memasaknya."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(contentIntent);

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build());
    }

    private void createChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Pengingat Memasak",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notifikasi pengingat memasak resep pilihan.");

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }
}
