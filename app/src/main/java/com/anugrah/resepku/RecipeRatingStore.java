package com.anugrah.resepku;

import android.content.Context;
import android.content.SharedPreferences;

public final class RecipeRatingStore {
    private static final String PREF_NAME = "resepku_recipe_ratings";

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
        if (userRating > 0) {
            return userRating + ",0 (Kamu)";
        }
        if (recipe == null || recipe.rating == null || recipe.rating.trim().isEmpty()) {
            return "4,8 (128)";
        }
        return recipe.rating;
    }

    private static String key(String recipeTitle) {
        return recipeTitle.trim().toLowerCase().replaceAll("\\s+", "_");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
}
