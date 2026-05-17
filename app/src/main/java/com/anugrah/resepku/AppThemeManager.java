package com.anugrah.resepku;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;

public final class AppThemeManager {
    private static final String PREF_NAME = "resepku_settings";
    private static final String KEY_THEME = "theme";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String THEME_LIGHT = "Light";
    private static final String THEME_ORANGE = "Orange";
    private static final String THEME_GREEN = "Green";

    private AppThemeManager() {
    }

    public static String getTheme(Context context) {
        return prefs(context).getString(KEY_THEME, THEME_LIGHT);
    }

    public static void saveTheme(Context context, String theme) {
        prefs(context).edit().putString(KEY_THEME, theme).apply();
    }

    public static int getAccentColor(Context context) {
        String theme = getTheme(context);
        if (THEME_GREEN.equals(theme)) {
            return Color.rgb(96, 153, 62);
        }
        if (THEME_ORANGE.equals(theme)) {
            return Color.rgb(255, 111, 0);
        }
        return ContextCompat.getColor(context, R.color.primary_orange);
    }

    public static int getAccentStrongColor(Context context) {
        String theme = getTheme(context);
        if (THEME_GREEN.equals(theme)) {
            return Color.rgb(76, 132, 48);
        }
        if (THEME_ORANGE.equals(theme)) {
            return Color.rgb(255, 90, 0);
        }
        return Color.rgb(255, 90, 0);
    }

