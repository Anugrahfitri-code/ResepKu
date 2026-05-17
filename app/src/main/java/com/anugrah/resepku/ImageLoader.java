package com.anugrah.resepku;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ImageLoader {
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(3);
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Map<String, Bitmap> CACHE = new ConcurrentHashMap<>();

    private ImageLoader() {
    }

    public static void load(String imageUrl, ImageView imageView, int placeholderRes) {
        imageView.setImageResource(placeholderRes);
        if (TextUtils.isEmpty(imageUrl)) {
            return;
        }

        imageView.setTag(imageUrl);
        Bitmap cachedBitmap = CACHE.get(imageUrl);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(imageUrl);
            if (bitmap == null) {
                return;
            }

            CACHE.put(imageUrl, bitmap);
            MAIN_HANDLER.post(() -> {
                Object tag = imageView.getTag();
                if (tag != null && imageUrl.equals(tag.toString())) {
                    imageView.setImageBitmap(bitmap);
                }
            });
        });
    }

    private static Bitmap downloadBitmap(String imageUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setDoInput(true);
            connection.connect();

            try (InputStream inputStream = connection.getInputStream()) {
                return BitmapFactory.decodeStream(inputStream);
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
