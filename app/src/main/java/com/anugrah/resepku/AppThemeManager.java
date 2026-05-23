package com.anugrah.resepku;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.switchmaterial.SwitchMaterial;

public final class AppThemeManager {
    private static final String PREF_NAME = "resepku_settings";
    private static final String KEY_THEME = "theme";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_TEXT_SIZE = "text_size";
    private static final String THEME_ORANGE = "Orange";
    private static final String THEME_GREEN = "Green";
    private static final String TEXT_SMALL = "Kecil";
    private static final String TEXT_MEDIUM = "Sedang";
    private static final String TEXT_LARGE = "Besar";

    private AppThemeManager() {
    }

    public static String getTheme(Context context) {
        String theme = prefs(context).getString(KEY_THEME, THEME_ORANGE);
        return THEME_GREEN.equals(theme) ? THEME_GREEN : THEME_ORANGE;
    }

    public static void saveTheme(Context context, String theme) {
        prefs(context).edit().putString(KEY_THEME, theme).apply();
    }

    public static String getTextSize(Context context) {
        String textSize = prefs(context).getString(KEY_TEXT_SIZE, TEXT_MEDIUM);
        if (TEXT_SMALL.equals(textSize) || TEXT_LARGE.equals(textSize)) {
            return textSize;
        }
        return TEXT_MEDIUM;
    }

    public static void saveTextSize(Context context, String textSize) {
        prefs(context).edit().putString(KEY_TEXT_SIZE, textSize).apply();
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
        root.setBackgroundColor(getPageBackgroundColor(root.getContext()));
        applyRecursive(root, accent);
    }

    public static void applyCategoryBackground(View categoryView, boolean selected) {
        if (categoryView == null) {
            return;
        }

        Context context = categoryView.getContext();
        boolean darkMode = isDarkMode(context);
        int fill = selected || !darkMode
                ? ContextCompat.getColor(context, R.color.white)
                : Color.TRANSPARENT;
        int stroke = selected ? getAccentStrongColor(context) : ContextCompat.getColor(context, R.color.search_bar_stroke);
        categoryView.setBackground(rounded(context, fill, 18, selected ? 2 : 1, stroke));
        categoryView.setAlpha(selected ? 1f : 0.92f);
        applyCategoryTextColors(categoryView, selected, darkMode);
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

        if (view instanceof NestedScrollView) {
            view.setBackgroundColor(getPageBackgroundColor(context));
        }

        if (view instanceof SwitchMaterial) {
            SwitchMaterial switchView = (SwitchMaterial) view;
            switchView.setThumbTintList(switchThumbTint(context, accent));
            switchView.setTrackTintList(switchTrackTint(context, accent));
        }

        if (view instanceof MaterialButton) {
            applyButtonTheme((MaterialButton) view, accent);
        }

        if (view instanceof ProgressBar) {
            ((ProgressBar) view).setIndeterminateTintList(ColorStateList.valueOf(accent));
        }

        if (view instanceof MaterialCardView && viewId == R.id.recommendationCard) {
            ((MaterialCardView) view).setCardBackgroundColor(recommendationCardColor(context));
        }

        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            applyTextTheme(textView, accent);
            applyTextSize(textView);
        }

        if (view instanceof ImageView) {
            applyImageTheme((ImageView) view, accent);
        }

        if (viewId == R.id.recommendationBadge || viewId == R.id.btnViewRecipe) {
            view.setBackground(rounded(context, accent, 12, 0, Color.TRANSPARENT));
        }

        if (viewId == R.id.recommendationContent) {
            view.setBackground(recommendationBackground(context));
        }

        if (viewId == R.id.savedPill) {
            view.setBackground(rounded(context, withAlpha(accent, 28), 14, 0, Color.TRANSPARENT));
        }

        if (viewId == R.id.ratingPill) {
            view.setBackground(rounded(context, isDarkMode(context)
                    ? Color.rgb(33, 26, 22)
                    : ContextCompat.getColor(context, R.color.white), 20, 1,
                    ContextCompat.getColor(context, R.color.search_bar_stroke)));
        }

        if (viewId == R.id.favoriteSavedCount) {
            int fill = isDarkMode(context)
                    ? ContextCompat.getColor(context, R.color.white)
                    : (isGreenTheme(context) ? Color.rgb(249, 253, 245) : Color.WHITE);
            view.setBackground(rounded(context, fill,
                    16, 1, ContextCompat.getColor(context, R.color.search_bar_stroke)));
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
        if (id == R.id.btnSaveSettings
                || id == R.id.btnRegisterSubmit
                || id == R.id.btnSaveUserRecipe) {
            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setTextColor(Color.WHITE);
            button.setIconTint(null);
        } else if (id == R.id.btnLoginSubmit) {
            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setTextColor(Color.WHITE);
            button.setIconTint(ColorStateList.valueOf(Color.WHITE));
        } else if (id == R.id.btnRefreshApiRecipes) {
            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setTextColor(Color.WHITE);
            button.setIconTint(ColorStateList.valueOf(Color.WHITE));
        } else if (id == R.id.btnSaveFavorite) {
            button.setBackgroundTintList(ColorStateList.valueOf(accent));
            button.setTextColor(Color.WHITE);
            button.setIconTint(ColorStateList.valueOf(Color.WHITE));
        } else if (id == R.id.btnResetSettings
                || id == R.id.btnLoginCreateAccount
                || id == R.id.btnRegisterLogin
                || id == R.id.btnPickRecipeImage
                || id == R.id.btnClearUserRecipe) {
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
        if (textView.getId() == R.id.tvForgotPassword
                || textView.getId() == R.id.tvHomeWelcome
                || textView.getId() == R.id.tvMyRecipeTitleAccent
                || textView.getId() == R.id.tvUserRecipeCount) {
            textView.setTextColor(accent);
        } else if ("apa hari ini?".equals(value)
                || "Favorit".equals(value)
                || "Lihat semua".equals(value)
                || "Disimpan".equals(value)
                || "Reset".equals(value)) {
            textView.setTextColor(accent);
        } else if (value.matches("^[0-9],[0-9] \\([0-9.]+\\)$")) {
            textView.setTextColor(ContextCompat.getColor(textView.getContext(), R.color.text_dark));
        } else if ("resep tersimpan".equals(value)) {
            textView.setTextColor(ContextCompat.getColor(textView.getContext(), R.color.text_dark));
        }
    }

    private static void applyTextSize(TextView textView) {
        Object originalSize = textView.getTag(R.id.tag_original_text_size_px);
        if (!(originalSize instanceof Float)) {
            originalSize = textView.getTextSize();
            textView.setTag(R.id.tag_original_text_size_px, originalSize);
        }

        float scale = getTextScale(textView.getContext());
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, ((Float) originalSize) * scale);
    }