    public static void applyToActivity(Activity activity) {
        int accent = getAccentColor(activity);
        boolean darkMode = prefs(activity).getBoolean(KEY_DARK_MODE, false);
        activity.getWindow().setStatusBarColor(darkMode
                ? ContextCompat.getColor(activity, R.color.background_cream)
                : accent);
        activity.getWindow().setNavigationBarColor(ContextCompat.getColor(activity, R.color.background_cream));

        BottomNavigationView bottomNavigation = activity.findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            int[][] states = new int[][]{
                    new int[]{android.R.attr.state_checked},
                    new int[]{}
            };
            int[] colors = new int[]{
                    accent,
                    ContextCompat.getColor(activity, R.color.text_grey)
            };
            ColorStateList navColors = new ColorStateList(states, colors);
            bottomNavigation.setItemIconTintList(navColors);
            bottomNavigation.setItemTextColor(navColors);
            bottomNavigation.setItemActiveIndicatorColor(ColorStateList.valueOf(withAlpha(accent, 34)));
        }
    }

    public static void applyToViewTree(View root) {
        if (root == null) {
            return;
        }

        int accent = getAccentColor(root.getContext());
        applyRecursive(root, accent);
    }

    public static void applyCategoryBackground(View categoryView, boolean selected) {
        if (categoryView == null) {
            return;
        }

        Context context = categoryView.getContext();
        int fill = ContextCompat.getColor(context, R.color.white);
        int stroke = selected ? getAccentStrongColor(context) : ContextCompat.getColor(context, R.color.search_bar_stroke);
        categoryView.setBackground(rounded(context, fill, 18, selected ? 2 : 1, stroke));
        categoryView.setAlpha(selected ? 1f : 0.92f);
    }

    public static void applyDot(View dot, boolean active) {
        if (dot == null) {
            return;
        }

        Context context = dot.getContext();
        int color = active ? getAccentColor(context) : ContextCompat.getColor(context, R.color.white);
        dot.setBackground(rounded(context, color, 12, 0, Color.TRANSPARENT));
    }

    public static void tintFavoriteIcon(ImageView icon, boolean favorite) {
        if (icon == null) {
            return;
        }
        icon.setColorFilter(getAccentStrongColor(icon.getContext()));
        icon.setAlpha(favorite ? 1f : 0.55f);
    }

    private static void applyRecursive(View view, int accent) {
        Context context = view.getContext();
        int viewId = view.getId();

        if (view instanceof SwitchMaterial) {
            SwitchMaterial switchView = (SwitchMaterial) view;
            switchView.setThumbTintList(switchThumbTint(context, accent));
            switchView.setTrackTintList(switchTrackTint(context, accent));
        }

        if (view instanceof MaterialButton) {
            applyButtonTheme((MaterialButton) view, accent);
        }

        if (view instanceof TextView) {
            applyTextTheme((TextView) view, accent);
        }

        if (view instanceof ImageView) {
            applyImageTheme((ImageView) view, accent);
        }

        if (viewId == R.id.recommendationBadge || viewId == R.id.btnViewRecipe) {
            view.setBackground(rounded(context, accent, 12, 0, Color.TRANSPARENT));
        }

        if (viewId == R.id.savedPill) {
            view.setBackground(rounded(context, withAlpha(accent, 28), 14, 0, Color.TRANSPARENT));
        }

        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                applyRecursive(group.getChildAt(i), accent);
            }
        }
    }

    private static void applyButtonTheme(MaterialButton button, int accent) {
        int id = button.getId();
        if (id == R.id.btnSaveSettings || id == R.id.btnSaveFavorite) {
            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setTextColor(Color.WHITE);
            button.setIconTint(ColorStateList.valueOf(Color.WHITE));
        } else if (id == R.id.btnResetSettings) {
            button.setTextColor(accent);
            button.setIconTint(ColorStateList.valueOf(accent));
            button.setStrokeColor(ColorStateList.valueOf(accent));
        }
    }

    private static void applyTextTheme(TextView textView, int accent) {
        CharSequence text = textView.getText();
        if (text == null) {
            return;
        }

        String value = text.toString();
        if ("apa hari ini?".equals(value)
                || "Favorit".equals(value)
                || "Lihat semua".equals(value)
                || "Disimpan".equals(value)
                || "Reset".equals(value)) {
            textView.setTextColor(accent);
        }
    }

    private static void applyImageTheme(ImageView imageView, int accent) {
        int id = imageView.getId();
        if (id == R.id.ivFavorite || id == R.id.ivRecommendationFavorite) {
            imageView.setColorFilter(getAccentStrongColor(imageView.getContext()));
            return;
        }

        if (imageView.getParent() instanceof View && ((View) imageView.getParent()).getId() == R.id.savedPill) {
            imageView.setColorFilter(accent);
            return;
        }

        if (isLevelIcon(imageView)) {
            imageView.setColorFilter(accent);
            return;
        }

        CharSequence description = imageView.getContentDescription();
        if (description != null && "Favorit".contentEquals(description)) {
            imageView.setColorFilter(accent);
        }
    }

    private static boolean isLevelIcon(ImageView imageView) {
        if (!(imageView.getParent() instanceof ViewGroup)) {
            return false;
        }

        ViewGroup parent = (ViewGroup) imageView.getParent();
        int index = parent.indexOfChild(imageView);
        for (int i = index + 1; i < parent.getChildCount(); i++) {
            View sibling = parent.getChildAt(i);
            if (sibling instanceof TextView) {
                CharSequence text = ((TextView) sibling).getText();
                return text != null && "Mudah".contentEquals(text);
            }
        }
        return false;
    }

    private static ColorStateList switchThumbTint(Context context, int accent) {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        ContextCompat.getColor(context, R.color.switch_thumb_off),
                        ContextCompat.getColor(context, R.color.switch_thumb_off)
                }
        );
    }

    private static ColorStateList switchTrackTint(Context context, int accent) {
        return new ColorStateList(
                new int[][]{
                        new int[]{android.R.attr.state_checked},
                        new int[]{}
                },
                new int[]{
                        accent,
                        ContextCompat.getColor(context, R.color.switch_track_off)
                }
        );
    }

    private static GradientDrawable rounded(Context context, int fill, int radiusDp, int strokeDp, int strokeColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(context, radiusDp));
        if (strokeDp > 0) {
            drawable.setStroke(dp(context, strokeDp), strokeColor);
        }
        return drawable;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
