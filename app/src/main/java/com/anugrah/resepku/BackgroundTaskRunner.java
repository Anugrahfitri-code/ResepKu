package com.anugrah.resepku;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class BackgroundTaskRunner {
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private BackgroundTaskRunner() {
    }

    public static void runInBackground(Runnable task) {
        EXECUTOR.execute(task);
    }

    public static void runOnMain(Runnable task) {
        MAIN_HANDLER.post(task);
    }
}