    private static float getTextScale(Context context) {
        String textSize = getTextSize(context);
        if (TEXT_SMALL.equals(textSize)) {
            return 0.9f;
        }
        if (TEXT_LARGE.equals(textSize)) {
            return 1.12f;
        }
        return 1f;
    }

    private static void applyImageTheme(ImageView imageView, int accent) {
        int id = imageView.getId();
        if (id == R.id.ivFavorite || id == R.id.ivRecommendationFavorite
                || id == R.id.iconClearCache || id == R.id.iconManageFavorite
                || id == R.id.iconAccountProfile || id == R.id.iconLogout) {
            imageView.setColorFilter(getAccentStrongColor(imageView.getContext()));
            return;
        }

        if (id == R.id.iconBack || id == R.id.iconShare) {
            imageView.setColorFilter(ContextCompat.getColor(imageView.getContext(), R.color.text_dark));
            return;
        }

        if (id == R.id.iconDetailFavorite) {
            imageView.setColorFilter(accent);
            return;
        }

        if (imageView.getParent() instanceof View && ((View) imageView.getParent()).getId() == R.id.favoriteSavedCount) {
            imageView.setColorFilter(accent);
            return;
        }

        if (isSettingsHeaderIcon(imageView)) {
            imageView.setColorFilter(accent);
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
                return text != null && ("Mudah".contentEquals(text)
                        || "Sedang".contentEquals(text)
                        || "Sulit".contentEquals(text));
            }
        }
        return false;
    }

    private static int getPageBackgroundColor(Context context) {
        if (isDarkMode(context)) {
            return ContextCompat.getColor(context, R.color.background_cream);
        }

        return isGreenTheme(context)
                ? Color.rgb(249, 252, 244)
                : ContextCompat.getColor(context, R.color.background_cream);
    }

    private static boolean isGreenTheme(Context context) {
        return THEME_GREEN.equals(getTheme(context));
    }

    private static boolean isDarkMode(Context context) {
        return prefs(context).getBoolean(KEY_DARK_MODE, false);
    }

    private static int recommendationCardColor(Context context) {
        if (isDarkMode(context)) {
            return isGreenTheme(context) ? Color.rgb(24, 38, 22) : Color.rgb(42, 29, 19);
        }
        return isGreenTheme(context) ? Color.rgb(242, 250, 232) : Color.rgb(255, 248, 233);
    }

    private static GradientDrawable recommendationBackground(Context context) {
        if (isGreenTheme(context)) {
            return gradient(
                    context,
                    isDarkMode(context) ? Color.rgb(29, 45, 26) : Color.rgb(242, 250, 232),
                    isDarkMode(context) ? Color.rgb(40, 62, 33) : Color.rgb(220, 242, 203),
                    20
            );
        }

        return gradient(
                context,
                isDarkMode(context) ? Color.rgb(50, 32, 20) : Color.rgb(255, 243, 224),
                isDarkMode(context) ? Color.rgb(70, 45, 26) : Color.rgb(255, 224, 178),
                20
        );
    }

    private static void applyCategoryTextColors(View categoryView, boolean selected, boolean darkMode) {
        if (!(categoryView instanceof ViewGroup)) {
            return;
        }

        int textColor = darkMode
                ? ContextCompat.getColor(categoryView.getContext(), R.color.text_dark)
                : ContextCompat.getColor(categoryView.getContext(), R.color.text_on_light_surface);
        applyTextColorRecursive((ViewGroup) categoryView, textColor);
    }

    private static void applyTextColorRecursive(ViewGroup group, int color) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView && !TextUtils.isEmpty(((TextView) child).getText())) {
                ((TextView) child).setTextColor(color);
            } else if (child instanceof ViewGroup) {
                applyTextColorRecursive((ViewGroup) child, color);
            }
        }
    }

    private static boolean isSettingsHeaderIcon(ImageView imageView) {
        if (!(imageView.getParent() instanceof ViewGroup)) {
            return false;
        }

        ViewGroup parent = (ViewGroup) imageView.getParent();
        int index = parent.indexOfChild(imageView);
        if (index != 0 || parent.getChildCount() < 2 || !(parent.getChildAt(1) instanceof TextView)) {
            return false;
        }

        CharSequence text = ((TextView) parent.getChildAt(1)).getText();
        return text != null && ("Akun Saya".contentEquals(text)
                || "Tampilan".contentEquals(text)
                || "Notifikasi".contentEquals(text)
                || "Data & Penyimpanan".contentEquals(text)
                || "Tentang Aplikasi".contentEquals(text));
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

    private static GradientDrawable gradient(Context context, int startColor, int endColor, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{startColor, endColor}
        );
        drawable.setCornerRadius(dp(context, radiusDp));
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
