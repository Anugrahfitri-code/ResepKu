package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;

public final class RecipeRatingStore {
    private static final String PREF_NAME = "resepku_recipe_ratings";
    private static final String DEFAULT_RATING = "4,8 (128)";

    private RecipeRatingStore() {
    }

    public static int getRating(Context context, String recipeTitle) {
        if (recipeTitle == null || recipeTitle.trim().isEmpty()) {
            return 0;
        }
        return prefs(context).getInt(key(recipeTitle), 0);
    }

    public static void saveRating(Context context, String recipeTitle, int rating) {
        if (recipeTitle == null || recipeTitle.trim().isEmpty()) {
            return;
        }

        int value = Math.max(1, Math.min(5, rating));
        prefs(context).edit().putInt(key(recipeTitle), value).apply();
    }

    public static String displayRating(Recipe recipe, int userRating) {
        if (recipe == null || recipe.rating == null || recipe.rating.trim().isEmpty()) {
            return userRating > 0 ? formatRating(userRating, 1) : DEFAULT_RATING;
        }

        RatingSummary summary = parseRating(recipe.rating);
        if (summary.count <= 0) {
            return userRating > 0 ? formatRating(userRating, 1) : recipe.rating;
        }

        if (userRating <= 0) {
            return formatRating(summary.average, summary.count);
        }

        int newCount = summary.count + 1;
        float newAverage = ((summary.average * summary.count) + userRating) / newCount;
        return formatRating(newAverage, newCount);
    }

    private static RatingSummary parseRating(String rating) {
        if (rating == null) {
            return new RatingSummary(0f, 0);
        }

        String value = rating.trim();
        int openIndex = value.indexOf('(');
        int closeIndex = value.indexOf(')', openIndex + 1);
        if (openIndex <= 0 || closeIndex <= openIndex) {
            return new RatingSummary(0f, 0);
        }

        try {
            String averageText = value.substring(0, openIndex).trim().replace(',', '.');
            String countText = value.substring(openIndex + 1, closeIndex)
                    .replace(".", "")
                    .replace(",", "")
                    .trim();
            return new RatingSummary(Float.parseFloat(averageText), Integer.parseInt(countText));
        } catch (NumberFormatException ignored) {
            return new RatingSummary(0f, 0);
        }
    }

    private static String formatRating(float average, int count) {
        int roundedTenths = Math.round(average * 10f);
        int safeCount = Math.max(1, count);
        return (roundedTenths / 10) + "," + (roundedTenths % 10) + " (" + formatCount(safeCount) + ")";
    }

    private static String formatCount(int count) {
        String raw = String.valueOf(count);
        StringBuilder builder = new StringBuilder();
        int firstGroup = raw.length() % 3;
        if (firstGroup == 0) {
            firstGroup = 3;
        }

        builder.append(raw, 0, firstGroup);
        for (int i = firstGroup; i < raw.length(); i += 3) {
            builder.append('.').append(raw, i, i + 3);
        }
        return builder.toString();
    }

    private static String key(String recipeTitle) {
        return recipeTitle.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static class RatingSummary {
        final float average;
        final int count;

        RatingSummary(float average, int count) {
            this.average = average;
            this.count = count;
        }
    }
}
