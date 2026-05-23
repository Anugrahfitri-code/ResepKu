package com.anugrah.resepku;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

        Context context = imageView.getContext().getApplicationContext();
        if (isLocalImage(imageUrl)) {
            Bitmap bitmap = loadLocalBitmap(context, imageUrl);
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
            return;
        }

        imageView.setTag(imageUrl);
        Bitmap cachedBitmap = CACHE.get(imageUrl);
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap);
            return;
        }

        Bitmap diskBitmap = loadFromDisk(context, imageUrl);
        if (diskBitmap != null) {
            CACHE.put(imageUrl, diskBitmap);
            imageView.setImageBitmap(diskBitmap);
            return;
        }

        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(context, imageUrl);
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

    private static boolean isLocalImage(String imageUrl) {
        return imageUrl.startsWith("/")
                || imageUrl.startsWith("file://")
                || imageUrl.startsWith("content://");
    }

    private static Bitmap loadLocalBitmap(Context context, String imageUrl) {
        try {
            if (imageUrl.startsWith("content://")) {
                try (InputStream inputStream = context.getContentResolver().openInputStream(Uri.parse(imageUrl))) {
                    return BitmapFactory.decodeStream(inputStream);
                }
            }

            String path = imageUrl.startsWith("file://")
                    ? Uri.parse(imageUrl).getPath()
                    : imageUrl;
            return TextUtils.isEmpty(path) ? null : BitmapFactory.decodeFile(path);
        } catch (Exception ignored) {
            return null;
        }
    }

    public static void prefetch(Context context, String imageUrl) {
        if (context == null || TextUtils.isEmpty(imageUrl) || CACHE.containsKey(imageUrl)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        if (imageFile(appContext, imageUrl).exists()) {
            return;
        }

        EXECUTOR.execute(() -> {
            Bitmap bitmap = downloadBitmap(appContext, imageUrl);
            if (bitmap != null) {
                CACHE.put(imageUrl, bitmap);
            }
        });
    }

    public static void prefetchNow(Context context, String imageUrl) {
        if (context == null || TextUtils.isEmpty(imageUrl)) {
            return;
        }

        Context appContext = context.getApplicationContext();
        if (imageFile(appContext, imageUrl).exists()) {
            return;
        }

        Bitmap bitmap = downloadBitmap(appContext, imageUrl);
        if (bitmap != null) {
            CACHE.put(imageUrl, bitmap);
        }
    }

    private static Bitmap downloadBitmap(Context context, String imageUrl) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(imageUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setDoInput(true);
            connection.connect();

            try (InputStream inputStream = connection.getInputStream()) {
                byte[] bytes = readAllBytes(inputStream);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap != null) {
                    saveToDisk(context, imageUrl, bytes);
                }
                return bitmap;
            }
        } catch (Exception ignored) {
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static Bitmap loadFromDisk(Context context, String imageUrl) {
        File file = imageFile(context, imageUrl);
        if (!file.exists()) {
            return null;
        }
        return BitmapFactory.decodeFile(file.getAbsolutePath());
    }

    private static void saveToDisk(Context context, String imageUrl, byte[] bytes) {
        File file = imageFile(context, imageUrl);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return;
        }

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
        } catch (Exception ignored) {
        }
    }

    private static byte[] readAllBytes(InputStream inputStream) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, read);
        }
        return outputStream.toByteArray();
    }

    private static File imageFile(Context context, String imageUrl) {
        return new File(new File(context.getFilesDir(), "api_image_cache"), hash(imageUrl) + ".img");
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : bytes) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (Exception ignored) {
            return String.valueOf(value.hashCode());
        }
    }
}
